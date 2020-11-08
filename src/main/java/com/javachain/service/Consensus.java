package com.javachain.service;

import java.math.BigDecimal;

/**
 * The {@code Consensus} class holds the network-wide rules every participant must apply
 * identically: the proof-of-work difficulty and the block incentive.
 * <p>
 * Keeping them in one place makes it obvious what would constitute a hard-fork change.
 */
public final class Consensus {

    /**
     * Block incentive - the reward a miner adds to itself in the coinbase transaction
     * (index 0) of every mined block. Until transaction fees are implemented, this is
     * also the ONLY value a coinbase may pay out.
     */
    public static final BigDecimal BLOCK_INCENTIVE = new BigDecimal(25);

    /**
     * Difficulty of finding a new block, expressed as the number of leading hex digits
     * the block hash must start with. In Bitcoin it is adjusted every 2016 blocks;
     * here it is constant.
     */
    public static final int DIFFICULTY = 2;

    private Consensus() {
    }
}
