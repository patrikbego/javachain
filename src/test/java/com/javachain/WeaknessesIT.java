package com.javachain;

import com.javachain.dto.Block;
import com.javachain.dto.Transaction;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WeaknessesIT extends JcApplicationIT {

    @Test
    public void testInvalidSignature() throws Exception {

        initializeWallets();

        Block b1 = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);//todo replace this with hash code //this is new block, patrik should get 25

        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        t3 = transactionService.send(johnsWallet, false, donnasWallet);

        johnsWallet.setAmountToBeSent(new BigDecimal(8));
        t4 = transactionService.send(donnasWallet, false, johnsWallet);

        Block b2 = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), b1); // this is new block john should get 25 coins
//        System.out.println("b2        : " + b2.getHash() + " with fee=" + transactionService.computeTotalFee(b2.getTransactionList()));

        assertTrue(transactionService.validateTransaction(t2));
        assertTrue(transactionService.validateTransaction(t3));
        assertTrue(transactionService.validateTransaction(t4));

        patriksWallet = walletService.syncBlockchain(patriksWallet, b2);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b2);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b2);

        Block b3 = blockService.mineBlock(johnsWallet, Arrays.asList(t3, t4), b2); // this is new block john should get 25 coins

        patriksWallet = walletService.syncBlockchain(patriksWallet, b3);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b3);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b3);


        johnsWallet.setAmountToBeSent(new BigDecimal(1));
        Transaction tx = transactionService.send(johnsWallet, false, johnsWallet);

        assertTrue(transactionService.validateTransaction(tx)); //??? TODO should this be false???

        Block block = blockService.mineBlock(johnsWallet, Collections.singletonList(tx), b3);

        assertFalse(blockService.verifyBlock(block));
    }


}
