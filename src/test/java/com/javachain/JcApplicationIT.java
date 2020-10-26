package com.javachain;

import com.javachain.dto.Block;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.shell.Shell;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.shell.result.DefaultResultHandler;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.*;



@SpringBootTest(properties = {
        InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
        ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT + ".enabled=false"
})
@RunWith(SpringRunner.class)
@Import(TestApplicationRunner.class)
public class JcApplicationIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(JcApplicationIT.class);
    @Autowired
    EncryptionUtility encryptionUtility;
    @Autowired
    HashingUtility hashingUtility;
    @Autowired
    MiningService miningService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    BlockService blockService;
    @Autowired
    protected WalletService walletService;
    @Autowired
    private Shell shell;
    @Autowired
    private DefaultResultHandler resultHandler;


    protected Wallet patriksWallet;
    protected Wallet donnasWallet;
    protected Wallet johnsWallet;

    protected Transaction t2;
    protected Transaction t3;
    protected Transaction t4;

    @Before
    public void given() throws Exception {
        LOGGER.info("Wallets initialization started");
        initializeWallets();
        assertEquals(new BigDecimal(0), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));

        LOGGER.info("Creating a new initial / genesis block");
        Block genesisBlock = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);

        patriksWallet = walletService.syncBlockchain(patriksWallet, genesisBlock);
        johnsWallet = walletService.syncBlockchain(johnsWallet, genesisBlock);
        donnasWallet = walletService.syncBlockchain(donnasWallet, genesisBlock);

        LOGGER.info("Block (in this case just initial block) is transferred to other wallets (each wallet does that)");
        assertEquals(new BigDecimal(25), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));

        LOGGER.info("5 tokens is being set to be sent to Johns and Donnas wallet (patriks wallet UI)");
        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        johnsWallet.setAmountToBeSent(new BigDecimal(5));

        LOGGER.info("Create first real transaction - send 5 tokens to Donna and 5 to John (patriks wallet UI)");
        t2 = transactionService.send(patriksWallet, false, donnasWallet, johnsWallet);

        Block b1 = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), genesisBlock);
        assertEquals(new BigDecimal(25), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(0), blockService.computeBalance(johnsWallet));
        LOGGER.info("Miner John approves the transaction (this could be any miner - first one wins the fee)");

        patriksWallet = walletService.syncBlockchain(patriksWallet, b1);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b1);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b1);
        assertEquals(new BigDecimal(15), blockService.computeBalance(patriksWallet));
        assertEquals(new BigDecimal(5), blockService.computeBalance(donnasWallet));
        assertEquals(new BigDecimal(30), blockService.computeBalance(johnsWallet));
        LOGGER.info("Once the transaction is approved the wallets need to be synced again (syncing is ongoing/looping process)");

        LOGGER.info("------------ Repeat the process - John will send 5 tokens to Donna and Patrik -------------");
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
    public void shellTestMine() {

        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
        miningService.mineNonce("test", 1);
    }

