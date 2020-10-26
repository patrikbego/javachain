package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

        List<IncomingTransaction> inTransactions = getPreviousInTransactions(senderWallet);
        transaction.setIncomingTransactions(inTransactions);
        transaction.setSignature(encryptionUtility.sign(transaction.toString(), senderWallet.getPrivateKey()));

        return transaction;
    }

    public List<IncomingTransaction> getPreviousInTransactions(Wallet senderWallet) {

        List<IncomingTransaction> inTransactions = new ArrayList<>();
        Block block = senderWallet.getBlockchain();

        //get last outTr for wallet
        while (block != null) {
            List<Transaction> transactions = block.getTransactionList();
            for (Transaction tr : transactions) {
                for (OutgoingTransaction otr : tr.getOutgoingTransactions()) {
                    if (otr.getRecipientAddress().equals(senderWallet.getPublicKey())) {
                        IncomingTransaction intr = new IncomingTransaction(tr, 0);
                        inTransactions.add(intr);
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

    public boolean validateTransaction(Transaction transaction) throws SignatureException {

        String transactionMessage = transaction.toString();
        if (transaction.isInitial() || transaction.getIncomingTransactions() == null || transaction.getIncomingTransactions().isEmpty())
            return true;

        OutgoingTransaction firstInputAddress = transaction.getIncomingTransactions().get(0).getRecipient();
        if (firstInputAddress != null && !encryptionUtility.verifySignature(transactionMessage, transaction.getSignature(),
                firstInputAddress.getRecipientAddress())) {
            LOGGER.info(("Invalid transaction signature, trying to spend someone else's money ?"));
            return false;
        }

        for (IncomingTransaction inTransaction : transaction.getIncomingTransactions()) {
            if (inTransaction.getRecipient() != null && !validateTransaction(inTransaction.getTransaction())) {
                LOGGER.info("Invalid parent transaction");
                return false;
            }
        }

        for (int i = 1; i < transaction.getIncomingTransactions().size(); i++) { //starts with one
            IncomingTransaction inTransaction = transaction.getIncomingTransactions().get(i);
            if (!inTransaction.getRecipient().equals(firstInputAddress)) {
                LOGGER.info("Transaction inputs belong to multiple wallets ({} and {})",
                        inTransaction.getRecipient(), firstInputAddress);
                return false;
            }
        }
        return true;
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
