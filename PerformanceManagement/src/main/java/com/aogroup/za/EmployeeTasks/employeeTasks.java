/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.EmployeeTasks;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class employeeTasks extends AbstractVerticle{
    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;
    
    
     @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        //apis    
        eventBus.consumer("ASSIGNTARGETS", this::assigningTargets);
        eventBus.consumer("FETCHEMPLOYEETASKBYUSERID", this::fetchUserTaskById);
        eventBus.consumer("UPDATETASKASSIGNED", this::updateTaskAssigned); 
        eventBus.consumer("UPDATEPROGRESSIVETRACKING", this::updateProgressiveTracking);
        eventBus.consumer("FETCH_ROS_BY_BRANCH_MANAGER", this::fetchROsByBranchManager);
        eventBus.consumer("FETCH_USER_SUBTASKS_WITH_TARGETS", this::fetchUserSubtasksWithTargets);
        eventBus.consumer("FETCHUSERSPERROLE", this::fetchUsersByRole);
        eventBus.consumer("FETCHUSERSBYROLEID", this::fetchUsersByRoleId);
        eventBus.consumer("FETCHUSERSBYBRANCH", this::fetchUsersByBranch);

    }
    
    private void assigningTargets(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String userId = requestBody.getString("UserId");
        String branchId = requestBody.getString("BranchId");
        JsonArray userTargets = requestBody.getJsonArray("usertargets");

        try {
            connection.setAutoCommit(false);

            // Check if the user already has a task assigned for any of the subtasks
            for (int i = 0; i < userTargets.size(); i++) {
                JsonObject subtaskData = userTargets.getJsonObject(i);
                String subtaskId = subtaskData.getString("SubtasksId");

                String checkExistingTaskSQL = "SELECT COUNT(*) FROM EmployeeTasks WHERE UserId = ? AND SubtasksId = ?";
                try (PreparedStatement psCheckExisting = connection.prepareStatement(checkExistingTaskSQL)) {
                    psCheckExisting.setString(1, userId);
                    psCheckExisting.setString(2, subtaskId);
                    ResultSet rsCheck = psCheckExisting.executeQuery();
                    if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                        response.put("responseCode", "999")
                                .put("responseDescription", "Error: User already has targets assigned for this subtask.");
                        message.reply(response);
                        return; // Stop further execution as the error was encountered
                    }
                }
            }

            // Continue assigning targets if no conflicts
            for (int i = 0; i < userTargets.size(); i++) {
                JsonObject subtaskData = userTargets.getJsonObject(i);
                String subtaskId = subtaskData.getString("SubtasksId");
                int totalTarget = Integer.parseInt(subtaskData.getString("Target"));

                // Fetch frequency and duration from Subtasks & Objectives
                String query = "SELECT s.Frequency, o.PeriodStart, o.PeriodEnd " +
                               "FROM Subtasks s JOIN Objectives o ON s.ObjectiveId = o.Id " +
                               "WHERE s.Id = ?";

                String frequency = "Weekly"; 
                int numPeriods = 1;
                int frequencydays = 0;
                LocalDate startDate = null;
                LocalDate endDate = null;

                try (PreparedStatement psFetch = connection.prepareStatement(query)) {
                    psFetch.setString(1, subtaskId);
                    ResultSet rs = psFetch.executeQuery();
                    if (rs.next()) {
                        frequency = rs.getString("Frequency"); 

                        switch (frequency.toLowerCase()) {
                            case "weekly":
                                frequencydays = 7;
                                break;
                            case "monthly":
                                frequencydays = 30;
                                break;
                            case "quarterly":
                                frequencydays = 90;
                                break;
                            case "daily":
                                frequencydays = 1;
                                break;
                        }

                        startDate = rs.getDate("PeriodStart").toLocalDate();
                        endDate = rs.getDate("PeriodEnd").toLocalDate();
                        numPeriods = (int) ChronoUnit.DAYS.between(startDate, endDate) / frequencydays;
                    }
                }

                // Generate a unique EmployeeTaskId
                String employeeTaskId = UUID.randomUUID().toString();

                // Insert into EmployeeTasks with predefined ID
                String insertTaskSQL = "INSERT INTO EmployeeTasks (Id, UserId, BranchId, SubtasksId, Target, CreatedAt, UpdatedAt) " +
                                       "VALUES (?, ?, ?, ?, ?, GETDATE(), GETDATE())";

                try (PreparedStatement psInsertTask = connection.prepareStatement(insertTaskSQL)) {
                    psInsertTask.setString(1, employeeTaskId);
                    psInsertTask.setString(2, userId);
                    psInsertTask.setString(3, branchId);
                    psInsertTask.setString(4, subtaskId);
                    psInsertTask.setInt(5, totalTarget);
                    psInsertTask.executeUpdate();
                }

                System.out.println("EmployeeTaskId : " + employeeTaskId); // Debugging

                // Insert tracking records for each period dynamically
                String insertProgressSQL = "INSERT INTO ProgressiveTracking (Id, EmployeeTaskId, TaskDate, ExpectedTarget, AchievedTarget, CreatedAt, UpdatedAt) " +
                                           "VALUES (NEWID(), ?, ?, ?, 0, GETDATE(), GETDATE())";

                LocalDate trackingDate = startDate;
                for (int p = 0; p < numPeriods; p++) {
                    try (PreparedStatement psInsertProgress = connection.prepareStatement(insertProgressSQL)) {
                        psInsertProgress.setString(1, employeeTaskId);
                        psInsertProgress.setDate(2, java.sql.Date.valueOf(trackingDate));
                        psInsertProgress.setInt(3, totalTarget);
                        psInsertProgress.executeUpdate();
                    }

                    trackingDate = trackingDate.plusDays(frequencydays); 
                }
            }

            connection.commit();
            response.put("responseCode", "000")
                    .put("responseDescription", "Targets assigned successfully");

        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException ignored) {}

            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }
        message.reply(response);
    }

    private void fetchUserTaskById(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String userId = requestBody.getString("UserId");

        String query = "SELECT o.Name AS ObjectiveName, " +
                       "       e.Id AS SubtaskID, " +  // <-- Added Subtask ID
                       "       s.Name AS SubtaskName, " +
                       "       s.Frequency, " +
                       "       s.Verification, " +
                       "       s.isMultiple, " +
                       "       e.Target, " +
                       "       p.TaskDate, " +
                       "       p.ExpectedTarget, " +
                       "       p.AchievedTarget " +
                       "FROM EmployeeTasks e " +
                       "INNER JOIN Subtasks s ON e.SubtasksId = s.Id " +
                       "INNER JOIN Objectives o ON s.ObjectiveId = o.Id " +
                       "INNER JOIN ProgressiveTracking p ON e.Id = p.EmployeeTaskId " +
                       "WHERE e.UserId = ? " +
                       "ORDER BY o.Name, s.Name, p.TaskDate";

        try (PreparedStatement psFetch = connection.prepareStatement(query)) {
            psFetch.setString(1, userId);
            ResultSet rs = psFetch.executeQuery();

            Map<String, JsonObject> groupedResults = new HashMap<>();

            while (rs.next()) {
                String objectiveName = rs.getString("ObjectiveName");
                String subtaskId = rs.getString("SubtaskID");  // <-- Extract SubtaskID
                String subtaskName = rs.getString("SubtaskName");
                String frequency = rs.getString("Frequency");
                int verification = rs.getInt("Verification");
                int isMultiple = rs.getInt("isMultiple");
                int target = rs.getInt("Target");
                String taskDate = !rs.getString("TaskDate").isEmpty() ? rs.getString("TaskDate") : null;
                int expected = rs.getInt("ExpectedTarget");
                int achieved = rs.getInt("AchievedTarget");

                JsonObject objectiveData = groupedResults.getOrDefault(objectiveName, new JsonObject()
                    .put("ObjectiveName", objectiveName)
                    .put("Subtasks", new JsonArray()));

                JsonArray subtasksArray = objectiveData.getJsonArray("Subtasks");

                Optional<JsonObject> subtaskEntry = subtasksArray.stream()
                    .map(obj -> (JsonObject) obj)
                    .filter(obj -> obj.getString("SubtaskName").equals(subtaskName))
                    .findFirst();

                JsonObject subtaskData;
                if (subtaskEntry.isPresent()) {
                    subtaskData = subtaskEntry.get();
                } else {
                    subtaskData = new JsonObject()
                        .put("SubtaskName", subtaskName)
                        .put("SubtaskID", subtaskId)  // <-- Added SubtaskID to JSON response
                        .put("Target", target)
                        .put("Frequency", frequency)
                        .put("Verification", String.valueOf(verification))
                        .put("IsMultiple", String.valueOf(isMultiple))
                        .put("Progress", new JsonArray());
                    subtasksArray.add(subtaskData);
                }

                JsonArray progressArray = subtaskData.getJsonArray("Progress");
                progressArray.add(new JsonObject()
                    .put("TaskDate", taskDate)
                    .put("ExpectedTarget", String.valueOf(expected))
                    .put("AchievedTarget", String.valueOf(achieved)));
                    

                groupedResults.put(objectiveName, objectiveData);
            }

            JsonArray finalResponseArray = new JsonArray();
            groupedResults.values().forEach(finalResponseArray::add);

            response.put("responseCode", "000")
                    .put("responseDescription", "Success")
                    .put("data", finalResponseArray);
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
            
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
    }

    private void updateTaskAssigned(Message<JsonObject> message) {
    JsonObject response = new JsonObject();
    DBConnection dbConnection = new DBConnection();
    Connection connection = dbConnection.getConnection();

    JsonObject requestBody = message.body();
    String id = requestBody.getString("Id");
    String target = requestBody.getString("Target");

    String updateTaskSQL = "UPDATE [dbo].[EmployeeTasks] SET [Target] = ?, [UpdatedAt] = GETDATE() WHERE [Id] = ?";
    String updateProgressiveTrackingSQL = "UPDATE [dbo].[ProgressiveTracking] SET [ExpectedTarget] = ?, [UpdatedAt] = GETDATE() WHERE [EmployeeTaskId] = ?";

    try {
        connection.setAutoCommit(false); // Begin transaction

        // Debugging: Print input values
        System.out.println("Updating EmployeeTasks - ID: " + id + ", Target: " + target);
        System.out.println("Updating ProgressiveTracking - EmployeeTaskId: " + id + ", ExpectedTarget: " + target);

        // Check if EmployeeTasks ID exists
        String checkTaskSQL = "SELECT COUNT(*) FROM [dbo].[EmployeeTasks] WHERE [Id] = ?";
        try (PreparedStatement checkTaskStmt = connection.prepareStatement(checkTaskSQL)) {
            checkTaskStmt.setString(1, id);
            ResultSet rs = checkTaskStmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                response.put("responseCode", "999")
                        .put("responseDescription", "Error: EmployeeTasks ID not found");
                message.reply(response);
                return;
            }
        }

        // Check if ProgressiveTracking EmployeeTaskId exists
        String checkProgressiveSQL = "SELECT COUNT(*) FROM [dbo].[ProgressiveTracking] WHERE [EmployeeTaskId] = ?";
        try (PreparedStatement checkProgressiveStmt = connection.prepareStatement(checkProgressiveSQL)) {
            checkProgressiveStmt.setString(1, id);
            ResultSet rs = checkProgressiveStmt.executeQuery();
            if (rs.next() && rs.getInt(1) == 0) {
                response.put("responseCode", "999")
                        .put("responseDescription", "Error: ProgressiveTracking EmployeeTaskId not found");
                message.reply(response);
                return;
            }
        }

        // Update EmployeeTasks
        try (PreparedStatement psUpdateTask = connection.prepareStatement(updateTaskSQL)) {
            psUpdateTask.setInt(1, Integer.parseInt(target));
            psUpdateTask.setString(2, id);
            int rows = psUpdateTask.executeUpdate();
            System.out.println("Rows affected in EmployeeTasks: " + rows);

            if (rows == 0) {
                response.put("responseCode", "999").put("responseDescription", "No EmployeeTasks updated");
                message.reply(response);
                return;
            }
        }

        // Update ProgressiveTracking
        try (PreparedStatement psUpdateProgressiveTracking = connection.prepareStatement(updateProgressiveTrackingSQL)) {
            psUpdateProgressiveTracking.setInt(1, Integer.parseInt(target));
            psUpdateProgressiveTracking.setString(2, id);
            int rowsProgress = psUpdateProgressiveTracking.executeUpdate();
            System.out.println("Rows affected in ProgressiveTracking: " + rowsProgress);

            if (rowsProgress == 0) {
                response.put("responseCode", "999").put("responseDescription", "No ProgressiveTracking updated");
                message.reply(response);
                return;
            }
        }

        connection.commit(); // Commit transaction

        response.put("responseCode", "000")
                .put("responseDescription", "Task updated successfully");

    } catch (Exception e) {
        try {
            connection.rollback(); // Rollback transaction on failure
        } catch (SQLException rollbackEx) {
            rollbackEx.printStackTrace();
        }
        response.put("responseCode", "999")
                .put("responseDescription", "Error: " + e.getMessage());
        e.printStackTrace();
    } finally {
        try {
            connection.setAutoCommit(true);
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        dbConnection.closeConn();
    }
    message.reply(response);
}
    
    private void updateProgressiveTracking(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String userId = requestBody.getString("UserId");
        JsonArray updates = requestBody.getJsonArray("updates");

        String updateSQL = "UPDATE ProgressiveTracking " +
                           "SET ExpectedTarget = ?, UpdatedAt = GETDATE() " +
                           "WHERE Id = ?";

        try {
            try (PreparedStatement psUpdate = connection.prepareStatement(updateSQL)) {
                for (int i = 0; i < updates.size(); i++) {
                    JsonObject updateData = updates.getJsonObject(i);
                    String trackingId = updateData.getString("ProgressiveTrackingId");
                    int newExpectedTarget = updateData.getInteger("ExpectedTarget");

                    psUpdate.setInt(1, newExpectedTarget);
                    psUpdate.setString(2, trackingId);
                    psUpdate.addBatch();
                }
                psUpdate.executeBatch();
            }

            response.put("responseCode", "000")
                    .put("responseDescription", "Progressive tracking updated successfully");

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
    }
          
    private void fetchROsByBranchManager(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String branchManagerId = requestBody.getString("BranchManagerId");

        try {
            // Fetch BranchId for the Branch Manager
            String branchQuery = "SELECT ub.BranchId " +
                                 "FROM users u " +
                                 "JOIN usersBranches ub ON u.uuid = ub.UserId " +
                                 "JOIN roles r ON u.type = r.id " +
                                 "WHERE u.uuid = ? AND r.name = 'BM'"; 
            
            String branchId = null;
            try (PreparedStatement psBranch = connection.prepareStatement(branchQuery)) {
                psBranch.setString(1, branchManagerId);
                ResultSet rs = psBranch.executeQuery();
                if (rs.next()) {
                    branchId = rs.getString("BranchId");
                }
            }

            // Debugging statement: Check retrieved BranchId
            System.out.println("Fetched BranchId: " + branchId);

            // If no branch is found, return an error
            if (branchId == null) {
                response.put("responseCode", "999")
                        .put("responseDescription", "Branch Manager not found or has no assigned branch");
                message.reply(response);
                return;
            }

            // Fetch ROs from the same branch
            String fetchROsQuery = "SELECT u.id AS UserId, u.first_name + ' ' + u.last_name AS Name, " +
                                   "u.email AS Email, u.uuid AS UUID, u.phone_number AS PhoneNumber " +
                                   "FROM users u " +
                                   "JOIN usersBranches ub ON u.uuid = ub.UserId " +
                                   "WHERE ub.BranchId = ? AND u.isRO = 1";
            

            JsonArray roList = new JsonArray();
            try (PreparedStatement psROs = connection.prepareStatement(fetchROsQuery)) {
                psROs.setString(1, branchId);
                ResultSet rs = psROs.executeQuery();
                while (rs.next()) {
                    JsonObject roData = new JsonObject()
                            .put("UserId", rs.getString("UserId"))
                            .put("Name", rs.getString("Name"))
                            .put("uuid", rs.getString("UUID"))
                            .put("Email", rs.getString("Email"))
                            .put("PhoneNumber", rs.getString("PhoneNumber"));
                    roList.add(roData);
                }
            }

            // Debugging statement: Check number of ROs retrieved
            System.out.println("Total ROs found: " + roList.size());

            response.put("responseCode", "000")
                    .put("responseDescription", "Success")
                    .put("ROs", roList);

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn(); // Ensures connection is properly closed
        }

        message.reply(response);
    }

    private void fetchUserSubtasksWithTargets(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String userId = requestBody.getString("UserId");

        String query = "SELECT o.Name AS ObjectiveName, " +
                       "       s.Id AS SubtaskID, " +
                       "       s.Name AS SubtaskName, " +
                       "       e.Target AS Target " +
                       "FROM EmployeeTasks e " +
                       "INNER JOIN Subtasks s ON e.SubtasksId = s.Id " +
                       "INNER JOIN Objectives o ON s.ObjectiveId = o.Id " +
                       "WHERE e.UserId = ? " +
                       "ORDER BY o.Name, s.Name";

        try (PreparedStatement psFetch = connection.prepareStatement(query)) {
            psFetch.setString(1, userId);
            ResultSet rs = psFetch.executeQuery();

            JsonArray subtasksArray = new JsonArray();

            while (rs.next()) {
                JsonObject subtaskData = new JsonObject()
                    .put("ObjectiveName", rs.getString("ObjectiveName"))
                    .put("SubtaskID", rs.getString("SubtaskID"))
                    .put("SubtaskName", rs.getString("SubtaskName"))
                    .put("Target", rs.getInt("Target"));

                subtasksArray.add(subtaskData);
            }

            response.put("responseCode", "000")
                    .put("responseDescription", "Success")
                    .put("data", subtasksArray);
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

    private void fetchUsersByBranch(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String branchId = requestBody.getString("branchId");

        // Check if branchId is provided
        if (branchId == null || branchId.isEmpty()) {
            response.put("responseCode", "400")
                    .put("responseDescription", "Error! branchId is required.");
            message.reply(response);
            return;
        }

        // SQL query to fetch users by branchId
        String fetchUsersQuery = "SELECT * FROM [Performance_Management].[dbo].[usersBranches] WHERE [BranchId] = ?";

        try (PreparedStatement fetchUsersStmt = connection.prepareStatement(fetchUsersQuery)) {
            fetchUsersStmt.setString(1, branchId);

            ResultSet resultSet = fetchUsersStmt.executeQuery();

            JsonArray usersArray = new JsonArray();

            // Loop through the result set and build the response
            while (resultSet.next()) {
                JsonObject user = new JsonObject();
                user.put("id", resultSet.getString("Id"));
                user.put("userId", resultSet.getString("UserId"));
                user.put("branchId", resultSet.getString("BranchId"));
                user.put("createdDate", resultSet.getString("CreatedDate"));

                usersArray.add(user);
            }

            // Check if users were found
            if (usersArray.isEmpty()) {
                response.put("responseCode", "404")
                        .put("responseDescription", "No users found for the provided branch ID.");
            } else {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Users fetched successfully.")
                        .put("users", usersArray);
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

    private void fetchUsersByRoleId(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        JsonObject requestBody = message.body();
        String roleIdStr = requestBody.getString("RoleId");

        // Validate input
        if (roleIdStr == null || roleIdStr.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Role ID is required");
            message.reply(response);
            return;
        }

        int roleId;
        try {
            roleId = Integer.parseInt(roleIdStr);
        } catch (NumberFormatException e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Invalid Role ID format");
            message.reply(response);
            return;
        }

        JsonArray userList = new JsonArray();
        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        PreparedStatement psUsers = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            String fetchUsersQuery = "SELECT u.id AS UserId, " +
                                     "u.first_name + ' ' + u.last_name AS Name, " +
                                     "u.email AS Email, " +
                                     "u.uuid AS UUID, " +
                                     "u.phone_number AS PhoneNumber, " +
                                     "u.branch AS Branch " +
                                     "FROM users u " +
                                     "WHERE u.type = ?";

            psUsers = connection.prepareStatement(fetchUsersQuery);
            psUsers.setInt(1, roleId);
            rs = psUsers.executeQuery();

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

            response.put("responseCode", "000")
                    .put("responseDescription", "Success")
                    .put("Users", userList);

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Properly close resources to avoid memory leaks
            try {
                if (rs != null) rs.close();
                if (psUsers != null) psUsers.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        message.reply(response);
    }

}
