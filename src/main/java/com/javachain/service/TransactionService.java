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

    /**
     * Creates a signed transaction.
     *
     * @param senderWallet         the wallet whose unspent outputs fund the transfer
     * @param isInitial            true for a coinbase (mints coins, ignores fee/inputs)
     * @param fee                  explicitly paid mining fee; must be affordable on top
     *                             of the transferred amounts
     * @param outgoingTransactions recipients and amounts, in payout order
     * @return the signed transaction, with any surplus returned to the sender as a
     * trailing change output, conserving value exactly: inputs = outputs + fee
     * @throws SecurityException if the wallet cannot afford transfers plus fee
     */
    public Transaction send(Wallet senderWallet, boolean isInitial, BigDecimal fee,
                            OutgoingTransaction... outgoingTransactions) {

        if (fee == null || fee.signum() < 0) {
            throw new SecurityException("Fee must be zero or positive");
        }

        Transaction transaction = new Transaction();
        transaction.setIncludeSignature(false);
        transaction.setWallet(senderWallet);
        transaction.setInitial(isInitial);

        List<OutgoingTransaction> outTransactions = new ArrayList<>();
        if (outgoingTransactions != null) {
            for (OutgoingTransaction out : outgoingTransactions) {
                if (out == null || out.getRecipientAddress() == null
                        || out.getAmount() == null || out.getAmount().signum() < 0) {
                    throw new SecurityException("Invalid output entry");
                }
                outTransactions.add(out);
            }
        }
        transaction.setOutgoingTransactions(outTransactions);

        // A coinbase creates new coins out of nothing - it must not claim to spend
        // anything, otherwise phantom inputs would pollute double-spend tracking.
        List<IncomingTransaction> inTransactions;
        if (isInitial) {
            transaction.setFee(BigDecimal.ZERO);
            inTransactions = new ArrayList<>();
        } else {
            inTransactions = getPreviousInTransactions(senderWallet);
            BigDecimal inSum = BigDecimal.ZERO;
            try {
                for (IncomingTransaction in : inTransactions) {
                    inSum = inSum.add(in.parentOutPut().getAmount());
                }
            } catch (RuntimeException e) {
                throw new SecurityException("Unresolvable wallet outputs: " + e.getMessage());
            }
            BigDecimal outSum = BigDecimal.ZERO;
            for (OutgoingTransaction out : outTransactions) {
                outSum = outSum.add(out.getAmount());
            }
            BigDecimal required = outSum.add(fee);
            if (required.compareTo(inSum) > 0) {
                throw new SecurityException("Insufficient funds: wallet owns " + inSum
                        + " but transfers plus fee require " + required);
            }
            BigDecimal change = inSum.subtract(required);
            if (change.signum() > 0) {
                // value conservation: whatever is neither transferred nor paid as fee
                // returns to the sender as a trailing change output
                outTransactions.add(new OutgoingTransaction(senderWallet.address(), change));
            }
            transaction.setFee(fee);
        }
        transaction.setIncomingTransactions(inTransactions);

        // The fee is deliberately NOT part of the canonical payload: it is fully
        // derived from inputs minus outputs, so validators recompute it and there is
        // nothing mutable left to sneak past a signature (the old post-signing
        // mutation in computeTotalFee was exactly such a hazard).
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
     * Structural rules: no inputs, exactly one output paying at least the
     * {@link Consensus#BLOCK_INCENTIVE} to the miner's own address, signed with the
     * miner's key. The EXACT payout (incentive plus the block's collected fees) is
     * enforced by {@code BlockService} once the sibling transactions - and therefore
     * their fees - are known; standalone this check can only reject underpayment,
     * which would silently burn money.
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
                || reward.getAmount() == null
                || Consensus.BLOCK_INCENTIVE.compareTo(reward.getAmount()) > 0) {
            LOGGER.info("Coinbase pays invalid amount {}, expected at least {}",
                    reward == null ? null : reward.getAmount(), Consensus.BLOCK_INCENTIVE);
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
     *     <li>the signature verifies against exactly that owner's public key,</li>
     *     <li>value is conserved: outputs never exceed inputs. The difference IS the
     *     fee - it is recomputed here, so a transaction cannot understate it.</li>
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
        BigDecimal inSum = BigDecimal.ZERO;
        for (IncomingTransaction inTransaction : ins) {
            OutgoingTransaction spentOutput = resolveSpentOutput(inTransaction);
            if (spentOutput == null || spentOutput.getRecipientAddress() == null
                    || spentOutput.getAmount() == null) {
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
            inSum = inSum.add(spentOutput.getAmount());
        }

        BigDecimal outSum = BigDecimal.ZERO;
        List<OutgoingTransaction> outs = transaction.getOutgoingTransactions();
        if (outs != null) {
            for (OutgoingTransaction out : outs) {
                if (out.getAmount() == null) {
                    LOGGER.info("Output without amount");
                    return false;
                }
                outSum = outSum.add(out.getAmount());
            }
        }
        if (outSum.compareTo(inSum) > 0) {
            LOGGER.info("Value creation rejected: outputs {} exceed owned inputs {}", outSum, inSum);
            return false;
        }
        // Derived fee - recomputed on every validation, immune to tampering because it
        // is not part of the signed payload at all.
        transaction.setFee(inSum.subtract(outSum));

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

    /**
     * Sums the fees of already-validated transactions. Since validation DERIVES each
     * fee (inputs minus outputs) and {@code send()} sets it up front, this no longer
     * mutates anything - the old version recomputed and overwrote fees AFTER signing,
     * which is why the fee had to stay out of the canonical payload.
     */
    public BigDecimal computeTotalFee(List<Transaction> transactionList) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction tr : transactionList) {
            if (tr.isInitial() || tr.getFee() == null) continue;
            sum = sum.add(tr.getFee());
        }
        return sum;
    }

}
