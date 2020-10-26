package com.javachain.dto;

import java.io.Serializable;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The {@code Block} class represents a block chain.
 * Each block is a growing chain of blocks, that are linked using cryptography.
 * Once block is mined/approved it's value cannot be changed.
 * <p>
 * Each block contains:
 * <ul>
 *     <li>list of transactions,</li>
 *     <li>the nonce, is the number that block chain miners are trying to solve,</li>
 *     <li>the hash (fixed length presentation of the block/data),</li>
 *     <li>address of a miner,</li>
 *     <li>creation date.</li>
 * </ul>
 *
 * <p>
 * To initialize Block we need to pass into constructor:
 * <ul>
 *     <li>miners wallet address</li>
 *     <li>list of transactions allocated to this block</li>
 *     <li>previous Block</li>
 * </ul>
 * <p>
 * For example:
 *  <blockquote><pre>
 *   Block block = new Block(wallet.address(), Arrays.asList(tr1, tr2, tr3), previousBlock);
 *  </pre></blockquote><p>
 */
public class Block implements Serializable {

    private String nonce;
    private String hash;
    private List<Transaction> transactionList;
    private Block previousBlock;
    private PublicKey minersAddress;
    private final Instant dateCreated;
    private boolean skipVerification;
    private boolean includeHash;

    public Block(PublicKey minersAddress, List<Transaction> transactions, Block previousBlock) {
        this.minersAddress = minersAddress;
        this.transactionList = transactions;
        this.previousBlock = previousBlock;
        this.dateCreated = Instant.now();
        includeHash = false;
    }

    @Override
    public String toString() {
        return "Block{" +
                (includeHash ? "nonce='" + nonce + '\'' +
                        ", hash='" + hash + '\'' : "") +
                ", transactionList=" + transactionList +
                ", ancestor=" + previousBlock +
                ", minerAddress=" + minersAddress +
                ", skipVerification=" + skipVerification +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Block block = (Block) o;
        return skipVerification == block.skipVerification &&
                includeHash == block.includeHash &&
                Objects.equals(nonce, block.nonce) &&
                Objects.equals(dateCreated, block.dateCreated) &&
                Objects.equals(hash, block.hash) &&
                Objects.equals(previousBlock, block.previousBlock) &&
                Objects.equals(minersAddress, block.minersAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, hash, previousBlock, minersAddress, skipVerification, includeHash, dateCreated);
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }


    public Block getPreviousBlock() {
        return previousBlock;
    }

    public void setPreviousBlock(Block previousBlock) {
        this.previousBlock = previousBlock;
    }

    public String getNonce() {
        return this.nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public PublicKey getMinersAddress() {
        return minersAddress;
    }

    public void setMinersAddress(PublicKey minersAddress) {
        this.minersAddress = minersAddress;
    }

    public boolean isSkipVerification() {
        return skipVerification;
    }

    public void setSkipVerification(boolean skipVerification) {
        this.skipVerification = skipVerification;
    }

    public boolean isIncludeHash() {
        return includeHash;
    }

    public void setIncludeHash(boolean includeHash) {
        this.includeHash = includeHash;
    }

}
