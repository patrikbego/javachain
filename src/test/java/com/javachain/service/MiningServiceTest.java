package com.javachain.service;

import com.javachain.util.EncodingUtility;
import com.javachain.util.HashingUtility;
import org.junit.jupiter.api.Test;

import java.security.NoSuchAlgorithmException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MiningServiceTest {

    final EncodingUtility encodingUtility = new EncodingUtility();
    final MiningService miningService = new MiningService(new HashingUtility(encodingUtility), encodingUtility);

//    @Before
//    public void given() {
//        EncodingUtility encodingUtility = new EncodingUtility();
//        miningService = new MiningService(new HashingUtility(encodingUtility), encodingUtility);
//    }

    @Test
    void demoMiner() {
        //given
        //when
        //then
        assertEquals("115d801f2e5f9b14e9b932bb6f71c00d52fc7922573d2d91dc46e4a00f8f4539", miningService.demoMiner());
    }

    @Test
    void mineNonce() {
        //given
        //when
        //then
        assertEquals("23", miningService.mineNonce("42", 1));
        assertEquals("2272", miningService.mineNonce("42", 3));
        assertEquals("879", miningService.mineNonce("Z6AR67BMX7", 3));
        assertEquals("16671", miningService.mineNonce("2U6TP42RIM", 5));
        assertEquals("0", miningService.mineNonce("Q4XZ1QL2MI", 1));
        assertEquals("9", miningService.mineNonce("VIH18FKQ4K", 1));
    }

    @Test
    void mineDigest() {
        //given
        //when
        assertEquals("190510626ea606a4d5e2dbb4136665d8b6a65cdaaaec3ffd7050310533643d1d", miningService.mineDigest("42", 1));
        assertEquals("111f1b7113e7c14a933421251b677a0f2493f65347976c64ada8cfc1e91229c0", miningService.mineDigest("42", 3));
        assertEquals("11198524e9493420ea84c775ba654a581930b1d443bed4c651be577d29197549", miningService.mineDigest("Z6AR67BMX7", 3));
        assertEquals("1111107fcf1ec3cf94dee69468029d379e3f7a96e6b419981ee2c9c50313f1c9", miningService.mineDigest("2U6TP42RIM", 5));
        assertEquals("1ac246502da130787309f61a4c7b3ba141ea763a7e4083de0158c5069114acce", miningService.mineDigest("Q4XZ1QL2MI", 1));
        assertEquals("1cb173515d2331932002a13a28ad7b0bcee4ae71ae350b28b7600dcd34905530", miningService.mineDigest("VIH18FKQ4K", 1));
    }

    @Test
    void generatePrefix() {
        EncodingUtility encodingUtility = new EncodingUtility();
        MiningService miningService = new MiningService(new HashingUtility(encodingUtility), encodingUtility);
        assertEquals("11111", miningService.generatePrefix(5));
    }

    @Test
    void mine() throws NoSuchAlgorithmException {
        EncodingUtility encodingUtility = new EncodingUtility();
        MiningService miningService = new MiningService(new HashingUtility(encodingUtility), encodingUtility);
        assertEquals(miningService.mine("test", 1), miningService.mine("test", 1));
    }
}
