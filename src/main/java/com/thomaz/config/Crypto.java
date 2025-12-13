package com.thomaz.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class Crypto {

    private static final Logger LOGGER = LoggerFactory.getLogger(Crypto.class);
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final String ENCRYPTED_PREFIX = "Encrypted: ";
    private static final int GCM_TAG_LENGTH = 128;
    private static SecretKey secretKey;
    private static Cipher cipher;

    private Crypto() {
        throw new IllegalStateException("Utility class");
    }

    public static void setSecretKey(String secretKeyString) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(secretKeyString);
            secretKey = new SecretKeySpec(keyBytes, "AES");
            cipher = Cipher.getInstance(ALGORITHM);
        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static String encrypt(String plainText) {
        if (plainText == null) {
            return null;
        }
        if (plainText.isEmpty()) {
            return "";
        }
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes());
            byte[] cipherPlusIv = new byte[iv.length + cipherText.length];

            System.arraycopy(iv, 0, cipherPlusIv, 0, iv.length);
            System.arraycopy(cipherText, 0, cipherPlusIv, iv.length, cipherText.length);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(cipherPlusIv);

        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static String decrypt(String cipherText) {
        if (cipherText == null) {
            return null;
        }
        if (cipherText.isEmpty()) {
            return "";
        }
        if (!isEncrypted(cipherText)) {
            LOGGER.warn("Text passed for decode but is not encrypted: {}", cipherText);
            return cipherText;
        }
        try {
            cipherText = cipherText.replace(ENCRYPTED_PREFIX, "");
            byte[] cipherData = Base64.getDecoder().decode(cipherText);
            byte[] iv = new byte[12];
            System.arraycopy(cipherData, 0, iv, 0, iv.length);

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            return new String(cipher.doFinal(cipherData, iv.length, cipherData.length - iv.length));

        } catch (Exception e) {
            throw new CryptoException(e.getMessage(), e);
        }
    }

    public static boolean isEncrypted(String text) {
        return text != null && text.startsWith(ENCRYPTED_PREFIX);
    }

    public static boolean isPlainText(String text) {
        return !isEncrypted(text);
    }

    public static String newBase64Secret256() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }


}
