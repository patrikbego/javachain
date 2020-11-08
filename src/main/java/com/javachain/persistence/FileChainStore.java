package com.javachain.persistence;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * The {@code FileChainStore} persists blocks (and whole wallets, keys included) to disk
 * using Java serialization.
 * <p>
 * This is only possible since the canonical-serialization rework: hashing and signing
 * no longer depend on JVM identity hash codes, so a chain written by one process
 * produces identical hashes and valid signatures when read back by another. Java
 * serialization is deliberately simple here - it preserves the object graph (embedded
 * parent transactions, previous-block links) that validation relies on. A production
 * system would define an explicit wire format instead.
 * <p>
 * All DTOs declare explicit serialVersionUID values so files survive unrelated code
 * changes; any field-layout change that alters semantics should bump them.
 */
public final class FileChainStore {

    private FileChainStore() {
    }

    /**
     * Writes any serializable object - a block tip carries the whole chain with it -
     * atomically enough for a demo: direct stream to the target file.
     */
    public static void store(Serializable object, File target) throws IOException {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(target))) {
            out.writeObject(object);
        }
    }

    /**
     * Reads back an object previously written by {@link #store}; the caller knows the
     * expected type (a block tip or a wallet) and casts.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T load(File source, Class<T> type) throws IOException {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(source))) {
            Object object = in.readObject();
            if (!type.isInstance(object)) {
                throw new IOException("File does not contain a " + type.getSimpleName());
            }
            return (T) object;
        } catch (ClassNotFoundException e) {
            throw new IOException("Unknown class in chain file - different software version?", e);
        }
    }
}
