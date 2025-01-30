/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.util;

import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.makerchecker.MakerCheckerUtil;
import com.aogroup.za.user.UserUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import log.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author nathan
 */
public class Utilities {

    Logging logger;
    private static final Logger LOG = LoggerFactory.getLogger(Utilities.class);

    public Utilities() {
        logger = new Logging();
    }

    public int updateTransactionAsPicked(String table, String field, String value, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field + "] = '" + value + "' WHERE [" + whereField + "] = '" + whereValue + "'";
        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            //e.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            conn.closeConn();
        }
        return updateSuccess;
    }

    public int updateTransactionAsPicked(String table, String field1, String value1, String field2, String value2, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "' WHERE [" + whereField + "] = '" + whereValue + "'";
        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        //////////System.out.println("updateSuccess Multiple: " + updateSuccess);
        return updateSuccess;
    }

    public boolean updatePinTrial(String username, int pinTrial, boolean changePin, boolean isBlocked) {
        long msgID;

        DBConnection conn = new DBConnection();
        try {
            String call = "UPDATE [dbo].[login_validation] \n"
                    + "	SET pin_trial = '" + pinTrial + "', change_pin = '" + changePin + "', is_blocked = '" + isBlocked + "', updated_at = GETDATE()\n"
                    + "	WHERE username = '" + username + "'";
            msgID = conn.update_db(call);
            return msgID == 0;
        } catch (Exception ex) {
            ex.printStackTrace();
            logger.applicationLog(logger.logPreString() + "DBOperations.updatePinTrial: \n[SQLException]: " + ex.getMessage() + "\n\n\n", "", 6);
            return false;
        } finally {
            conn.closeConn();
        }
    }

    public String getCurrentFinancialYear() {
        int year = Calendar.getInstance().get(Calendar.YEAR);

        return String.valueOf(year);
    }

    public String getTodayString() {
        java.util.Date date = new java.util.Date();
        date.setTime(date.getTime()); //2020-07-23
        String formattedDate = new SimpleDateFormat("YYYY-MM-dd").format(date);
        ////////////System.out.println("formattedDate " + formattedDate);
        return formattedDate;
    }
    
    public String getTodayStringTime() {
        java.util.Date date = new java.util.Date();
        date.setTime(date.getTime()); //2020-07-23 14:58 
        String formattedDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        return formattedDate;
    }

    public boolean checkIfExists(String searchTable, String searchField, String value) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField + " = '" + value + "'";

        DBConnection con = new DBConnection();
        int count = 0;
        boolean exists = false;
        try {
            ResultSet rs = con.query_all(query);

            while (rs.next()) {
                count = rs.getInt("counts");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }
        return exists;
    }

    public boolean checkIfAccountExists(String searchTable, String searchField1, String searchField2, String value1, String value2) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField1 + " = '" + value1 + "' AND " + searchField2 + " = '" + value2 + "'";

        DBConnection con = new DBConnection();
        int count = 0;
        boolean exists = false;
        try {
            ResultSet rs = con.query_all(query);

            while (rs.next()) {
                count = rs.getInt("counts");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }
        return exists;
    }

    public int checkIfExistsStatus(String searchTable, String searchField, String value) {
        String query = "SELECT status  FROM " + searchTable + "  WHERE " + searchField + " = '" + value + "'";

        DBConnection con = new DBConnection();
        int status = 0;
        try {
            ResultSet rs = con.query_all(query);
            while (rs.next()) {
                status = rs.getInt("status");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }

        //////System.out.println("###########exists CHECK " + exists);
        return status;
    }

    public int checkIfExistsStatus(String searchTable, String searchField, String value, String searchField2, String value2) {
        String query = "SELECT status  FROM " + searchTable + "  WHERE " + searchField + " = '" + value + "'  AND " + searchField2 + " = '" + value2 + "'";

        DBConnection con = new DBConnection();
        int status = 0;
        try {
            ResultSet rs = con.query_all(query);
            while (rs.next()) {
                status = rs.getInt("status");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return status;
    }

    public boolean checkIfExistsMultiple(String searchTable, String searchField1, String value1, String searchField2, String value2) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField1 + " = '" + value1 + "'  AND " + searchField2 + " = '" + value2 + "'";
        DBConnection con = new DBConnection();
        int count = 0;
        boolean exists = false;
        try {
            ResultSet rs = con.query_all(query);

            while (rs.next()) {
                count = rs.getInt("counts");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }

        return exists;
    }

    public boolean checkIfUserExists(String user_id, String role) {
        String query = "SELECT count(*) as counts FROM users  WHERE [id] = '" + Integer.parseInt(user_id) + "'  AND [role] = '" + Integer.parseInt(role) + "'";
        DBConnection con = new DBConnection();
        int count = 0;
        boolean exists = false;
        try {
            ResultSet rs = con.query_all(query);

            while (rs.next()) {
                count = rs.getInt("counts");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }

        return exists;
    }

    public boolean checkIfExistsMultiple(String searchTable, String searchField1, String value1, String searchField2, String value2, String searchField3, String value3) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField1 + " = '" + value1 + "'  AND " + searchField2 + " = '" + value2 + "'  AND " + searchField3 + " = '" + value3 + "'";
        DBConnection con = new DBConnection();
        int count = 0;
        boolean exists = false;
        try {
            ResultSet rs = con.query_all(query);

            while (rs.next()) {
                count = rs.getInt("counts");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }

        return exists;
    }

    public int updateTransactionAsPicked(String table, String field1, String value1, String field2, String value2, String whereField, String whereValue, String whereField2, String whereValue2) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "' WHERE [" + whereField + "] = '" + whereValue + "' AND [" + whereField2 + "] = '" + whereValue2 + "'";

        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            //e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
    }

    public boolean validAccountNumber(String accNumber) {
        boolean valid = false;
        valid = !accNumber.isEmpty();
        return valid;
    }

    public Timestamp todayStart() {
        java.util.Date date = new Date();
        Utilities util = new Utilities();
        int month = util.getCurrentMonth();

        month = month - 1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMinimum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime().getTime());
    }

    public Timestamp todayEnd() {
        java.util.Date date = new Date();
        Utilities util = new Utilities();
        int month = util.getCurrentMonth();
        month = month - 1;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.MONTH, month);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return new Timestamp(calendar.getTime().getTime());
    }

    public int getCurrentMonth() {
        Date date = new Date();
        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        int month = localDate.getMonthValue();
        return month;
    }

    public Timestamp atEndOfDay(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return new Timestamp(calendar.getTime().getTime());
    }

    public Timestamp atStartOfDay(java.util.Date date) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime().getTime());
    }

    public Timestamp getStartOfYearDay(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return new Timestamp(calendar.getTime().getTime());
    }

    public Timestamp getEndOfYearDay(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, 11); // 11 = december
        calendar.set(Calendar.DAY_OF_MONTH, 31); // new years eve
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 000);
        return new Timestamp(calendar.getTime().getTime());
    }

    public static String formatPhone(String msisdn) {
        String start_char = String.valueOf(msisdn.charAt(0));
        String phoneNumber;

        int msisdn_length = msisdn.length();
        if (start_char.equals("+")) {
            phoneNumber = msisdn.substring(1);
        } else if (start_char.equals("2") ) {
            phoneNumber = msisdn;
        } else if (start_char.equals("0") ) {
            msisdn = "+254" + msisdn;
            phoneNumber = msisdn.replace("+2540", "254");
        } else if (start_char.equals("7") ) {
            phoneNumber = "254" + msisdn;
        } else if (start_char.equals("1") ) {
            phoneNumber = "254" + msisdn;
        } else {
            phoneNumber = msisdn;
        }

        return phoneNumber;
    }
}
