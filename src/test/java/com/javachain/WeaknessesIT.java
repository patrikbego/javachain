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

        // 1) minting rules: a coinbase paying more than the block incentive is invalid,
        //    no matter who signs it
        donnasWallet.setAmountToBeSent(new BigDecimal(999));
        Transaction fakeMint = transactionService.send(donnasWallet, true, donnasWallet);
        assertFalse(transactionService.validateTransaction(fakeMint),
                "coinbase above the block incentive must be rejected");

        // 2) double spend: donna re-spends the output t4 already consumed.
        //    Signature and ownership are perfectly valid here - the theft is only
        //    visible at CHAIN level, which is exactly what verifyBlock() must catch.
        donnasWallet.setAmountToBeSent(new BigDecimal(1));
        Transaction doubleSpend = transactionService.send(donnasWallet, false, patriksWallet);
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
