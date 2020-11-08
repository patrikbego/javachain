package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

/**
 * The {@code TransactionService} class is used for block related functionalities.
 * <p>
 * It has supporting methods for mining fees, validation and sending tokens.
 */
@Service
public class TransactionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionService.class);

    final EncryptionUtility encryptionUtility;

    final HashingUtility hashingUtility;

    final MiningService miningService;

    @Autowired
    public TransactionService(EncryptionUtility encryptionUtility, HashingUtility hashingUtility, MiningService miningService) {
        this.encryptionUtility = encryptionUtility;
        this.hashingUtility = hashingUtility;
        this.miningService = miningService;
    }

    public Transaction send(Wallet senderWallet, boolean isInitial, Wallet... receiverWallets) {

        Transaction transaction = new Transaction();
        transaction.setIncludeSignature(false);
        transaction.setFee(BigDecimal.ZERO);// TODO for now this is disabled
        transaction.setWallet(senderWallet);
        transaction.setInitial(isInitial);

        List<OutgoingTransaction> outTransactions = new ArrayList<>();
        for (Wallet receiverWallet : receiverWallets) {
            OutgoingTransaction outTransaction = new OutgoingTransaction(receiverWallet.address(), receiverWallet.getAmountToBeSent());
            outTransactions.add(outTransaction);
        }
        transaction.setOutgoingTransactions(outTransactions);

        // A coinbase creates new coins out of nothing - it must not claim to spend
        // anything, otherwise phantom inputs would pollute double-spend tracking.
        List<IncomingTransaction> inTransactions = isInitial
                ? new ArrayList<>()
                : getPreviousInTransactions(senderWallet);
        transaction.setIncomingTransactions(inTransactions);
        transaction.setSignature(encryptionUtility.sign(transaction.getCanonicalPayload(), senderWallet.getPrivateKey()));

        return transaction;
    }

    public List<IncomingTransaction> getPreviousInTransactions(Wallet senderWallet) {

        List<IncomingTransaction> inTransactions = new ArrayList<>();
        Block block = senderWallet.getBlockchain();

        //get last outTr for wallet
        while (block != null) {
            List<Transaction> transactions = block.getTransactionList();
            for (Transaction tr : transactions) {
                List<OutgoingTransaction> outs = tr.getOutgoingTransactions();
                if (outs == null) continue;
                for (int index = 0; index < outs.size(); index++) {
                    OutgoingTransaction otr = outs.get(index);
                    if (otr.getRecipientAddress() != null
                            && otr.getRecipientAddress().equals(senderWallet.getPublicKey())) {
                        // reference the exact output - hardcoding index 0 used to make
                        // wallets spend other people's outputs whenever theirs was not first
                        inTransactions.add(new IncomingTransaction(tr, index));
                    }
                }
                //we just need transaction in last blocks
                if (!inTransactions.isEmpty())
                    return inTransactions;
            }
            block = block.getPreviousBlock();
        }
        return inTransactions;
    }

    /**
     * Validates any transaction by dispatching on its type: coinbase transactions
     * (initial) follow minting rules, every other transaction must prove ownership of
     * everything it spends.
     */
    public boolean validateTransaction(Transaction transaction) throws SignatureException {
        if (transaction == null)
            return false;
        return transaction.isInitial()
                ? validateCoinbaseTransaction(transaction)
                : validateSpendTransaction(transaction);
    }

    /**
     * Coinbase ("initial") transactions are the ONLY way new coins may be created.
     * Rules: no inputs, exactly one output paying exactly the {@link Consensus#BLOCK_INCENTIVE}
     * to the miner's own address, signed with the miner's key. This replaces the old
     * blanket "initial -> valid" bypass that allowed unlimited minting.
     */
    public boolean validateCoinbaseTransaction(Transaction transaction) throws SignatureException {

        List<IncomingTransaction> ins = transaction.getIncomingTransactions();
        if (ins != null && !ins.isEmpty()) {
            LOGGER.info("Coinbase transaction carries inputs - creating coins out of someone else's money?");
            return false;
        }

        List<OutgoingTransaction> outs = transaction.getOutgoingTransactions();
        if (outs == null || outs.size() != 1) {
            LOGGER.info("Coinbase transaction must have exactly one output, got {}",
                    outs == null ? 0 : outs.size());
            return false;
        }

        OutgoingTransaction reward = outs.get(0);
        if (reward.getRecipientAddress() == null
                || Consensus.BLOCK_INCENTIVE.compareTo(reward.getAmount()) != 0) {
            LOGGER.info("Coinbase pays invalid amount {}, expected {}", reward.getAmount(), Consensus.BLOCK_INCENTIVE);
            return false;
        }

        if (transaction.getWallet() == null || transaction.getWallet().address() == null
                || !transaction.getWallet().address().equals(reward.getRecipientAddress())) {
            LOGGER.info("Coinbase output does not pay the miner");
            return false;
        }

        // The miner must sign the claim over its own key.
        return encryptionUtility.verifySignature(transaction.getCanonicalPayload(),
                transaction.getSignature(), reward.getRecipientAddress());
    }

    /**
     * A spending transaction is valid when:
     * <ul>
     *     <li>it has at least one input and every referenced output resolves,</li>
     *     <li>all referenced outputs belong to the SAME wallet,</li>
     *     <li>the signature verifies against exactly that owner's public key.</li>
     * </ul>
     * Unlike the previous implementation there is no silent skip: a missing or
     * unresolvable input reference makes the transaction invalid, so nobody can spend
     * an output without proving ownership.
     */
    public boolean validateSpendTransaction(Transaction transaction) throws SignatureException {

        List<IncomingTransaction> ins = transaction.getIncomingTransactions();
        if (ins == null || ins.isEmpty()) {
            LOGGER.info("Spending transaction without inputs - refusing non-coinbase money creation");
            return false;
        }

        PublicKey owner = null;
        for (IncomingTransaction inTransaction : ins) {
            OutgoingTransaction spentOutput = resolveSpentOutput(inTransaction);
            if (spentOutput == null || spentOutput.getRecipientAddress() == null) {
                LOGGER.info("Unresolvable input reference {}", inTransaction);
                return false;
            }
            if (owner == null) {
                owner = spentOutput.getRecipientAddress();
            } else if (!owner.equals(spentOutput.getRecipientAddress())) {
                LOGGER.info("Transaction inputs belong to multiple wallets ({} and {})",
                        spentOutput.getRecipientAddress(), owner);
                return false;
            }
        }

        String transactionMessage = transaction.getCanonicalPayload();
        if (!encryptionUtility.verifySignature(transactionMessage, transaction.getSignature(), owner)) {
            LOGGER.info("Invalid transaction signature, trying to spend someone else's money ?");
            return false;
        }

        // Chained validity: the parent transactions themselves must be sound.
        for (IncomingTransaction inTransaction : ins) {
            if (!validateTransaction(inTransaction.getTransaction())) {
                LOGGER.info("Invalid parent transaction");
                return false;
            }
        }
        return true;
    }

    /**
     * Resolves the output a given input spends; malformed references yield null rather
     * than throwing, so hostile structures fail validation instead of crashing nodes.
     */
    private OutgoingTransaction resolveSpentOutput(IncomingTransaction inTransaction) {
        try {
            return inTransaction.parentOutPut();
        } catch (RuntimeException e) {
            LOGGER.info("Malformed input reference: {}", e.getMessage());
            return null;
        }
    }

    public BigDecimal computeTotalFee(List<Transaction> transactionList) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction tr : transactionList) {
            tr.setFee(tr.isInitial() ? BigDecimal.ZERO : fee(tr.getIncomingTransactions(), tr.getOutgoingTransactions()));
            sum = sum.add(tr.getFee());
        }
        return sum;
    }

    /**
     * Transaction fee - if the output value of a transaction is
     * less than its input value, the difference is a transaction fee that is added to the incentive value of
     * the block containing the transaction.
     */
    public BigDecimal fee(List<IncomingTransaction> ins, List<OutgoingTransaction> outs) {
        BigDecimal inSum = BigDecimal.ZERO;

        for (IncomingTransaction i : ins) {
            inSum = inSum.add(i.parentOutPut().getAmount());
        }
        BigDecimal outSum = BigDecimal.ZERO;
        for (OutgoingTransaction o : outs) {
            outSum = outSum.add(o.getAmount());
        }
        assert (inSum.compareTo(outSum) >= 0);
        return inSum.subtract(outSum);
    }

}
