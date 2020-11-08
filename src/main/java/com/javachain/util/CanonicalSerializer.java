package com.javachain.util;

import com.javachain.dto.Block;
import com.javachain.dto.IncomingTransaction;
import com.javachain.dto.OutgoingTransaction;
import com.javachain.dto.Transaction;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;

/**
 * The {@code CanonicalSerializer} produces one deterministic string representation of
 * transactions and blocks. This is the representation that gets hashed, signed and verified.
 * <p>
 * Why this exists: historically hashing/signing was done over {@code Object::toString()},
 * which leaked JVM identity hash codes (via {@code Wallet}, which has no {@code toString()}),
 * omitted fields such as timestamps and fees, and therefore produced hashes and signatures
 * that were not reproducible across JVM instances - making persistence and network transfer
 * impossible to verify.
 * <p>
 * Rules of the canonical form:
 * <ul>
 *     <li>keys are serialized as hex of their X.509 encoded bytes (stable across JVMs),</li>
 *     <li>amounts are normalized via {@code stripTrailingZeros().toPlainString()},</li>
 *     <li>inputs reference the parent transaction by its id (no nested object graphs),
 *         which keeps payloads short and free of cycles,</li>
 *     <li>the signer's wallet object is NOT part of a transaction payload; ownership is
 *         proven by the signature against the referenced output (Bitcoin-style),</li>
 *     <li>signatures and nonces are excluded from the payload they themselves cover,
 *         but are committed via the transaction/block id.</li>
 * </ul>
 * <p>
 * All methods are null-tolerant so that partially built objects (and unit-test mocks)
 * serialize to a stable placeholder instead of throwing.
 */
public final class CanonicalSerializer {

    /**
     * Value used as "previous hash" marker inside the genesis block, which by definition
     * has no ancestor.
     */
    public static final String GENESIS_PREVIOUS_HASH = zeroHash();

    private static final String NULL = "null";

    private CanonicalSerializer() {
    }

    // ------------------------------------------------------------------ keys

    /**
     * Hex of the X.509 encoding of the given public key - the canonical wallet address.
     */
    public static String address(PublicKey publicKey) {
        return publicKey == null ? NULL : EncodingUtility.bytesToHexStatic(publicKey.getEncoded());
    }

    // --------------------------------------------------------------- amounts

    /**
     * Normalized decimal representation; {@code 5}, {@code 5.0} and {@code 5.00} collapse
     * to {@code "5"} so they always hash identically.
     */
    public static String amount(BigDecimal amount) {
        return amount == null ? "0" : amount.stripTrailingZeros().toPlainString();
    }

    // ---------------------------------------------------------- transactions

    /**
     * Canonical transaction payload: everything a miner or verifier needs to check,
     * excluding the signature itself (which covers exactly this string).
     * <p>
     * The fee is deliberately NOT part of the payload: fees are not implemented yet
     * (see TransactionService TODOs) and {@code computeTotalFee} currently mutates the
     * fee field as a side effect - committing a mutable field would invalidate
     * signatures after the fact. Revisit once fees become a real, immutable part of a
     * transaction.
     */
    public static String transactionPayload(Transaction tx) {
        StringBuilder sb = new StringBuilder("tx|v=1");
        sb.append("|initial=").append(tx != null && tx.isInitial());
        sb.append("|ts=").append(timestamp(tx == null ? null : tx.getDateCreated()));
        sb.append("|in=").append(inputs(tx == null ? null : tx.getIncomingTransactions()));
        sb.append("|out=").append(outputs(tx == null ? null : tx.getOutgoingTransactions()));
        return sb.toString();
    }

    /**
     * Transaction id: SHA-256 over the canonical payload plus the signature, so the id
     * commits to both meaning and authorship. Stable across JVMs.
     */
    public static String transactionId(Transaction tx) {
        if (tx == null) return NULL;
        return sha256Hex(transactionPayload(tx) + "|sig=" + (tx.getSignature() == null ? "" : tx.getSignature()));
    }

    /**
     * Canonical reference to a spent output: {@code <parentTxId>:<outputIndex>}.
     */
    public static String inputReference(IncomingTransaction in) {
        if (in == null) return NULL;
        return "(" + transactionId(in.getTransaction()) + ":" + in.getOutPutIndex() + ")";
    }

    /**
     * Canonical reference to a created output: {@code (<address>:<amount>)}.
     */
    public static String outputReference(OutgoingTransaction out) {
        if (out == null) return NULL;
        return "(" + address(out.getRecipientAddress()) + ":" + amount(out.getAmount()) + ")";
    }

    // ---------------------------------------------------------------- blocks

    /**
     * Canonical block payload. Note that the chain link is the ancestor's HASH (a value),
     * not a nested object, and that the block timestamp is committed here - both were
     * previously missing from the mined message.
     */
    public static String blockPayload(Block block) {
        StringBuilder sb = new StringBuilder("blk|v=1");
        sb.append("|prev=").append(block == null || block.getPreviousHash() == null
                ? GENESIS_PREVIOUS_HASH : block.getPreviousHash());
        sb.append("|ts=").append(timestamp(block == null ? null : block.getDateCreated()));
        sb.append("|miner=").append(address(block == null ? null : block.getMinersAddress()));
        sb.append("|txs=").append(ids(block == null ? null : block.getTransactionList()));
        return sb.toString();
    }

    /**
     * Exact message that proof-of-work is computed over: the canonical payload with the
     * nonce appended by the miner, e.g. {@code sha256(payload + "|nonce=" + i)}.
     */
    public static String hashingMessage(Block block) {
        return blockPayload(block) + "|nonce=";
    }

    // --------------------------------------------------------------- helpers

    private static String inputs(List<IncomingTransaction> incoming) {
        if (incoming == null || incoming.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < incoming.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(inputReference(incoming.get(i)));
        }
        return sb.append("]").toString();
    }

    private static String outputs(List<OutgoingTransaction> outgoing) {
        if (outgoing == null || outgoing.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < outgoing.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(outputReference(outgoing.get(i)));
        }
        return sb.append("]").toString();
    }

    private static String ids(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < transactions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("(").append(transactionId(transactions.get(i))).append(")");
        }
        return sb.append("]").toString();
    }

    private static long timestamp(Instant instant) {
        return instant == null ? 0L : instant.toEpochMilli();
    }

    private static String zeroHash() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 64; i++) sb.append('0');
        return sb.toString();
    }

    /**
     * Lowercase hex SHA-256. Kept beside the canonical form so that id computation never
     * depends on bean wiring (DTOs must stay dependency-free).
     */
    public static String sha256Hex(String message) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HashingUtility.CRYPTO_HASH_ALGORITHM);
            return EncodingUtility.bytesToHexStatic(digest.digest(message.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
