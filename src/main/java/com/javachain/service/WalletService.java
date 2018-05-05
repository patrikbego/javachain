package com.javachain.service;

import com.javachain.dto.Block;
import com.javachain.dto.Wallet;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;

@Service
public class WalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    @Autowired
    EncryptionUtility encryptionUtility;
    @Autowired
    HashingUtility hashingUtility;
    @Autowired
    MiningService miningService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    BlockService blockService;

    public Wallet syncBlockchain(Wallet wallet, Block blockchain) throws Exception {
        if (!blockService.verifyBlock(blockchain)) {
            LOGGER.error("Invalid block present");
            throw new Exception("Invalid block present");
        }
        if (!blockService.isNewBlockBigger(wallet.getBlockchain(), blockchain)) {
            LOGGER.error("Trying to sync with smaller block");
            throw new Exception("Trying to sync with smaller block");
        }
        wallet.setBlockchain(blockchain);
        return wallet;
    }

    public Wallet generateNewWallet(String signer) throws Exception {
        LOGGER.info("Generating new wallet for : {}", signer);
        KeyPair keyPair = encryptionUtility.generateKeyPair();
        Wallet wallet = new Wallet();
        wallet.setSigner(signer);
        wallet.setPrivateKey(keyPair.getPrivate());
        wallet.setPublickey(keyPair.getPublic());
        return wallet;
    }

    public void setEncryptionUtility(EncryptionUtility encryptionUtility) {
        this.encryptionUtility = encryptionUtility;
    }
}
