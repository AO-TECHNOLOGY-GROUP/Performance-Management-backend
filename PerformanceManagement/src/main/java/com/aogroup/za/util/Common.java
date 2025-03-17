package com.aogroup.za.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.resource.spi.work.WorkManager;
import log.Logging;

/**
 *
 * @author Stephen.Okoth
 */
public class Common {

    public static String T24_IP = null;
    public static String T24_PORT = null;
    
    
    public String generateRandomPassword(int len) {
        // ASCII range – alphanumeric (0-9, a-z, A-Z)
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // each iteration of the loop randomly chooses a character from the given
        // ASCII range and appends it to the `StringBuilder` instance
        for (int i = 0; i < len; i++) {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }

        return sb.toString();
    }

    public static String generateRandom(int length) {
        Random random = new Random();
        char[] digits = new char[length];
        digits[0] = (char) (random.nextInt(9) + '1');
        for (int i = 1; i < length; i++) {
            digits[i] = (char) (random.nextInt(10) + '0');
        }

        return new String(digits);
    }

    public static WorkManager getWorkManager() {
        WorkManager wm = null;
        // some code..
        return wm;
    }

    public static String formatDateToday(String format) {
        try {
            if ("".equals(format)) {
                format = "yyyy-MM-dd HH:mm:ss";
            }
            SimpleDateFormat formatter = new SimpleDateFormat(format);
            Date today = new Date();
            return formatter.format(today);
        } catch (Exception ex) {
            Logging lg = new Logging();
            lg.applicationLog(lg.logPreString() + "Error at formatDateToday: " + ex.getMessage() + "\n\n", "", 5);
            return "";
        }
    }

    public static String dateTimeStr() {
        Date date = new Date();
        String currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(date);
        return currentTime;
    }

    public static String formatDateRemoveExtras(String dateStr) {
        String newDate = null;
        if (dateStr.equals(null)) {
            newDate = "00000000000000";
        } else {
            newDate = dateStr.replaceAll("[-:. ]", "");
        }
        return newDate;
    }

    public String generateNewPinRandomFour() {
        String generatedStr = null;
        Random random = new Random();
        generatedStr = String.format("%04d", random.nextInt(10000));
        return generatedStr;
    }

    public String generatedHashedPin(String unhashedPin, String lastName, String idNumber) {
        String hashedPin = null;
        String toHash = unhashedPin + lastName + idNumber;
        try {
            hashedPin = Common.hashSHA256(toHash);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(Common.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hashedPin;
    }

    public String generatedHashedPin(String unhashedPin, String surname, String username, String idNumber) {
        String hashedPin = null;
        String toHash = unhashedPin + surname + username + idNumber;
        try {
            hashedPin = Common.hashSHA256(toHash);
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
            Logger.getLogger(Common.class.getName()).log(Level.SEVERE, null, ex);
        }
        return hashedPin;
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

}
