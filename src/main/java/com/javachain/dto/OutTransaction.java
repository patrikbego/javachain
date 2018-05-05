package com.javachain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Objects;

public class OutTransaction implements Serializable {

    private PublicKey recipientAddress;
    private BigDecimal amount;

    public OutTransaction(PublicKey recipientAddress, BigDecimal amount) {
        this.recipientAddress = recipientAddress;
        this.amount = amount;
    }

    public OutTransaction() {
    }

    public PublicKey getRecipientAddress() {
        return recipientAddress;
    }

    public void setRecipientAddress(PublicKey recipientAddress) {
        this.recipientAddress = recipientAddress;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    @Override
    public String toString() {
        return "OutTransaction{" +
                "recipientAddress=" + recipientAddress +
                ", amount=" + amount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutTransaction that = (OutTransaction) o;
        return Objects.equals(recipientAddress, that.recipientAddress) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {

        return Objects.hash(recipientAddress, amount);
    }

}
