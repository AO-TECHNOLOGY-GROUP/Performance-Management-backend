/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package log;

import com.co.ke.main.EntryPoint;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class Logging {

    private static String LOGS_PATH;

    public Logging() {
    }

    static {
        LOGS_PATH = "";
    }

    public void applicationLog(String details, String uniqueId, int logLevel) {
        LOGS_PATH = System.getProperty("user.dir") + File.separator + EntryPoint.LOGS_PATH;

        String typeOfLog = "";

        switch (logLevel) {
            case 2:
                typeOfLog = "CHANNEL_REQUESTS";
                break;
            case 3:
                typeOfLog = "CHANNEL_RESPONCES";
                break;
            case 5:
                typeOfLog = "APPLICATION_ERRORS";
                break;
            case 6:
                typeOfLog = "DATABASE";
                break;
            case 12:
                typeOfLog = "EMAILS";
                break;
            case 15:
                typeOfLog = "TO_RESPONSE_QUEUE_ADAPTORS";
                break;
            case 16:
                typeOfLog = "TO_NOTIFICATION_QUEUE";
                break;
            case 17:
                typeOfLog = "TO_BIO_REGISTRATION_QUEUE";
                break;
            case 51:
                typeOfLog = "DFA_REQUEST_TO_CBS";
                break;
            case 52:
                typeOfLog = "DFA_TO_CBS";
                break;
            case 53:
                typeOfLog = "DFA_FROM_CBS";
                break;
            case 54:
                typeOfLog = "DFA_ERRORS_CBS";
                break;
            case 82:
                typeOfLog = "TO_3RD_PARTY";
                break;
            default:
                typeOfLog = "OTHERS";
                break;
        }

        Date today = Calendar.getInstance().getTime();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        String LogDate = formatter.format(today);
        SimpleDateFormat LogTimeformatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String LogTime = LogTimeformatter.format(today);
        File dir = new File(LOGS_PATH + File.separator + LogDate + File.separator + typeOfLog);
        BufferedWriter writer = null;
        if (dir.exists()) {
            dir.setWritable(true);
        } else {
            boolean mkdirs = dir.mkdirs();
            dir.setWritable(true);
        }

        String randomNum = "";
        int maximum = 100000;

        try {
            if (uniqueId.equals("")) {
                String minimum = "5";
                randomNum = minimum + (int) (Math.random() * maximum);
                uniqueId = randomNum;

            } else {
                randomNum = "10" + (int) (Math.random() * maximum);
            }

            String fileName = "";

            File[] fileList = dir.listFiles();

            if (fileList.length > 0) {
                for (File fileList1 : fileList) {
                    if (fileList1.length() < 25000000) {
                        fileName = File.separator + fileList1.getName();
                    } else {
                        fileName = File.separator + typeOfLog + "-" + uniqueId + ".log";
                    }
                }
            } else {
                fileName = File.separator + typeOfLog + "-" + randomNum + ".log";
            }

            writer = new BufferedWriter(new FileWriter(dir + fileName, true));
            writer.write(LogTime + " ~ " + details);
            writer.newLine();
        } catch (IOException e) {
            System.err.printf(logPreString() + "ERROR IN LOGS:-" + e.getMessage(), e);
        } finally {
            try {
                // Close the writer regardless of what happens...
                writer.close();
            } catch (IOException e) {
                System.err.printf(logPreString() + "ERROR IN LOGS:-" + e.getMessage(), e);
            }
        }
    }

    /**
     * Log pre string.
     *
     * @return the string
     */
    public String logPreString() {
        return "DFA | " + Thread.currentThread().getStackTrace()[2].getClassName() + " | "
                + Thread.currentThread().getStackTrace()[2].getLineNumber() + " | "
                + Thread.currentThread().getStackTrace()[2].getMethodName() + "() | ";
    }
    
    public void info(String message) {
        System.out.println("[INFO] " + message);
    }

    public void error(String message) {
        System.err.println("[ERROR] " + message);
    }

    public void error(String message, Throwable throwable) {
        System.err.println("[ERROR] " + message);
        throwable.printStackTrace();
    }
    
    public void warn(String message) {
        System.out.println("[WARN]: " + message);
    }

}


