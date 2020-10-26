package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * Block incentive is the reward miner gets once successfully resolving the nonce.
     */
    private static final BigDecimal BLOCK_INCENTIVE = new BigDecimal(25);

    /**
     * Difficulty of finding a new block. In Bitcoin It is updated every 2016 blocks when the difficulty reset occurs.
     */
    private static final Integer DIFFICULTY = 2;

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

        wallet.setAmountToBeSent(BLOCK_INCENTIVE);
        Transaction miningTransaction = transactionService.send(wallet, true, wallet);

        List<Transaction> trs = new ArrayList<>();

        miningTransaction.setSignature(encryptionUtility.sign(miningTransaction.toString(), wallet.getPrivateKey()));

        trs.add(miningTransaction);
        if (transactions != null) {
            trs.addAll(transactions);
        }

        block.setTransactionList(trs);
        String clazz = block.toString();
        block.setNonce(miningService.mineNonce(clazz, DIFFICULTY));
        block.setHash(miningService.mineDigest(clazz, DIFFICULTY));

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
        return verifyBlock(null, block);
    }

    private boolean verifyBlock(List<OutgoingTransaction> usedOutputs, Block block)
            throws SignatureException {
        if (block == null) {
            return false;
        }

        String prefix = miningService.generatePrefix(DIFFICULTY);

        while (block != null) {
            String hash = block.getHash();
            if (!hash.startsWith(prefix)) {
                LOGGER.info("Block hash ({}}) doesn't start with prefix {}}", hash, prefix);
                return false;
            }

            if (usedOutputs == null) {
                usedOutputs = new ArrayList<>();
            }
            if (verifyListOfTransactions(usedOutputs, block)) {
                return false;
            }

//            Block block1 = blocks[1];
//            if (block1 != null && block1.getHash() == null) {
//                String clazz = block1.toString();
//                block1.setHash(miningService.mineDigest(clazz, 2));
//            }
//            if (block1 != null && !block.getHash().equals(block1.getHash())) {
//                if (!verifyBlock(usedOutputs, block.getAncestor(), blocks[1])) {
//                    LOGGER.info("Failed to validate ancestor block");
//                    return false;
//                }
//            }

            //TODO enable this
//            BigDecimal reward = transactionService.computeTotalFee(block.getTransactionList()).add(BLOCK_INCENTIVE);
//            if (tr0.getOutTransactions().get(0).getAmount().compareTo(reward) != 0) {
//                LOGGER.info(String.format("Invalid amount in transaction 0 : %s, expected %s",
//                        tr0.getOutTransactions().get(0).getAmount(), reward));
//                return false; //TODO fix - this is not working, reward is 26 and tr amount is 25
//            }

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

    private boolean verifyListOfTransactions(List<OutgoingTransaction> usedOutputs, Block block)
            throws SignatureException {
        for (Transaction tr : block.getTransactionList()) {
            if (!transactionService.validateTransaction(tr))
                return true;
            for (IncomingTransaction intr : tr.getIncomingTransactions()) {
                if (tr.isInitial() && usedOutputs.contains(intr.parentOutPut())) {
                    LOGGER.info("Transaction uses an already spent output : {} {}", intr.parentOutPut(), intr.getRecipient());
                    return true;
                }
                usedOutputs.add(intr.parentOutPut());
            }
        }
        return false;
    }

    public boolean isNewBlockBigger(Block walletsBlockChain, Block newBlockChain) {
        int oldblockCount = countBlocks(walletsBlockChain);
        int newblockCount = countBlocks(newBlockChain);
        return newblockCount >= oldblockCount;
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
