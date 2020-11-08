package com.javachain;

import com.javachain.dto.Block;
import com.javachain.dto.OutgoingTransaction;
import com.javachain.dto.Wallet;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class BlockServiceIT extends JcApplicationIT {

    private Block block;
    private Wallet testWallet;

    @Test
    public void mineBlock() throws Exception {
        //given - after given() patrik's only unspent output is the 1-coin change of t4
        testWallet = walletService.generateNewWallet("testWallet");
        t2 = transactionService.send(patriksWallet, false, BigDecimal.ZERO,
                new OutgoingTransaction(johnsWallet.getPublicKey(), BigDecimal.ONE));
        //when new block
        block = blockService.mineBlock(testWallet, new ArrayList<>(), null);
        OutgoingTransaction outTransaction = block.getTransactionList().get(0).getOutgoingTransactions().get(0);
        //then
        assertEquals(1, block.getTransactionList().size());
        assertEquals(outTransaction.getAmount(), new BigDecimal(25));
        assertEquals(outTransaction.getRecipientAddress(), testWallet.address());

        //when existing block
        Block block1 = blockService.mineBlock(testWallet, Collections.singletonList(t2), block);
        //then
        assertEquals(2, block1.getTransactionList().size());
    }

    @Test
    public void computeBalance() throws Exception {
        //given
        testWallet = walletService.generateNewWallet("testWallet");
        block = blockService.mineBlock(testWallet, new ArrayList<>(), null);
        //when initial block
        //then
        testWallet = walletService.syncBlockchain(testWallet, block);
        assertEquals(blockService.computeBalance(testWallet), new BigDecimal(25));

        //when existing block
        Block b1 = blockService.mineBlock(testWallet, Collections.singletonList(t2), block);
        testWallet = walletService.syncBlockchain(testWallet, b1);
        //then
        assertEquals(blockService.computeBalance(testWallet), new BigDecimal(50));
    }

    @Test
    public void verifyBlock() throws Exception {
        //TODO implement following checks https://en.bitcoin.it/wiki/Protocol_rules#.22tx.22_messages
        //given / when
        //then
        assertFalse(blockService.verifyBlock(block));

        //given
        testWallet = walletService.generateNewWallet("testWallet");
        block = blockService.mineBlock(testWallet, new ArrayList<>(), null);

        assertTrue(blockService.verifyBlock(block));
    }

}
