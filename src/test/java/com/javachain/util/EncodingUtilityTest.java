package com.javachain.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EncodingUtilityTest {

    private byte[] bytes = {11, 22, 33};
    private EncodingUtility encodingUtility = new EncodingUtility();

    @Test
    void bytesToHex1() {
        assertEquals(encodingUtility.bytesToHex1("123".getBytes()), "313233");
        assertEquals(encodingUtility.hexToString(encodingUtility.bytesToHex1("test".getBytes())), "test");
    }

    @Test
    void bytesToHex2() {
        assertEquals(encodingUtility.hexToString(encodingUtility.bytesToHex2(bytes)), "\u000B\u0016!");
        assertEquals(encodingUtility.hexToString(encodingUtility.bytesToHex2("test".getBytes())), "test");
    }

    @Test
    void bytesToHex() {
        assertEquals(encodingUtility.bytesToHex(bytes), "0b1621");
        assertEquals(encodingUtility.hexToString(encodingUtility.bytesToHex("test".getBytes())), "test");
    }

    @Test
    void hexToString() {
        assertEquals(encodingUtility.hexToString("74657374"), "test");
    }


}