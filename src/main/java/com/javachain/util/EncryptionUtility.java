package com.javachain.util;

import com.javachain.dto.Wallet;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.Base64;

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
 * Alice write a message
 * Alice encrypts it using Alice's PRIVATE key
 * Alice encrypts it again using Bob's PUBLIC key
 * Alice sends the results to Bob.
 * Then:
 * <p>
 * Bob receives a message (supposedly) from Alice
 * Bob decrypts it using Bob's PRIVATE key
 * Bob decrypts it again using Alice's PUBLIC key
 * If all goes well, Bob reads the message.
 **/

@Service
public class EncryptionUtility {

    public static final String SHA_256_WITH_RSA = "SHA256withRSA";
    public static final String RSA = "RSA";

    public String sign(String message, PrivateKey privateKey) throws Exception {
        Signature privateSignature = Signature.getInstance(SHA_256_WITH_RSA);//hash the data (SHA256) and encrypt it (RSA)
        privateSignature.initSign(privateKey);
        privateSignature.update(message.getBytes(StandardCharsets.UTF_8));

        byte[] signature = privateSignature.sign();

        return Base64.getEncoder().encodeToString(signature);
    }

    public boolean verifySignature(String signer, String signature, PublicKey publicKey) throws Exception {
        Signature publicSignature = Signature.getInstance(SHA_256_WITH_RSA);//hash the data (SHA256) and encrypt it (RSA)
        publicSignature.initVerify(publicKey);
        publicSignature.update(signer.getBytes(StandardCharsets.UTF_8));

        byte[] signatureBytes = Base64.getDecoder().decode(signature);

        return publicSignature.verify(signatureBytes);
    }

    /**
     * RSA can only be used to encrypt data that is no longer than the RSA key size.
     * With a key size of 2048 bits that would be somewhat less than 256 bytes.
     * You could use an even larger key size, but that would make the process rather slow - RSA isn't
     * well suited for encrypting large data sizes. The usual approach
     * would be to use a symmetric cipher like AES or Triple-DES, and then to use RSA just to encrypt the AES/Triple-DES key.
     */
    public String encrypt(String plainText, PublicKey publicKey) throws Exception {
        Cipher encryptCipher = Cipher.getInstance(RSA);
        encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);

        byte[] cipherText = encryptCipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(cipherText);
    }

    public String decrypt(String cipherText, PrivateKey privateKey) throws Exception {
        byte[] bytes = Base64.getDecoder().decode(cipherText);

        Cipher decriptCipher = Cipher.getInstance(RSA);
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
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA);
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    public KeyPair getKeyPairFromKeyStore() throws Exception {
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
