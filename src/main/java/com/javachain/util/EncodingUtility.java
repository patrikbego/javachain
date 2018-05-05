package com.javachain.util;

import org.springframework.stereotype.Component;

import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;

/**
 * The purpose of encoding is to transform data so that it can be properly (and safely)
 * consumed by a different type of system, e.g. binary data being sent over email,
 * or viewing special characters on a web page. The goal is not to keep information secret,
 * but rather to ensure that it’s able to be properly consumed.
 * Encoding transforms data into another format using a scheme that is
 * publicly available so that it can easily be reversed.
 * It does not require a key as the only thing required to decode it is the algorithm that was used to encode it.
 * <p>
 * e.g. ascii, unicode, url encoding, base64, UTF-8 ...
 */
@Component
public class EncodingUtility {

    public String bytesToHex1(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String bytesToHex2(byte[] hash) {
        StringBuffer hexString = new StringBuffer();
        for (byte aHash : hash) {
            String hex = Integer.toHexString(0xff & aHash);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public String hexToString(String hexString) {
        byte[] bytes = DatatypeConverter.parseHexBinary(hexString);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte byt : bytes) result.append(Integer.toString((byt & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

}
