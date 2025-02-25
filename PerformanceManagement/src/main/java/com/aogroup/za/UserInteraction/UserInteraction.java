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
        eventBus.consumer("SUBMIT_TASK", this::submitTask);
        eventBus.consumer("FETCHESCALATEDTASKS", this::fetchEscalatedTasks);
        eventBus.consumer("FETCH_CUSTOMER_HISTORY", this::fetchCustomerInteractionHistory);
        eventBus.consumer("FETCHTASKSBYMULTIPLE", this::fetchTasksByIsmultiple);
        
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
                response.put("responseCode", "404")
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
            message.fail(666, "Unauthenticated User");
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
                    response.put("responseCode", "401")
                            .put("responseDescription", "Invalid OTP.");
                }
            } else {
                response.put("responseCode", "404")
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
            String achieved = requestBody.getString("Achieved");
            double amount = Double.parseDouble(requestBody.getString("Amount"));
            String escalatedToUserUUID = requestBody.getString("EscalatedToUserUUID", null);
            String escalatedToEmail = requestBody.getString("EscalatedToEmail", null);
            String escalatedToPhoneNumber = requestBody.getString("EscalatedToPhoneNumber", null);
            String escalatedToName = requestBody.getString("EscalatedToName", null);
//            int status = Integer.parseInt(requestBody.getString("Status"));

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

                String insertSubmissionSQL = "INSERT INTO UserTaskSubmissions " +
                        "(EmployeeTaskId, TaskDate, CustomerPhoneNumber, customerNumber, AccountNumber, Channel, name, Line_of_Business, Header, Notes, Longitude, Latitude, Achieved, Amount, Escalation, Status, CreatedAt, UpdatedAt, IsMultiple, ConfirmationStatus) " +
                        "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 0, 0)";

                try (PreparedStatement psInsert = connection.prepareStatement(insertSubmissionSQL)) {
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
                    psInsert.setString(12, achieved);
                    psInsert.setDouble(13, amount);
//                    psInsert.setInt(14, status);
                    if (isValidUUID(escalatedToUserUUID)) {
                        psInsert.setString(14, escalatedToUserUUID);
                    } else {
                        psInsert.setNull(14, java.sql.Types.OTHER);
                    }
                    psInsert.setInt(15, status);
                    psInsert.executeUpdate();
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

            String achieved = requestBody.getString("Achieved");
            double amount = Double.parseDouble(requestBody.getString("Amount"));
//            int verificationstatus = Integer.parseInt(requestBody.getString("VerificationStatus"));
            String nameOfForum = requestBody.getString("Name_of_Forum");
            String venue = requestBody.getString("Venue");
            String employeeTaskId = requestBody.getString("EmployeeTaskId");
            String header = requestBody.getString("Header");
            String notes = requestBody.getString("Notes");
            String longitude = requestBody.getString("Longitude");
            String latitude = requestBody.getString("Latitude");
            String escalatedToUserUUID = requestBody.getString("EscalatedToUserUUID", null);
            String escalatedToEmail = requestBody.getString("EscalatedToEmail", null);
            String escalatedToPhoneNumber = requestBody.getString("EscalatedToPhoneNumber", null);
            String escalatedToName = requestBody.getString("EscalatedToName", null);

            int status = 0; // Default: No escalation
                if (isValidUUID(escalatedToUserUUID)) {
                    if (escalatedToUserUUID.equals(user_uuid)) {
                        status = 1; // Escalated to self
                    } else {
                        status = 2; // Escalated to someone else
                    }
                }
            
            try {
                String insertSubmissionSQL = "INSERT INTO UserTaskSubmissions " +
                        "(EmployeeTaskId, TaskDate, Achieved, Amount, Name_of_Forum, Venue, Header, Notes, Longitude, Latitude, Escalation, Status, CreatedAt, UpdatedAt, IsMultiple, ConfirmationStatus) " +
                        "VALUES (?, GETDATE(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE(), 1, NULL)";

                try (PreparedStatement psInsert = connection.prepareStatement(insertSubmissionSQL)) {
                    if (isValidUUID(employeeTaskId)) {
                        psInsert.setString(1, employeeTaskId);
                    } else {
                        psInsert.setNull(1, java.sql.Types.OTHER);
                    }
                    psInsert.setString(2, achieved);
                    psInsert.setDouble(3, amount);
//                    psInsert.setInt(4, verificationstatus);
                    psInsert.setString(4, nameOfForum);
                    psInsert.setString(5, venue);
                    psInsert.setString(6, header);
                    psInsert.setString(7, notes);
                    psInsert.setString(8, longitude);
                    psInsert.setString(9, latitude);
                     if (isValidUUID(escalatedToUserUUID)) {
                        psInsert.setString(10, escalatedToUserUUID);
                    } else {
                        psInsert.setNull(10, java.sql.Types.OTHER);
                    }
                    psInsert.setInt(11, status);
                    psInsert.executeUpdate();
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
            message.fail(400, "Customer phone number is required");
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
          response.put("responseCode", "400")
                  .put("responseDescription", "Both 'ismultiple' and 'branchId' values are required.");
          message.reply(response);
          return;
      }
        
        // SQL query strictly matching the provided ismultiple value
//        String fetchTasksQuery = "SELECT * FROM [Performance_Management].[dbo].[UserTaskSubmissions] WHERE CAST([IsMultiple] AS VARCHAR) = ?";

        String fetchTasksQuery = "SELECT uts.* FROM [Performance_Management].[dbo].[UserTaskSubmissions] uts " +
            "INNER JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON uts.EmployeeTaskId = et.Id " +
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
                task.put("confirmationStatus", resultSet.getString("ConfirmationStatus"));
                task.put("status", resultSet.getString("Status"));

                tasksArray.add(task);
            }

            // Return response based on whether any records were found
            if (tasksArray.isEmpty()) {
                response.put("responseCode", "404")
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

    
}

   