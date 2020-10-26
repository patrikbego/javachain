package com.javachain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Objects;

/**
 * The {@code OutgoingTransaction} class represents transaction going from the wallet.
 * It contains recipient wallet address and amount.
 */
public class OutgoingTransaction implements Serializable {

    private PublicKey recipientAddress;
    private BigDecimal amount;

    public OutgoingTransaction(PublicKey recipientAddress, BigDecimal amount) {
        this.recipientAddress = recipientAddress;
        this.amount = amount;
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
        OutgoingTransaction that = (OutgoingTransaction) o;
        return Objects.equals(recipientAddress, that.recipientAddress) &&
                Objects.equals(amount, that.amount);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipientAddress, amount);
    }

}
