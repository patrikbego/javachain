package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.EncryptionUtility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @InjectMocks
    TransactionService transactionService;
    @Mock
    Wallet senderWallet;
    @Mock
    Wallet receiversWallet;
    @Mock
    Transaction transaction;
    @Mock
    Block block;
    @Mock
    OutgoingTransaction outTransaction;
    @Mock
    IncomingTransaction inTransaction;
    @Mock
    EncryptionUtility encryptionUtility;


    @Test
    void send() throws Exception {
        //given
        EncryptionUtility eu = new EncryptionUtility();
        PrivateKey privateKey = eu.generateKeyPair().getPrivate();
        //when
        when(senderWallet.getPrivateKey()).thenReturn(privateKey);
        when(encryptionUtility.sign(anyString(), eq(privateKey))).thenReturn("123");
        //then
        Transaction transaction = transactionService.send(senderWallet, false, receiversWallet);
        assertNotNull(transaction);
        assertEquals(transaction.getWallet(), senderWallet);
    }

    @Test
    void getPreviousInTransactions() throws Exception {
        //given
        EncryptionUtility eu = new EncryptionUtility();
        PublicKey publicKey = eu.generateKeyPair().getPublic();
        //when
        when(senderWallet.getBlockchain()).thenReturn(block);
        when(block.getTransactionList()).thenReturn(Collections.singletonList(transaction));
        when(transaction.getOutgoingTransactions()).thenReturn(Collections.singletonList(outTransaction));
        when(outTransaction.getRecipientAddress()).thenReturn(publicKey);
        when(senderWallet.getPublicKey()).thenReturn(publicKey);
        //then
        List<IncomingTransaction> inTransactions = transactionService.getPreviousInTransactions(senderWallet);
        assertEquals(1, inTransactions.size());
    }

    @Test
    void validateTransaction() throws Exception {
        //when
        when(transaction.isInitial()).thenReturn(true);
        //then
        assertTrue(transactionService.validateTransaction(transaction));
    }

    @Test
    void validateTransaction1() throws Exception {
        //given
        //when
        when(transaction.getIncomingTransactions()).thenReturn(Collections.singletonList(inTransaction));
        when(inTransaction.getRecipient()).thenReturn(outTransaction);
        //then
        assertFalse(transactionService.validateTransaction(transaction));
    }

    @Test
//TODO
    void computeTotalFee() {
        //when
        when(transaction.getFee()).thenReturn(BigDecimal.ONE);
        //then
        assertEquals(BigDecimal.ONE, transactionService.computeTotalFee(Collections.singletonList(transaction)));
    }

//    TODO @Test
//    void fee() {
//        //then
//        assertThat(transactionService.fee(Arrays.asList(inTransaction), Arrays.asList(outTransaction))).isEqualTo(BigDecimal.ZERO);
//    }

}
