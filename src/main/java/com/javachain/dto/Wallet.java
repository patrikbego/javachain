package com.javachain.dto;

import java.io.Serializable;
import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Objects;

public class Wallet implements Serializable {

    private PrivateKey privateKey;
    private PublicKey publickey;
    private String signer;
    private String signature;
    private Block blockchain;
    private BigDecimal amountToBeSent;

    public Wallet() {

    }

    public Wallet(PrivateKey privateKey, PublicKey publickey, String signer, Block blockchain) {
        this.blockchain = blockchain;
        this.privateKey = privateKey;
        this.publickey = publickey;
        this.signer = signer;
    }

    public PublicKey address() {
//        TODO this should be a set of different public keys and after it should generate unique address
        return getPublickey();
    }


    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    public PublicKey getPublickey() {
        return publickey;
    }

    public void setPublickey(PublicKey publickey) {
        this.publickey = publickey;
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
                Objects.equals(publickey, wallet.publickey) &&
                Objects.equals(signer, wallet.signer) &&
                Objects.equals(signature, wallet.signature);
    }

    @Override
    public int hashCode() {

        return Objects.hash(privateKey, publickey, signer, signature);
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
