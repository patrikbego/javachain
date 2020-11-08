package com.javachain.review;

import com.javachain.dto.Block;
import com.javachain.dto.IncomingTransaction;
import com.javachain.dto.OutgoingTransaction;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import com.javachain.util.EncryptionUtility;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * NOT part of the original project. Added during an external code review to document
 * three structural weaknesses with hard evidence:
 *
 * 1) Stored PoW hash is never re-computed during verification -> forged hashes pass.
 * 2) "Initial"/coinbase-style transactions bypass ALL validation -> unlimited minting.
 * 3) Signature verification is silently skipped whenever IncomingTransaction.getRecipient()
 *    returns null, which is the common case -> anyone can spend someone else's output.
 */
@SpringBootTest(properties = {
        InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
        ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT + ".enabled=false"
})
@RunWith(SpringRunner.class)
public class ReviewFindingsIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReviewFindingsIT.class);

    @Autowired
    private WalletService walletService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private MiningService miningService;

    /**
     * Finding 1: verifyBlock() only checks hash.startsWith(prefix); it never re-computes
     * sha256(blockContents + nonce). Any string starting with "11" passes as "proof of work".
     */
    @Test
    public void forgedProofOfWorkIsAccepted() throws Exception {
        Wallet miner = walletService.generateNewWallet("pow-review-miner");
        Block genesis = blockService.mineBlock(miner, Collections.emptyList(), null);
        assertTrue(blockService.verifyBlock(genesis));

        // Pretend we mined without doing any work at all:
        genesis.setNonce("never-mined");
        genesis.setHash("11deadbeefcafebabe"); // starts with difficulty-2 prefix "11"
        assertTrue(blockService.verifyBlock(genesis),
                "A fabricated hash passes verification - PoW is decorative");
    }

    /**
     * Finding 2: validateTransaction() short-circuits to 'true' for initial transactions,
     * and the coinbase-amount check in BlockService is commented out ("TODO fix").
     * A hand-built block paying itself 1,000,000 tokens therefore verifies and syncs.
     */
    @Test
    public void unlimitedMintingViaForgedCoinbase() throws Exception {
        Wallet attacker = walletService.generateNewWallet("mint-review-attacker");

        Block genesis = blockService.mineBlock(attacker, Collections.emptyList(), null);
        attacker = walletService.syncBlockchain(attacker, genesis);

        // Exactly what mineBlock() does internally - except the reward amount:
        attacker.setAmountToBeSent(new BigDecimal(1_000_000));
        Transaction mint = transactionService.send(attacker, true, attacker);

        assertTrue(transactionService.validateTransaction(mint),
                "initial=true skips all validation, incl. amount and signature rules");

        Block forged = new Block(attacker.address(), Collections.singletonList(mint), genesis);
        String hashingMessage = forged.getHashingMessage();
        forged.setNonce(miningService.mineNonce(hashingMessage, 2));
        forged.setHash(miningService.mineDigest(hashingMessage, 2));

        assertTrue(blockService.verifyBlock(forged), "block minting 1,000,000 coins verifies");
        attacker = walletService.syncBlockchain(attacker, forged);
        assertEquals(new BigDecimal(1_000_025), blockService.computeBalance(attacker),
                "attacker now owns 1,000,025 tokens created out of thin air");
    }

    /**
     * Finding 3: validateTransaction() looks up the public key that must verify the spender's
     * signature via IncomingTransaction.getRecipient(), which searches the PARENT transaction's
     * outputs for one addressed to the PARENT'S OWN SENDER. For a normal A->B payment that
     * output does not exist, getRecipient() returns null, and the whole signature check is
     * skipped. Result: a third party can spend an output it does not own.
     */
    @Test
    public void thirdPartyCanSpendSomeoneElsesOutput() throws Exception {
        Wallet patrik = walletService.generateNewWallet("review-patrik");
        Wallet donna = walletService.generateNewWallet("review-donna");
        Wallet mallory = walletService.generateNewWallet("review-mallory");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);

        donna.setAmountToBeSent(new BigDecimal(5));
        Transaction t2 = transactionService.send(patrik, false, donna);
        Block b1 = blockService.mineBlock(patrik, Collections.singletonList(t2), genesis);

        // Mallory builds a transaction consuming Patrik->Donna's 5-coin output, paid to Mallory.
        Transaction stolen = new Transaction();
        stolen.setWallet(mallory);
        stolen.setInitial(false);
        stolen.setFee(BigDecimal.ZERO);
        stolen.setIncomingTransactions(Collections.singletonList(new IncomingTransaction(t2, 0)));
        stolen.setOutgoingTransactions(Collections.singletonList(
                new OutgoingTransaction(mallory.address(), new BigDecimal(5))));
        // Signed with MALLORY's key - Mallory is not the owner of that output:
        stolen.setSignature(new EncryptionUtility().sign(stolen.getCanonicalPayload(), mallory.getPrivateKey()));

        assertTrue(transactionService.validateTransaction(stolen),
                "transaction spending Donna's coins, signed by Mallory, is accepted");

        // And it really flows through mining + verification:
        Block b2 = blockService.mineBlock(mallory, Collections.singletonList(stolen), b1);
        assertTrue(blockService.verifyBlock(b2));
        LOGGER.info("Stolen-output transaction accepted into a verified block");
    }
}
