package com.javachain.service;

import com.javachain.dto.Block;
import com.javachain.dto.Wallet;
import com.javachain.util.EncryptionUtility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.security.KeyPair;
import java.security.KeyPairGenerator;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private EncryptionUtility encryptionUtility = new EncryptionUtility();

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

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    public void testWallet() throws Exception {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(512);
        KeyPair keyPair = keyGen.genKeyPair();

        Wallet wallet = new Wallet(keyPair.getPrivate(), keyPair.getPublic(), null, null);
        String signature = encryptionUtility.sign("foobar", keyPair.getPrivate());
        wallet.setSignature(signature);
        assertTrue(wallet.address().toString().contains("Sun RSA public key, 512 bits"));
        assertEquals(true, encryptionUtility.verifySignature("foobar", signature, wallet.address()));
        assertEquals(false, encryptionUtility.verifySignature("rogue message", signature, wallet.address()));
    }

    @Test
    void syncBlockchain() throws Exception {

        //when
        when(blockService.verifyBlock(block)).thenReturn(true);
        when(wallet.getBlockchain()).thenReturn(block);
        when(blockService.isNewBlockBigger(wallet.getBlockchain(), block)).thenReturn(true);
        //then
        assertThat(walletService.syncBlockchain(wallet, block).getBlockchain()).isEqualTo(block);
    }

    @Test
    void generateNewWallet() throws Exception {

        //given
        KeyPair keyPair = encryptionUtility.generateKeyPair();
        //when
        when(encUtil.generateKeyPair()).thenReturn(keyPair);
        //then
        assertThat(walletService.generateNewWallet("patriks")).isNotNull();
        assertThat(walletService.generateNewWallet("patriks").getPublickey()).isEqualTo(keyPair.getPublic());
    }
}