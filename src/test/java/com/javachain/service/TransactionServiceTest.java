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
        //given - a wallet owning one 10-coin output; it spends 1 and gets 9 change
        EncryptionUtility eu = new EncryptionUtility();
        java.security.KeyPair keyPair = eu.generateKeyPair();
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        Transaction parent = new Transaction(); // funding tx paying senderWallet 10 coins
        parent.setInitial(true);
        parent.setOutgoingTransactions(Collections.singletonList(
                new OutgoingTransaction(publicKey, BigDecimal.TEN)));
        Block chain = new Block(publicKey, Collections.singletonList(parent), null);

        when(senderWallet.getBlockchain()).thenReturn(chain);
        when(senderWallet.getPublicKey()).thenReturn(publicKey);
        when(senderWallet.address()).thenReturn(publicKey);
        when(senderWallet.getPrivateKey()).thenReturn(privateKey);
        when(encryptionUtility.sign(anyString(), eq(privateKey))).thenReturn("123");

        //when
        Transaction transaction = transactionService.send(senderWallet, false, BigDecimal.ZERO,
                new OutgoingTransaction(publicKey, BigDecimal.ONE));

        //then - signed by the sender, with value conserved: 10 = 1 transferred + 9 change
        assertNotNull(transaction);
        assertEquals(transaction.getWallet(), senderWallet);
        assertEquals(2, transaction.getOutgoingTransactions().size());
        assertEquals(BigDecimal.ZERO, transaction.getFee());
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
    void validateCoinbaseWithoutOutputsIsInvalid() throws Exception {
        //when
        when(transaction.isInitial()).thenReturn(true);
        //then - coinbases follow minting rules now, an empty one is NOT auto-valid
        assertFalse(transactionService.validateTransaction(transaction));
    }

    @Test
    void validateValidCoinbase() throws Exception {
        //given - a properly mined coinbase: self-payment of the block incentive, signed
        EncryptionUtility eu = new EncryptionUtility();
        java.security.KeyPair keyPair = eu.generateKeyPair();
        Wallet miner = new Wallet(keyPair.getPrivate(), keyPair.getPublic(), "miner", null);
        when(encryptionUtility.sign(anyString(), eq(keyPair.getPrivate())))
                .thenAnswer(inv -> eu.sign(inv.getArgument(0), keyPair.getPrivate()));
        when(encryptionUtility.verifySignature(anyString(), anyString(), eq(keyPair.getPublic())))
                .thenAnswer(inv -> eu.verifySignature(inv.getArgument(0), inv.getArgument(1), keyPair.getPublic()));
        Transaction coinbase = transactionService.send(miner, true, BigDecimal.ZERO,
                new com.javachain.dto.OutgoingTransaction(miner.getPublicKey(),
                        com.javachain.service.Consensus.BLOCK_INCENTIVE));
        //then
        assertTrue(transactionService.validateTransaction(coinbase));
    }

    @Test
    void validateTransaction1() throws Exception {
        //given
        //when
        when(transaction.getIncomingTransactions()).thenReturn(Collections.singletonList(inTransaction));
        //then - unresolvable input reference => invalid, no silent skip anymore
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
