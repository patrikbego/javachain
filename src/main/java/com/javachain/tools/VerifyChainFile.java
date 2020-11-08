package com.javachain.tools;

import com.javachain.dto.Block;
import com.javachain.persistence.FileChainStore;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import com.javachain.service.TransactionService;
import com.javachain.util.EncodingUtility;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;

import java.io.File;

/**
 * Standalone verifier: loads a saved chain file and re-validates it from scratch -
 * proof-of-work, coinbase rules, ownership, double spends - without any Spring context
 * and without trusting the writing process in any way.
 * <p>
 * Exit code 0 and {@code CHAIN_VALID=true} on stdout mean the file contains a fully
 * valid chain; anything else means it does not. Run it against a file produced by
 * another JVM instance:
 *
 * <pre>
 *     java -cp jc.jar com.javachain.tools.VerifyChainFile chain.bin
 * </pre>
 */
public final class VerifyChainFile {

    private VerifyChainFile() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("usage: VerifyChainFile <chain-file>");
            System.exit(2);
        }

        // Manual wiring - the services are plain objects, Spring is optional.
        EncodingUtility encodingUtility = new EncodingUtility();
        HashingUtility hashingUtility = new HashingUtility(encodingUtility);
        MiningService miningService = new MiningService(hashingUtility, encodingUtility);
        EncryptionUtility encryptionUtility = new EncryptionUtility();
        TransactionService transactionService = new TransactionService(encryptionUtility, hashingUtility, miningService);
        BlockService blockService = new BlockService(encryptionUtility, transactionService, hashingUtility, miningService);

        Block chainTip = FileChainStore.load(new File(args[0]), Block.class);
        boolean valid = blockService.verifyBlock(chainTip);

        System.out.println("CHAIN_VALID=" + valid);
        System.exit(valid ? 0 : 1);
    }
}
