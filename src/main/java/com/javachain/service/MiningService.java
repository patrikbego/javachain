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

/**
 * The {@code MiningService} class is used for mining related functionalities.
 * <p>
 * It has supporting methods regarding mining (miningNonce, miningHash).
 */
@Component
public class MiningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MiningService.class);

    final HashingUtility hashingUtility;

    final EncodingUtility encodingUtility;

    @Autowired
    public MiningService(HashingUtility hashingUtility, EncodingUtility encodingUtility) {
        this.hashingUtility = hashingUtility;
        this.encodingUtility = encodingUtility;
    }

    /**
     * A simple demonstration on how the mining works
     *
     * @return String digest
     */
    public String demoMiner() {
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
     * @param message    String
     * @param difficulty int
     * @return String
     */
    public String mineNonce(String message, int difficulty) {
        long nrOfIterationsToMineNonce = 0;
        String prefix = generatePrefix(difficulty);
        while (difficulty >= 1) { // extra precondition check if difficulty is bigger or higher than 1. It is suppose to be always true.
            String digest = hashingUtility.hexHash(message + nrOfIterationsToMineNonce);
            if (digest.startsWith(prefix))
                return String.valueOf(nrOfIterationsToMineNonce);
            nrOfIterationsToMineNonce += 1;
        }
        return String.valueOf(nrOfIterationsToMineNonce);
    }

    public String mineDigest(String message, int difficulty) {
        long nrOfIterationsToMineNonce = 0;
        String prefix = generatePrefix(difficulty);
        while (difficulty >= 1) { // extra precondition check if difficulty is bigger or higher than 1. It is suppose to be always true.
            String digest = hashingUtility.hexHash(message + nrOfIterationsToMineNonce);
            if (digest.startsWith(prefix))
                return digest;
            nrOfIterationsToMineNonce += 1;
        }
        return String.valueOf(nrOfIterationsToMineNonce);
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
        long nrOfIterationsToMineNonce = 0;
        MessageDigest digest = MessageDigest.getInstance(HashingUtility.CRYPTO_HASH_ALGORITHM);
        String prefix = IntStream.range(0, difficulty).mapToObj(j -> "1").collect(Collectors.joining());
        StringBuilder result = new StringBuilder();
        while (difficulty >= 1) { // extra precondition check if difficulty is bigger or higher than 1. It is suppose to be always true.

            result.setLength(0);
            byte[] bytes = digest.digest((message + nrOfIterationsToMineNonce).getBytes(StandardCharsets.UTF_8));

            for (byte byt : bytes) {
                result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
            }

            if (result.toString().startsWith(prefix))
                return String.valueOf(nrOfIterationsToMineNonce);
            nrOfIterationsToMineNonce += 1;
        }
        return String.valueOf(nrOfIterationsToMineNonce);
    }

}
