package com.javachain.util;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class HashingUtilityTest {

    private HashingUtility hashingUtility = new HashingUtility();
    private EncodingUtility encodingUtility = new EncodingUtility();

    @Test
    void getMD5() {
        assertEquals("098F6BCD4621D373CADE4E832627B4F6", hashingUtility.getMD5("test"));
    }

    @Test
    void getHash32() {
        assertEquals("098F6BCD", hashingUtility.getHash32("test"));
    }

    @Test
    void getHash64() {
        assertEquals("098F6BCD4621D373", hashingUtility.getHash64("test"));
    }

    @Test
    void sha256() throws NoSuchAlgorithmException {
        assertNotNull(hashingUtility.sha256("test"));
        assertEquals("龆킁行絥騯\uEAA0앚퀕ꎿ伛⬋般텝氕냰ਈ", new String(hashingUtility.sha256("test"), StandardCharsets.UTF_16));
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", encodingUtility.bytesToHex(hashingUtility.sha256("test")));
    }

    @Test
    void hexHash() throws NoSuchAlgorithmException {
        assertEquals("9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", hashingUtility.hexHash("test"));
    }
}