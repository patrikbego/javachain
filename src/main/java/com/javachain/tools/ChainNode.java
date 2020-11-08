package com.javachain.tools;

import com.javachain.dto.Block;
import com.javachain.dto.IncomingTransaction;
import com.javachain.dto.Transaction;
import com.javachain.dto.Wallet;
import com.javachain.persistence.FileChainStore;
import com.javachain.service.BlockService;
import com.javachain.service.MiningService;
import com.javachain.service.TransactionService;
import com.javachain.service.WalletService;
import com.javachain.util.CanonicalSerializer;
import com.javachain.util.EncodingUtility;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A minimal peer node: the one-shot verifier turned into a running participant.
 * <p>
 * The "network" is a directory shared with other peers. Every peer owns three files:
 * {@code <name>.wallet} (keys + starting chain), {@code <name>.chain} (its current
 * tip, published atomically) and optionally {@code <name>.mempool} (a
 * {@code Transaction[]} to include in the next mined block).
 * <p>
 * Each round the node pulls every foreign chain file, validates it completely and
 * adopts it only if it is strictly longer (identical tips are idempotent no-ops,
 * equal-height forks are ignored) - then mines its own next block, including whatever
 * valid mempool transactions do not double-spend anything already confirmed.
 *
 * <pre>
 *     java -cp jc.jar com.javachain.tools.ChainNode alice /tmp/net 5 200
 * </pre>
 */
