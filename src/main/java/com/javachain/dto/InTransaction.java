package com.javachain.dto;

import java.io.Serializable;
import java.util.Objects;

public class InTransaction implements Serializable {

    private Transaction transaction;
    private int outPutIndex;

    public InTransaction(Transaction transaction, int outPutIndex) {
        this.transaction = transaction;
        this.outPutIndex = outPutIndex;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public OutTransaction getRecipient() {
        if (transaction.getOutTransactions().size() > 0) {
            for (OutTransaction otr : getTransaction().getOutTransactions()) {
                if (otr.getRecipientAddress().equals(transaction.getWallet().address()))//TODO MAKE SURE THAT ALL OUT TRANSACTIONS GET MERGED INTO ONE OUT TRANSACTION PER USER
                    return otr;
            }
        }
        return null;
    }

    public OutTransaction parentOutPut() {
        return this.transaction.getOutTransactions().get(this.outPutIndex);
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
        InTransaction that = (InTransaction) o;
        return outPutIndex == that.outPutIndex &&
                Objects.equals(transaction, that.transaction);
    }

    @Override
    public int hashCode() {

        return Objects.hash(transaction, outPutIndex);
    }
}
