package com.javachain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public class Transaction implements Serializable {

    private List<InTransaction> inTransactions;
    private List<OutTransaction> outTransactions;
    private BigDecimal fee;
    private String signature; //TODO change this into Signature
    private Wallet wallet;
    private boolean initial = false;
    private boolean includeSignature = true;

    public List<InTransaction> getInTransactions() {
        return inTransactions;
    }

    public void setInTransactions(List<InTransaction> inTransactions) {
        this.inTransactions = inTransactions;
    }

    public List<OutTransaction> getOutTransactions() {
        return outTransactions;
    }

    public void setOutTransactions(List<OutTransaction> outTransactions) {
        this.outTransactions = outTransactions;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void setWallet(Wallet wallet) {
        this.wallet = wallet;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public boolean isIncludeSignature() {
        return includeSignature;
    }

    public void setIncludeSignature(boolean includeSignature) {
        this.includeSignature = includeSignature;
    }


    @Override
    public String toString() {
        return "Transaction{" +
                "inTransactions=" + inTransactions +
                ", outTransactions=" + outTransactions +
//                ", fee=" + fee +
                (includeSignature ? ", signature='" + signature + '\'' : "") +
                ", wallet=" + wallet +
                ", initial=" + initial +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(inTransactions, that.inTransactions) &&
                Objects.equals(outTransactions, that.outTransactions) &&
                Objects.equals(fee, that.fee) &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(wallet, that.wallet);
    }

    @Override
    public int hashCode() {

        return Objects.hash(inTransactions, outTransactions, fee, signature, wallet);
    }

    public boolean isInitial() {
        return initial;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public String getSignature() {
        return signature;
    }

}
