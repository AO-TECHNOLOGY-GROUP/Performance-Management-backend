package com.aogroup.za.util;

import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Arrays;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import log.Logging;

public class Encryption {

     private final String key = "U&M3#";
    private final String HASHING_ALG = "MD5";
    private final String ENCODING = "utf-8";
    private final String ENCRYPTION_ALG = "DESede";

    private static Logging logger;

    public Encryption() {
    }

    public String encrypt(String plainPass) throws Exception {
        MessageDigest md = MessageDigest.getInstance(HASHING_ALG);
        byte[] digestOfPassword = md.digest(key.getBytes(ENCODING));
        byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        SecretKey key = new SecretKeySpec(keyBytes, ENCRYPTION_ALG);
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALG);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] plainTextBytes = plainPass.getBytes(ENCODING);
        byte[] buf = cipher.doFinal(plainTextBytes);
        byte[] base64Bytes = Base64.encodeBase64(buf);
        String base64EncryptedString = new String(base64Bytes);
        return base64EncryptedString;
    }

    public String decrypt(String encryptedPass) throws Exception {
        byte[] message = Base64.decodeBase64(encryptedPass.getBytes(ENCODING));
        MessageDigest md = MessageDigest.getInstance(HASHING_ALG);
        byte[] digestOfPassword = md.digest(key.getBytes(ENCODING));
        byte[] keyBytes = Arrays.copyOf(digestOfPassword, 24);
        SecretKey key = new SecretKeySpec(keyBytes, ENCRYPTION_ALG);
        Cipher decipher = Cipher.getInstance(ENCRYPTION_ALG);
        decipher.init(Cipher.DECRYPT_MODE, key);
        byte[] plainText = decipher.doFinal(message);
        return new String(plainText, ENCODING);
    }

    public static String hashSHA256(String data) throws NoSuchAlgorithmException {

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(data.getBytes());

        byte byteData[] = md.digest();

        StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < byteData.length; i++) {
            String hex = Integer.toHexString(0xff & byteData[i]);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static String encryptIniPass(String pass, PublicKey key) {
        logger = new Logging();
        byte[] cipherText = null;
        try {
            // get an RSA cipher object and print the provider
            final Cipher cipher = Cipher.getInstance("RSA");
            // encrypt the plain text using the public key
            cipher.init(Cipher.ENCRYPT_MODE, key);
            cipherText = cipher.doFinal(pass.getBytes());
        } catch (InvalidKeyException e) {
             logger.applicationLog(logger.logPreString() + "Error at encryptIniPass - " + e.getMessage() + "\n\n", "", 54);
        } catch (NoSuchAlgorithmException e) {
            logger.applicationLog(logger.logPreString() + "Error at encryptIniPass - " + e.getMessage() + "\n\n", "", 54);
        } catch (BadPaddingException e) {
            logger.applicationLog(logger.logPreString() + "Error at encryptIniPass - " + e.getMessage() + "\n\n", "", 54);
        } catch (IllegalBlockSizeException e) {
            logger.applicationLog(logger.logPreString() + "Error at encryptIniPass - " + e.getMessage() + "\n\n", "", 54);
        } catch (NoSuchPaddingException e) {
            logger.applicationLog(logger.logPreString() + "Error at encryptIniPass - " + e.getMessage() + "\n\n", "", 54);
        }
        return Base64.encodeBase64String(cipherText);
    }
}