public final class ChainNode {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChainNode.class);

    private final String name;
    private final File networkDirectory;
    private final WalletService walletService;
    private final BlockService blockService;
    private Wallet wallet;
    private final File chainFile;
    private final File mempoolFile;

    private ChainNode(String name, File networkDirectory, int rounds, long delayMillis)
            throws Exception {
        this.name = name;
        this.networkDirectory = networkDirectory;
        this.chainFile = new File(networkDirectory, name + ".chain");
        this.mempoolFile = new File(networkDirectory, name + ".mempool");

        EncodingUtility encodingUtility = new EncodingUtility();
        HashingUtility hashingUtility = new HashingUtility(encodingUtility);
        MiningService miningService = new MiningService(hashingUtility, encodingUtility);
        EncryptionUtility encryptionUtility = new EncryptionUtility();
        TransactionService transactionService = new TransactionService(encryptionUtility, hashingUtility, miningService);
        BlockService blockService = new BlockService(encryptionUtility, transactionService, hashingUtility, miningService);
        this.blockService = blockService;
        this.walletService = new WalletService(encryptionUtility, hashingUtility, miningService,
                transactionService, blockService);

        this.wallet = FileChainStore.load(new File(networkDirectory, name + ".wallet"), Wallet.class);
        publish(); // announce ourselves immediately

        for (int round = 1; round <= rounds; round++) {
            pullForeignChains();
            minePendingTransactions();
            publish();
            LOGGER.info("NODE={} ROUND={} HEIGHT={} TIP={}",
                    name, round, height(wallet.getBlockchain()), shortHash(wallet.getBlockchain()));
            if (round < rounds) {
                Thread.sleep(delayMillis);
            }
        }
        System.out.println("NODE_DONE=" + name
                + " HEIGHT=" + height(wallet.getBlockchain())
                + " TIP=" + wallet.getBlockchain().getHash());
    }

    /**
     * Validates and adopts every foreign chain that is strictly better than ours.
     * All the hard decisions live in walletService.syncBlockchain: full chain
     * verification, then the strictly-longer/idempotent-tip fork rule.
     */
    private void pullForeignChains() {
        File[] files = networkDirectory.listFiles((dir, fileName) ->
                fileName.endsWith(".chain") && !fileName.equals(name + ".chain"));
        if (files == null) return;
        for (File file : files) {
            try {
                Block foreignTip = FileChainStore.load(file, Block.class);
                int heightBefore = height(wallet.getBlockchain());
                wallet = walletService.syncBlockchain(wallet, foreignTip);
                if (height(wallet.getBlockchain()) != heightBefore) {
                    LOGGER.info("NODE={} adopted chain from {} (height {} > {})",
                            name, file.getName(), height(wallet.getBlockchain()), heightBefore);
                }
            } catch (SecurityException e) {
                LOGGER.info("NODE={} ignoring {} ({})", name, file.getName(), e.getMessage());
            } catch (Exception e) {
                LOGGER.info("NODE={} could not read {}: {}", name, file.getName(), e.getMessage());
            }
        }
    }

    /**
     * Mines one block on top of our tip, including any mempool transactions that are
     * still valid - i.e. they do not spend an output already spent somewhere in our
     * current chain (the winner of a double-spend race keeps the coins).
     */
    private void minePendingTransactions() throws Exception {
        List<Transaction> pending = loadMempool();
        if (!pending.isEmpty()) {
            Set<String> confirmedReferences = collectSpentReferences(wallet.getBlockchain());
            pending.removeIf(transaction -> spendsAlreadyConfirmedOutput(transaction, confirmedReferences));
        }

        try {
            Block newTip = blockService.mineBlock(wallet, pending, wallet.getBlockchain());
            wallet.setBlockchain(newTip);
            // the mempool is consumed by a successful round even if every entry was
            // dropped as double-spending - those transactions lost the race for good
            if (mempoolFile.exists() && !mempoolFile.delete()) {
                LOGGER.warn("could not delete mempool file {}", mempoolFile.getName());
            }
        } catch (SecurityException e) {
            LOGGER.info("NODE={} skipped mining this round: {}", name, e.getMessage());
        }
    }

    private boolean spendsAlreadyConfirmedOutput(Transaction transaction, Set<String> confirmed) {
        List<IncomingTransaction> ins = transaction.getIncomingTransactions();
        if (ins == null) return false;
        for (IncomingTransaction input : ins) {
            String reference = CanonicalSerializer.transactionId(input.getTransaction())
                    + ":" + input.getOutPutIndex();
            if (confirmed.contains(reference)) {
                LOGGER.info("NODE={} drops double-spending tx from mempool ({})", name, reference);
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private List<Transaction> loadMempool() throws Exception {
        if (!mempoolFile.exists()) {
            return new ArrayList<>();
        }
        try {
            Transaction[] transactions = FileChainStore.load(mempoolFile, Transaction[].class);
            List<Transaction> result = new ArrayList<>();
            for (Transaction transaction : transactions) {
                result.add(transaction);
            }
            return result;
        } catch (Exception e) {
            LOGGER.info("NODE={} unreadable mempool, ignoring: {}", name, e.getMessage());
            return new ArrayList<>();
        }
    }

    private void publish() throws Exception {
        FileChainStore.storeAtomic(wallet.getBlockchain(), chainFile);
    }

    private static Set<String> collectSpentReferences(Block tip) {
        Set<String> references = new HashSet<>();
        Block block = tip;
        while (block != null && block.getTransactionList() != null) {
            for (Transaction transaction : block.getTransactionList()) {
                List<IncomingTransaction> ins = transaction.getIncomingTransactions();
                if (ins == null) continue;
                for (IncomingTransaction input : ins) {
                    references.add(CanonicalSerializer.transactionId(input.getTransaction())
                            + ":" + input.getOutPutIndex());
                }
            }
            block = block.getPreviousBlock();
        }
        return references;
    }

    private static int height(Block tip) {
        int height = 0;
        while (tip != null) {
            height++;
            tip = tip.getPreviousBlock();
        }
        return height;
    }

    private static String shortHash(Block tip) {
        String hash = tip.getHash();
        return hash == null ? "null" : hash.substring(0, Math.min(8, hash.length()));
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            System.out.println("usage: ChainNode <name> <network-directory> <rounds> <delay-millis>");
            System.exit(2);
        }
        new ChainNode(args[0], new File(args[1]), Integer.parseInt(args[2]), Long.parseLong(args[3]));
    }
}
