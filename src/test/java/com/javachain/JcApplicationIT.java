package com.javachain;

import com.javachain.dto.Block;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import com.javachain.util.EncryptionUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.shell.Shell;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;


@RunWith(SpringRunner.class)
@SpringBootTest
@Import(TestApplicationRunner.class)
public class JcApplicationIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcApplicationIT.class);

    @Autowired
    protected TransactionService transactionService;
    @Autowired
    protected WalletService walletService;
    @Autowired
    protected MiningService miningService;
    @Autowired
    protected EncryptionUtility encryptionUtility;
    @Autowired
    protected BlockService blockService;
    @Autowired
    private Shell shell;

    protected Wallet patriksWallet;
    protected Wallet donnasWallet;
    protected Wallet johnsWallet;

    protected Transaction t2;
    protected Transaction t3;
    protected Transaction t4;

    @Before
    public void given() throws Exception {

        initializeWallets();

        Block genesisBlock = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);

        patriksWallet = walletService.syncBlockchain(patriksWallet, genesisBlock);
        johnsWallet = walletService.syncBlockchain(johnsWallet, genesisBlock);
        donnasWallet = walletService.syncBlockchain(donnasWallet, genesisBlock);

        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        johnsWallet.setAmountToBeSent(new BigDecimal(5));
        t2 = transactionService.send(patriksWallet, false, donnasWallet, johnsWallet);

        Block b1 = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), genesisBlock);

        patriksWallet = walletService.syncBlockchain(patriksWallet, b1);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b1);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b1);

        assertEquals(new BigDecimal(15), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(5), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(30), blockService.computeBalance(johnsWallet));

        patriksWallet.setAmountToBeSent(new BigDecimal(5));
        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        t3 = transactionService.send(johnsWallet, false, donnasWallet, patriksWallet);

        Block b2 = blockService.mineBlock(johnsWallet, Collections.singletonList(t3), b1);

        patriksWallet = walletService.syncBlockchain(patriksWallet, b2);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b2);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b2);

        assertEquals(new BigDecimal(20), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(10), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(45), blockService.computeBalance(johnsWallet));

        patriksWallet.setAmountToBeSent(new BigDecimal(1));
        johnsWallet.setAmountToBeSent(new BigDecimal(8));
        t4 = transactionService.send(donnasWallet, false, johnsWallet, patriksWallet);

        Block b3 = blockService.mineBlock(donnasWallet, Collections.singletonList(t4), b2);

        patriksWallet = walletService.syncBlockchain(patriksWallet, b3);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b3);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b3);

        assertEquals(new BigDecimal(21), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(26), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(53), blockService.computeBalance(johnsWallet));

    }

    public void initializeWallets() throws Exception {
        patriksWallet = walletService.generateNewWallet("patriks");
        donnasWallet = walletService.generateNewWallet("donnas");
        johnsWallet = walletService.generateNewWallet("johns");
    }


    @Test
    public void shellTestMine() throws NoSuchAlgorithmException {

        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    @Test
    public void shellTestCreateWallet1() throws NoSuchAlgorithmException {
        //creates new empty wallet
        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    @Test
    public void shellTestCreateWallet2() throws NoSuchAlgorithmException {
        //creates new empty wallet2
        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    @Test
    public void shellTestMine1() throws NoSuchAlgorithmException {
        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    @Test
    public void shellTestSyncChain() throws NoSuchAlgorithmException {
        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    @Test
    public void shellTestTransaction1() throws NoSuchAlgorithmException {
        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    @Test
    public void shellTestTransaction2() throws NoSuchAlgorithmException {
        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

    /**
     * Step1
     */
    @Test
    public void testNewWallet() {
        assertNotNull(patriksWallet.getPublickey());
        assertEquals(patriksWallet.getSigner(), "patriks");
        assertEquals(patriksWallet.getSignature(), null);

        assertNotNull(donnasWallet.getPublickey());
        assertEquals(donnasWallet.getSigner(), "donnas");
        assertEquals(donnasWallet.getSignature(), null);
    }

    @Test
    public void simpleBlockTest() throws Exception {

        initializeWallets();

        Block b1 = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);//todo replace this with hash code //this is new block, patrik should get 25

        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        t2 = transactionService.send(johnsWallet, false, donnasWallet);

        assertEquals(blockService.computeBalance(patriksWallet), new BigDecimal(0));
        assertEquals(blockService.computeBalance(donnasWallet), new BigDecimal(0));
        assertEquals(blockService.computeBalance(johnsWallet), new BigDecimal(0));

        System.out.println("b1        : " + b1.getHash() + " with fee=" + transactionService.computeTotalFee(b1.getTransactionList()));

        Block b2 = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), b1); // this is new block john should get 25 coins

        patriksWallet.setAmountToBeSent(new BigDecimal(5));
        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        t3 = transactionService.send(johnsWallet, false, donnasWallet, patriksWallet);

        patriksWallet.setAmountToBeSent(new BigDecimal(1));
        johnsWallet.setAmountToBeSent(new BigDecimal(8));
        t4 = transactionService.send(donnasWallet, false, johnsWallet, patriksWallet);

        assertTrue(transactionService.validateTransaction(t2));
        assertTrue(transactionService.validateTransaction(t3));
        assertTrue(transactionService.validateTransaction(t4));

        Block b3 = blockService.mineBlock(johnsWallet, Arrays.asList(t3, t4), b2); // this is new block john should get 25 coins

        patriksWallet = walletService.syncBlockchain(patriksWallet, b3);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b3);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b3);

        BigDecimal patriksCoins = blockService.computeBalance(patriksWallet);
        assertEquals(patriksCoins, new BigDecimal(31)); //25 + 1 + 5 ?30
        System.out.println(String.format("Patrik  has %s javacoins", patriksCoins));

        BigDecimal donnasCoins = blockService.computeBalance(donnasWallet);
        assertEquals(donnasCoins, new BigDecimal(1)); // 5 + 5 - 9 ?7
        System.out.println(String.format("Donna  has %s javacoins", donnasCoins));

        BigDecimal johnsCoins = blockService.computeBalance(johnsWallet);
        assertEquals(johnsCoins, new BigDecimal(43)); // 50 - 15 + 8 = 43 ?38
        System.out.println(String.format("John  has %s javacoins", johnsCoins));
    }

    @Test
    public void testEndToEnd() throws Exception {

        initializeWallets();
//        Initial transaction
        Block initialBlock = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);

//        sync wallets
        patriksWallet = walletService.syncBlockchain(patriksWallet, initialBlock);
        johnsWallet = walletService.syncBlockchain(johnsWallet, initialBlock);
        donnasWallet = walletService.syncBlockchain(donnasWallet, initialBlock);

//        create first real transaction - send 5 coins to Donna and 5 to John
        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        johnsWallet.setAmountToBeSent(new BigDecimal(5));
        Transaction t2 = transactionService.send(patriksWallet, false, donnasWallet, johnsWallet);

        assertEquals(new BigDecimal(25), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));

//        blockchain needs to be validated first (in that case john is the miner and we don't take into account that more than x should approve the transaction)
        Block newValidatedBlock = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), initialBlock);

        assertEquals(new BigDecimal(25), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));

        patriksWallet = walletService.syncBlockchain(patriksWallet, initialBlock);
        johnsWallet = walletService.syncBlockchain(johnsWallet, initialBlock);
        donnasWallet = walletService.syncBlockchain(donnasWallet, initialBlock);

        assertEquals(new BigDecimal(25), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));


