package com.javachain;

import com.javachain.dto.Block;
import com.javachain.dto.IncomingTransaction;
import com.javachain.dto.Transaction;
import com.javachain.util.EncryptionUtility;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The original version of this class demonstrated weaknesses of the old code (blanket
 * "initial transaction -> valid" bypass, silent signature-check skips). After the
 * verification & consensus rework those attacks are rejected, so this class now pins
 * the REJECTIONS as regression guards:
 *
 * 1) an inflated coinbase fails minting validation,
 * 2) a double spend passes standalone signature/ownership checks but is caught by
 *     chain verification.
 */
public class WeaknessesIT extends JcApplicationIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(WeaknessesIT.class);

    @Test
    public void testForgedCoinbaseAndDoubleSpendAreRejected() throws Exception {
        // state inherited from given():
        // patrik/donna/john are funded and synced to the shared b3 chain

        // 1) minting rules: a coinbase paying 999 instead of incentive+fees is
        //    structurally well-formed (>= incentive, correctly signed) - but the
        //    EXACT payout is enforced when the block is verified against the fees of
        //    its sibling transactions. Standalone checks cannot know that total,
        //    so this is proven at block level.
        Transaction fakeMint = transactionService.send(donnasWallet, true, BigDecimal.ZERO,
                new com.javachain.dto.OutgoingTransaction(donnasWallet.getPublicKey(), new BigDecimal(999)));
        assertTrue(transactionService.validateTransaction(fakeMint),
                "standalone, an overpaying coinbase looks structurally fine");

        Block fakeTip = donnasWallet.getBlockchain();
        Block forgedBlock = new Block(donnasWallet.address(),
                Collections.singletonList(fakeMint), fakeTip);
        String hashingMessage = forgedBlock.getHashingMessage();
        forgedBlock.setNonce(miningService.mineNonce(hashingMessage, 2));
        forgedBlock.setHash(miningService.mineDigest(hashingMessage, 2));
        assertFalse(blockService.verifyBlock(forgedBlock),
                "a coinbase not paying exactly incentive+fees must be rejected by block verification");

        // ...and so is a coinbase UNDERPAYING the miner (would silently burn money)
        Transaction underpayingMint = transactionService.send(donnasWallet, true, BigDecimal.ZERO,
                new com.javachain.dto.OutgoingTransaction(donnasWallet.getPublicKey(), new BigDecimal(10)));
        assertFalse(transactionService.validateTransaction(underpayingMint),
                "coinbase below the block incentive must be rejected standalone");

        // 2) double spend: donna re-spends the output t3 already consumed.
        //    Hand-built so its shape is exactly what an attacker would forge (a real
        //    send() would attach donna's actual change output). Signature and
        //    ownership are perfectly valid here - the theft is only visible at CHAIN
        //    level, which is exactly what verifyBlock() must catch.
        Transaction doubleSpend = new Transaction();
        doubleSpend.setWallet(donnasWallet);
        doubleSpend.setInitial(false);
        doubleSpend.setFee(BigDecimal.ZERO);
        doubleSpend.setOutgoingTransactions(Collections.singletonList(
                new com.javachain.dto.OutgoingTransaction(patriksWallet.getPublicKey(), BigDecimal.ONE)));
        doubleSpend.setIncomingTransactions(Collections.singletonList(new IncomingTransaction(t3, 0)));
        doubleSpend.setSignature(new EncryptionUtility()
                .sign(doubleSpend.getCanonicalPayload(), donnasWallet.getPrivateKey()));

        assertTrue(transactionService.validateTransaction(doubleSpend),
                "standalone, the transaction looks perfectly valid");

        Block tip = donnasWallet.getBlockchain();
        Block cheatBlock = blockService.mineBlock(donnasWallet, Collections.singletonList(doubleSpend), tip);
        LOGGER.info("cheat block mined: {}", cheatBlock.getHash());

        // mining validates transactions standalone; the double spend is caught when
        // the produced block is verified against the chain
        assertFalse(blockService.verifyBlock(cheatBlock),
                "chain verification must detect the double spend");
    }
}
