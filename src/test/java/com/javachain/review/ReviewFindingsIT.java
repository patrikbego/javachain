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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression guards for the three structural weaknesses found during the original
 * review. Each test used to DEMONSTRATE the exploit succeeding; since the verification
 * and consensus rework they must all demonstrate REJECTION instead.
 *
 * 1) Stored PoW hash is re-computed during verification -> fabricated hashes fail.
 * 2) Coinbase transactions follow minting rules -> unlimited minting fails validation.
 * 3) Spend signatures are verified against the referenced output's owner -> a third
 *    party cannot spend someone else's output.
 */
@SpringBootTest(properties = {
        InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
        ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT + ".enabled=false"
})
@RunWith(SpringRunner.class)
public class ReviewFindingsIT {

    @Autowired
    private WalletService walletService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private MiningService miningService;

    /**
     * Finding 1: verifyBlock() recomputes sha256(payload|nonce) and compares it with the
     * stored hash, so pretending to have mined is no longer enough.
     */
    @Test
    public void forgedProofOfWorkIsRejected() throws Exception {
        Wallet miner = walletService.generateNewWallet("pow-review-miner");
        Block genesis = blockService.mineBlock(miner, Collections.emptyList(), null);
        assertTrue(blockService.verifyBlock(genesis));

        // Pretend we mined without doing any work at all:
        genesis.setNonce("never-mined");
        genesis.setHash("11deadbeefcafebabe"); // starts with difficulty-2 prefix "11"
        assertFalse(blockService.verifyBlock(genesis),
                "a fabricated hash must NOT pass verification");
    }

    /**
     * Finding 2: the minting rules hold. A hand-built "coinbase" paying itself
     * 1,000,000 tokens is structurally well-formed standalone (>= incentive, correctly
     * signed) - but the exact payout (incentive plus sibling fees) is enforced when
     * the containing block is verified, so unlimited minting cannot be mined.
     */
    @Test
    public void unlimitedMintingViaForgedCoinbaseIsRejected() throws Exception {
        Wallet attacker = walletService.generateNewWallet("mint-review-attacker");

        Block genesis = blockService.mineBlock(attacker, Collections.emptyList(), null);
        attacker = walletService.syncBlockchain(attacker, genesis);

        // Exactly what mineBlock() does internally - except the reward amount:
        Transaction mint = transactionService.send(attacker, true, BigDecimal.ZERO,
                new OutgoingTransaction(attacker.getPublicKey(), new BigDecimal(1_000_000)));

        assertTrue(transactionService.validateTransaction(mint),
                "standalone, an overpaying coinbase passes structural checks");

        Block forged = new Block(attacker.address(), Collections.singletonList(mint), genesis);
        String hashingMessage = forged.getHashingMessage();
        forged.setNonce(miningService.mineNonce(hashingMessage, 2));
        forged.setHash(miningService.mineDigest(hashingMessage, 2));

        assertFalse(blockService.verifyBlock(forged),
                "block verification must reject a coinbase not paying incentive+fees");
    }

    /**
     * Finding 3: spend signatures are verified against the owner of every referenced
     * output, with no silent skip on unresolvable references - Mallory cannot spend
     * Donna's coins, neither standalone nor inside a mined block.
     */
    @Test
    public void thirdPartyCannotSpendSomeoneElsesOutput() throws Exception {
        Wallet patrik = walletService.generateNewWallet("review-patrik");
        Wallet donna = walletService.generateNewWallet("review-donna");
        Wallet mallory = walletService.generateNewWallet("review-mallory");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);

        Transaction t2 = transactionService.send(patrik, false, BigDecimal.ZERO,
                new OutgoingTransaction(donna.getPublicKey(), new BigDecimal(5)));
        Block b1 = blockService.mineBlock(patrik, Collections.singletonList(t2), genesis);

        // Mallory builds a transaction consuming Patrik->Donna's 5-coin output, paid to
        // Mallory, signed with MALLORY's key:
        Transaction stolen = new Transaction();
        stolen.setWallet(mallory);
        stolen.setInitial(false);
        stolen.setFee(BigDecimal.ZERO);
        stolen.setIncomingTransactions(Collections.singletonList(new IncomingTransaction(t2, 0)));
        stolen.setOutgoingTransactions(Collections.singletonList(
                new OutgoingTransaction(mallory.address(), new BigDecimal(5))));
        stolen.setSignature(new EncryptionUtility().sign(stolen.getCanonicalPayload(), mallory.getPrivateKey()));

        assertFalse(transactionService.validateTransaction(stolen),
                "spending Donna's output signed by Mallory must be rejected");

        // And it cannot sneak in through mining either:
        assertThrows(SecurityException.class,
                () -> blockService.mineBlock(mallory, Collections.singletonList(stolen), b1));
    }
}
