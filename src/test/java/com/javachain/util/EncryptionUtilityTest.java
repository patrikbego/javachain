package com.javachain.util;

import com.javachain.dto.Wallet;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;

class EncryptionUtilityTest {

    private final String message = "test";
    private final EncryptionUtility encryptionUtility = new EncryptionUtility();
    private final EncodingUtility encodingUtility = new EncodingUtility();
    final HashingUtility hashingUtility = new HashingUtility(encodingUtility);
    final MiningService miningService = new MiningService(hashingUtility, encodingUtility);
    final TransactionService transactionService = new TransactionService(encryptionUtility, hashingUtility, miningService);
    final BlockService blockService = new BlockService(encryptionUtility, transactionService, hashingUtility, miningService);
    private final WalletService walletService = new WalletService(encryptionUtility, hashingUtility, miningService,
            transactionService, blockService);
    private Wallet patriksWallet;

    @BeforeEach
    void setUpWallet() throws Exception {
        patriksWallet = walletService.generateNewWallet("patriks");
    }

    @Test
    void sign() {
        assertEquals(344, encryptionUtility.sign(message, patriksWallet.getPrivateKey()).length());
    }

    @Test
    void verifySignature() throws Exception {

        String message1 = "test1";
        String signature = encryptionUtility.sign(message, patriksWallet.getPrivateKey());

        assertTrue(encryptionUtility.verifySignature(message, signature, patriksWallet.getPublicKey()));

        assertFalse(encryptionUtility.verifySignature(message1, signature, patriksWallet.getPublicKey()));

    }

    @Test
    void encrypt() throws Exception {

        //First generate a public/private key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(1024);
        KeyPair keyPair = keyGen.genKeyPair();
        //KeyPair pair = getKeyPairFromKeyStore();

        //Our secret message
        String message = "The Only Thing Necessary for the Triumph of Evil is t"; //hat Good Men Do Nothing;

        //Encrypt the message
        String cipherText = encryptionUtility.encrypt(message, keyPair.getPublic());
        assertEquals(172, cipherText.length());

        //Now decrypt it
        String decipheredMessage = encryptionUtility.decrypt(cipherText, keyPair.getPrivate());
        assertEquals(decipheredMessage, message);

    }

    @Test
    void decrypt() throws Exception {

        KeyPair keyPair1 = encryptionUtility.generateKeyPair();
        KeyPairGenerator keyGen1 = KeyPairGenerator.getInstance("RSA");
        keyGen1.initialize(1024);
        KeyPair keyPair = keyGen1.genKeyPair();

        String message = "Good example of encryption";
        String firstTimeENcryptedWithClientsPubKey = encryptionUtility.encrypt(message, keyPair.getPublic());
        String secondTimeENcryptedWithServersPrivateKey = encryptionUtility.encrypt(firstTimeENcryptedWithClientsPubKey, keyPair1.getPublic());

        String firstTimeDecryptionOnClientsSideWithServerPubKey = encryptionUtility.decrypt(secondTimeENcryptedWithServersPrivateKey, keyPair1.getPrivate());
        String secondTimeDecryption = encryptionUtility.decrypt(firstTimeDecryptionOnClientsSideWithServerPubKey, keyPair.getPrivate());
        assertEquals(secondTimeDecryption, message);
    }

    @Test
    void generateHexStringKeyPair() throws Exception {
        KeyPair keyPair1 = encryptionUtility.generateKeyPair();
        assertNotNull(encryptionUtility.generateHexStringKey(keyPair1, true));
        assertEquals(588, encryptionUtility.generateHexStringKey(keyPair1, true).length());
        assertNotNull(encryptionUtility.generateHexStringKey(keyPair1, false));
        assertTrue(encryptionUtility.generateHexStringKey(keyPair1, false).length() >= 2432);
    }

    @Test
    void generateKeyPair() throws Exception {
        KeyPair keyPair = encryptionUtility.generateKeyPair();
        assertNotNull(keyPair);
        assertNotNull(keyPair.getPrivate());
        assertEquals("RSA", keyPair.getPublic().getAlgorithm());
    }

}
