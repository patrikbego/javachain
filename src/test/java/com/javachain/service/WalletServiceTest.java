package com.javachain.service;

import com.javachain.dto.Block;
import com.javachain.dto.Wallet;
import com.javachain.util.EncryptionUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private final EncryptionUtility encryptionUtility = new EncryptionUtility();

    @InjectMocks
    WalletService walletService;
    @Mock
    EncryptionUtility encUtil;
    @Mock
    Wallet wallet;
    @Mock
    Block block;
    @Mock
    BlockService blockService;

    @Test
    void testWallet() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.genKeyPair();

        Wallet wallet = new Wallet(keyPair.getPrivate(), keyPair.getPublic(), null, null);
        String signature = encryptionUtility.sign("foobar", keyPair.getPrivate());
        wallet.setSignature(signature);
        assertTrue(wallet.address().toString().contains("Sun RSA public key, 512 bits"));
        assertTrue(encryptionUtility.verifySignature("foobar", signature, wallet.address()));
        assertFalse(encryptionUtility.verifySignature("rogue message", signature, wallet.address()));
    }

    @Test
    void syncBlockchain() throws Exception {
        //given
        //when
        when(blockService.verifyBlock(block)).thenReturn(true);
        when(wallet.getBlockchain()).thenReturn(block);
        when(blockService.isNewBlockBigger(wallet.getBlockchain(), block)).thenReturn(true);
        //then
        assertEquals(walletService.syncBlockchain(wallet, block).getBlockchain(), block);
    }

    @Test
    void generateNewWallet() throws Exception {
        //given
        KeyPair keyPair = encryptionUtility.generateKeyPair();
        //when
        when(encUtil.generateKeyPair()).thenReturn(keyPair);
        //then
        assertNotNull(walletService.generateNewWallet("patriks"));
        assertEquals(walletService.generateNewWallet("patriks").getPublicKey(), keyPair.getPublic());
    }
}
