package com.javachain.dto;

import java.io.Serializable;
import java.util.Objects;

/**
 * The {@code IncomingTransaction} class represents transaction coming into wallet.
 * It contains the transaction itself (for verification reasons)
 * and outPutIndex (position in a list of all incoming / outgoing transactions)
 */
public class IncomingTransaction implements Serializable {

    private Transaction transaction; // TODO here for validation, consider refactoring
    private final int outPutIndex;

    public IncomingTransaction(Transaction transaction, int outPutIndex) {
        this.transaction = transaction;
        this.outPutIndex = outPutIndex;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public OutgoingTransaction getRecipient() {
        if (!transaction.getOutgoingTransactions().isEmpty()) {
            for (OutgoingTransaction otr : getTransaction().getOutgoingTransactions()) {
                if (otr.getRecipientAddress().equals(transaction.getWallet().address()))//TODO MAKE SURE THAT ALL OUT TRANSACTIONS GET MERGED INTO ONE OUT TRANSACTION PER USER
                    return otr;
            }
        }
        return null;
    }

    public OutgoingTransaction parentOutPut() {
        return this.transaction.getOutgoingTransactions().get(this.outPutIndex);
    }

    @Override
    public String toString() {
        return "InTransaction{" +
                "transaction=" + transaction +
                ", outPutIndex=" + outPutIndex +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IncomingTransaction that = (IncomingTransaction) o;
        return outPutIndex == that.outPutIndex &&
                Objects.equals(transaction, that.transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transaction, outPutIndex);
    }
}
