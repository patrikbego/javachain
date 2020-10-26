package com.javachain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Objects;

/**
 * The {@code Wallet} is the main "store" of blocks and credentials linked to user.
 * The keys stored in wallet are used to encrypt/decrypt and track ownership, of transactions.
 * The block in wallet is synced and mined/approved by other wallets.
 * <p>
 * Each wallet contains:
 * <ul>
 *     <li>the chain of blocks (Block object),</li>
 *     <li>private and public keys used for encryption blocks and transactions,</li>
 *     <li>signer, mainly used for human readability ,</li>
 *     <li>digital signature</li>
 *     <li>the amount we are sending (temporary storage)</li>
 *     <li></li>
 * </ul>
 * <p>
 *  To initialize Wallet we need to pass into constructor name of the wallet (or signer).
 *  Private and public keys are initialized in constructor.
 * <p>
 * For example:
 *  <blockquote><pre>
 *   Wallet wallet = new Wallet("patriks");
 *  </pre></blockquote><p>
 */
public class Wallet implements Serializable {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private String signer;
    private String signature;
    private Block blockchain;
    private BigDecimal amountToBeSent;
    private final Instant dateCreated;

    public Wallet() {
        this.dateCreated = Instant.now();
    }

    public Wallet(PrivateKey privateKey, PublicKey publicKey, String signer, Block blockchain) {
        this.blockchain = blockchain;
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.signer = signer;
        this.dateCreated = Instant.now();
    }

    public PublicKey address() {
//        TODO this should be a set of different public keys and after it should generate unique address
        return getPublicKey();
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(PublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public String getSigner() {
        return signer;
    }

    public void setSigner(String signer) {
        this.signer = signer;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Wallet wallet = (Wallet) o;
        return Objects.equals(privateKey, wallet.privateKey) &&
                Objects.equals(publicKey, wallet.publicKey) &&
                Objects.equals(dateCreated, wallet.dateCreated) &&
                Objects.equals(signer, wallet.signer) &&
                Objects.equals(signature, wallet.signature);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateKey, publicKey, signer, signature, dateCreated);
    }

    public Block getBlockchain() {
        return blockchain;
    }

    public void setBlockchain(Block blockchain) {
        this.blockchain = blockchain;
    }

    public BigDecimal getAmountToBeSent() {
        return amountToBeSent;
    }

    public void setAmountToBeSent(BigDecimal amountToBeSent) {
        this.amountToBeSent = amountToBeSent;
    }
}