//    @Test
//    public void shellTestCreateWallet1() throws NoSuchAlgorithmException {
//        //creates new empty wallet
//        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
//        miningService.mineNonce("test", 1);
//    }
//
//    @Test
//    public void shellTestCreateWallet2() throws NoSuchAlgorithmException {
//        //creates new empty wallet2
//        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
//        miningService.mineNonce("test", 1);
//    }
//
//    @Test
//    public void shellTestMine1() throws NoSuchAlgorithmException {
//        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
//        miningService.mineNonce("test", 1);
//    }
//
//    @Test
//    public void shellTestSyncChain() throws NoSuchAlgorithmException {
//        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
//        miningService.mineNonce("test", 1);
//    }
//
//    @Test
//    public void shellTestTransaction1() throws NoSuchAlgorithmException {
//        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
//        miningService.mineNonce("test", 1);
//    }
//
//    @Test
//    public void shellTestTransaction2() throws NoSuchAlgorithmException {
//        assertEquals("35", shell.evaluate(() -> "miner 7 1"));
//        miningService.mineNonce("test", 1);
//    }

    /**
     * Step1
     */
    @Test
    public void testNewWallet() {
        assertNotNull(patriksWallet.getPublicKey());
        assertEquals("patriks", patriksWallet.getSigner());
        assertNull(patriksWallet.getSignature());

        assertNotNull(donnasWallet.getPublicKey());
        assertEquals("donnas", donnasWallet.getSigner());
        assertNull(donnasWallet.getSignature());
    }

    @Test
    public void simpleBlockTest() throws Exception {

        initializeWallets();

        Block b1 = blockService.mineBlock(patriksWallet, new ArrayList<>(), null);//TODO replace this with hash code //this is new block, patrik should get 25
        LOGGER.debug("Genesis block mined");

        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        t2 = transactionService.send(johnsWallet, false, donnasWallet);

        assertEquals(blockService.computeBalance(patriksWallet), new BigDecimal(0));
        assertEquals(blockService.computeBalance(donnasWallet), new BigDecimal(0));
        assertEquals(blockService.computeBalance(johnsWallet), new BigDecimal(0));

        LOGGER.debug("b1        : " + b1.getHash() + " with fee=" + transactionService.computeTotalFee(b1.getTransactionList()));

        Block b2 = blockService.mineBlock(johnsWallet, Collections.singletonList(t2), b1); // this is new block john should get 25 tokens

        patriksWallet.setAmountToBeSent(new BigDecimal(5));
        donnasWallet.setAmountToBeSent(new BigDecimal(5));
        t3 = transactionService.send(johnsWallet, false, donnasWallet, patriksWallet);

        patriksWallet.setAmountToBeSent(new BigDecimal(1));
        johnsWallet.setAmountToBeSent(new BigDecimal(8));
        t4 = transactionService.send(donnasWallet, false, johnsWallet, patriksWallet);

        assertTrue(transactionService.validateTransaction(t2));
        assertTrue(transactionService.validateTransaction(t3));
        assertTrue(transactionService.validateTransaction(t4));

        Block b3 = blockService.mineBlock(johnsWallet, Arrays.asList(t3, t4), b2); // this is new block john should get 25 tokens

        patriksWallet = walletService.syncBlockchain(patriksWallet, b3);
        johnsWallet = walletService.syncBlockchain(johnsWallet, b3);
        donnasWallet = walletService.syncBlockchain(donnasWallet, b3);

        BigDecimal patriksCoins = blockService.computeBalance(patriksWallet);
        assertEquals(patriksCoins, new BigDecimal(31)); //25 + 1 + 5 ?30
        LOGGER.debug("Patrik  has {} javacoins\n", patriksCoins);

        BigDecimal donnasCoins = blockService.computeBalance(donnasWallet);
        assertEquals(donnasCoins, new BigDecimal(1)); // 5 + 5 - 9 ?7
        LOGGER.debug("Donna  has {} javacoins\n", donnasCoins);

        BigDecimal johnsCoins = blockService.computeBalance(johnsWallet);
        assertEquals(johnsCoins, new BigDecimal(43)); // 50 - 15 + 8 = 43 ?38
        LOGGER.debug("John  has {} javacoins\n", johnsCoins);
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

        LOGGER.debug("b1        : " + initialBlock.getHash() + " with fee=" + transactionService.computeTotalFee(initialBlock.getTransactionList()));

        try {
            //Sync the wallet (t2, t21 are validated) this transaction will fail
            donnasWallet = walletService.syncBlockchain(donnasWallet, newValidatedBlock);
            johnsWallet.setAmountToBeSent(new BigDecimal(3));
            t4 = transactionService.send(donnasWallet, false, johnsWallet);
        } catch (Exception e) {
            LOGGER.debug("Expected fail : transaction 3 was not yet added to the chain (t3 was not approved)");
        }

        //TODO add to initial transaction created and check if we really need the wallet in transaction
        t4.setSignature(encryptionUtility.sign(t4.toString(), donnasWallet.getPrivateKey()));
        assertTrue(encryptionUtility.verifySignature(t4.toString(), t4.getSignature(), t4.getWallet().address()));

//        t3 transaction needs to be approved
//        blockchain needs to be validated first (in that case john is the miner and we don't take into account that more than x should approve the transaction)
        Block b2 = null;
        try { // case of not synced
            b2 = blockService.mineBlock(patriksWallet, Arrays.asList(t2, t3), initialBlock);
            LOGGER.debug("b2        : " + b2.getHash() + " with fee=" + transactionService.computeTotalFee(b2.getTransactionList()));
        } catch (Exception e) {
            LOGGER.debug("Expected fail : initialBlock and transactions are already used");
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
            LOGGER.debug(("b3        : " + b3.getHash() + " with fee=" + transactionService.computeTotalFee(b3.getTransactionList())));
        } catch (Exception e) {
            LOGGER.debug("Expected fail : initialBlock and transactions are already used");
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
