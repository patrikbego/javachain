package com.javachain.dto;

import java.security.PublicKey;
import java.util.List;
import java.util.Objects;

public class Block {

    private String nonce;
    private String hash;
    private List<Transaction> transactionList;
    private Block ancestor;
    private PublicKey minerAddress;
    private boolean skipVerification;
    private boolean includeHash;
    private boolean initial = false;

    public Block(PublicKey address, List<Transaction> transactions, Block ancestor) {
        this.minerAddress = address;
        this.transactionList = transactions;
        this.ancestor = ancestor;
        includeHash = false;
    }

    @Override
    public String toString() {
        return "Block{" +
                (includeHash ? "nonce='" + nonce + '\'' +
                        ", hash='" + hash + '\'' : "") +
                ", transactionList=" + transactionList +
                ", ancestor=" + ancestor +
                ", minerAddress=" + minerAddress +
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
                initial == block.initial &&
                Objects.equals(nonce, block.nonce) &&
                Objects.equals(hash, block.hash) &&
                Objects.equals(ancestor, block.ancestor) &&
                Objects.equals(minerAddress, block.minerAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nonce, hash, ancestor, minerAddress, skipVerification, includeHash, initial);
    }

    public List<Transaction> getTransactionList() {
        return transactionList;
    }

    public void setTransactionList(List<Transaction> transactionList) {
        this.transactionList = transactionList;
    }


    public Block getAncestor() {
        return ancestor;
    }

    public void setAncestor(Block ancestor) {
        this.ancestor = ancestor;
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

    public PublicKey getMinerAddress() {
        return minerAddress;
    }

    public void setMinerAddress(PublicKey minerAddress) {
        this.minerAddress = minerAddress;
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
