package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TransactionService {

//    Check syntactic correctness
//    Make sure neither in or out lists are empty
//    Size in bytes <= MAX_BLOCK_SIZE
//    Each output value, as well as the total, must be in legal money range
//    Make sure none of the inputs have hash=0, n=-1 (coinbase transactions)
//    Check that nLockTime <= INT_MAX[1], size in bytes >= 100[2], and sig opcount <= 2[3]
//    Reject "nonstandard" transactions: scriptSig doing anything other than pushing numbers on the stack, or scriptPubkey not matching the two usual forms[4]
//    Reject if we already have matching tx in the pool, or in a block in the main branch
//    For each input, if the referenced output exists in any other tx in the pool, reject this transaction.[5]
//    For each input, look in the main branch and the transaction pool to find the referenced output transaction. If the output transaction is missing for any input, this will be an orphan transaction. Add to the orphan transactions, if a matching transaction is not in there already.
//    For each input, if the referenced output transaction is coinbase (i.e. only 1 input, with hash=0, n=-1), it must have at least COINBASE_MATURITY (100) confirmations; else reject this transaction
//    For each input, if the referenced output does not exist (e.g. never existed or has already been spent), reject this transaction[6]
//    Using the referenced output transactions to get input values, check that each input value, as well as the sum, are in legal money range
//    Reject if the sum of input values < sum of output values
//    Reject if transaction fee (defined as sum of input values minus sum of output values) would be too low to get into an empty block
//    Verify the scriptPubKey accepts for each input; reject if any are bad
//    Add to transaction pool[7]
//            "Add to wallet if mine"
//    Relay transaction to peers
//    For each orphan transaction that uses this one as one of its inputs, run all these steps (including this one) recursively on that orphan

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    EncryptionUtility encryptionUtility;

    @Autowired
    HashingUtility hashingUtility;

    @Autowired
    MiningService miningService;

    public Transaction send(Wallet senderWallet, boolean isMining, Wallet... receiverWallets) throws Exception {

        Transaction transaction = new Transaction();
        transaction.setIncludeSignature(false);
        transaction.setFee(BigDecimal.ZERO);// TODO for now this is disabled
        transaction.setWallet(senderWallet);
        transaction.setInitial(isMining);

        List<OutTransaction> outTransactions = new ArrayList<>();
        for (Wallet receiverWallet : receiverWallets) {
            OutTransaction outTransaction = new OutTransaction(receiverWallet.address(), receiverWallet.getAmountToBeSent());
            outTransactions.add(outTransaction);
        }
        transaction.setOutTransactions(outTransactions);

        List<InTransaction> inTransactions = getPreviousInTransactions(senderWallet);
        transaction.setInTransactions(inTransactions);
        transaction.setSignature(encryptionUtility.sign(transaction.toString(), senderWallet.getPrivateKey()));

        return transaction;
    }


    public List<InTransaction> getPreviousInTransactions(Wallet senderWallet) {

        List<InTransaction> inTransactions = new ArrayList<>();
        Block block = senderWallet.getBlockchain();

        //get last outTr for wallet
        while (block != null) {
            List<Transaction> transactions = block.getTransactionList();
            for (Transaction tr : transactions) {
                for (OutTransaction otr : tr.getOutTransactions()) {
                    if (otr.getRecipientAddress().equals(senderWallet.getPublickey())) {
                        InTransaction intr = new InTransaction(tr, 0);
                        inTransactions.add(intr);
                    }
                }
                //we just need transaction in last block
                if (inTransactions.size() > 0)
                    return inTransactions;
            }
            block = block.getAncestor();
        }
        return inTransactions;
    }

    public boolean validateTransaction(Transaction transaction) throws Exception {

        String transactionMessage = transaction.toString();
        if (transaction.isInitial() || transaction.getInTransactions() == null || transaction.getInTransactions().size() <= 0)
            return true;

        OutTransaction firstInputAddress = transaction.getInTransactions().get(0).getRecipient();
        if (firstInputAddress != null && !encryptionUtility.verifySignature(transactionMessage, transaction.getSignature(), firstInputAddress.getRecipientAddress())) {
            LOGGER.info(("Invalid transaction signature, trying to spend someone else's money ?"));
            return false;
        }

        for (InTransaction inTransaction : transaction.getInTransactions()) {
            if (inTransaction.getRecipient() != null && !validateTransaction(inTransaction.getTransaction())) {
                LOGGER.info("Invalid parent transaction");
                return false;
            }
        }

        for (int i = 1; i < transaction.getInTransactions().size(); i++) { //starts with one
            InTransaction inTransaction = transaction.getInTransactions().get(i);
            if (!inTransaction.getRecipient().equals(firstInputAddress)) {
                LOGGER.info(String.format("Transaction inputs belong to multiple wallets (%s and %s)",
                        inTransaction.getRecipient(), firstInputAddress));
                return false;
            }
        }
        return true;
    }

    public BigDecimal computeTotalFee(List<Transaction> transactionList) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Transaction tr : transactionList) {
            tr.setFee(tr.isInitial() ? BigDecimal.ZERO : fee(tr.getInTransactions(), tr.getOutTransactions()));
            sum = sum.add(tr.getFee());
        }
        return sum;
    }

    /**
     * Transaction fee - if the output value of a transaction is
     * less than its input value, the difference is a transaction fee that is added to the incentive value of
     * the block containing the transaction.
     */
    public BigDecimal fee(List<InTransaction> ins, List<OutTransaction> outs) {
        BigDecimal inSum = BigDecimal.ZERO;

        for (InTransaction i : ins) {
            inSum = inSum.add(i.parentOutPut().getAmount());
        }
        BigDecimal outSum = BigDecimal.ZERO;
        for (OutTransaction o : outs) {
            outSum = outSum.add(o.getAmount());
        }
        assert (inSum.compareTo(outSum) >= 0);
        return inSum.subtract(outSum);
    }

}
