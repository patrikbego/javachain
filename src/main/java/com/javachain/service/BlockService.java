package com.javachain.service;

import com.javachain.dto.*;
import com.javachain.util.EncryptionUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class BlockService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockService.class);

    @Autowired
    EncryptionUtility encryptionUtility;

    @Autowired
    TransactionService transactionService;

    @Autowired
    HashingUtility hashingUtility;

    @Autowired
    MiningService miningService;

    private static final BigDecimal BLOCK_INCENTIVE = new BigDecimal(25);

//  Difficulty of finding a new block. In Bitcoin It is updated every 2016 blocks when the difficulty reset occurs.
    private static final Integer DIFFICULTY = 2;

    public Block mineBlock(Wallet wallet, List<Transaction> transactions, Block previousBlock) throws Exception {

        if (transactions != null || transactions.size() > 0) {
            for (Transaction tr : transactions) {
                if (!transactionService.validateTransaction(tr))
                    throw new Exception("Invalid transaction present");
            }
        }

        Block block = new Block(wallet.address(), null, previousBlock);

        if (previousBlock != null) {
            while (previousBlock != null) {
                if (!verifyBlock(previousBlock)) {
                    throw new Exception("Invalid block present");
                }
                previousBlock = previousBlock.getAncestor();
            }
        }

//        Wallet systemWallet = SystemWallet.getInstance();

        wallet.setAmountToBeSent(BLOCK_INCENTIVE);
        Transaction miningTransaction = transactionService.send(wallet, true, wallet);

        List<Transaction> trs = new ArrayList<>();

        miningTransaction.setSignature(encryptionUtility.sign(miningTransaction.toString(), wallet.getPrivateKey()));

        trs.add(miningTransaction);
        trs.addAll(transactions);

        block.setTransactionList(trs);
        String clazz = block.toString();
        block.setNonce(miningService.mineNonce(clazz, DIFFICULTY));
        block.setHash(miningService.mineDigest(clazz, DIFFICULTY));

        return block;
    }

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
                for (OutTransaction outTransaction : transaction.getOutTransactions()) {
                    if (wallet.address().equals(transaction.getWallet().address()) && !transaction.isInitial()) {
                        outcome = outcome.add(outTransaction.getAmount());
                    }
                }

            }
            block = block.getAncestor();
        }
        return outcome;
    }

    private BigDecimal computeTotalIncome(Wallet wallet) {
        BigDecimal income = BigDecimal.ZERO;
        Block block = wallet.getBlockchain();
        while (block != null) {
            for (Transaction transaction : block.getTransactionList()) {
                for (OutTransaction outTransaction : transaction.getOutTransactions()) {
                    if (wallet.address().equals(outTransaction.getRecipientAddress())) {
                        income = income.add(outTransaction.getAmount());
                    }
                }

            }
            block = block.getAncestor();
        }
        return income;
    }

    public boolean verifyBlock(Block block) throws Exception {
        return verifyBlock(null, block);
    }

    private boolean verifyBlock(List<OutTransaction> usedOutputs, Block block) throws Exception {
        if (block == null) return false;
        String prefix = miningService.generatePrefix(DIFFICULTY);

        while (block != null) {
            String hash = block.getHash();
            if (!hash.startsWith(prefix)) {
                LOGGER.info(String.format("Block hash (%s) doesn't start with prefix %s", hash, prefix));
                return false;
            }

            if (usedOutputs == null)
                usedOutputs = new ArrayList<>();
            for (Transaction tr : block.getTransactionList()) {
                if (!transactionService.validateTransaction(tr))
                    return false;
                for (InTransaction intr : tr.getInTransactions()) {
                    if (tr.isInitial() && usedOutputs.contains(intr.parentOutPut())) {
                        LOGGER.info(String.format("Transaction uses an already spent output : %s %s", intr.parentOutPut().toString(), intr.getRecipient()));
                        return false;
                    }
                    usedOutputs.add(intr.parentOutPut());
                }
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

            Transaction tr0 = block.getTransactionList().get(0);
            if (!tr0.isInitial()) {
                LOGGER.info("Transaction 0 is not a GenesisTransaction");
                return false;
            }

            //TODO enable this
//            BigDecimal reward = transactionService.computeTotalFee(block.getTransactionList()).add(BLOCK_INCENTIVE);
//            if (tr0.getOutTransactions().get(0).getAmount().compareTo(reward) != 0) {
//                LOGGER.info(String.format("Invalid amount in transaction 0 : %s, expected %s",
//                        tr0.getOutTransactions().get(0).getAmount(), reward));
//                return false; //TODO fix - this is not working, reward is 26 and tr amount is 25
//            }

            for (int i = 0; i < block.getTransactionList().size(); i++) {
                if (i == 0 && !block.getTransactionList().get(i).isInitial()) {
                    LOGGER.info("Non-genesis transaction at index 0");
                    return false;
                } else if (i != 0 && block.getTransactionList().get(i).isInitial()) {
                    LOGGER.info(String.format("GenesisTransaction (hash=%s) at index %d != 0",
                            block.getTransactionList().get(i).hashCode(), i));
                    return false;
                }

            }
            block = block.getAncestor();
        }

        return true;
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
            walletsBlockChain = walletsBlockChain.getAncestor();
        }
        return counter;
    }

}
