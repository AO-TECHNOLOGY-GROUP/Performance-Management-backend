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
        eventBus.consumer("FETCHUSERSPERROLE", this::fetchUsersByRole);

        
    }
    
//    private void sendOTP(Message<JsonObject> message) {
//        JsonObject data = message.body();
//        MultiMap headers = message.headers();
//
//        if (headers.isEmpty()) {
//            message.fail(666, "Unauthenticated User");
//            return;
//        }
//        
//        DBConnection dbConnection = new DBConnection();
//        JsonObject response = new JsonObject();
//        String otp = new Common().generateRandom(6);
//        String customerNumber = data.getString("customerNumber");
//        
//        // Fetch from customer details
//        String query = "SELECT * FROM [Dfa].[dbo].[customer_details_stub] WHERE customer_number = '"+ customerNumber +"'";
//        
//        try {
//            ResultSet rs = dbConnection.query_all(query);
//            
//            if (rs.next()){
//                String phoneNumber = rs.getString("phone_number");
//                String name = rs.getString("first_name") + rs.getString("last_name");
//                
//                // update unused previous codes
//                String updateSql = "UPDATE verification_codes SET [status] = 1 WHERE customer_number = '"+ customerNumber +"'";
//                
//                int i = dbConnection.update_db(updateSql);
//                
//                String sql = "INSERT INTO verification_codes ([code],[phone_number],[intent],[customer_number])" +
//                " VALUES ('"+ otp +"','"+ phoneNumber +"','Customer Verification','"+ customerNumber +"')";
//
//
//                int j = dbConnection.update_db(sql);
//
//                String otpSMS = "Dear "+name.toUpperCase()+", your One Time Password is "+otp+".";
//
//                JsonObject messageObject = new JsonObject();
//
//                messageObject
//                        .put("phonenumber", phoneNumber)
//                        .put("msg", otpSMS);
//
//                eventBus.send("COMMUNICATION_ADAPTOR",messageObject);
//
//                response
//                        .put("responseCode", "000")
//                        .put("responseDescription", "OTP sent successfully.");
//                
//            }
//        } catch (Exception e) {
//            e.getMessage();
//            response
//                        .put("responseCode", "999")
//                        .put("responseDescription", "OTP failed to send.");
//        } finally {
//            dbConnection.closeConn();
//        }
//
//        
//        message.reply(response);
//    }

//

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

    private void submitTask(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user_uuid = headers.get("user_uuid");
        String user_branch_id = headers.get("user_branch_id");
        
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
            // Fetch the expected target from EmployeeTasks table
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
                    return;
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
                psInsert.setString(8, escalatedToUserUUID);
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

                    try (PreparedStatement psClosest = connection.prepareStatement(closestFutureSQL)) {
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

            // Fetch Role Name based on Status
            String fetchRoleSQL = "SELECT name FROM roles WHERE id = ?";
            String roleName = null;

            try (PreparedStatement psFetchRole = connection.prepareStatement(fetchRoleSQL)) {
                psFetchRole.setInt(1, status);
                ResultSet rsRole = psFetchRole.executeQuery();
                if (rsRole.next()) {
                    roleName = rsRole.getString("name");
                } else {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Error: Role ID not found.");
                    message.reply(response);
                    return;
                }
            }

            // Handle escalation logic based on role
            if ("RO".equals(roleName)) {
                // Check if the RO is escalating to themselves
                if (escalatedToUserUUID != null && escalatedToUserUUID.equals(user_uuid)) {
                    // If RO is escalating to themselves, they should receive the SMS and Email
                    sendEscalationNotification(escalatedToEmail, escalatedToPhoneNumber, escalatedToName, header, notes);
                } else {
                    // If RO escalates to their BM, we need to get the BM's details.
                    String fetchBMDetailsSQL = "SELECT u.email, u.phone_number, u.first_name + ' ' + u.last_name AS Name FROM users u INNER JOIN roles r ON u.type = r.id "
                            + "INNER JOIN usersBranches ub ON ub.UserId = u.uuid "
                            + " WHERE r.name = 'BM' AND ub.BranchId = ?";

                    try (PreparedStatement psFetchBM = connection.prepareStatement(fetchBMDetailsSQL)) {
                        psFetchBM.setString(1, user_branch_id);  // fetch BM based on RO's BranchId
                        ResultSet rsBM = psFetchBM.executeQuery();
                        if (rsBM.next()) {
                            String bmEmail = rsBM.getString("email");
                            String bmPhoneNumber = rsBM.getString("phone_number");
                            String bmName = rsBM.getString("Name");

                            // Send escalation notifications to BM
                            sendEscalationNotification(bmEmail, bmPhoneNumber, bmName, header, notes);
                        } else {
                            response.put("responseCode", "999")
                                    .put("responseDescription", "Error: Branch Manager not found for the given RO.");
                            message.reply(response);
                            return;
                        }
                    }
                }
            }

            response.put("responseCode", "000")
                    .put("responseDescription", "Task submitted" + ("RO".equals(roleName) ? " and escalated" : "") + " successfully");

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
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
    
    private void fetchUsersByRole(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String roleName = requestBody.getString("RoleName");

        try {
            // Validate input
            if (roleName == null || roleName.isEmpty()) {
                response.put("responseCode", "999")
                        .put("responseDescription", "Role name is required");
                message.reply(response);
                return;
            }

            // Fetch users by role name
            String fetchUsersQuery = "SELECT u.id AS UserId, " +
                                     "u.first_name + ' ' + u.last_name AS Name, " +
                                     "u.email AS Email, " +
                                     "u.uuid AS UUID, " +
                                     "u.phone_number AS PhoneNumber, " +
                                     "u.branch AS Branch " + 
                                     "FROM users u " +
                                     "JOIN roles r ON u.type = r.id " +  // Join with roles table
                                     "WHERE r.name = ?";  // Use role name instead of role ID

            JsonArray userList = new JsonArray();
            try (PreparedStatement psUsers = connection.prepareStatement(fetchUsersQuery)) {
                psUsers.setString(1, roleName);
                ResultSet rs = psUsers.executeQuery();
                while (rs.next()) {
                    JsonObject userData = new JsonObject()
                            .put("UserId", rs.getString("UserId"))
                            .put("Name", rs.getString("Name"))
                            .put("UUID", rs.getString("UUID"))
                            .put("Email", rs.getString("Email"))
                            .put("PhoneNumber", rs.getString("PhoneNumber"))
                            .put("Branch", rs.getString("Branch"));
                    userList.add(userData);
                }
            }

            // Debugging: Check number of users retrieved
            System.out.println("Total users found for role " + roleName + ": " + userList.size());

            response.put("responseCode", "000")
                    .put("responseDescription", "Success")
                    .put("Users", userList);

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn(); // Ensures connection is properly closed
        }

        message.reply(response);
    }

}

   