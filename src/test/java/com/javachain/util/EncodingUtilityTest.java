package com.javachain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingUtilityTest {

    private final byte[] bytes = {11, 22, 33};
    private final EncodingUtility encodingUtility = new EncodingUtility();

    @Test
    void bytesToHexJ5() {
        assertEquals("313233", encodingUtility.bytesToHexJ5("123".getBytes()));
        assertEquals("test", encodingUtility.hexToString(encodingUtility.bytesToHexJ5("test".getBytes())));
    }

    @Test
    void bytesToHexJ11() {
        assertEquals("\u000B\u0016!", encodingUtility.hexToString(encodingUtility.bytesToHexJ11(bytes)));
        assertEquals("test", encodingUtility.hexToString(encodingUtility.bytesToHexJ11("test".getBytes())));
    }

    @Test
    void bytesToHex() {
        assertEquals("0b1621", encodingUtility.bytesToHex(bytes));
        assertEquals("test", encodingUtility.hexToString(encodingUtility.bytesToHex("test".getBytes())));
    }

    @Test
    void hexToString() {
        assertEquals("test", encodingUtility.hexToString("74657374"));
    }


}
