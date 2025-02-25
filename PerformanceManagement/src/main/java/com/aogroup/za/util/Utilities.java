/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.util;

import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.datasource.DBConnectionMocash;
import com.aogroup.za.makerchecker.MakerCheckerUtil;
import com.aogroup.za.user.UserUtil;
import com.co.ke.main.EntryPoint;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;

import log.Logging;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

    public String getCurrentFinancialYear() {
        int year = Calendar.getInstance().get(Calendar.YEAR);

        return String.valueOf(year);
    }

    public String getTodayString() {
        java.util.Date date = new java.util.Date();
        date.setTime(date.getTime()); //2020-07-23
        String formattedDate = new SimpleDateFormat("YYYY-MM-dd").format(date);
        return formattedDate;
    }

    public String formatDate(String startDate) {
        Date formattedDate;
        String output = "";
        try {
            formattedDate = new SimpleDateFormat("YYYYMMdd").parse(startDate);
            output = new SimpleDateFormat("YYYY-MM-dd").format(formattedDate);
        } catch (Exception e) {
        }

        return output;
    }

    public String formatDate2(String startDate) {
        Date formattedDate;
        String output = "";
        try {
            formattedDate = new SimpleDateFormat("YYYYMMdd").parse(startDate);
            output = new SimpleDateFormat("YYYY-MM-dd HH:mm").format(formattedDate);
        } catch (Exception e) {
        }
        return output;
    }

    public String getTodayStringTime() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = dtf.format(now);
        return formattedDate;
    }

    public String convertStringToDate(String date) {
        String formattedDate = new SimpleDateFormat("YYYYMMdd").format(date);
        return formattedDate;
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

    public JsonObject fetchSmeUserDetails(JsonObject req) {
        JsonObject response = new JsonObject();
        String email = req.getString("userEmail");
        String sqlQuery = " SELECT u.first_name,u.last_name,u.phone_number,u.app_password,u.type,u.status,u.account_lock,u.company_id,"
                + "i.name,i.account_number,i.customer_number FROM [users] u INNER JOIN [dbo].institution i ON u.company_id = i.id WHERE u.email = '" + email + "'";
        DBConnection conn = new DBConnection();
        String name = "";
        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {
                name = rs.getString("first_name") + " " + rs.getString("last_name");
                response.put("name", name);
                response.put("phoneNumber", rs.getString("phone_number"));
                response.put("pass", rs.getString("app_password"));
                response.put("type", rs.getString("type"));
                response.put("status", rs.getString("status"));
                response.put("accountNumber", rs.getString("account_number"));
                response.put("accountLock", rs.getString("account_lock"));
                response.put("institutionId", rs.getString("company_id"));
                response.put("instName", rs.getString("name"));
                response.put("custNumber", rs.getString("customer_number"));

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }

        return response;
    }

    public JsonObject fetchAccount(String acc) {
        JsonObject jo = new JsonObject();
        String sqlQuery = " SELECT i.name,i.account_number,customer_number,phone_number,alt_phone_number FROM [dbo].institution i WHERE account_number = '" + acc + "'";
        
        DBConnection conn = new DBConnection();
        String account = "";
        String phone = "";
        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {

                account = rs.getString("name");
                phone = rs.getString("phone_number");
                jo.put("name", account);
                jo.put("phone", phone);
                jo.put("account", rs.getString("account_number"));
                jo.put("altPhone", rs.getString("alt_phone_number"));

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }

        return jo;
    }

    public JsonObject fetchAccountUsingCode(String acc) {
        JsonObject jo = new JsonObject();
        String sqlQuery = " SELECT i.name,i.account_number,customer_number,phone_number,alt_phone_number FROM [dbo].institution i WHERE code = '" + acc + "'";
        
        DBConnection conn = new DBConnection();
        String account = "";
        String phone = "";
        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {

                account = rs.getString("name");
                phone = rs.getString("phone_number");
                jo.put("name", account);
                jo.put("phone", phone);
                jo.put("account", rs.getString("account_number"));
                jo.put("altPhone", rs.getString("alt_phone_number"));

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }

        return jo;
    }

    public String fetchAgentDetails(String code) {
        String sqlQuery = " SELECT TOP 1 i.name,i.account_number FROM [dbo].institution i WHERE code = '" + code + "' ORDER BY created_at DESC";
        DBConnection conn = new DBConnection();
        String account = "";
        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {

                account = rs.getString("account_number");

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }

        return account;
    }

    public int insertC2B(JsonObject data) {
        int i = 0;

        String transactionType = data.getString("TransType");
        String transactionId = data.getString("TransID");
        String transactionTime = data.getString("TransTime");
        String transactionAmount = data.getString("TransAmount");
        String businessShortCode = data.getString("BusinessShortCode");
        String billRefNumber = data.getString("accountNo"); // Account Number in PayBill
        String OrgAccountBalance = data.getString("OrgAccountBalance");
//        String thirdPartyTransID = data.getString("ThirdPartyTransID");
        String msisdn = data.getString("MSISDN");
//        String firstName = data.getString("FirstName");
//        String middleName = data.getString("MiddleName");
//        String lastName = data.getString("LastName");
        String code = data.getString("code");
        String narration = data.getString("narration");

        String accountNo = fetchAgentDetails(code);

//        if (lastName != null || lastName.isEmpty()) {
//            lastName = "";
//        }
        String sql = "INSERT INTO mpesa_c2b([MSISDN],[TransType],[TransID],[TransTime],[TransAmount],\n"
                + "  [BusinessShortCode],[BillRefNumber],[Narration],[OrgAccountBalance],[InstCode],[status]) VALUES(?,?,?,?,?,?,?,?,?,?,?)";

        DBConnection dbCon = new DBConnection();

        try (PreparedStatement preparedStatement = dbCon.getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, msisdn);
            preparedStatement.setString(2, transactionType);
            preparedStatement.setString(3, transactionId);
            preparedStatement.setString(4, transactionTime);
            preparedStatement.setString(5, transactionAmount);
            preparedStatement.setString(6, businessShortCode);
            preparedStatement.setString(7, accountNo);
            preparedStatement.setString(8, narration);
            preparedStatement.setString(9, OrgAccountBalance);
            preparedStatement.setString(10, code.toUpperCase());
            preparedStatement.setString(11, "0");
            i = preparedStatement.executeUpdate();

        } catch (Exception e) {
//            data.put("successIndicator", false);
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            try {
                dbCon.closeConn();
            } catch (Exception e) {
                e.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            }
        }
        return i;
    }

    public JsonArray fetchTransactions() {
        JsonArray result = new JsonArray();
        String sqlQuery = " SELECT TOP 100 ThirdPartyTransID,TransID,TransType,TransAmount,BillRefNumber,"
                + "InvoiceNumber,MSISDN,Narration,TransTime FROM mpesa_c2b WHERE status = '1907' ORDER BY TransTime ASC";
        DBConnection conn = new DBConnection();
        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo.put("thirdPartyTransID", rs.getString("ThirdPartyTransID"));
                jo.put("mpesaRef", rs.getString("TransID"));
                jo.put("transType", rs.getString("TransType"));
                jo.put("TransAmount", rs.getString("TransAmount"));
                jo.put("billRefNumber", rs.getString("BillRefNumber"));
                jo.put("invoiceNumber", rs.getString("InvoiceNumber"));
                jo.put("phoneNumber", rs.getString("MSISDN"));
                jo.put("narration", rs.getString("Narration"));
                jo.put("date", rs.getString("TransTime"));
                result.add(jo);

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }

        return result;
    }

    public JsonArray fetchTransactionsToRetry() {
        JsonArray result = new JsonArray();
        String sqlQuery = " SELECT ThirdPartyTransID,TransID,TransType,TransAmount,BillRefNumber,"
                + "InvoiceNumber,MSISDN,Narration,TransTime FROM mpesa_c2b WHERE status = '2' AND ConfirmationStatus ='T24Error'";
        DBConnection conn = new DBConnection();
        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo.put("thirdPartyTransID", rs.getString("ThirdPartyTransID"));
                jo.put("transID", rs.getString("TransID"));
                jo.put("transType", rs.getString("TransType"));
                jo.put("amount", rs.getString("TransAmount"));
                jo.put("billRefNumber", rs.getString("BillRefNumber"));
                jo.put("invoiceNumber", rs.getString("InvoiceNumber"));
                jo.put("phoneNumber", rs.getString("MSISDN"));
                jo.put("narration", rs.getString("Narration"));
                jo.put("date", rs.getString("TransTime"));
                result.add(jo);

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }

        return result;
    }

    public String formatPhoneNumber(String phoneNumber) {
        try {

            if (phoneNumber.length() == 10) {
                phoneNumber = "254" + phoneNumber.substring(phoneNumber.length() - 9);
            } else if (phoneNumber.length() > 10) {
                phoneNumber = "254" + phoneNumber.substring(phoneNumber.length() - 9);
            } else if (phoneNumber.length() < 10) {
                phoneNumber = "254" + phoneNumber.substring(phoneNumber.length() - 9);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return phoneNumber;
    }

    public int runDBQuery(String query) {
        DBConnection conn = new DBConnection();
        int i = 0;
        try {
            i = conn.update_db(query);
            i = 1;
        } catch (Exception e) {
            i = 0;
            logger.applicationLog(logger.logPreString() + "Error - " + e.getMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        return i;
    }

    public int updateTransaction(String table, String field, String value, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field + "] = '" + value + "' WHERE [" + whereField + "] = '" + whereValue + "'";
        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            e.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            conn.closeConn();
        }
        return updateSuccess;
    }

    public int updateTransactionMocash(String table, String field, String value, String whereField, String whereValue, String whereField2, String whereValue2) {
        String query = "UPDATE [" + table + "] SET [" + field + "] = '" + value + "' WHERE [" + whereField + "] = '" + whereValue + "' AND [" + whereField2 + "] = '" + whereValue2 + "'";
        System.out.println("query :: " + query);
        DBConnectionMocash conn = new DBConnectionMocash();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            e.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            conn.closeConn();
        }
         System.out.println("query :: " + updateSuccess);
        return updateSuccess;
    }

    public int updateTransaction(String table, String field1, String value1, String field2, String value2, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "' WHERE [" + whereField + "] = '" + whereValue + "'";
        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
    }
    
        public int updateTransactionMocash2(String table, String field1, String value1, String field2, String value2, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "' WHERE [" + whereField + "] = '" + whereValue + "'";
        DBConnectionMocash conn = new DBConnectionMocash();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
    }
    

    public int updateTransactionMultipleSearch(String table, String field1, String value1, String whereField, String whereValue, String whereField1, String whereValue2) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "' WHERE [" + whereField + "] = '" + whereValue + "' AND [" + whereField1 + "] = '" + whereValue2 + "'";

        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
    }

    public int updateTransaction(String table, String field1, String value1, String field2, String value2, String whereField, String whereValue, String whereField2, String whereValue2) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "' WHERE [" + whereField + "] = '" + whereValue + "' AND [" + whereField2 + "] = '" + whereValue2 + "'";

        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
    }

    public int updateTransactionAsIs(String table, String field1, String value1, String field2, String value2, String field3, String value3, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "',[" + field3 + "] = '" + value3 + "' WHERE [" + whereField + "] = '" + whereValue + "'";

        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
    }

    public int updateTransactionAsIs(String table, String field1, String value1, String field2, String value2, String field3, String value3, String field4, String value4, String whereField, String whereValue) {
        String query = "UPDATE [" + table + "] SET [" + field1 + "] = '" + value1 + "',[" + field2 + "] = '" + value2 + "',[" + field3 + "] = '" + value3 + "',[" + field4 + "] = '" + value4 + "' WHERE [" + whereField + "] = '" + whereValue + "'";

        DBConnection conn = new DBConnection();
        int updateSuccess = 0;
        try {
            updateSuccess = conn.update_db(query);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        if (updateSuccess != 0) {
            updateSuccess = 1;
        }
        return updateSuccess;
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
            e.printStackTrace();
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
            e.printStackTrace();
        } finally {
            con.closeConn();
        }

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
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return status;
    }

    public boolean checkIfExists(String searchTable, String searchField, String value, String searchField2, String value2) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField + " = '" + value + "'  AND " + searchField2 + " = '" + value2 + "'";

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
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        if (count > 0) {
            exists = true;
        }
        return exists;
    }

    public boolean checkIfExistsMocash(String searchTable, String searchField, String value, String searchField2, String value2) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField + " = '" + value + "'  AND " + searchField2 + " = '" + value2 + "'";

        DBConnectionMocash con = new DBConnectionMocash();
        int count = 0;
        boolean exists = false;
        try {
            ResultSet rs = con.query_all(query);
            while (rs.next()) {
                count = rs.getInt("counts");
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        if (count > 0) {
            exists = true;
        }
        return exists;
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
            e.printStackTrace();
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
            e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }
        return exists;
    }

    public boolean checkIfExistsMultipleMocash(String searchTable, String searchField1, String value1, String searchField2, String value2, String searchField3, String value3) {
        String query = "SELECT count(*) as counts FROM " + searchTable + "  WHERE " + searchField1 + " = '" + value1 + "'  AND " + searchField2 + " = '" + value2 + "'  AND " + searchField3 + " = '" + value3 + "'";
//        String q1 = "SELECT count(*) as counts FROM customer_login_otp  WHERE user_phone = '" + +"'  AND user_otp = '" + +"'  AND used_state = '0'";

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
            e.printStackTrace();
        } finally {
            con.closeConn();
        }

        if (count > 0) {
            exists = true;
        }
        return exists;
    }

    public int saveRequest(JsonObject data) {
        Utilities util = new Utilities();
        JsonObject agent = new JsonObject();
        int res = 0;

        String userId = "";
        String account = "";
        String transactionType = "";
        String amount = "";
        String transactedBy = "";
        String transactorPhone = "";
        String phoneNumber = "";
        int status = 0;
        String agentName = "";
        String branchCode = "";
        String narration = "";
        String otherAccount = "";
        String customerName = "";
        String date = data.getString("date");
        double depComm = 0.00;

        agentName = data.getJsonObject("agent").getString("name");

        Timestamp timestamp = new Timestamp(System.currentTimeMillis());

        userId = data.getJsonObject("agent").getString("agentId");

        if (data.containsKey("creditAccount")) {
            account = data.getString("creditAccount");
        }
        if (data.containsKey("debitAccount")) {
            account = data.getString("debitAccount");
        }
        String referenceNumber = data.getString("referenceNumber");

        if (data.containsKey("transactionType")) {
            transactionType = data.getString("transactionType");
        }
        if (data.containsKey("amount")) {
            amount = data.getString("amount");
        }
        if (data.containsKey("phoneNumber")) {
            phoneNumber = data.getString("phoneNumber");
        }
        if (data.containsKey("depositorPhone")) {
            transactorPhone = data.getString("depositorPhone");
        }
        if (data.containsKey("depositorName")) {
            transactedBy = data.getString("depositorName");
        }

        branchCode = data.getJsonObject("agent").getString("branch");

        if (data.containsKey("accountTo")) {
            otherAccount = data.getString("accountTo");
        }

        if (data.containsKey("floatAccount")) {
            otherAccount = data.getString("floatAccount");
        }

        if (data.containsKey("narration")) {
            narration = data.getString("narration");
        }

        if (data.containsKey("name")) {
            customerName = data.getString("name");
        }

        if (data.containsKey("depComm")) {
            depComm = data.getDouble("depComm");
        }
        int agentType = data.getJsonObject("agent").getInteger("typeId");
        String idNo = data.getString("idNo");

        String query = "INSERT INTO transactions ([agent_id],[type_id],[customer_account],[other_account],[reference_number],[tran_type],[amount],"
                + "[transacted_by],[status],[agent_name],[branch_code],[customer_name],[narration],[created_at],[updated_at],[client_id],[commission])"
                + " VALUES ('" + userId + "','" + agentType + "','" + account + "','" + otherAccount + "','" + referenceNumber + "',"
                + "'" + transactionType + "','" + amount + "','" + transactedBy + "',"
                + "'" + status + "','" + agentName + "',"
                + "'" + branchCode + "','" + customerName + "','" + narration + "','" + date + "','" + date + "','" + idNo + "','" + depComm + "')";

        res = util.runDBQuery(query);

        return res;
    }

    public int fetchRemainingCount(String batchId) {
        int count = 0;
        DBConnectionMocash con = new DBConnectionMocash();
        String query = "SELECT COUNT(id) AS counts FROM bulk_deposits WHERE batch_id = '" + batchId + "' AND batch_status = '1'";
        try {
            ResultSet rs = con.query_all(query);
            while (rs.next()) {
                count = Integer.parseInt(rs.getString("counts"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return count;
    }

//    public static String getAccessToken(String app_key, String app_secret) {
//
//        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
//
//        String access_token = "";
//        try {
//
////            String TOKEN_URL = "https://sandbox.safaricom.co.ke/oauth/v1/generate?grant_type=client_credentials";
//            String appKeySecret = app_key + ":" + app_secret;
//            byte[] bytes = appKeySecret.getBytes("ISO-8859-1");
//            String auth = com.sun.org.apache.xerces.internal.impl.dv.util.Base64.encode(bytes);
//
//            OkHttpClient client = new OkHttpClient();
//
//            Request request = new Request.Builder()
//                    .url(EntryPoint.TOKEN_URL)
//                    .get()
//                    .addHeader("authorization", "Basic " + auth)
//                    .addHeader("cache-control", "no-cache")
//                    .build();
//
//            Response response = client.newCall(request).execute();
//            JsonObject accessT = new JsonObject(response.body().string());
//            
//
//            if (accessT.containsKey("access_token")) {
//                access_token = accessT.getString("access_token");
//            }
//
//            response.close();
//        } catch (Exception ex) {
//            access_token = "";
//            ex.printStackTrace();
//        }
//        // ("access_token r  : " + access_token);
//
//        return access_token;
//    }

    public JsonObject validateOTP(JsonObject data) {
        String otp = data.getString("otp");
        boolean status = false;

        DBConnection conn = new DBConnection();
        String name = data.getJsonObject("user").getString("instName");
        String email = data.getString("userEmail");

        String validationOTP = null;
        try {
            validationOTP = Common.hashSHA256(otp + email.toLowerCase().trim() + name.toLowerCase().trim());
        } catch (NoSuchAlgorithmException ex) {
            java.util.logging.Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        String res = fetchOTP(validationOTP);
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        LocalDateTime expiry = LocalDateTime.parse(res, dateTimeFormatter);

        if (now.isAfter(expiry) == true) {
            data.put("response", "999");
            data.put("responseDescription", "OTP has expired");
        } else {
            status = checkIfExistsMultiple("user_login_otp", "user_email", email, "user_otp", validationOTP, "used_state", "0");

            if (status) {
                // Update otp field
                String sql = "UPDATE user_login_otp SET used_state='1' WHERE user_email='" + email + "' AND used_state='0'";
                int i = conn.update_db(sql);
                data.put("response", "000");
                data.put("responseDescription", "OTP Valid");
            } else {
                data.put("response", "999");
                data.put("responseDescription", "OTP Invalid");
            }

        }
        return data;
    }

    public String fetchOTP(String otp) {
        String res = "";
        String sqlQuery = " SELECT TOP (1) expiry FROM user_login_otp WHERE user_otp = '" + otp + "' ";
        DBConnection conn = new DBConnection();
        String name = "";

        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {
                res = rs.getString("expiry");

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        return res;
    }

    //MOCASH
    public JsonObject fetchCustomerDetails(String phone) {
        JsonObject response = new JsonObject();
        String no = formatPhoneNumber(phone);
        String query = " SELECT PhoneNumber,CustomerNumber,FirstName,LastName,IDNumber"
                + " FROM Customer WHERE PhoneNumber = '" + no + "'";

        DBConnectionMocash conn = new DBConnectionMocash();

        try {
            ResultSet rs = conn.query_all(query);
            while (rs.next()) {
                response.put("phoneNumber", rs.getString("PhoneNumber"));
                response.put("customerNumber", rs.getString("PhoneNumber"));
                response.put("firstName", rs.getString("firstName"));
                response.put("lastName", rs.getString("lastName"));
                response.put("idNumber", rs.getString("idNumber"));
            }
        } catch (Exception e) {
        }
        return response;
    }

    public JsonObject validateMocashOTP(JsonObject data) {
        String otp = data.getString("otp");
        boolean status = false;

        DBConnectionMocash conn = new DBConnectionMocash();

        String name = data.getJsonObject("user").getString("firstName") + " " + data.getJsonObject("user").getString("lastName");
        String numbers = data.getJsonObject("user").getString("customerNumber") + " " + data.getJsonObject("user").getString("idNumber");
        String phone = formatPhoneNumber(data.getString("phone"));
        
        String validationOTP = null;
        try {
            validationOTP = Common.hashSHA256(otp + name.toLowerCase().trim() + numbers.trim());
        } catch (NoSuchAlgorithmException ex) {
            java.util.logging.Logger.getLogger(Utilities.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        String res = fetchMocashOTP(validationOTP);

        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
       
        LocalDateTime expiry = null;
        try {
            expiry = LocalDateTime.parse(res, dateTimeFormatter);
        } catch (Exception e) {
            e.printStackTrace();
        }

       

        if (now.isAfter(expiry) == true) {
            data.put("response", "999");
            data.put("responseDescription", "OTP has expired");
        } else {
//status = checkIfExistsMultipleMocash("customer_login_otp", "user_phone", phone, "user_otp", validationOTP, "used_state", "0");

            String q1 = "SELECT count(*) as counts FROM customer_login_otp  WHERE user_phone = '" + phone + "'  AND user_otp = '" + validationOTP + "'  AND used_state = '0'";

            DBConnectionMocash con = new DBConnectionMocash();
            int count = 0;
            boolean exists = false;
            try {
                ResultSet rs = con.query_all(q1);

                while (rs.next()) {
                    count = rs.getInt("counts");
                }
            } catch (Exception e) {
                logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
                e.printStackTrace();
            } finally {
                con.closeConn();
            }

            if (count > 0) {
                exists = true;
            }

            if (exists) {
                // Update otp field
                String sql = "UPDATE customer_login_otp SET used_state='1' WHERE user_phone='" + phone + "' AND used_state='0'";
                int i = conn.update_db(sql);
                data.put("response", "000");
                data.put("responseDescription", "OTP Valid");
            } else {
                data.put("response", "999");
                data.put("responseDescription", "OTP Invalid");
            }

        }
        return data;
    }

    public String fetchMocashOTP(String otp) {
        String res = "";
        String sqlQuery = " SELECT TOP (1) expiry FROM customer_login_otp WHERE user_otp = '" + otp + "' ";
        DBConnectionMocash conn = new DBConnectionMocash();
        String name = "";

        try {
            ResultSet rs = conn.query_all(sqlQuery);
            while (rs.next()) {
                res = rs.getString("expiry");

            }
        } catch (SQLException e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
        return res;
    }

    public JsonObject createSerial(JsonObject data) {
        JsonObject res = new JsonObject();
        DBConnection conn = new DBConnection();

        String sql = "UPDATE users SET serial_number='" + data.getString("serial") + "',serial_status='1' WHERE email='" + data.getString("userEmail") + "'";
        int i = conn.update_db(sql);
        if (i == 1) {
            JsonObject sms = new JsonObject();
            res.put("response", "000");
            res.put("responseDescription", "serial updated");

        } else {
            res.put("response", "999");
            res.put("responseDescription", "serial update failed");
        }
        return res;
    }

    public JsonObject createMocashSerial(JsonObject data) {
        JsonObject res = new JsonObject();
        DBConnectionMocash conn = new DBConnectionMocash();

        String sql = "UPDATE LoginValidation SET serial_number='" + data.getString("serial") + "',serial_status='1' WHERE PhoneNumber='" + data.getString("phone") + "'";
        int i = conn.update_db(sql);
        if (i == 1) {
            JsonObject sms = new JsonObject();
            res.put("response", "000");
            res.put("responseDescription", "serial updated");

        } else {
            res.put("response", "999");
            res.put("responseDescription", "serial update failed");
        }
        return res;
    }

    public JsonArray fetchBatchTransactions(String batch) {
        JsonArray transactions = new JsonArray();
        DBConnectionMocash con = new DBConnectionMocash();
        String q1 = "  SELECT trans_amount,thirdparty_trans_id,trans_id,trans_type,"
                + "trans_time,bill_ref_number,msisdn,narration,business_shortcode,inst_code"
                + " from bulk_deposits where batch_status = '1' and batch_id = '" + batch + "'";

        try {
            ResultSet rs = con.query_all(q1);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo.put("thirdPartyTransId", rs.getString("thirdparty_trans_id"))
                        .put("transID", rs.getString("trans_id"))
                        .put("transType", rs.getString("trans_type"))
                        .put("transTime", rs.getString("trans_time"))
                        .put("billRefNumber", rs.getString("bill_ref_number"))
                        .put("phoneNumber", rs.getString("msisdn"))
                        .put("narration", rs.getString("narration"))
                        .put("code", rs.getString("inst_code"))
                        .put("shortCode", rs.getString("business_shortcode"));
                if (rs.getString("trans_amount").contains(",")) {
                    jo.put("amount", rs.getString("trans_amount").replace(",", ""));
                } else {
                    jo.put("amount", rs.getString("trans_amount"));
                }

                transactions.add(jo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return transactions;
    }

    public JsonArray fetchBatchTransactions() {
        JsonArray transactions = new JsonArray();
        DBConnectionMocash con = new DBConnectionMocash();
        String q1 = "  SELECT trans_amount,thirdparty_trans_id,trans_id,trans_type,"
                + "trans_time,bill_ref_number,msisdn,narration,business_shortcode,inst_code"
                + " from bulk_deposits where batch_status = '4'";

        try {
            ResultSet rs = con.query_all(q1);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo.put("amount", rs.getString("trans_amount"))
                        .put("thirdPartyTransId", rs.getString("thirdparty_trans_id"))
                        .put("transID", rs.getString("trans_id"))
                        .put("transType", rs.getString("trans_type"))
                        .put("transTime", rs.getString("trans_time"))
                        .put("billRefNumber", rs.getString("bill_ref_number"))
                        .put("phoneNumber", rs.getString("msisdn"))
                        .put("narration", rs.getString("narration"))
                        .put("code", rs.getString("inst_code"))
                        .put("shortCode", rs.getString("business_shortcode"));

                transactions.add(jo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return transactions;
    }

    public JsonObject fetchInstitutions() {
        JsonObject details = new JsonObject();
        JsonArray institutions = new JsonArray();
        String query = "SELECT [id],[name],[category_id],[account_number],[email],[phone_number],[customer_number]"
                + ",[created_at],[updated_at],[deleted_at],[code],[branch],[status],[alt_phone_number] "
                + "FROM institution ORDER BY created_at DESC";
        DBConnection con = new DBConnection();
        try {
            ResultSet rs = con.query_all(query);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo.put("id", rs.getInt("id"));
                jo.put("name", rs.getString("name"));
                jo.put("categoryId", rs.getInt("category_id"));
                jo.put("accountNumber", rs.getString("account_number"));
                jo.put("email", rs.getString("email"));
                jo.put("phoneNumber", rs.getString("phone_number"));
                jo.put("customerNumber", rs.getString("customer_number"));
                jo.put("createdAt", rs.getString("created_at"));
                jo.put("updatedAt", rs.getString("updated_at"));
                jo.put("deletedAt", rs.getString("deleted_at"));
                jo.put("code", rs.getString("code"));
                jo.put("branch", rs.getString("branch"));
                jo.put("status", rs.getString("status"));
                jo.put("altPhoneNumber", rs.getString("alt_phone_number"));
                institutions.add(jo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        details.put("institutions", institutions);
        return details;
    }

    public JsonObject fetchUsers() {
        JsonObject details = new JsonObject();
        JsonArray users = new JsonArray();
        String query = "SELECT [id],[first_name],[last_name],[phone_number],[email],[type],[creator_id],"
                + "[verifier_id],[status],[account_lock],[company_id],[created_at],[updated_at],[branch],"
                + "[serial_status] FROM users ORDER BY created_at DESC";
        DBConnection con = new DBConnection();
        try {
            ResultSet rs = con.query_all(query);
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                jo.put("id", rs.getInt("id"));
                jo.put("firstName", rs.getString("first_name"));
                jo.put("lastName", rs.getString("last_name"));
                jo.put("phoneNumber", rs.getString("phone_number"));
                jo.put("email", rs.getString("email"));
                jo.put("type", rs.getString("type"));
                jo.put("creatorId", rs.getInt("creator_id"));
                jo.put("verifierId", rs.getInt("verifier_id"));
                jo.put("status", rs.getString("status"));
                jo.put("accountLock", rs.getInt("account_lock"));
                jo.put("companyId", rs.getString("company_id"));
                jo.put("createdAt", rs.getString("created_at"));
                jo.put("updatedAt", rs.getString("updated_at"));
                jo.put("branch", rs.getString("branch"));
                jo.put("serialStatus", rs.getString("serial_status"));
                users.add(jo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        details.put("users", users);
        return details;
    }

//    public JsonArray fetchTransactions() {
//        JsonArray transactions = new JsonArray();
//        DBConnectionMocash con = new DBConnectionMocash();
//        String q1 = "  SELECT trans_amount,thirdparty_trans_id,trans_id,trans_type,"
//                + "trans_time,bill_ref_number,msisdn,narration,business_shortcode,inst_code"
//                + " from mpesa_c2b where status = '1";
//        System.out.println("Q! :: " + q1);
//        try {
//            ResultSet rs = con.query_all(q1);
//            while (rs.next()) {
//                JsonObject jo = new JsonObject();
//                jo.put("amount", rs.getString("trans_amount"))
//                        .put("thirdPartyTransId", rs.getString("thirdparty_trans_id"))
//                        .put("transID", rs.getString("trans_id"))
//                        .put("transType", rs.getString("trans_type"))
//                        .put("transTime", rs.getString("trans_time"))
//                        .put("billRefNumber", rs.getString("bill_ref_number"))
//                        .put("phoneNumber", rs.getString("msisdn"))
//                        .put("narration", rs.getString("narration"))
//                        .put("code", rs.getString("inst_code"))
//                        .put("shortCode", rs.getString("business_shortcode"));
//
//                transactions.add(jo);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            con.closeConn();
//        }
//        return transactions;
//    }
    public int saveUploadRequest(JsonObject req) {
        int save = 0;
        DBConnection con = new DBConnection();

        try {
            // Parse and reformat the `transTime` field
            String transTime = req.getString("transTime");
            SimpleDateFormat inputFormat = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String formattedTransTime = outputFormat.format(inputFormat.parse(transTime));

            String q1 = "insert into mpesa_c2b (TransID,TransAmount,TransType,TransTime,"
                    + "BusinessShortCode,InstCode,BillRefNumber,Narration,MSISDN,status,upload_id) values "
                    + "('" + req.getString("transID") + "',"
                    + "'" + req.getString("amount") + "','" + req.getString("transType") + "',"
                    + "'" + formattedTransTime + "','" + req.getString("shortCode") + "',"
                    + "'" + req.getString("code") + "','" + req.getString("billRefNumber") + "',"
                    + "'" + req.getString("narration") + "','" + req.getString("phoneNumber") + "','0','" + req.getString("thirdPartyTransId") + "')";

            save = con.update_db(q1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return save;
    }

    public int saveUploadRequestStaging(JsonObject req) {
        int save = 0;
        DBConnectionMocash con = new DBConnectionMocash();
//                req.put("transId", rowData.get(0));
//                req.put("time", rowData.get(1));
//                req.put("transStatus", rowData.get(4));
//                req.put("amount", rowData.get(5));
//                req.put("transType", rowData.get(10));
//                req.put("msisdn", msisdn);
//                req.put("billRefNo", rowData.get(13));
        String refNo = "UPLOAD-" + req.getString("transId");
        try {

            String q1 = "  insert into bulk_deposits (batch_id,thirdparty_trans_id,trans_id,"
                    + "trans_type,trans_time,trans_amount,business_shortcode,msisdn,confirmation_status,"
                    + "narration,inst_code,batch_status) values ('" + req.getValue("batchId") + "','" + refNo + "',"
                    + "'" + req.getValue("transId") + "','" + req.getValue("transType") + "','" + req.getValue("transTime") + "',"
                    + "'" + req.getValue("amount") + "','" + req.getValue("shortCode") + "','" + req.getValue("msisdn") + "',"
                    + "'" + req.getValue("transStatus") + "','" + req.getValue("narration") + "','" + req.getValue("billRefNo") + "','1')";

            save = con.update_db(q1);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            con.closeConn();
        }
        return save;
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

    public String maskString(String strText, int start, int end, char maskChar)
            throws Exception {

        if (strText == null || strText.equals("")) {
            return "";
        }

        if (start < 0) {
            start = 0;
        }

        if (end > strText.length()) {
            end = strText.length();
        }

        if (start > end) {
            throw new Exception("End index cannot be greater than start index");
        }

        int maskLength = end - start;

        if (maskLength == 0) {
            return strText;
        }

        StringBuilder sbMaskString = new StringBuilder(maskLength);

        for (int i = 0; i < maskLength; i++) {
            sbMaskString.append(maskChar);
        }

        return strText.substring(0, start)
                + sbMaskString.toString()
                + strText.substring(start + maskLength);
    }

}


