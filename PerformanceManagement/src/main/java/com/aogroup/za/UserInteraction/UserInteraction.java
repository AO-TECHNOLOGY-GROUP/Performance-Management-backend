package com.aogroup.za.UserInteraction;

import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.user.UserUtil;
import com.aogroup.za.util.Common;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Date;
import java.util.Random;
import java.util.UUID;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class UserInteraction extends AbstractVerticle{
    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;
    
    
     @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        
        //apis
        
        eventBus.consumer("SEND_OTP", this::sendOTP);
        eventBus.consumer("VERIFY_OTP", this::verifyOTP);
        eventBus.consumer("FETCHPHONENUMBER", this::fetchPhoneNumber);
        eventBus.consumer("SUBMIT_TASK", this::submitTask);
        eventBus.consumer("FETCHESCALATEDTASKS", this::fetchEscalatedTasks);
        eventBus.consumer("FETCH_CUSTOMER_HISTORY", this::fetchCustomerInteractionHistory);
        eventBus.consumer("FETCHTASKSBYMULTIPLE", this::fetchTasksByIsmultiple);
        eventBus.consumer("FETCHPENDINGCONFIRMATIONS", this::fetchPendingConfirmations);
        eventBus.consumer("FETCHPENDINGCONFIRMATIONSISMULTIPLE", this::fetchPendingConfirmationsisMultiple);
    }
    

    private void sendOTP(Message<JsonObject> message) {
        JsonObject data = message.body();
        MultiMap headers = message.headers();

        if (headers.isEmpty()) {
            message.fail(666, "Unauthenticated User");
            return;
        }

        JsonObject response = new JsonObject();
        String otp = new Common().generateRandom(6);
        String customerNumber = data.getString("customerNumber");

        String query = "SELECT first_name, last_name, phone_number FROM [Dfa].[dbo].[customer_details_stub] WHERE customer_number = ?";

        DBConnection dbConnection = new DBConnection();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, customerNumber);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String phoneNumber = rs.getString("phone_number");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");

                // Update unused previous codes
                String updateSql = "UPDATE verification_codes SET [status] = 1 WHERE customer_number = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setString(1, customerNumber);
                    updateStmt.executeUpdate();
                }

                // Insert new OTP
                String insertSql = "INSERT INTO verification_codes ([code], [phone_number], [intent], [customer_number]) VALUES (?, ?, 'Customer Verification', ?)";
                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, otp);
                    insertStmt.setString(2, phoneNumber);
                    insertStmt.setString(3, customerNumber);
                    insertStmt.executeUpdate();
                }

                // Send SMS
                String otpSMS = "Dear " + name.toUpperCase() + ", your One Time Password is " + otp + ".";
                JsonObject messageObject = new JsonObject()
                        .put("phonenumber", phoneNumber)
                        .put("msg", otpSMS);

                eventBus.send("COMMUNICATION_ADAPTOR", messageObject);

                response.put("responseCode", "000")
                        .put("responseDescription", "OTP sent successfully.");
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "Customer not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("responseCode", "999")
                    .put("responseDescription", "OTP failed to send.");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
                dbConnection.closeConn();  // Close connection properly
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        message.reply(response);
    }

    private void verifyOTP(Message<JsonObject> message) {
        JsonObject data = message.body();
        MultiMap headers = message.headers();

        if (headers.isEmpty()) {
            message.fail(999, "Unauthenticated User");
            return;
        }

        String customerNumber = data.getString("customerNumber");
        String otp = data.getString("otp");
        JsonObject response = new JsonObject();

        String query = "SELECT code, phone_number FROM verification_codes WHERE customer_number = ? AND [status] = 0";

        DBConnection dbConnection = new DBConnection();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, customerNumber);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String dbOtp = rs.getString("code");
                String phoneNumber = rs.getString("phone_number");

                if (dbOtp.equals(otp)) {
                    // Update OTP status
                    String updateSql = "UPDATE verification_codes SET [status] = 1 WHERE customer_number = ?";
                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setString(1, customerNumber);
                        updateStmt.executeUpdate();
                    }

                    response.put("responseCode", "000")
                            .put("responseDescription", "OTP verified successfully.")
                            .put("phoneNumber", phoneNumber);
                } else {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Invalid OTP.");
                }
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "No OTP found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("responseCode", "999")
                    .put("responseDescription", "OTP verification failed.");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
                dbConnection.closeConn();  // Close connection properly
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        message.reply(response);
    }

    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return false;
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    private void fetchPhoneNumber(Message<JsonObject> message) {
        JsonObject data = message.body();
        MultiMap headers = message.headers();

        if (headers.isEmpty()) {
            message.fail(666, "Unauthenticated User");
            return;
        }

        JsonObject response = new JsonObject();
        String customerNumber = data.getString("customerNumber");

        String query = "SELECT first_name, last_name, phone_number FROM [Dfa].[dbo].[customer_details_stub] WHERE customer_number = ?";

        DBConnection dbConnection = new DBConnection();
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            conn = dbConnection.getConnection();
            stmt = conn.prepareStatement(query);
            stmt.setString(1, customerNumber);
            rs = stmt.executeQuery();

            if (rs.next()) {
                String phoneNumber = rs.getString("phone_number");
                String name = rs.getString("first_name") + " " + rs.getString("last_name");
                
                JsonObject result = new JsonObject();
                result
                        .put("phonenumber", phoneNumber)
                        .put("name", name);


                response.put("responseCode", "000")
                        .put("responseDescription", "Customer details fetched successfully.")
                        .put("data", result);
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "Customer not found.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.put("responseCode", "999")
                    .put("responseDescription", "Failed to fetch customer details");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
                dbConnection.closeConn();  // Close connection properly
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        message.reply(response);
    }

    private void submitTask(Message<JsonObject> message) {
        JsonObject response = new JsonObject();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            message.fail(666, "Unauthenticated User");
            return;
        }

        String user_uuid = headers.get("user_uuid");
        String user_branch_id = headers.get("user_branch_id");
        String user_type = headers.get("user_role_id");

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String ismultiple = requestBody.getString("IsMultiple");

        if (ismultiple.equals("0")) {
            String employeeTaskId = requestBody.getString("EmployeeTaskId");
            String customerPhoneNumber = requestBody.getString("CustomerPhoneNumber");
            String customerNumber = requestBody.getString("customerNumber");
            String AccountNumber = requestBody.getString("AccountNumber");
            String channel = requestBody.getString("Channel");
            String name = requestBody.getString("name");
            String lineofBusiness = requestBody.getString("Line_of_Business");
            String header = requestBody.getString("Header");
            String notes = requestBody.getString("Notes");
            String longitude = requestBody.getString("Longitude");
            String latitude = requestBody.getString("Latitude");
            String achievedStr = requestBody.getString("Achieved", "").trim();
            int achieved = 0; // Default to null
            if (!achievedStr.isEmpty()) {
                if (achievedStr.equals("1")) {
                    achieved = 1;
                } else if (achievedStr.equals("0")) {
                    achieved = 0;
                } else {
                    response.put("responseCode", "999").put("responseDescription", "Error: Achieved must be '1' or '0'.");
                    message.reply(response);
                    return;
                }
            }
            String achievedDate = requestBody.getString("AchievedDate");
          
            int diaspora = requestBody.containsKey("Diaspora") ? Integer.parseInt(requestBody.getString("Diaspora")) : 0;
            int referral = requestBody.containsKey("Referral") ? Integer.parseInt(requestBody.getString("Referral")) : 0;

            
            String referralCustomerNumber = requestBody.getString("ReferralCustomerNumber");

            String amountStr = requestBody.getString("Amount");
            double amount = (amountStr != null && !amountStr.isEmpty()) ? Double.parseDouble(amountStr) : 0.0;

            
            String escalatedToUserUUID = requestBody.getString("EscalatedToUserUUID", null);
            String escalatedToEmail = requestBody.getString("EscalatedToEmail", null);
            String escalatedToPhoneNumber = requestBody.getString("EscalatedToPhoneNumber", null);
            String escalatedToName = requestBody.getString("EscalatedToName", null);
     
            Double deposit = null;
                if (requestBody.containsKey("deposit") && !requestBody.getString("deposit").isEmpty()) {
                    deposit = Double.parseDouble(requestBody.getString("deposit"));
                }

            String depositDate = requestBody.getString("depositdate");

            Double loanProspect = null;

            if (requestBody.containsKey("loan prospect") && requestBody.getString("loan prospect") != null && !requestBody.getString("loan prospect").isEmpty()) {
                try {
                    loanProspect = Double.parseDouble(requestBody.getString("loan prospect"));
                } catch (NumberFormatException e) {
                    loanProspect = null; // or log error
                }
            }


            String loanProspectDate = requestBody.getString("loan prospect date");

            String newAccount = requestBody.getString("new account");
            if (newAccount != null && newAccount.isEmpty()) {
                newAccount = null;
            }
            String newAccountDate = requestBody.getString("new account date");


            int status = 0; // Default: No escalation
                if (isValidUUID(escalatedToUserUUID)) {
                    if (escalatedToUserUUID.equals(user_uuid)) {
                        status = 1; // Escalated to self
                    } else {
                        status = 2; // Escalated to someone else
                    }
                }
            try {
                String fetchTargetSQL = "SELECT Target FROM EmployeeTasks WHERE Id = ?";
                int expectedTarget = 0;

                try (PreparedStatement psFetchTarget = connection.prepareStatement(fetchTargetSQL)) {
                    if (isValidUUID(employeeTaskId)) {
                        psFetchTarget.setString(1, employeeTaskId);
                    } else {
                        psFetchTarget.setNull(1, java.sql.Types.OTHER);
                    }
                    ResultSet rsTarget = psFetchTarget.executeQuery();
                    if (rsTarget.next()) {
                        expectedTarget = rsTarget.getInt("Target");
                    } else {
                        response.put("responseCode", "999").put("responseDescription", "Error: EmployeeTaskId not found.");
                        message.reply(response);
                        return;
                    }
                }
                
                String businessRealizedId = null;
                String businessRealizedClusteredId = null;
                
                if (deposit != null || (newAccount != null && !newAccount.isEmpty()) || loanProspect != null) {
                    String insertBusinessSQL = "INSERT INTO BusinessRealized (deposit, depositDate, newAccount, newAccountDate, loanProspect, loanProspectDate, createdAt, updatedAt, ConfirmationStatus, AcountOpeningStatus, LoanStatus, NewAccountsStatus) " +
                                               "VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 0, 0, 0, 0)";
                    
                    String sql1 = "SELECT [Id]  FROM [BusinessRealized] WHERE [ClusteredId] = ? ";

                    try (PreparedStatement psBusiness = connection.prepareStatement(insertBusinessSQL, PreparedStatement.RETURN_GENERATED_KEYS);
                     PreparedStatement prQuery = connection.prepareStatement(sql1)) {


                        if (deposit != null) {
                            psBusiness.setDouble(1, deposit);
                        } else {
                            psBusiness.setNull(1, Types.DOUBLE);
                        }

                        if (depositDate != null && !depositDate.trim().isEmpty()) {
                            psBusiness.setString(2, depositDate);
                        } else {
                            psBusiness.setNull(2, Types.DATE);
                        }

                        if (newAccount != null) {
                            psBusiness.setString(3, newAccount);
                        } else {
                            psBusiness.setNull(3, Types.VARCHAR);
                        }

                       if (newAccountDate != null && !newAccountDate.trim().isEmpty()) {
                            psBusiness.setString(4, newAccountDate);
                        } else {
                            psBusiness.setNull(4, Types.DATE);
                        }

                        if (loanProspect != null) {
                            psBusiness.setDouble(5, loanProspect);
                        } else {
                            psBusiness.setNull(5, Types.DOUBLE);
                        }


                        if (loanProspectDate != null && !loanProspectDate.trim().isEmpty()) {
                            psBusiness.setString(6, loanProspectDate);
                        } else {
                            psBusiness.setNull(6, Types.DATE);
                        }

                        psBusiness.executeUpdate();

                        // Retrieve generated BusinessRealizedId
                        ResultSet rs = psBusiness.getGeneratedKeys();
                        if (rs.next()) {
                            businessRealizedClusteredId = rs.getString(1);
                        }
                        System.out.println("Retrieved BusinessRealized ClusteredID: " + businessRealizedClusteredId);
                        
                                // Get business realized id
                        prQuery.setString(1, businessRealizedClusteredId);
                        prQuery.execute();

                        ResultSet resultSet1 = prQuery.getResultSet();
                        if (resultSet1.next()) {
                            businessRealizedId = resultSet1.getString("Id");
                        } else {
                            throw new SQLException("Failed to Fetch Inserted business realized id.");
                        }
                        resultSet1.close();
                        
                        System.out.println("Retrieved BusinessRealized ID: " + businessRealizedId);

                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                
                String selectReferrals = "SELECT first_name + ' ' + last_name AS Name, phone_number FROM [Dfa].[dbo].[customer_details_stub] WHERE customer_number = ?";
                
                 String insertSubmissionSQL = "INSERT INTO UserTaskSubmissions " +
                "(EmployeeTaskId, TaskDate, CustomerPhoneNumber, customerNumber, AccountNumber, Channel, name, Line_of_Business, Header, Notes, Longitude, Latitude, Achieved, AchievedDate, Amount, Escalation, Status, CreatedAt, UpdatedAt, IsMultiple, Diaspora, Referral, ReferralCustomerNumber, ReferralPhoneNumber, ReferralName, BusinessRealizedId) " +
                "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 0, ?, ?, ?, ?, ?, ?)";

                try (PreparedStatement psInsert = connection.prepareStatement(insertSubmissionSQL); 
                     PreparedStatement psselect = connection.prepareStatement(selectReferrals);) {
                    
             
                   psselect.setString(1, referralCustomerNumber);
                   
                    ResultSet rs = psselect.executeQuery();

                    String referralphoneNumber = "";
                    String referralname = "";
                    
                    if (rs.next()) {
                         referralphoneNumber = rs.getString("phone_number");
                         referralname = rs.getString("Name");
                    }
                    
                    if (isValidUUID(employeeTaskId)) {
                        psInsert.setString(1, employeeTaskId);
                    } else {
                        psInsert.setNull(1, java.sql.Types.OTHER);
                    }
                    psInsert.setString(2, customerPhoneNumber);
                    psInsert.setString(3, customerNumber);
                    psInsert.setString(4, AccountNumber);
                    psInsert.setString(5, channel);
                    psInsert.setString(6, name);
                    psInsert.setString(7, lineofBusiness);
                    psInsert.setString(8, header);
                    psInsert.setString(9, notes);
                    psInsert.setString(10, longitude);
                    psInsert.setString(11, latitude);
                    psInsert.setInt(12, achieved);
                    psInsert.setString(13, achievedDate);
                    psInsert.setDouble(14, amount);
//                    psInsert.setInt(14, status);
                    if (isValidUUID(escalatedToUserUUID)) {
                        psInsert.setString(15, escalatedToUserUUID);
                    } else {
                        psInsert.setNull(15, java.sql.Types.OTHER);
                    }
                    psInsert.setInt(16, status);
                    psInsert.setInt(17, diaspora);
                    psInsert.setInt(18, referral);
                    psInsert.setString(19, referralCustomerNumber);
                    psInsert.setString(20, referralphoneNumber);
                    psInsert.setString(21, referralname);
                    psInsert.setString(22, businessRealizedId); // Link BusinessRealizedId

                    psInsert.executeUpdate();
                }
                
                // Update AchievedTarget in ProgressiveTracking
                String updateProgressSQL = "UPDATE ProgressiveTracking " +
                    "SET AchievedTarget = AchievedTarget + 1, UpdatedAt = GETDATE() " +
                    "WHERE EmployeeTaskId = ? AND TaskDate = CAST(GETDATE() AS DATE)";

                try (PreparedStatement psUpdate = connection.prepareStatement(updateProgressSQL)) {
                    psUpdate.setString(1, employeeTaskId);
                    int rowsUpdated = psUpdate.executeUpdate();

                    if (rowsUpdated == 0) {
                        // If no row exists for today, find the next closest future TaskDate
                        String closestFutureSQL = "SELECT TOP 1 Id FROM ProgressiveTracking " +
                                "WHERE TaskDate > CAST(GETDATE() AS DATE) AND EmployeeTaskId = ? ORDER BY TaskDate ASC";

                        try (PreparedStatement psClosest = connection.prepareStatement(closestFutureSQL);) {
                            psClosest.setString(1, employeeTaskId);

                            ResultSet rs = psClosest.executeQuery();

                            if (rs.next()) {
                                String closestTaskId = rs.getString("Id");

                                String updateClosestSQL = "UPDATE ProgressiveTracking " +
                                        "SET AchievedTarget = AchievedTarget + 1, UpdatedAt = GETDATE() " +
                                        "WHERE Id = ?";

                                try (PreparedStatement psUpdateClosest = connection.prepareStatement(updateClosestSQL)) {
                                    psUpdateClosest.setString(1, closestTaskId);
                                    psUpdateClosest.executeUpdate();
                                }
                            } else {
                                response.put("responseCode", "999")
                                        .put("responseDescription", "No suitable TaskDate found for updating AchievedTarget");
                                message.reply(response);
                                return;
                            }
                        }
                    }
                }

                response.put("responseCode", "000").put("responseDescription", "Task submitted successfully");

                // Only escalate if valid escalation details are provided
                boolean shouldEscalate = isValidUUID(escalatedToUserUUID);

                if (shouldEscalate) {
                    String fetchRoleSQL = "SELECT name FROM roles WHERE id = ?";
                    String roleName = null;

                    try (PreparedStatement psFetchRole = connection.prepareStatement(fetchRoleSQL)) {
                        psFetchRole.setInt(1, Integer.parseInt(user_type));
                        ResultSet rsRole = psFetchRole.executeQuery();
                        if (rsRole.next()) {
                            roleName = rsRole.getString("name");
                        } else {
                            response.put("responseCode", "999").put("responseDescription", "Error: Role ID not found.");
                            message.reply(response);
                            return;
                        }
                    }

                    // Handle escalation logic
                    if (escalatedToUserUUID.equals(user_uuid)) {
                        sendEscalationNotification(escalatedToEmail, escalatedToPhoneNumber, escalatedToName, header, notes);
                    } else {
                        String targetRole = getNextEscalationRole(roleName);
                        if (targetRole != null) {
                            String fetchNextRoleDetailsSQL = "SELECT u.email, u.phone_number, u.first_name + ' ' + u.last_name AS Name FROM users u " +
                                    "INNER JOIN roles r ON u.type = r.id " +
                                    "INNER JOIN usersBranches ub ON ub.UserId = u.uuid WHERE r.name = ? AND ub.BranchId = ?";

                            try (PreparedStatement psFetchNextRole = connection.prepareStatement(fetchNextRoleDetailsSQL)) {
                                psFetchNextRole.setString(1, targetRole);
                                psFetchNextRole.setString(2, user_branch_id);
                                ResultSet rsNextRole = psFetchNextRole.executeQuery();
                                if (rsNextRole.next()) {
                                    String nextRoleEmail = rsNextRole.getString("email");
                                    String nextRolePhoneNumber = rsNextRole.getString("phone_number");
                                    String nextRoleName = rsNextRole.getString("Name");

                                    sendEscalationNotification(nextRoleEmail, nextRolePhoneNumber, nextRoleName, header, notes);
                                }
                            }
                        }
                    }
                }
                
            } catch (Exception e) {
                response.put("responseCode", "999").put("responseDescription", "Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                dbConnection.closeConn();
            }
            
            
            
            
            
            
            
            
            
            
            
            
            
        } else if (ismultiple.equals("1")) {

            String nameOfForum = requestBody.getString("Name_of_Forum");
            String venue = requestBody.getString("Venue");
            String employeeTaskId = requestBody.getString("EmployeeTaskId");
            String header = requestBody.getString("Header");
            String notes = requestBody.getString("Notes");
            String longitude = requestBody.getString("Longitude");
            String latitude = requestBody.getString("Latitude");
            String achievedStr = requestBody.getString("Achieved", "").trim();
            int achieved = 0; // Default to null
            if (!achievedStr.isEmpty()) {
                if (achievedStr.equals("1")) {
                    achieved = 1;
                } else if (achievedStr.equals("0")) {
                    achieved = 0;
                } else {
                    response.put("responseCode", "999").put("responseDescription", "Error: Achieved must be '1' or '0'.");
                    message.reply(response);
                    return;
                }
            }
            String achievedDate = requestBody.getString("AchievedDate");
//            int amount = requestBody.getInteger("Amount");
            String escalatedToUserUUID = requestBody.getString("EscalatedToUserUUID", null);
            String escalatedToEmail = requestBody.getString("EscalatedToEmail", null);
            String escalatedToPhoneNumber = requestBody.getString("EscalatedToPhoneNumber", null);
            String escalatedToName = requestBody.getString("EscalatedToName", null);
            

            int amount = Integer.parseInt(requestBody.getString("Amount"));
            
            int diaspora = requestBody.containsKey("Diaspora") ? Integer.parseInt(requestBody.getString("Diaspora")) : 0;

            int isMember = Integer.parseInt(requestBody.getString("isMember"));
            int notMembers = Integer.parseInt(requestBody.getString("notMembers"));


            
            Double deposit = null;
             if (requestBody.containsKey("deposit") && !requestBody.getString("deposit").isEmpty()) {
                 deposit = Double.parseDouble(requestBody.getString("deposit"));
             }

            String depositDate = requestBody.getString("depositdate");

            Double loanProspect = null;
            if (requestBody.containsKey("loan prospect") && !requestBody.getString("loan prospect").isEmpty()) {
                loanProspect = Double.parseDouble(requestBody.getString("loan prospect"));
            }

            String loanProspectDate = requestBody.getString("loan prospect date");

            String newAccount = requestBody.getString("new account");
            if (newAccount != null && newAccount.isEmpty()) {
                newAccount = null;
            }
            String newAccountDate = requestBody.getString("new account date");


            int status = 0; // Default: No escalation
                if (isValidUUID(escalatedToUserUUID)) {
                    if (escalatedToUserUUID.equals(user_uuid)) {
                        status = 1; // Escalated to self
                    } else {
                        status = 2; // Escalated to someone else
                    }
                }
            
            try {
                
                String businessRealizedId = null;
                String businessRealizedClusteredId = null;
               
                
                if (deposit != null || newAccount != null || loanProspect != null) {
                String insertBusinessSQL = "INSERT INTO BusinessRealized (deposit, depositDate, newAccount, newAccountDate, loanProspect, loanProspectDate, createdAt, updatedAt, ConfirmationStatus, AcountOpeningStatus, LoanStatus, NewAccountsStatus) " +
                                           "VALUES (?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 0, 0, 0, 0)";

                String sql1 = "SELECT [Id]  FROM [BusinessRealized] WHERE [ClusteredId] = ? ";

                    try (PreparedStatement psBusiness = connection.prepareStatement(insertBusinessSQL, PreparedStatement.RETURN_GENERATED_KEYS);
                     PreparedStatement prQuery = connection.prepareStatement(sql1)) {


                        if (deposit != null) {
                            psBusiness.setDouble(1, deposit);
                        } else {
                            psBusiness.setNull(1, Types.DOUBLE);
                        }

                        if (depositDate != null && !depositDate.trim().isEmpty()) {
                            psBusiness.setString(2, depositDate);
                        } else {
                            psBusiness.setNull(2, Types.DATE);
                        }

                        if (newAccount != null) {
                            psBusiness.setString(3, newAccount);
                        } else {
                            psBusiness.setNull(3, Types.VARCHAR);
                        }

                       if (newAccountDate != null && !newAccountDate.trim().isEmpty()) {
                            psBusiness.setString(4, newAccountDate);
                        } else {
                            psBusiness.setNull(4, Types.DATE);
                        }

                        if (loanProspect != null) {
                            psBusiness.setDouble(5, loanProspect);
                        } else {
                            psBusiness.setNull(5, Types.DOUBLE);
                        }


                        if (loanProspectDate != null && !loanProspectDate.trim().isEmpty()) {
                            psBusiness.setString(6, loanProspectDate);
                        } else {
                            psBusiness.setNull(6, Types.DATE);
                        }

                        psBusiness.executeUpdate();

                        // Retrieve generated BusinessRealizedId
                        ResultSet rs = psBusiness.getGeneratedKeys();
                        if (rs.next()) {
                            businessRealizedClusteredId = rs.getString(1);
                        }
                        System.out.println("Retrieved BusinessRealized ClusteredID: " + businessRealizedClusteredId);
                        
                                // Get business realized id
                        prQuery.setString(1, businessRealizedClusteredId);
                        prQuery.execute();

                        ResultSet resultSet1 = prQuery.getResultSet();
                        if (resultSet1.next()) {
                            businessRealizedId = resultSet1.getString("Id");
                        } else {
                            throw new SQLException("Failed to Fetch Inserted business realized id.");
                        }
                        resultSet1.close();
                        
                        System.out.println("Retrieved BusinessRealized ID: " + businessRealizedId);

                    }catch (Exception e) {
                        e.printStackTrace();
                    }

                    }
                
                  String insertSubmissionSQL = "INSERT INTO UserTaskSubmissions " +
                        "(EmployeeTaskId, TaskDate, Name_of_Forum, Venue, Header, Notes, Longitude, Latitude, Achieved, AchievedDate, Amount, Escalation, Status, CreatedAt, UpdatedAt, IsMultiple, Diaspora, isMember, notMembers, BusinessRealizedId) " +
                        "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 1, ?, ?, ?, ?)";

                try (PreparedStatement psInsert = connection.prepareStatement(insertSubmissionSQL)) {
                    if (isValidUUID(employeeTaskId)) {
                        psInsert.setString(1, employeeTaskId);
                    } else {
                        psInsert.setNull(1, java.sql.Types.OTHER);
                    }
                 
                    psInsert.setString(2, nameOfForum);
                    psInsert.setString(3, venue);
                    psInsert.setString(4, header);
                    psInsert.setString(5, notes);
                    psInsert.setString(6, longitude);
                    psInsert.setString(7, latitude);
                    psInsert.setInt(8, achieved);
                    psInsert.setString(9, achievedDate);
                    psInsert.setInt(10, amount);
                    
                     if (isValidUUID(escalatedToUserUUID)) {
                        psInsert.setString(11, escalatedToUserUUID);
                    } else {
                        psInsert.setNull(11, java.sql.Types.OTHER);
                    }
        
                    psInsert.setInt(12, status);
                    psInsert.setInt(13, diaspora);
                    psInsert.setInt(14, isMember);
                    psInsert.setInt(15, notMembers);
                    psInsert.setString(16, businessRealizedId);

                    psInsert.executeUpdate();
                }
                
                // Update AchievedTarget in ProgressiveTracking
                String updateProgressSQL = "UPDATE ProgressiveTracking " +
                    "SET AchievedTarget = AchievedTarget + 1, UpdatedAt = GETDATE() " +
                    "WHERE EmployeeTaskId = ? AND TaskDate = CAST(GETDATE() AS DATE)";

                try (PreparedStatement psUpdate = connection.prepareStatement(updateProgressSQL)) {
                    psUpdate.setString(1, employeeTaskId);
                    int rowsUpdated = psUpdate.executeUpdate();

                    if (rowsUpdated == 0) {
                        // If no row exists for today, find the next closest future TaskDate
                        String closestFutureSQL = "SELECT TOP 1 Id FROM ProgressiveTracking " +
                                "WHERE TaskDate > CAST(GETDATE() AS DATE) AND EmployeeTaskId = ? ORDER BY TaskDate ASC";

                        try (PreparedStatement psClosest = connection.prepareStatement(closestFutureSQL);) {
                            psClosest.setString(1, employeeTaskId);

                            ResultSet rs = psClosest.executeQuery();

                            if (rs.next()) {
                                String closestTaskId = rs.getString("Id");

                                String updateClosestSQL = "UPDATE ProgressiveTracking " +
                                        "SET AchievedTarget = AchievedTarget + 1, UpdatedAt = GETDATE() " +
                                        "WHERE Id = ?";

                                try (PreparedStatement psUpdateClosest = connection.prepareStatement(updateClosestSQL)) {
                                    psUpdateClosest.setString(1, closestTaskId);
                                    psUpdateClosest.executeUpdate();
                                }
                            } else {
                                response.put("responseCode", "999")
                                        .put("responseDescription", "No suitable TaskDate found for updating AchievedTarget");
                                message.reply(response);
                                return;
                            }
                        }
                    }
                }

                response.put("responseCode", "000").put("responseDescription", "Task submitted successfully");

                boolean shouldEscalate = isValidUUID(escalatedToUserUUID);

                if (shouldEscalate) {
                    String fetchRoleSQL = "SELECT name FROM roles WHERE id = ?";
                    String roleName = null;

                        try (PreparedStatement psFetchRole = connection.prepareStatement(fetchRoleSQL)) {
                            psFetchRole.setInt(1, Integer.parseInt(user_type));
                            ResultSet rsRole = psFetchRole.executeQuery();
                            if (rsRole.next()) {
                                roleName = rsRole.getString("name");
                            } else {
                                response.put("responseCode", "999").put("responseDescription", "Error: Role ID not found.");
                                message.reply(response);
                                return;
                            }
                        }

                    if (escalatedToUserUUID.equals(user_uuid)) {
                    sendEscalationNotification(escalatedToEmail, escalatedToPhoneNumber, escalatedToName, header, notes);
                    } else {
                        String targetRole = getNextEscalationRole(roleName);
                        if (targetRole != null) {
                            String fetchNextRoleDetailsSQL = "SELECT u.email, u.phone_number, u.first_name + ' ' + u.last_name AS Name FROM users u " +
                                    "INNER JOIN roles r ON u.type = r.id " +
                                    "INNER JOIN usersBranches ub ON ub.UserId = u.uuid WHERE r.name = ? AND ub.BranchId = ?";

                            try (PreparedStatement psFetchNextRole = connection.prepareStatement(fetchNextRoleDetailsSQL)) {
                                psFetchNextRole.setString(1, targetRole);
                                psFetchNextRole.setString(2, user_branch_id);
                                ResultSet rsNextRole = psFetchNextRole.executeQuery();
                                if (rsNextRole.next()) {
                                    String nextRoleEmail = rsNextRole.getString("email");
                                    String nextRolePhoneNumber = rsNextRole.getString("phone_number");
                                    String nextRoleName = rsNextRole.getString("Name");

                                    sendEscalationNotification(nextRoleEmail, nextRolePhoneNumber, nextRoleName, header, notes);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                response.put("responseCode", "999").put("responseDescription", "Error: " + e.getMessage());
                e.printStackTrace();
            } finally {
                dbConnection.closeConn();
            }
        }
        
        

        message.reply(response);
    }

    private String getNextEscalationRole(String currentRole) {
        switch (currentRole) {
            case "RO":
                return "BM";
            case "BM":
                return "AM";
            case "AM":
                return "Chief";
            case "Chief":
                return "CEO";
            default:
                return null;
        }
    }

    private void sendEscalationNotification(String recipientEmail, String recipientPhoneNumber, String recipientName, String header, String notes) {
        // Send Email Notification
        if (recipientEmail != null) {
            JsonObject emailPayload = new JsonObject();
            emailPayload
                .put("emailRecipient", recipientEmail)
                .put("emailSubject", "ESCALATION - " + header)
                .put("emailBody", notes);

            DeliveryOptions deliveryOptions = new DeliveryOptions()
                .addHeader("emailRecipient", emailPayload.getString("emailRecipient"))
                .addHeader("emailSubject", emailPayload.getString("emailSubject"))
                .addHeader("emailBody", emailPayload.getString("emailBody"));

            eventBus.send("SEND_EMAIL", emailPayload, deliveryOptions);
        }

        // Send SMS Notification
        if (recipientPhoneNumber != null) {
            JsonObject smsPayload = new JsonObject()
                .put("phonenumber", recipientPhoneNumber)
                .put("msg", "Dear " + recipientName + ", you have received an escalation email. Kindly take action.");

            eventBus.send("COMMUNICATION_ADAPTOR", smsPayload);
        }
    }

    private void fetchEscalatedTasks(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        MultiMap headers = message.headers();
        if (!headers.contains("user_uuid")) {
            message.fail(401, "Unauthorized access");
            return;
        }

        String userUUID = headers.get("user_uuid");

        try {
            // Step 1: Get Role Name of the User from the `users` table
            String roleQuery = "SELECT r.name AS role_name FROM users u " +
                               "JOIN roles r ON u.type = r.id " + // `type` is the role_id
                               "WHERE u.uuid = ?";

            String userRole = null;

            try (PreparedStatement psRole = connection.prepareStatement(roleQuery)) {
                psRole.setString(1, userUUID);
                ResultSet rsRole = psRole.executeQuery();

                if (rsRole.next()) {
                    userRole = rsRole.getString("role_name");
                } else {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Error: User role not found.");
                    message.reply(response);
                    return;
                }
            }

            // Step 2: Fetch Escalated Tasks Based on Role Name (BM or RO)
            String query = "SELECT Id, EmployeeTaskId, TaskDate, CustomerPhoneNumber, Header, Notes, Longitude, Latitude, Status, Escalation, CreatedAt, UpdatedAt " +
                           "FROM UserTaskSubmissions " +
                           "WHERE Escalation = ? AND Status IN (SELECT id FROM roles WHERE name = ?)";

            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, userUUID); // Escalation is stored as user_uuid
                ps.setString(2, userRole); // Use role name instead of hardcoded status
                ResultSet rs = ps.executeQuery();

                JsonArray tasks = new JsonArray();
                while (rs.next()) {
                    JsonObject task = new JsonObject()
                            .put("Id", rs.getInt("Id"))
                            .put("EmployeeTaskId", rs.getString("EmployeeTaskId"))
                            .put("TaskDate", rs.getString("TaskDate"))
                            .put("CustomerPhoneNumber", rs.getString("CustomerPhoneNumber"))
                            .put("Header", rs.getString("Header"))
                            .put("Notes", rs.getString("Notes"))
                            .put("Longitude", rs.getString("Longitude"))
                            .put("Latitude", rs.getString("Latitude"))
                            .put("Status", rs.getInt("Status"))
                            .put("Escalation", rs.getString("Escalation"))
                            .put("CreatedAt", rs.getString("CreatedAt"))
                            .put("UpdatedAt", rs.getString("UpdatedAt"));

                    tasks.add(task);
                }
                response.put("responseCode", "000")
                        .put("responseDescription", "Escalated tasks retrieved successfully")
                        .put("tasks", tasks);
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
    }

    private void fetchCustomerInteractionHistory(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String customerPhoneNumber = requestBody.getString("CustomerPhoneNumber");

        if (customerPhoneNumber == null || customerPhoneNumber.isEmpty()) {
            message.fail(999, "Customer phone number is required");
            return;
        }

        try {
            String query = "SELECT uts.Id, uts.EmployeeTaskId, uts.TaskDate, uts.CustomerPhoneNumber, uts.Header, uts.Notes, " +
                          "uts.Longitude, uts.Latitude, uts.Status, uts.CreatedAt, uts.UpdatedAt, " +
                           "u.first_name, u.last_name, u.email " +
                           "FROM UserTaskSubmissions uts " +
                           "JOIN EmployeeTasks et ON et.Id = uts.EmployeeTaskId " +
                           "JOIN users u ON u.uuid = et.UserId " +
                           "WHERE uts.CustomerPhoneNumber = ? " +
                           "ORDER BY uts.TaskDate DESC";

            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, customerPhoneNumber);
                ResultSet rs = ps.executeQuery();

                JsonArray history = new JsonArray();
                while (rs.next()) {
                    JsonObject record = new JsonObject()
                            .put("Id", rs.getString("Id"))
                            .put("EmployeeTaskId", rs.getString("EmployeeTaskId"))
                            .put("TaskDate", rs.getString("TaskDate"))
                            .put("CustomerPhoneNumber", rs.getString("CustomerPhoneNumber"))
                            .put("Header", rs.getString("Header"))
                            .put("Notes", rs.getString("Notes"))
                            .put("Longitude", rs.getString("Longitude"))
                            .put("Latitude", rs.getString("Latitude"))
                            .put("Status", rs.getInt("Status"))
                            .put("CreatedAt", rs.getString("CreatedAt"))
                            .put("UpdatedAt", rs.getString("UpdatedAt"))
                            .put("VisitedBy", rs.getString("first_name") + " " + rs.getString("last_name"))
                            .put("EmployeeEmail", rs.getString("email"));
                    history.add(record);
                }

                response.put("responseCode", "000")
                        .put("responseDescription", "Customer interaction history retrieved successfully")
                        .put("history", history);
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
    }
        
    private void fetchTasksByIsmultiple(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String ismultiple = requestBody.getString("ismultiple");
        String branchId = requestBody.getString("branchId");

        if (ismultiple == null || ismultiple.isEmpty() || branchId == null || branchId.isEmpty()) {
          response.put("responseCode", "999")
                  .put("responseDescription", "Both 'ismultiple' and 'branchId' values are required.");
          message.reply(response);
          return;
      }
        
 
        String fetchTasksQuery = "SELECT uts.*, bs.ConfirmationStatus AS ConfirmationStatus, bs.deposit AS Deposit, bs.newAccount AS NewAccount, bs.loanProspect AS LoanProspect" +
            " FROM [Performance_Management].[dbo].[UserTaskSubmissions] uts " +
            "INNER JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON uts.EmployeeTaskId = et.Id " +
            "INNER JOIN [Performance_Management].[dbo].[BusinessRealized] bs ON bs.id = uts.BusinessRealizedId " +
            "WHERE CAST(uts.IsMultiple AS VARCHAR) = ? AND et.BranchId = ?";

        
        try (PreparedStatement fetchTasksStmt = connection.prepareStatement(fetchTasksQuery)) {
            fetchTasksStmt.setString(1, ismultiple); 
            fetchTasksStmt.setString(2, branchId);

            ResultSet resultSet = fetchTasksStmt.executeQuery();

            JsonArray tasksArray = new JsonArray();

            // Loop through the result set and build the response array
            while (resultSet.next()) {
                JsonObject task = new JsonObject();
                task.put("id", resultSet.getString("Id"));
                task.put("employeeTaskId", resultSet.getString("EmployeeTaskId"));
                task.put("taskDate", resultSet.getString("TaskDate"));
                task.put("customerPhoneNumber", resultSet.getString("CustomerPhoneNumber"));
                task.put("achieved", resultSet.getString("Achieved"));
                task.put("amount", resultSet.getString("Amount"));
                task.put("name", resultSet.getString("name"));
                task.put("channel", resultSet.getString("Channel"));
                task.put("lineOfBusiness", resultSet.getString("Line_of_Business"));
                task.put("nameOfForum", resultSet.getString("Name_of_Forum"));
                task.put("venue", resultSet.getString("Venue"));
                task.put("header", resultSet.getString("Header"));
                task.put("notes", resultSet.getString("Notes"));
                task.put("longitude", resultSet.getString("Longitude"));
                task.put("latitude", resultSet.getString("Latitude"));
                task.put("escalation", resultSet.getString("Escalation"));
                task.put("createdAt", resultSet.getString("CreatedAt"));
                task.put("updatedAt", resultSet.getString("UpdatedAt"));
                task.put("isMultiple", resultSet.getString("IsMultiple"));
                task.put("ConfirmationStatus", resultSet.getString("ConfirmationStatus"));
                task.put("status", resultSet.getString("Status"));
                task.put("deposit", resultSet.getString("Deposit"));
                task.put("newAccount", resultSet.getString("NewAccount"));
                task.put("loanProspect", resultSet.getString("LoanProspect"));

                tasksArray.add(task);
            }

            // Return response based on whether any records were found
            if (tasksArray.isEmpty()) {
                response.put("responseCode", "999")
                        .put("responseDescription", "No tasks found for the provided ismultiple value.");
            } else {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Tasks fetched successfully.")
                        .put("tasks", tasksArray);
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }

        message.reply(response);
    }
    
    private void fetchPendingConfirmations(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String branchId = requestBody.getString("branchId");
        String type = requestBody.getString("type");
        
        if (branchId == null || branchId.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "'branchId' is required.");
            message.reply(response);
            return;
        }
       

        String fetchQuery = "SELECT br.id, br.deposit, br.depositDate, br.newAccount, br.newAccountDate, " +
                "br.loanProspect, br.loanProspectDate, br.createdAt, br.updatedAt, " +
                "br.ConfirmationStatus, br.AcountOpeningStatus, br.LoanStatus, br.NewAccountsStatus, " +
                "uts.TaskDate, uts.CustomerPhoneNumber, uts.customerNumber, uts.AccountNumber, uts.Achieved, " +
                "uts.AchievedDate, uts.name, uts.Channel, uts.Line_of_Business, uts.Notes " +
                "FROM [Performance_Management].[dbo].[BusinessRealized] br " +
                "INNER JOIN [Performance_Management].[dbo].[UserTaskSubmissions] uts ON br.id = uts.BusinessRealizedId " +
                "INNER JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON et.Id = uts.EmployeeTaskId " +
                "WHERE ( (br.deposit IS NOT NULL AND br.ConfirmationStatus = 0) " +
                "OR (br.newAccount IS NOT NULL AND br.AcountOpeningStatus = 0) " +
                "OR (br.loanProspect IS NOT NULL AND br.LoanStatus = 0) " +
                "OR (uts.Achieved = 1 AND br.NewAccountsStatus = 0) ) " +
                "AND (uts.IsMultiple = 0) " +
                "AND et.BranchId = ?";

        try (PreparedStatement fetchStmt = connection.prepareStatement(fetchQuery)) {
            fetchStmt.setString(1, branchId);
            ResultSet resultSet = fetchStmt.executeQuery();

            JsonArray results = new JsonArray();

            while (resultSet.next()) {
                if ("deposits".equalsIgnoreCase(type) && resultSet.getDouble("deposit") > 0 && resultSet.getInt("ConfirmationStatus") == 0) {
                    JsonObject depositRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("depositamount", resultSet.getDouble("deposit"))
                            .put("depositdate", resultSet.getString("depositDate"))
                            .put("ConfirmationStatus", resultSet.getString("ConfirmationStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("customerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("AchievedDate"))
                            .put("name", resultSet.getString("name"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("Line_of_Business"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("createdAt"))
                            .put("updatedAt", resultSet.getString("updatedAt"));
                    results.add(depositRecord);
                    
                } else if ("loanProspects".equalsIgnoreCase(type) && resultSet.getDouble("loanProspect") > 0 && resultSet.getInt("LoanStatus") == 0) {
                    JsonObject loanRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("loanamount", resultSet.getDouble("loanProspect"))
                            .put("loandate", resultSet.getString("loanProspectDate"))
                            .put("ConfirmationStatus", resultSet.getString("ConfirmationStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("customerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("AchievedDate"))
                            .put("name", resultSet.getString("name"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("Line_of_Business"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("createdAt"))
                            .put("updatedAt", resultSet.getString("updatedAt"));
                    results.add(loanRecord);
                } else if ("newAccounts".equalsIgnoreCase(type) && resultSet.getString("newAccount") != null && resultSet.getInt("AcountOpeningStatus") == 0) {
                    JsonObject newAccountRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("accountName", resultSet.getString("newAccount"))
                            .put("accountdate", resultSet.getString("newAccountDate"))
                            .put("ConfirmationStatus", resultSet.getString("ConfirmationStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("customerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("AchievedDate"))
                            .put("name", resultSet.getString("name"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("Line_of_Business"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("createdAt"))
                            .put("updatedAt", resultSet.getString("updatedAt"));
                    results.add(newAccountRecord);
                } else if ("completelynewAccounts".equalsIgnoreCase(type) && resultSet.getString("Achieved") != null && resultSet.getInt("NewAccountsStatus") == 0) {
                    JsonObject completelynewAccountRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("AchievedDate", resultSet.getString("AchievedDate"))
                            .put("ConfirmationStatus", resultSet.getString("ConfirmationStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("customerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("AchievedDate"))
                            .put("name", resultSet.getString("name"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("Line_of_Business"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("createdAt"))
                            .put("updatedAt", resultSet.getString("updatedAt"));
                    results.add(completelynewAccountRecord);
                }
            }
            if (results.isEmpty()) {
                response.put("responseCode", "999")
                        .put("responseDescription", "No pending confirmations found for the given branch and type.");
            } else {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Pending confirmations fetched.")
                        .put("results", results);
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
        message.reply(response);
    }

    private void fetchPendingConfirmationsisMultiple(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String branchId = requestBody.getString("branchId");
        String type = requestBody.getString("type");

        if (branchId == null || branchId.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "'branchId' is required.");
            message.reply(response);
            return;
        }
        
        String fetchQuery = "SELECT tu.Id, tu.BranchId, tu.TaskId, tu.CustomerPhoneNumber, tu.CustomerName, tu.CustomerNumber, " +
                            "tu.AccountNumber, tu.Venue, tu.Forum, tu.LineOfBusiness, tu.Channel, tu.Deposit, tu.Notes, tu.batchId, " +
                            "tu.CustomerType, tu.TaskDate, tu.DepositDate, tu.LoanProspect, tu.LoanDate, tu.NewAccounts, tu.AccountDate, " +
                            "tu.RO, tu.CreatedAt, tu.UpdatedAt, tu.ConfirmationStatus, tu.AcountOpeningStatus, tu.LoanStatus, tu.NewAccountsStatus, " +
                            "uts.Achieved, tu.OpeningDate " +
                            "FROM [Performance_Management].[dbo].[TaskBulkUpload] tu " +
                            "INNER JOIN [Performance_Management].[dbo].[UserTaskSubmissions] uts ON tu.TaskId = uts.Id " +
                            "INNER JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON et.Id = uts.EmployeeTaskId " +
                            "WHERE ( (tu.Deposit IS NOT NULL AND tu.ConfirmationStatus = 0) " +
                            "OR (tu.NewAccounts IS NOT NULL AND tu.AcountOpeningStatus = 0) " +
                            "OR (tu.LoanProspect IS NOT NULL AND tu.LoanStatus = 0) " +
                            "OR (uts.Achieved = 1 AND tu.NewAccountsStatus = 0) ) " +
//                            "AND (uts.IsMultiple = 0) " +
                            "AND et.BranchId = ?";

        try (PreparedStatement fetchStmt = connection.prepareStatement(fetchQuery)) {
            fetchStmt.setString(1, branchId);
            ResultSet resultSet = fetchStmt.executeQuery();

            JsonArray results = new JsonArray();
          
          
           while (resultSet.next()) {
                if ("deposits".equalsIgnoreCase(type) && resultSet.getDouble("Deposit") > 0 && resultSet.getInt("ConfirmationStatus") == 0) {
                    JsonObject depositRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("depositamount", resultSet.getDouble("Deposit"))
                            .put("depositdate", resultSet.getString("DepositDate"))
                            .put("ConfirmationStatus", resultSet.getString("ConfirmationStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("CustomerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("OpeningDate"))
                            .put("name", resultSet.getString("CustomerName"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("LineOfBusiness"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("CreatedAt"))
                            .put("updatedAt", resultSet.getString("UpdatedAt"));
                    results.add(depositRecord);
                    
                } else if ("loanProspects".equalsIgnoreCase(type) && resultSet.getDouble("LoanProspect") > 0 && resultSet.getInt("LoanStatus") == 0) {
                    JsonObject loanRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("loanamount", resultSet.getDouble("LoanProspect"))
                            .put("loandate", resultSet.getString("LoanDate"))
                            .put("ConfirmationStatus", resultSet.getString("LoanStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("CustomerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("OpeningDate"))
                            .put("name", resultSet.getString("CustomerName"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("LineOfBusiness"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("CreatedAt"))
                            .put("updatedAt", resultSet.getString("UpdatedAt"));
                    results.add(loanRecord);
                    
                } else if ("newAccounts".equalsIgnoreCase(type) && resultSet.getString("NewAccounts") != null && resultSet.getInt("AcountOpeningStatus") == 0) {
                    JsonObject newAccountRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("accountName", resultSet.getString("NewAccounts"))
                            .put("accountdate", resultSet.getString("AccountDate"))
                            .put("ConfirmationStatus", resultSet.getString("AcountOpeningStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("CustomerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("OpeningDate"))
                            .put("name", resultSet.getString("CustomerName"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("LineOfBusiness"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("CreatedAt"))
                            .put("updatedAt", resultSet.getString("UpdatedAt"));
                    results.add(newAccountRecord);
                    
                } else if ("completelynewAccounts".equalsIgnoreCase(type) && resultSet.getString("Achieved") != null && resultSet.getInt("NewAccountsStatus") == 0) {
                    JsonObject completelynewAccountRecord = new JsonObject()
                            .put("id", resultSet.getString("id"))
                            .put("AchievedDate", resultSet.getString("AchievedDate"))
                            .put("ConfirmationStatus", resultSet.getString("NewAccountsStatus"))
                            .put("TaskDate", resultSet.getString("TaskDate"))
                            .put("CustomerPhoneNumber", resultSet.getString("CustomerPhoneNumber"))
                            .put("customerNumber", resultSet.getString("CustomerNumber"))
                            .put("AccountNumber", resultSet.getString("AccountNumber"))
                            .put("Achieved", resultSet.getString("Achieved"))
                            .put("AchievedDate", resultSet.getString("OpeningDate"))
                            .put("name", resultSet.getString("CustomerName"))
                            .put("Channel", resultSet.getString("Channel"))
                            .put("Line_of_Business", resultSet.getString("LineOfBusiness"))
                            .put("Notes", resultSet.getString("Notes"))
                            .put("createdAt", resultSet.getString("CreatedAt"))
                            .put("updatedAt", resultSet.getString("UpdatedAt"));
                    results.add(completelynewAccountRecord);
                }
            }
            if (results.isEmpty()) {
                response.put("responseCode", "999")
                        .put("responseDescription", "No pending bulk upload confirmations found for the given branch and type.");
            } else {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Pending bulk upload confirmations fetched.")
                        .put("results", results);
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            dbConnection.closeConn();
        }
        message.reply(response);
    }
}