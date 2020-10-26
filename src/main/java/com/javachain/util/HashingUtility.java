package com.javachain.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Cryptographic Hashing serves the purpose of ensuring integrity, i.e. making it so that if
 * something is changed you can know that it’s changed. Technically,
 * hashing takes arbitrary input and produce a fixed-length string that has the following attributes:
 * <p>
 * The same input will always produce the same output.
 * Multiple disparate inputs should not produce the same output.
 * It should not be possible to go from the output to the input.
 * Any modification of a given input should result in drastic change to the hash.
 * Hashing is used in conjunction with authentication to produce strong evidence
 * that a given message has not been modified. This is accomplished by taking a given input, hashing it,
 * and then signing the hash with the sender’s private key.
 * <p>
 * When the recipient opens the message, they can then validate the signature of the hash with the sender’s
 * public key and then hash the message themselves and compare it to the hash that was signed by the sender.
 * If they match it is an unmodified message, sent by the correct person.
 * <p>
 * Examples: sha-3, md5 (now obsolete), etc.
 */
@Service
public class HashingUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(HashingUtility.class);

    public static final String CRYPTO_HASH_ALGORITHM = "SHA-256";

    private static final MessageDigest MESSAGE_DIGEST;

    static {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException err) {
            throw new IllegalStateException();
        }
        MESSAGE_DIGEST = md;
    }

    private static final String HEX_CHARS = "0123456789ABCDEF";
    private final EncodingUtility encodingUtility;

    public HashingUtility(EncodingUtility encodingUtility) {
        this.encodingUtility = encodingUtility;
    }

    public String getMD5(String source) {
        byte[] bytes;
        try {
            bytes = source.getBytes(StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException ue) {
            throw new IllegalStateException(ue);
        }
        byte[] result;
        synchronized (MESSAGE_DIGEST) {
            MESSAGE_DIGEST.update(bytes);
            result = MESSAGE_DIGEST.digest();
        }
        char[] resChars = new char[32];
        int len = result.length;
        for (int i = 0; i < len; i++) {
            byte b = result[i];
            int lo4 = b & 0x0F;
            int hi4 = (b & 0xF0) >> 4;
            resChars[i * 2] = HEX_CHARS.charAt(hi4);
            resChars[i * 2 + 1] = HEX_CHARS.charAt(lo4);
        }
        return new String(resChars);
    }

    public String getHash32(String source) {
        String md5 = getMD5(source);
        return md5.substring(0, 8);
    }

    public String getHash64(String source) {
        String md5 = getMD5(source);
        return md5.substring(0, 16);
    }

    public byte[] sha256(String data) {
        MessageDigest messageDigest = null;
        try {
            messageDigest = MessageDigest.getInstance(CRYPTO_HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Can't digest the message. {}", e.getMessage(), e);
        }
        return Objects.requireNonNull(messageDigest).digest(data.getBytes(StandardCharsets.UTF_8));
    }

    public String hexHash(String message) {
        return encodingUtility.bytesToHex(sha256(message));
    }

}
