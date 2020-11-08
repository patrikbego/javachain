package com.javachain.dto;

import com.javachain.util.CanonicalSerializer;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The {@code Transaction} class represents transaction made between wallets.
 *
 <p>
 * Each transaction contains:
 * <ul>
 *     <li>list of incoming and outgoing sub transactions,</li>
 *     <li>fee (miner gets once it is approved),</li>
 *     <li>a digital signature ,</li>
 *     <li>digital signature,</li>
 *     <li>the amount we are sending (temporary storage),</li>
 *     <li>wallet related fields - like keys, sender address ...</li>
 * </ul>
 */
public class Transaction implements Serializable {

    public Transaction() {
        dateCreated = Instant.now();
    }

    private List<IncomingTransaction> incomingTransactions;
    private List<OutgoingTransaction> outgoingTransactions;
    private BigDecimal fee;
    private String signature; //TODO change this into Signature object
    private Wallet wallet; //TODO remove wallet object from transaction
    private final Instant dateCreated;
    private boolean initial = false;
    private boolean includeSignature = true;

    public List<IncomingTransaction> getIncomingTransactions() {
        return incomingTransactions;
    }

    public void setIncomingTransactions(List<IncomingTransaction> incomingTransactions) {
        this.incomingTransactions = incomingTransactions;
    }

    public List<OutgoingTransaction> getOutgoingTransactions() {
        return outgoingTransactions;
    }

    public void setOutgoingTransactions(List<OutgoingTransaction> outgoingTransactions) {
        this.outgoingTransactions = outgoingTransactions;
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

    /**
     * Canonical, deterministic representation of everything this transaction commits to
     * (inputs as parent-tx references, outputs, fee, timestamp). This - not
     * {@link #toString()} - is what gets signed and later verified. Note that the sender's
     * {@code Wallet} object is deliberately excluded: ownership is proven by the signature
     * against the referenced output, and including a live object would leak JVM identity
     * hash codes into signatures.
     */
    public String getCanonicalPayload() {
        return CanonicalSerializer.transactionPayload(this);
    }

    /**
     * Stable cross-JVM id committing to payload and signature.
     */
    public String getId() {
        return CanonicalSerializer.transactionId(this);
    }

    @Override
    public String toString() {
        // Human-readable summary only; identity-hash-free and STABLE across signature
        // changes (no id, no signature content). Signed content is getCanonicalPayload().
        return "Transaction{" +
                "initial=" + initial +
                ", inTransactions=" + (incomingTransactions == null ? 0 : incomingTransactions.size()) +
                ", outTransactions=" + (outgoingTransactions == null ? 0 : outgoingTransactions.size()) +
                ", wallet=" + (wallet == null ? "null" : CanonicalSerializer.address(wallet.address())) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(incomingTransactions, that.incomingTransactions) &&
                Objects.equals(outgoingTransactions, that.outgoingTransactions) &&
                Objects.equals(fee, that.fee) &&
                Objects.equals(dateCreated, that.dateCreated) &&
                Objects.equals(signature, that.signature) &&
                Objects.equals(wallet, that.wallet);
    }

    @Override
    public int hashCode() {
        return Objects.hash(incomingTransactions, outgoingTransactions, fee, signature, wallet, dateCreated);
    }

    public boolean isInitial() {
        return initial;
    }

    public Instant getDateCreated() {
        return dateCreated;
    }

    public void setInitial(boolean initial) {
        this.initial = initial;
    }

    public String getSignature() {
        return signature;
    }
}