//        sync the block
        johnsWallet = walletService.syncBlockchain(johnsWallet, newValidatedBlock); //john should now have new 25 coins + 5 from patrik
        patriksWallet = walletService.syncBlockchain(patriksWallet, newValidatedBlock);


        assertEquals(new BigDecimal(15), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));

        donnasWallet = walletService.syncBlockchain(donnasWallet, newValidatedBlock);
        assertEquals(new BigDecimal(5), blockService.computeBalance(donnasWallet));


        assertEquals(new BigDecimal(30), blockService.computeBalance(johnsWallet));
//
//        once the block was approved and synced john can send 5 to donna
        Transaction t3 = transactionService.send(johnsWallet, false, donnasWallet);

        donnasWallet = walletService.syncBlockchain(donnasWallet, newValidatedBlock); // -10 s

        assertEquals(new BigDecimal(15), blockService.computeBalance(patriksWallet));//inFromMining(25)
        assertEquals(new BigDecimal(5), blockService.computeBalance(donnasWallet));//inFromPatrik(5)
        assertEquals(new BigDecimal(30), blockService.computeBalance(johnsWallet));//inFromMining()

        System.out.println("b1        : " + initialBlock.getHash() + " with fee=" + transactionService.computeTotalFee(initialBlock.getTransactionList()));

        try {
            //Sync the wallet (t2, t21 are validated) this transaction will fail
            donnasWallet = walletService.syncBlockchain(donnasWallet, newValidatedBlock);
            johnsWallet.setAmountToBeSent(new BigDecimal(3));
            t4 = transactionService.send(donnasWallet, false, johnsWallet);
        } catch (Exception e) {
            System.out.println("Expected fail : transaction 3 was not yet added to the chain (t3 was not approved)");
        }

        //TODO add to initial transaction created and check if we really need the wallet in transaction
        t4.setSignature(encryptionUtility.sign(t4.toString(), donnasWallet.getPrivateKey()));
        assertTrue(encryptionUtility.verifySignature(t4.toString(), t4.getSignature(), t4.getWallet().address()));

