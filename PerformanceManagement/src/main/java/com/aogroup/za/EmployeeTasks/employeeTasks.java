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
        eventBus.consumer("SUBMITTASKS", this::submitTask);

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
                int frequencydays = 1;

                try (PreparedStatement psFetch = connection.prepareStatement(query)) {
                    psFetch.setString(1, subtaskId);
                    ResultSet rs = psFetch.executeQuery();
                    if (rs.next()) {
                        frequency = rs.getString("Frequency"); 

                        switch (frequency) {
                            case "Weekly":
                                frequencydays = 7;
                                break;
                            case "Monthly":
                                frequencydays = 30;
                                break;
                        }

                        LocalDate startDate = rs.getDate("PeriodStart").toLocalDate();
                        LocalDate endDate = rs.getDate("PeriodEnd").toLocalDate();
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

                LocalDate trackingDate = LocalDate.now();
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
                       "       s.Name AS SubtaskName, " +
                       "       s.Frequency," +
                       "       s.Verification," +
                       "       e.Target, " +
                       "       p.TaskDate, " +
                       "       p.ExpectedTarget, " +
                       "       p.AchievedTarget " +
                       "FROM EmployeeTasks e " +
                       "JOIN Subtasks s ON e.SubtasksId = s.Id " +
                       "JOIN Objectives o ON s.ObjectiveId = o.Id " +
                       "LEFT JOIN ProgressiveTracking p ON e.Id = p.EmployeeTaskId " +
                       "WHERE e.UserId = ? " +
                       "ORDER BY o.Name, s.Name, p.TaskDate";

        try (PreparedStatement psFetch = connection.prepareStatement(query)) {
            psFetch.setString(1, userId);
            ResultSet rs = psFetch.executeQuery();

            Map<String, JsonObject> groupedResults = new HashMap<>();

            while (rs.next()) {
                String objectiveName = rs.getString("ObjectiveName");
                String subtaskName = rs.getString("SubtaskName");
                String frequency = rs.getString("Frequency");
                int verification = rs.getInt("Verification");
                int target = rs.getInt("Target");
                LocalDate taskDate = rs.getDate("TaskDate").toLocalDate();
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
                        .put("Target", target)
                        .put("Progress", new JsonArray());
                    subtasksArray.add(subtaskData);
                }

                JsonArray progressArray = subtaskData.getJsonArray("Progress");
                progressArray.add(new JsonObject()
                    .put("TaskDate", taskDate.toString())
                    .put("ExpectedTarget", String.valueOf(expected))
                    .put("AchievedTarget", String.valueOf(achieved))
                    .put("Frequency", frequency)
                    .put("Verification", String.valueOf(verification)));

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
    
    private void submitTask(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String employeeTaskId = requestBody.getString("EmployeeTaskId");
        String progressiveTrackingId = requestBody.getString("ProgressiveTrackingId");
        String header = requestBody.getString("Header");
        String notes = requestBody.getString("Notes");
        double longitude = requestBody.getDouble("Longitude");
        double latitude = requestBody.getDouble("Latitude");

        try {

            // Insert new submission
            String insertSubmissionSQL = "INSERT INTO UserTaskSubmissions (EmployeeTaskId, ProgressiveTrackingId, TaskDate, Header, Notes, Longitude, Latitude, CreatedAt, UpdatedAt) " +
                                         "VALUES (?, ?, GETDATE(), ?, ?, ?, ?, GETDATE(), GETDATE())";

            try (PreparedStatement psInsert = connection.prepareStatement(insertSubmissionSQL)) {
                psInsert.setString(1, employeeTaskId);
                psInsert.setString(2, progressiveTrackingId);
                psInsert.setString(3, header);
                psInsert.setString(4, notes);
                psInsert.setDouble(5, longitude);
                psInsert.setDouble(6, latitude);
                psInsert.executeUpdate();
            }

            // Update AchievedTarget in ProgressiveTracking
            String updateProgressSQL = "UPDATE ProgressiveTracking " +
                                       "SET AchievedTarget = AchievedTarget + 1, UpdatedAt = GETDATE() " +
                                       "WHERE Id = ?";

            try (PreparedStatement psUpdate = connection.prepareStatement(updateProgressSQL)) {
                psUpdate.setString(1, progressiveTrackingId);
                int rowsUpdated = psUpdate.executeUpdate();

                if (rowsUpdated == 0) {
                    response.put("responseCode", "999").put("responseDescription", "Failed to update AchievedTarget");
                    message.reply(response);
                    return;
                }
            }


            response.put("responseCode", "000")
                    .put("responseDescription", "Task submitted and progress updated successfully");

        } catch (Exception e) {
            try {

            response.put("responseCode", "999").put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
            } finally {
                dbConnection.closeConn();
            }
            message.reply(response);
        }

    }
      
}
