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
import java.util.Date;
import java.util.Random;
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
        eventBus.consumer("SUBMIT_TASK", this::submitTask);
        eventBus.consumer("FETCHESCALATEDTASKS", this::fetchEscalatedTasks);
        eventBus.consumer("FETCH_CUSTOMER_HISTORY", this::fetchCustomerInteractionHistory);
        eventBus.consumer("FETCHUSERSPERBRANCH", this::fetchUsersByBranch);

        
    }
    
    private void sendOTP(Message<JsonObject> message) {
        JsonObject data = message.body();
        MultiMap headers = message.headers();

        if (headers.isEmpty()) {
            message.fail(666, "Unauthenticated User");
            return;
        }
        
        DBConnection dbConnection = new DBConnection();
        JsonObject response = new JsonObject();
        String otp = new Common().generateRandom(6);
        String customerNumber = data.getString("customerNumber");
        
        // Fetch from customer details
        String query = "SELECT * FROM [Dfa].[dbo].[customer_details_stub] WHERE customer_number = '"+ customerNumber +"'";
        
        try {
            ResultSet rs = dbConnection.query_all(query);
            
            if (rs.next()){
                String phoneNumber = rs.getString("phone_number");
                String name = rs.getString("first_name") + rs.getString("last_name");
                
                // update unused previous codes
                String updateSql = "UPDATE verification_codes SET [status] = 1 WHERE customer_number = '"+ customerNumber +"'";
                
                int i = dbConnection.update_db(updateSql);
                
                String sql = "INSERT INTO verification_codes ([code],[phone_number],[intent],[customer_number])" +
                " VALUES ('"+ otp +"','"+ phoneNumber +"','Customer Verification','"+ customerNumber +"')";


                int j = dbConnection.update_db(sql);

                String otpSMS = "Dear "+name.toUpperCase()+", your One Time Password is "+otp+".";

                JsonObject messageObject = new JsonObject();

                messageObject
                        .put("phonenumber", phoneNumber)
                        .put("msg", otpSMS);

                eventBus.send("COMMUNICATION_ADAPTOR",messageObject);

                response
                        .put("responseCode", "000")
                        .put("responseDescription", "OTP sent successfully.");
                
            }
        } catch (Exception e) {
            e.getMessage();
            response
                        .put("responseCode", "999")
                        .put("responseDescription", "OTP failed to send.");
        } finally {
            dbConnection.closeConn();
        }

        
        message.reply(response);
    }

    private void verifyOTP(Message<JsonObject> message) {
        JsonObject data = message.body();
        DBConnection dbConnection = new DBConnection();
        MultiMap headers = message.headers();
        
        String customerNumber = data.getString("customerNumber");
        String otp = data.getString("otp");
        JsonObject response = new JsonObject();

        if (headers.isEmpty()) {
            message.fail(666, "Unauthenticated User");
            return;
        }

        // Fetch from verification codes
        String query = "SELECT * FROM verification_codes WHERE customer_number = '"+ customerNumber +"' AND [status] = 0";
        
        try {
            ResultSet rs = dbConnection.query_all(query);
            
            if (rs.next()){
                String dbOtp = rs.getString("code");
                String phoneNumber = rs.getString("phone_number");
                
                if (dbOtp.equals(otp)) {
                    // update unused previous codes
                    String updateSql = "UPDATE verification_codes SET [status] = 1 WHERE customer_number = '"+ customerNumber +"'";

                    int i = dbConnection.update_db(updateSql);
                    
                    response
                            .put("responseCode", "000")
                            .put("responseDescription", "OTP verified successfully.")
                            .put("phoneNumber", phoneNumber);
                } else {
                    response
                            .put("responseCode", "999")
                            .put("responseDescription", "OTP failed to verify.");
                }
            }
        } catch (Exception e) {
            e.getMessage();
        } finally {
            dbConnection.closeConn();
        }
        
        message.reply(response);
    }
    
    private void submitTask(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String employeeTaskId = requestBody.getString("EmployeeTaskId");
        String customerPhoneNumber = requestBody.getString("CustomerPhoneNumber");
        String header = requestBody.getString("Header");
        String notes = requestBody.getString("Notes");
        String longitude = requestBody.getString("Longitude");
        String latitude = requestBody.getString("Latitude");
        String escalatedToUserUUID = requestBody.getString("EscalatedToUserUUID", null); 
        String escalatedToEmail = requestBody.getString("EscalatedToEmail", null); 
        String escalatedToPhoneNumber = requestBody.getString("EscalatedToPhoneNumber", null); 
        String escalatedToName = requestBody.getString("EscalatedToName", null);
        int status = Integer.parseInt(requestBody.getString("Status"));

        try {
            
              // Fetch the expected target from EmployeeTasks table based on EmployeeTaskId
            String fetchTargetSQL = "SELECT Target FROM EmployeeTasks WHERE Id = ?";
            int expectedTarget = 0;

            try (PreparedStatement psFetchTarget = connection.prepareStatement(fetchTargetSQL)) {
                psFetchTarget.setString(1, employeeTaskId);
                ResultSet rsTarget = psFetchTarget.executeQuery();
                if (rsTarget.next()) {
                    expectedTarget = rsTarget.getInt("Target");
                } else {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Error: EmployeeTaskId not found.");
                    message.reply(response);
                    return; // Stop further execution if EmployeeTaskId is invalid
                }
            }
            // Insert new task submission
            String insertSubmissionSQL = "INSERT INTO UserTaskSubmissions " +
                    "(EmployeeTaskId, TaskDate, CustomerPhoneNumber, Header, Notes, Longitude, Latitude, Status, Escalation, CreatedAt, UpdatedAt) " +
                    "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";

            try (PreparedStatement psInsert = connection.prepareStatement(insertSubmissionSQL)) {
                psInsert.setString(1, employeeTaskId);
                psInsert.setString(2, customerPhoneNumber);
                psInsert.setString(3, header);
                psInsert.setString(4, notes);
                psInsert.setString(5, longitude);
                psInsert.setString(6, latitude);
                psInsert.setInt(7, status);
                psInsert.setString(8, escalatedToUserUUID); // Null if not escalated
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

            // If escalated, send email and SMS notification using event bus
            if (status == 2) {
                
                if (escalatedToEmail != null) {
                    JsonObject emailPayload = new JsonObject();
                    emailPayload
                    .put("emailRecipient", escalatedToEmail)
                    .put("emailSubject", "ESCALATION - " + header)
                    .put("emailBody", notes);
                    
                    DeliveryOptions deliveryOptions = new DeliveryOptions()
                    .addHeader("emailRecipient", emailPayload.getString("emailRecipient"))
                    .addHeader("emailSubject", emailPayload.getString("emailSubject"))
                    .addHeader("emailBody", emailPayload.getString("emailBody"));
                    eventBus.send("SEND_EMAIL",emailPayload,deliveryOptions);
                }

                if (escalatedToPhoneNumber != null) {
                    JsonObject smsPayload = new JsonObject()
                            .put("phonenumber", escalatedToPhoneNumber)
                            .put("msg", "Dear "+ escalatedToName +" you have received an escalation email. Kindly take action.");
                    eventBus.send("COMMUNICATION_ADAPTOR", smsPayload);
                }
            }

            response.put("responseCode", "000")
                    .put("responseDescription", "Task submitted" + (status == 2 ? " and escalated" : "") + " successfully");

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
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
            String query = "SELECT Id, EmployeeTaskId, TaskDate, CustomerPhoneNumber, Header, Notes, Longitude, Latitude, Status, Escalation, CreatedAt, UpdatedAt " +
                           "FROM UserTaskSubmissions WHERE Status = 2 AND Escalation = ?";

            try (PreparedStatement ps = connection.prepareStatement(query)) {
                ps.setString(1, userUUID);
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
            message.fail(400, "Customer phone number is required");
            return;
        }

        try {
            String query = "SELECT uts.Id, uts.EmployeeTaskId, uts.TaskDate, uts.CustomerPhoneNumber, uts.Header, uts.Notes, uts.Longitude, uts.Latitude, uts.Status, uts.CreatedAt, uts.UpdatedAt, " +
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
    
    private void fetchUsersByBranch(Message<JsonObject> message) {
        DBConnection dbConnection = new DBConnection();
        JsonObject response = new JsonObject();
        JsonObject branchManager = new JsonObject();
        JsonArray roArray = new JsonArray();

        String branchId = message.body().getString("branchId"); 

        // SQL query to get users for a given branch, including their role
        String query = "SELECT u.id AS UserId, u.first_name, u.last_name, u.isRO, r.name AS RoleName " +
                       "FROM [Performance_Management].[dbo].[usersBranches] ub " +
                       "JOIN [Performance_Management].[dbo].[users] u ON ub.UserId = u.id " +
                       "LEFT JOIN [Performance_Management].[dbo].[roles] r ON u.type = r.id " +
                       "WHERE ub.BranchId = ?";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(query)) {

            preparedStatement.setString(1, branchId);
            ResultSet rs = preparedStatement.executeQuery();

            while (rs.next()) {
                String userId = rs.getString("UserId");
                String fullName = rs.getString("first_name") + " " + rs.getString("last_name");
                String roleName = rs.getString("RoleName");
                boolean isRO = rs.getBoolean("isRO");

                JsonObject userObj = new JsonObject()
                        .put("id", userId)
                        .put("name", fullName);

                if (roleName != null && roleName.equalsIgnoreCase("Branch Manager")) {
                    // Store branch manager details
                    branchManager.put("id", userId)
                                 .put("name", fullName)
                                 .put("role", roleName);
                } else if (isRO) {
                    // Add ROs to the array
                    roArray.add(userObj);
                }
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
            message.reply(response);
            return;
        } finally {
            dbConnection.closeConn();
        }

        // Construct final response
        response.put("responseCode", "000")
                .put("responseDescription", "Users fetched successfully.")
                .put("branchManager", branchManager)
                .put("relationshipOfficers", roArray);

        message.reply(response);
    }

}

   