//        t3 transaction needs to be approved
//        blockchain needs to be validated first (in that case john is the miner and we don't take into account that more than x should approve the transaction)
        Block b2 = null;
        try { // case of not synced
            b2 = blockService.mineBlock(patriksWallet, Arrays.asList(t2, t3), initialBlock);
            System.out.println("b2        : " + b2.getHash() + " with fee=" + transactionService.computeTotalFee(b2.getTransactionList()));
        } catch (Exception e) {
            System.out.println("Expected fail : initialBlock and transactions are already used");
        }

//        b2 = newblockService.mineBlock(patriksWallet, Arrays.asList(t2, t3), initialBlock);
        //sync the new block (with approved t3 tran)
        donnasWallet = walletService.syncBlockchain(donnasWallet, b2);
        t4 = transactionService.send(donnasWallet, false, johnsWallet);

//        donnasWallet.setBlockchain(b2);
//        assertTrue(transactionService.validateTransaction(t4));
        Block b3 = null;
        try {
            b3 = blockService.mineBlock(johnsWallet, Arrays.asList(t3, t4), b2); // this is new block john should get 25 coins
            System.out.println(("b3        : " + b3.getHash() + " with fee=" + transactionService.computeTotalFee(b3.getTransactionList())));
        } catch (Exception e) {
            System.out.println("Expected fail : initialBlock and transactions are already used");
        }

        johnsWallet.setAmountToBeSent(BigDecimal.ONE);
        Transaction tx = transactionService.send(johnsWallet, false, johnsWallet);

        tx.setSignature(encryptionUtility.sign(tx.toString(), johnsWallet.getPrivateKey()));
        assertTrue(encryptionUtility.verifySignature(tx.toString(), tx.getSignature(), tx.getWallet().address()));

        Block block = blockService.mineBlock(johnsWallet, Collections.singletonList(tx), b3);

        assertFalse(blockService.verifyBlock(block));

        assertEquals(new BigDecimal(15), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(10), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(30), blockService.computeBalance(johnsWallet));

    }

}
