package com.javachain.persistence;

import com.javachain.dto.Block;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.service.BlockService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import com.javachain.tools.ChainNode;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The network moment: TWO independent JVM peers share nothing but a directory of
 * files. They start from the same genesis, mine CONFLICTING blocks - each confirming
 * a different leg of a double spend - fight the fork, and resolve it through the
 * consensus rules alone.
 *
 * After both peers shut down:
 * - every published chain verifies from scratch,
 * - the peers agree on history: the shorter published chain is an exact prefix of the
 *   longer one (they may simply differ in how many blocks they mined afterwards),
 * - exactly one leg of the double spend is confirmed anywhere - the other was orphaned
 *   together with its fork,
 * - donna's coins were spent exactly once, network-wide.
 */
@SpringBootTest(properties = {
        InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
        ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT + ".enabled=false"
})
@RunWith(SpringRunner.class)
public class PeerSyncIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PeerSyncIT.class);

    @Rule
    public TemporaryFolder networkFolder = new TemporaryFolder();

    @Autowired
    private WalletService walletService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;

    @Test
    public void twoPeersResolveDoubleSpendThroughConsensus() throws Exception {
        // ---------------------------------------------------------------- setup
        // trusted initializer: keys, funded chain, conflicting mempool entries
        Wallet patrik = walletService.generateNewWallet("net-patrik");
        Wallet john = walletService.generateNewWallet("net-john");
        Wallet donna = walletService.generateNewWallet("net-donna");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);
        john = walletService.syncBlockchain(john, genesis);
        donna = walletService.syncBlockchain(donna, genesis);

        donna.setAmountToBeSent(new BigDecimal(10));
        Transaction funding = transactionService.send(patrik, false, donna);
        Block funded = blockService.mineBlock(john, Collections.singletonList(funding), genesis);
        patrik = walletService.syncBlockchain(patrik, funded);
        john = walletService.syncBlockchain(john, funded);
        donna = walletService.syncBlockchain(donna, funded);

        // two spends of the SAME output - one per peer's mempool.
        // (Quirk of the original API: send() takes the amount from the RECEIVING
        // wallet's amountToBeSent, so the recipient sets how much they collect.)
        patrik.setAmountToBeSent(new BigDecimal(10));
        Transaction spendToPatrik = transactionService.send(donna, false, patrik);
        john.setAmountToBeSent(new BigDecimal(10));
        Transaction spendToJohn = transactionService.send(donna, false, john);
        assertNotEquals(spendToPatrik.getSignature(), spendToJohn.getSignature());

        File netDir = networkFolder.getRoot();
        FileChainStore.store(patrik, new File(netDir, "patrik.wallet"));
        FileChainStore.store(new Transaction[]{spendToPatrik}, new File(netDir, "patrik.mempool"));
        FileChainStore.store(john, new File(netDir, "john.wallet"));
        FileChainStore.store(new Transaction[]{spendToJohn}, new File(netDir, "john.mempool"));

        // --------------------------------------------------- run the two nodes
        // launched concurrently: both will mine block #2 on top of the same genesis -
        // a genuine fork battle over which spend gets confirmed
        ProcessLauncher.ProcessResult patrikResult =
                ProcessLauncher.launch(ChainNode.class, 120,
                        "patrik", netDir.getAbsolutePath(), "3", "200");
        ProcessLauncher.ProcessResult johnResult =
                ProcessLauncher.launch(ChainNode.class, 120,
                        "john", netDir.getAbsolutePath(), "5", "250");
        LOGGER.info("patrik node:\n{}", patrikResult.output);
        LOGGER.info("john node:\n{}", johnResult.output);

        assertEquals(0, patrikResult.exitCode, "patrik node exit code");
        assertEquals(0, johnResult.exitCode, "john node exit code");

        // ------------------------------------------------------------ consensus
        Block patrikTip = FileChainStore.load(new File(netDir, "patrik.chain"), Block.class);
        Block johnTip = FileChainStore.load(new File(netDir, "john.chain"), Block.class);

        assertTrue(blockService.verifyBlock(patrikTip), "patrik's published chain must be valid");
        assertTrue(blockService.verifyBlock(johnTip), "john's published chain must be valid");

        // agreement on history: the shorter chain is an exact prefix of the longer one
        Block shorter = height(patrikTip) <= height(johnTip) ? patrikTip : johnTip;
        Block longer = shorter == patrikTip ? johnTip : patrikTip;
        Set<String> longerHashes = collectHashes(longer);
        assertTrue(longerHashes.contains(shorter.getHash()),
                "peers must agree on history: "
                        + height(shorter) + "-block chain must extend into the "
                        + height(longer) + "-block chain");
        LOGGER.info("history agrees: shorter chain (h={}) is a prefix of longer chain (h={})",
                height(shorter), height(longer));

        // ...and the fork battle picked exactly one winner for the double spend
        Set<String> confirmedTransactionIds = collectConfirmedTransactionIds(longer);
        boolean patrikLegConfirmed = confirmedTransactionIds.contains(spendToPatrik.getId());
        boolean johnLegConfirmed = confirmedTransactionIds.contains(spendToJohn.getId());
        assertTrue(patrikLegConfirmed ^ johnLegConfirmed,
                "exactly one leg of the double spend must survive, got patrik="
                        + patrikLegConfirmed + " john=" + johnLegConfirmed);
        LOGGER.info("double spend resolved in favour of {}", patrikLegConfirmed ? "patrik" : "john");

        // donna spent her only UTXO exactly once, network-wide
        Wallet donnaView = new Wallet(null, donna.getPublicKey(), "donna-view", longer);
        assertEquals(new BigDecimal(0), blockService.computeBalance(donnaView),
                "donna must not own anything after both conflicting spends raced");

        LOGGER.info("CONSENSUS REACHED: longest published chain h={} tip {}",
                height(longer), longer.getHash());
    }

    private Set<String> collectConfirmedTransactionIds(Block tip) {
        Set<String> ids = new HashSet<>();
        Block block = tip;
        while (block != null && block.getTransactionList() != null) {
            for (Transaction transaction : block.getTransactionList()) {
                if (transaction.getId() != null) {
                    ids.add(transaction.getId());
                }
            }
            block = block.getPreviousBlock();
        }
        return ids;
    }

    private Set<String> collectHashes(Block tip) {
        Set<String> hashes = new HashSet<>();
        Block block = tip;
        while (block != null) {
            hashes.add(block.getHash());
            block = block.getPreviousBlock();
        }
        return hashes;
    }

    private int height(Block tip) {
        int height = 0;
        while (tip != null) {
            height++;
            tip = tip.getPreviousBlock();
        }
        return height;
    }
}
