package com.javachain.persistence;

import com.javachain.dto.Block;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.service.BlockService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import com.javachain.tools.VerifyChainFile;
import org.junit.Test;
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
import java.nio.file.Files;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the point of canonical serialization end to end: a chain built inside this JVM
 * is written to disk and verified by a COMPLETELY SEPARATE java process (fresh JVM,
 * no shared memory, no Spring context) - plus a tamper scenario where rewriting the
 * file without redoing the proof-of-work is detected.
 * <p>
 * This was impossible before the data-structures rework: hashes and signatures used to
 * depend on JVM identity hash codes, so no serialized chain could ever verify again
 * after a restart.
 */
@SpringBootTest(properties = {
        InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
        ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT + ".enabled=false"
})
@RunWith(SpringRunner.class)
public class PersistenceIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceIT.class);

    @Autowired
    private WalletService walletService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;

    @Test
    public void chainFileIsVerifiableByIndependentProcess() throws Exception {
        Block tip = buildTwoBlockChain();

        assertTrue(blockService.verifyBlock(tip));

        File chainFile = Files.createTempFile("javachain-", ".chain").toFile();
        chainFile.deleteOnExit();
        FileChainStore.store(tip, chainFile);
        LOGGER.info("Saved chain to {} ({} bytes)", chainFile.getAbsolutePath(), chainFile.length());

        // In-process round trip first: balances must survive serialization exactly.
        Block reloaded = FileChainStore.load(chainFile, Block.class);
        assertTrue(blockService.verifyBlock(reloaded));

        Wallet originalView = new Wallet(null, tip.getMinersAddress(), "original", tip);
        Wallet reloadedView = new Wallet(null, reloaded.getMinersAddress(), "reloaded", reloaded);
        assertEquals(blockService.computeBalance(originalView),
                blockService.computeBalance(reloadedView),
                "balance changed across persistence round trip");

        // Now the real thing: a fresh JVM verifies the file from scratch.
        ProcessLauncher.ProcessResult result = runVerifier(chainFile);
        LOGGER.info("Verifier said: {}", result.output.trim());
        assertTrue(result.output.contains("CHAIN_VALID=true"),
                "independent process must accept the saved chain, got: " + result.output);
        assertEquals(0, result.exitCode, "verifier exit code for a valid chain");
    }

    @Test
    public void tamperedChainFileIsRejectedByIndependentProcess() throws Exception {
        Block tip = buildTwoBlockChain();
        File chainFile = Files.createTempFile("javachain-tampered-", ".chain").toFile();
        chainFile.deleteOnExit();
        FileChainStore.store(tip, chainFile);

        // Attacker rewrites history WITHOUT redoing proof-of-work: nudge the genesis
        // nonce and save again. The stored hash no longer matches the recomputed work.
        Block tampered = FileChainStore.load(chainFile, Block.class);
        Block genesis = tampered;
        while (genesis.getPreviousBlock() != null) {
            genesis = genesis.getPreviousBlock();
        }
        genesis.setNonce(String.valueOf(Long.parseLong(genesis.getNonce()) + 1));
        FileChainStore.store(tampered, chainFile);

        assertFalse(blockService.verifyBlock(tampered),
                "locally rewritten history must already fail verification");

        ProcessLauncher.ProcessResult result = runVerifier(chainFile);
        LOGGER.info("Verifier said: {}", result.output.trim());
        assertTrue(result.output.contains("CHAIN_VALID=false"),
                "independent process must reject the tampered chain, got: " + result.output);
        assertTrue(result.exitCode != 0, "verifier exit code for an invalid chain");
    }

    // ------------------------------------------------------------------ helpers

    /**
     * Genesis (patrik mines, 25 coins) + one spending block (patrik -> 7 coins to donna,
     * mined by john). Small enough to mine instantly, rich enough to exercise coinbase,
     * spend signature and double-spend bookkeeping.
     */
    private Block buildTwoBlockChain() throws Exception {
        Wallet patrik = walletService.generateNewWallet("persist-patrik");
        Wallet donna = walletService.generateNewWallet("persist-donna");
        Wallet john = walletService.generateNewWallet("persist-john");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);
        john = walletService.syncBlockchain(john, genesis);
        donna = walletService.syncBlockchain(donna, genesis);

        Transaction t2 = transactionService.send(patrik, false, BigDecimal.ZERO,
                new com.javachain.dto.OutgoingTransaction(donna.getPublicKey(), new BigDecimal(7)));
        Block b1 = blockService.mineBlock(john, Collections.singletonList(t2), genesis);

        patrik = walletService.syncBlockchain(patrik, b1);
        john = walletService.syncBlockchain(john, b1);
        donna = walletService.syncBlockchain(donna, b1);
        return b1;
    }

    /**
     * Launches {@link VerifyChainFile} in a brand-new JVM against the given file and
     * collects its stdout and exit code.
     */
    private ProcessLauncher.ProcessResult runVerifier(File chainFile) throws Exception {
        return ProcessLauncher.launch(VerifyChainFile.class, 120, chainFile.getAbsolutePath());
    }
}
