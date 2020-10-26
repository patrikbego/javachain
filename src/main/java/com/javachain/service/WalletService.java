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
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;

/**
 * The {@code WalletService} class is used for wallet related functionalities.
 * It is uses interface for interaction with other wallets.
 * <p>
 * It has supporting method for syncing blockchain.
 */
@Service
public class WalletService {

    private static final Logger LOGGER = LoggerFactory.getLogger(WalletService.class);

    final EncryptionUtility encryptionUtility;
    final HashingUtility hashingUtility;
    final MiningService miningService;
    final TransactionService transactionService;
    final BlockService blockService;

    @Autowired
    public WalletService(EncryptionUtility encryptionUtility, HashingUtility hashingUtility, MiningService miningService,
                         TransactionService transactionService, BlockService blockService) {
        this.encryptionUtility = encryptionUtility;
        this.hashingUtility = hashingUtility;
        this.miningService = miningService;
        this.transactionService = transactionService;
        this.blockService = blockService;
    }

    public Wallet syncBlockchain(Wallet wallet, Block blockchain)
            throws SignatureException {
        if (!blockService.verifyBlock(blockchain)) {
            LOGGER.error("Invalid block present");
            throw new SecurityException("Invalid block present");
        }
        if (!blockService.isNewBlockBigger(wallet.getBlockchain(), blockchain)) {
            LOGGER.error("Trying to sync with smaller block");
            throw new SecurityException("Trying to sync with smaller block");
        }
        wallet.setBlockchain(blockchain);
        return wallet;
    }

    public Wallet generateNewWallet(String signer) throws NoSuchAlgorithmException {
        LOGGER.info("Generating new wallet for : {}", signer);
        KeyPair keyPair = encryptionUtility.generateKeyPair();
        Wallet wallet = new Wallet();
        wallet.setSigner(signer);
        wallet.setPrivateKey(keyPair.getPrivate());
        wallet.setPublicKey(keyPair.getPublic());
        return wallet;
    }

}
