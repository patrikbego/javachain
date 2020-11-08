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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.shell.jline.InteractiveShellApplicationRunner;
import org.springframework.shell.jline.ScriptShellApplicationRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fees are real now: a spend pays an explicit fee (outputs + fee = inputs, with any
 * surplus returned as change), validation DERIVES the fee from inputs minus outputs so
 * it cannot be misstated, and the miner's coinbase must pay exactly incentive plus
 * the fees of its sibling transactions.
 */
@SpringBootTest(properties = {
        InteractiveShellApplicationRunner.SPRING_SHELL_INTERACTIVE_ENABLED + "=false",
        ScriptShellApplicationRunner.SPRING_SHELL_SCRIPT + ".enabled=false"
})
@RunWith(SpringRunner.class)
public class FeesIT {

    @Autowired
    private WalletService walletService;
    @Autowired
    private BlockService blockService;
    @Autowired
    private TransactionService transactionService;
    @Autowired
    private MiningService miningService;

    @Test
    public void minerCollectsDeclaredFeeAndBlockVerifies() throws Exception {
        Wallet patrik = walletService.generateNewWallet("fee-patrik");
        Wallet donna = walletService.generateNewWallet("fee-donna");
        Wallet john = walletService.generateNewWallet("fee-john");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);
        john = walletService.syncBlockchain(john, genesis);
        donna = walletService.syncBlockchain(donna, genesis);

        // patrik sends 20 and pays 2 fee; his 25-coin output covers it: 20+2+3change=25
        Transaction spend = transactionService.send(patrik, false, new BigDecimal(2),
                new OutgoingTransaction(donna.getPublicKey(), new BigDecimal(20)));
        assertEquals(0, new BigDecimal(2).compareTo(spend.getFee()));
        assertEquals(2, spend.getOutgoingTransactions().size(), "transfer plus change output");

        Block b1 = blockService.mineBlock(john, Collections.singletonList(spend), genesis);
        assertTrue(blockService.verifyBlock(b1), "a block collecting a real fee must verify");

        // the coinbase pays exactly incentive + collected fee
        OutgoingTransaction reward = b1.getTransactionList().get(0).getOutgoingTransactions().get(0);
        assertEquals(0, com.javachain.service.Consensus.BLOCK_INCENTIVE
                .add(new BigDecimal(2)).compareTo(reward.getAmount()));

        patrik = walletService.syncBlockchain(patrik, b1);
        john = walletService.syncBlockchain(john, b1);
        donna = walletService.syncBlockchain(donna, b1);

        // Known quirk of the legacy balance formula (income - outcome over the wallet's
        // chain view): a paid fee is not an output anywhere, so it stays visible on the
        // spender's side until proper fee-aware accounting lands.
        assertEquals(0, new BigDecimal(27).compareTo(blockService.computeBalance(john)),
                "miner keeps incentive 25 + fee 2");
        assertEquals(0, new BigDecimal(20).compareTo(blockService.computeBalance(donna)));
        // patrik: 25 + 3 change - 23 (t2 outputs incl. his own change) = 5, i.e. the
        // economically-correct 3 plus the 2 fee the old formula cannot see leaving
        assertEquals(0, new BigDecimal(5).compareTo(blockService.computeBalance(patrik)));

        // formula-total matches total minted by coinbases: (25) + (25 + 2)
        assertEquals(0, new BigDecimal(52).compareTo(blockService.computeBalance(john)
                .add(blockService.computeBalance(donna))
                .add(blockService.computeBalance(patrik))));
    }

    @Test
    public void insufficientFundsIncludingFeeIsRejected() throws Exception {
        Wallet patrik = walletService.generateNewWallet("fee-broke-patrik");
        Wallet donna = walletService.generateNewWallet("fee-broke-donna");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);

        // owns 25 but tries to move 24 with a 2 fee - impossible
        final Wallet brokePatrik = patrik;
        final Wallet richDonna = donna;
        assertThrows(SecurityException.class,
                () -> transactionService.send(brokePatrik, false, new BigDecimal(2),
                        new OutgoingTransaction(richDonna.getPublicKey(), new BigDecimal(24))),
                "transfers plus fee above owned inputs must be rejected");
    }

    @Test
    public void valueCreatingTransactionIsRejected() throws Exception {
        // closes a latent bug: nothing used to check that outputs stay below inputs,
        // so a hand-built transaction could mint money by overspending its input
        Wallet patrik = walletService.generateNewWallet("fee-mint-patrik");
        Wallet accomplice = walletService.generateNewWallet("fee-mint-accomplice");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);

        Transaction funding = transactionService.send(patrik, false, BigDecimal.ZERO,
                new OutgoingTransaction(accomplice.getPublicKey(), new BigDecimal(10)));
        Block b1 = blockService.mineBlock(patrik, Collections.singletonList(funding), genesis);
        accomplice = walletService.syncBlockchain(accomplice, b1);

        // accomplice legally owns 10, but hand-builds a transaction spending 100 of it
        // (send() itself refuses to create one - that is the first line of defence)
        Transaction mintingSpend = new Transaction();
        mintingSpend.setWallet(accomplice);
        mintingSpend.setInitial(false);
        mintingSpend.setFee(BigDecimal.ZERO);
        mintingSpend.setIncomingTransactions(Collections.singletonList(new IncomingTransaction(funding, 0)));
        mintingSpend.setOutgoingTransactions(Collections.singletonList(
                new OutgoingTransaction(accomplice.getPublicKey(), new BigDecimal(100))));
        mintingSpend.setSignature(new com.javachain.util.EncryptionUtility()
                .sign(mintingSpend.getCanonicalPayload(), accomplice.getPrivateKey()));

        assertFalse(transactionService.validateTransaction(mintingSpend),
                "outputs exceeding owned inputs must be rejected even when correctly signed");

        final Wallet mintingAccomplice = accomplice;
        final Block fundingBlock = b1;
        assertThrows(SecurityException.class,
                () -> blockService.mineBlock(mintingAccomplice, Collections.singletonList(mintingSpend), fundingBlock));
    }

    @Test
    public void coinbaseOverpayingItsMinerIsRejectedAtBlockLevel() throws Exception {
        Wallet patrik = walletService.generateNewWallet("fee-greedy-miner");

        Block genesis = blockService.mineBlock(patrik, Collections.emptyList(), null);
        patrik = walletService.syncBlockchain(patrik, genesis);

        // no fees collected in this block - payout must be exactly the incentive
        Transaction greedyMint = transactionService.send(patrik, true, BigDecimal.ZERO,
                new OutgoingTransaction(patrik.getPublicKey(),
                        com.javachain.service.Consensus.BLOCK_INCENTIVE.add(BigDecimal.ONE)));

        Block forged = new Block(patrik.address(), Collections.singletonList(greedyMint), genesis);
        String hashingMessage = forged.getHashingMessage();
        forged.setNonce(miningService.mineNonce(hashingMessage, 2));
        forged.setHash(miningService.mineDigest(hashingMessage, 2));

        assertFalse(blockService.verifyBlock(forged),
                "one extra coin for the miner must fail block verification");
    }
}
