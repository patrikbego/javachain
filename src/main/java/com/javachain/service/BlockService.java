package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.CanonicalSerializer;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The {@code BlockService} class is used for block related functionalities.
 * <p>
 * It has supporting methods for mining, computing balance and verification.
 * MineBlock method solves the nonce of the block and adds fee to the miner.
 * ComputeBalance method returns balance of the whole block chain.
 * VerifyBlock method verifies the whole block chain if it was not corrupted or hacked.
 */
@Service
public class BlockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockService.class);

    final EncryptionUtility encryptionUtility;

    final TransactionService transactionService;

    final HashingUtility hashingUtility;

    final MiningService miningService;

    @Autowired
    public BlockService(EncryptionUtility encryptionUtility, TransactionService transactionService,
                        HashingUtility hashingUtility, MiningService miningService) {
        this.encryptionUtility = encryptionUtility;
        this.transactionService = transactionService;
        this.hashingUtility = hashingUtility;
        this.miningService = miningService;
    }

    /**
     * Validates the transactions and assigning it to a block.
     * Each block needs a nonce and a hash solution before
     * it is successfully approved.
     *
     * @param wallet Wallet
     * @param transactions List<Transactions>
     * @param previousBlock Block
     * @return Block
     * @throws SignatureException in case
     */
    public Block mineBlock(Wallet wallet, List<Transaction> transactions, Block previousBlock)
            throws SignatureException {
        if (transactions != null && !transactions.isEmpty()) {
            for (Transaction tr : transactions) {
                if (!transactionService.validateTransaction(tr))
                    throw new SecurityException("Invalid transaction present");
            }
        }

        Block block = new Block(wallet.address(), null, previousBlock);

        if (previousBlock != null) {
            while (previousBlock != null) {
                if (!verifyBlock(previousBlock)) {
                    throw new SecurityException("Invalid block present");
                }
                previousBlock = previousBlock.getPreviousBlock();
            }
        }

//        Wallet systemWallet = SystemWallet.getInstance();

        wallet.setAmountToBeSent(Consensus.BLOCK_INCENTIVE);
        Transaction miningTransaction = transactionService.send(wallet, true, wallet);

        List<Transaction> trs = new ArrayList<>();

        miningTransaction.setSignature(encryptionUtility.sign(miningTransaction.getCanonicalPayload(), wallet.getPrivateKey()));

        trs.add(miningTransaction);
        if (transactions != null) {
            trs.addAll(transactions);
        }

        block.setTransactionList(trs);
        String hashingMessage = block.getHashingMessage();
        block.setNonce(miningService.mineNonce(hashingMessage, Consensus.DIFFICULTY));
        block.setHash(miningService.mineDigest(hashingMessage, Consensus.DIFFICULTY));

        return block;
    }

    /**
     * Computes the balance of the given wallet.
     *
     * @param wallet Wallet
     * @return BigDecimal
     */
    public BigDecimal computeBalance(Wallet wallet) {
        BigDecimal income = computeTotalIncome(wallet);
        BigDecimal outcome = computeTotalOutcome(wallet);

        return income.subtract(outcome);
    }

    private BigDecimal computeTotalOutcome(Wallet wallet) {
        BigDecimal outcome = BigDecimal.ZERO;
        Block block = wallet.getBlockchain();
        while (block != null) {
            for (Transaction transaction : block.getTransactionList()) {
                for (OutgoingTransaction outTransaction : transaction.getOutgoingTransactions()) {
                    if (wallet.address().equals(transaction.getWallet().address()) && !transaction.isInitial()) {
                        outcome = outcome.add(outTransaction.getAmount());
                    }
                }

            }
            block = block.getPreviousBlock();
        }
        return outcome;
    }

    private BigDecimal computeTotalIncome(Wallet wallet) {
        BigDecimal income = BigDecimal.ZERO;
        Block block = wallet.getBlockchain();
        while (block != null) {
            for (Transaction transaction : block.getTransactionList()) {
                for (OutgoingTransaction outTransaction : transaction.getOutgoingTransactions()) {
                    if (wallet.address().equals(outTransaction.getRecipientAddress())) {
                        income = income.add(outTransaction.getAmount());
                    }
                }

            }
            block = block.getPreviousBlock();
        }
        return income;
    }

    public boolean verifyBlock(Block block) throws SignatureException {
        return verifyBlock(new HashSet<>(), block);
    }

    /**
     * Verifies an entire chain, walking from the newest block towards genesis. For every
     * block it checks:
     * <ol>
     *     <li>the stored hash really is sha256(canonical payload + nonce) - real
     *     proof-of-work, not just a string with the right prefix,</li>
     *     <li>every transaction is valid (coinbase rules or ownership-proofed spend),</li>
     *     <li>no output is spent twice anywhere in the chain,</li>
     *     <li>exactly one coinbase exists and it sits at index 0.</li>
     * </ol>
     */
    private boolean verifyBlock(Set<String> usedOutputReferences, Block block)
            throws SignatureException {
        if (block == null) {
            return false;
        }

        String prefix = miningService.generatePrefix(Consensus.DIFFICULTY);

        while (block != null) {
            String hash = block.getHash();
            if (hash == null || !hash.startsWith(prefix)) {
                LOGGER.info("Block hash ({}) doesn't start with prefix {}", hash, prefix);
                return false;
            }

            // Proof-of-work must be real: recompute sha256(payload|nonce=X) and require
            // equality with the stored hash. Earlier versions only checked the prefix,
            // so any fabricated "11..." string passed as valid work.
            String recomputed = hashingUtility.hexHash(block.getHashingMessage() + block.getNonce());
            if (recomputed == null || !recomputed.equals(hash)) {
                LOGGER.info("Block hash {} does not match recomputed proof-of-work {}", hash, recomputed);
                return false;
            }

            if (verifyListOfTransactions(usedOutputReferences, block)) {
                return false;
            }

            if (verifyGenesisTransaction(block)) {
                return false;
            }
            block = block.getPreviousBlock();
        }

        return true;
    }

    private boolean verifyGenesisTransaction(Block block) {
        for (int i = 0; i < block.getTransactionList().size(); i++) {
            if (i == 0 && !block.getTransactionList().get(i).isInitial()) {
                LOGGER.info("Non-genesis transaction at index 0");
                return true;
            } else if (i != 0 && block.getTransactionList().get(i).isInitial()) {
                LOGGER.info("GenesisTransaction (hash={}) at index {} != 0",
                        block.getTransactionList().get(i).hashCode(), i);
                return true;
            }

        }
        return false;
    }

    /**
     * Validates all transactions of one block and records spent outputs as
     * {@code parentTxId:outputIndex} references. Returning true signals FAILURE (an
     * invalid or double-spending transaction was found).
     */
    private boolean verifyListOfTransactions(Set<String> usedOutputReferences, Block block)
            throws SignatureException {
        for (Transaction tr : block.getTransactionList()) {
            if (!transactionService.validateTransaction(tr))
                return true;
            List<IncomingTransaction> ins = tr.getIncomingTransactions();
            if (ins == null) continue;
            for (IncomingTransaction intr : ins) {
                String reference = CanonicalSerializer.transactionId(intr.getTransaction())
                        + ":" + intr.getOutPutIndex();
                if (!usedOutputReferences.add(reference)) {
                    LOGGER.info("Double spend detected - output already spent: {}", reference);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Fork choice: a candidate chain replaces the local one only when it is STRICTLY
     * longer, or when it is literally the same chain (idempotent re-sync of an identical
     * tip). The old {@code >=} rule allowed a wallet's chain to be silently replaced by
     * a different fork of equal length.
     */
    public boolean isNewBlockBigger(Block walletsBlockChain, Block newBlockChain) {
        int oldblockCount = countBlocks(walletsBlockChain);
        int newblockCount = countBlocks(newBlockChain);
        if (newblockCount > oldblockCount) {
            return true;
        }
        return newblockCount == oldblockCount && walletsBlockChain != null
                && Objects.equals(walletsBlockChain.getHash(), newBlockChain.getHash());
    }

    private int countBlocks(Block walletsBlockChain) {
        int counter = 0;
        while (walletsBlockChain != null) {
            counter++;
            walletsBlockChain = walletsBlockChain.getPreviousBlock();
        }
        return counter;
    }

}
