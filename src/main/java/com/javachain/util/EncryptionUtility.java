package com.javachain.util;

import com.javachain.dto.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Objects;

/**
 * The purpose of encryption is to transform data in order to keep it secret from others,
 * e.g. sending someone a secret letter that only they should be able to read,
 * or securely sending a password over the Internet. Rather than focusing on usability,
 * the goal is to ensure the data cannot be consumed by anyone other than the intended recipient(s).
 * <p>
 * Encryption transforms data into another format in such a way that only specific individual(s)
 * can reverse the transformation. It uses a key, which is kept secret,
 * in conjunction with the plaintext and the algorithm, in order to perform the encryption operation.
 * As such, the ciphertext, algorithm, and key are all required to return to the plaintext.
 * <p>
 * e.g. aes, blowfish, rsa
 * <p>
 * e.g. of RSA encryption
 * U1 initiates a transaction
 * U1 encrypts it using U1's PRIVATE key
 * U1 encrypts it again using U2's PUBLIC key
 * U1 sends the transaction to U2.
 * Then:
 * <p>
 * U2 receives a transaction from U1
 * U2 decrypts it using U2's PRIVATE key
 * U2 decrypts it again using U1's PUBLIC key
 * U2 is able to see the transaction details.
 **/
@Service
public class EncryptionUtility {

    private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionUtility.class);

    public static final String SHA_256_WITH_RSA = "SHA256withRSA";
    public static final String KEY_ALGORITHM = "RSA";
    public static final String CIPHER = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";

    public String sign(String message, PrivateKey privateKey) {
        Signature privateSignature;//hash the data (SHA256) and encrypt it (RSA)
        byte[] signature = new byte[0];
        try {
            privateSignature = Signature.getInstance(SHA_256_WITH_RSA);
            privateSignature.initSign(privateKey);
            privateSignature.update(message.getBytes(StandardCharsets.UTF_8));

            signature = privateSignature.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            LOGGER.error("Can't sign the message {}", e.getMessage(), e);
        }
        return Base64.getEncoder().encodeToString(signature);
    }

    public boolean verifySignature(String signer, String signature, PublicKey publicKey) throws SignatureException {
        Signature publicSignature = null;//hash the data (SHA256) and encrypt it (RSA)
        byte[] signatureBytes = new byte[0];
        try {
            publicSignature = Signature.getInstance(SHA_256_WITH_RSA);
            publicSignature.initVerify(publicKey);
            publicSignature.update(signer.getBytes(StandardCharsets.UTF_8));
            signatureBytes = Base64.getDecoder().decode(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            LOGGER.error("Can't verify signature: {}", e.getMessage(), e);
        }
        return Objects.requireNonNull(publicSignature).verify(signatureBytes);
    }

    /**
     * RSA can only be used to encrypt data that is no longer than the RSA key size.
     * With a key size of 2048 bits that would be somewhat less than 256 bytes.
     * You could use an even larger key size, but that would make the process rather slow - RSA isn't
     * well suited for encrypting large data sizes. The usual approach
     * would be to use a symmetric cipher like AES or Triple-DES, and then to use RSA just to encrypt the AES/Triple-DES key.
     */
    public String encrypt(String plainText, PublicKey publicKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        Cipher encryptCipher = Cipher.getInstance(CIPHER);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public String decrypt(String cipherText, PrivateKey privateKey)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        byte[] bytes = Base64.getDecoder().decode(cipherText);

        Cipher decriptCipher = Cipher.getInstance(CIPHER);
        decriptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        return new String(decriptCipher.doFinal(bytes), StandardCharsets.UTF_8);
    }

    public String generateHexStringKey(KeyPair keyPair, boolean getPublic) {
        byte[] keyBytes;
        if (getPublic)
            keyBytes = keyPair.getPublic().getEncoded();
        else
            keyBytes = keyPair.getPrivate().getEncoded();
        StringBuffer retString = new StringBuffer();
        for (byte keyByte : keyBytes) {
            retString.append(Integer.toHexString(0x0100 + (keyByte & 0x00FF)).substring(1));
        }
        return retString.toString();
    }

    //=============================================================================
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM);
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    public KeyPair getKeyPairFromKeyStore() throws KeyStoreException, CertificateException,
            NoSuchAlgorithmException, IOException, UnrecoverableEntryException {
        //Generated with:
        //  keytool -genkeypair -alias mykey -storepass secret -keypass secret -keyalg RSA -keystore keystore.jks

        InputStream ins = Wallet.class.getResourceAsStream("/keystore.jks");

        KeyStore keyStore = KeyStore.getInstance("JCEKS");
        keyStore.load(ins, "secret".toCharArray());   //Keystore password
        KeyStore.PasswordProtection keyPassword =       //Key password
                new KeyStore.PasswordProtection("secret".toCharArray());

        KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("jkey", keyPassword);

        java.security.cert.Certificate cert = keyStore.getCertificate("jkey");
        PublicKey publicKey = cert.getPublicKey();
        PrivateKey privateKey = privateKeyEntry.getPrivateKey();

        return new KeyPair(publicKey, privateKey);
    }


}
