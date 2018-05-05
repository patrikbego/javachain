package com.javachain.service;

import com.javachain.dto.Block;
import com.javachain.dto.OutTransaction;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.util.EncryptionUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlockServiceTest {

    @InjectMocks
    BlockService blockService;
    @Mock
    TransactionService transactionService;
    @Mock
    MiningService miningService;
    @Mock
    Wallet wallet;
    @Mock
    Transaction transaction;
    @Mock
    Block block;
    @Mock
    Block newBlock;
    @Mock
    EncryptionUtility encUtil;

    @Test
    void shouldInjectMocks() throws Exception {
        assertThat(blockService).isNotNull();
        assertThat(transactionService).isNotNull();
        assertThat(transaction).isNotNull();
        assertThat(wallet).isNotNull();
//        assertThat(blockService.mineBlock(wallet, null, null).isSameAs(mineBlock(wallet, null, null));
    }

    @Test
    void mineBlock() throws Throwable {
        //given
        Executable e = () -> blockService.mineBlock(wallet, Arrays.asList(transaction), null);
        //then
        assertThrows(Exception.class, e);

        //given
        EncryptionUtility eu = new EncryptionUtility();
        PrivateKey privateKey = eu.generateKeyPair().getPrivate();
        OutTransaction outTransaction = new OutTransaction(wallet.address(), BigDecimal.ONE);
        Transaction transaction = new Transaction();
        transaction.setOutTransactions(Arrays.asList(outTransaction));
        //when
        when(transactionService.send(wallet, true, wallet)).thenReturn(transaction);
        when(miningService.mineNonce(anyString(), anyInt())).thenReturn("123");
        when(miningService.mineDigest(anyString(), anyInt())).thenReturn("123");
        when(wallet.getPrivateKey()).thenReturn(privateKey);
        when(encUtil.sign(anyString(), eq(privateKey))).thenReturn("123");

        //then
        Block block = blockService.mineBlock(wallet, new ArrayList<>(), null);
        assertNotNull(block);
        assertThat(block.getTransactionList()).isNotNull();
        assertThat(block.getTransactionList().size()).isEqualTo(1);
        assertThat(block.getNonce()).isEqualTo("123");
    }

    @Test
    void computeBalance() throws Exception {
        //given
        EncryptionUtility eu = new EncryptionUtility();
        PublicKey publicKey = eu.generateKeyPair().getPublic();
        OutTransaction outTransaction = new OutTransaction(publicKey, BigDecimal.ONE);
        outTransaction.setRecipientAddress(publicKey);
        Transaction transaction = spy(new Transaction());
        transaction.setOutTransactions(Arrays.asList(outTransaction));
        Block block = new Block(publicKey, Arrays.asList(transaction), null);
        //when
        when(wallet.getBlockchain()).thenReturn(block);
        wallet.setBlockchain(block);
        when(wallet.address()).thenReturn(publicKey);
        when(transaction.getWallet()).thenReturn(wallet);
        when(transaction.isInitial()).thenReturn(true);
        //then
        BigDecimal totalBalance = blockService.computeBalance(wallet);
        assertNotNull(totalBalance);
        assertThat(totalBalance).isEqualTo(BigDecimal.ONE);
    }

    @Test
    void verifyBlock() throws Exception {
        //given
        EncryptionUtility eu = new EncryptionUtility();
        PublicKey publicKey = eu.generateKeyPair().getPublic();
        OutTransaction outTransaction = new OutTransaction(publicKey, BigDecimal.ONE);
        outTransaction.setRecipientAddress(publicKey);
        Transaction transaction = spy(new Transaction());
        transaction.setOutTransactions(Arrays.asList(outTransaction));
        Block block = spy(new Block(publicKey, Arrays.asList(transaction), null));
        //when
        when(block.getHash()).thenReturn("1123");
        when(miningService.generatePrefix(2)).thenReturn("11");

        //then
        assertFalse(blockService.verifyBlock(block));

    }

    @Test
    void isNewBlockBigger() {

        assertTrue(blockService.isNewBlockBigger(block, newBlock));
    }

}