package com.javachain.service;

import com.javachain.util.EncodingUtility;
import com.javachain.util.HashingUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class MiningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiningService.class);

    @Autowired
    HashingUtility hashingUtility;

    @Autowired
    EncodingUtility encodingUtility;

    @Autowired
    TransactionService transactionService;

    /**
     * A simple demonstration on how the mining works
     *
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String demoMiner() throws NoSuchAlgorithmException {
        String message = "hello javacoin";
        for (int i = 0; i < 1000; i++) {
            String digest = encodingUtility.bytesToHex(hashingUtility.sha256(message + i));
            if (digest.startsWith("11")) {
                LOGGER.info("Found nonce : {} ", i);
                LOGGER.info("Found digest : {} ", digest);
                return digest;
            }
        }
        return null;
    }


    /**
     * For our timestamp network, we implement the proof-of-work by incrementing a nonce in the
     * block until a value is found that gives the block's hash the required zero bits. Once the CPU
     * effort has been expended to make it satisfy the proof-of-work, the block cannot be changed
     * without redoing the work. As later blocks are chained after it, the work to change the block
     * would include redoing all the blocks after it.
     * <p>
     * The more you increase the number of leading ones you require, the harder it becomes (on average) to find a nonce.
     * In bitcoin, this is called the mining difficulty. Note that bitcoin doesn't require a number of leading digits,
     * but instead requires the hash to be below a certain value. But it's the same idea.
     *
     * @param message
     * @param difficulty
     * @return
     * @throws NoSuchAlgorithmException
     */
    public String mineNonce(String message, int difficulty) throws NoSuchAlgorithmException {
        assert (difficulty >= 1);
        long nrOfIterationsToMineNonce = 0;
        String prefix = generatePrefix(difficulty);
        while (true) {
            String digest = hashingUtility.hexHash(message + nrOfIterationsToMineNonce);
            if (digest.startsWith(prefix))
                return String.valueOf(nrOfIterationsToMineNonce);
            nrOfIterationsToMineNonce += 1;
        }
    }

    public String mineDigest(String message, int difficulty) throws NoSuchAlgorithmException {
        assert (difficulty >= 1);
        long nrOfIterationsToMineNonce = 0;
        String prefix = generatePrefix(difficulty);
        while (true) {
            String digest = hashingUtility.hexHash(message + nrOfIterationsToMineNonce);
            if (digest.startsWith(prefix))
                return digest;
            nrOfIterationsToMineNonce += 1;
        }
    }

    public String generatePrefix(int difficulty) {
        String prefix = "";
        for (int j = 0; j < difficulty; j++) {
            String s = "1";
            prefix = prefix.concat(s);
        }
        return prefix;
    }

    public String mine(String message, int difficulty) throws NoSuchAlgorithmException {
        assert (difficulty >= 1);
        long nrOfIterationsToMineNonce = 0;
        MessageDigest digest = MessageDigest.getInstance(hashingUtility.CRYPTO_HASH_ALGORITHM);
        String prefix = IntStream.range(0, difficulty).mapToObj(j -> "1").collect(Collectors.joining());
        StringBuilder result = new StringBuilder();
        while (true) {

            result.setLength(0);
            byte[] bytes = digest.digest((message + nrOfIterationsToMineNonce).getBytes(StandardCharsets.UTF_8));

            for (byte byt : bytes) {
                result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
            }

            if (result.toString().startsWith(prefix))
                return String.valueOf(nrOfIterationsToMineNonce);
            nrOfIterationsToMineNonce += 1;
        }
    }

    public void setHashingUtility(HashingUtility hashingUtility) {
        this.hashingUtility = hashingUtility;
    }

    public void setEncodingUtility(EncodingUtility encodingUtility) {
        this.encodingUtility = encodingUtility;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
}
