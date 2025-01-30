/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.Subtasks;

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
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class subtasks extends AbstractVerticle{
   private Logging logger;
   static int TIMEOUT_TIME = 120000;
   EventBus eventBus;
    
    
     @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        
        //apis
        
        eventBus.consumer("CREATESUBTASK", this::creatingSubtask);
        eventBus.consumer("FETCHSUBTASKBYOBJ", this::fetchSubtasksByObjectives);
        eventBus.consumer("FETCHALLSUBTASKS", this::fetchAllSubtasks);
        eventBus.consumer("UPDATESUBTASKS", this::updateSubtasks); 
        
    }
    
    private void creatingSubtask(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        JsonArray subtasks = requestBody.getJsonArray("subtasks"); // Expecting an array of subtasks
        String objectiveId = requestBody.getString("objectiveId");

       if (subtasks == null || subtasks.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! Subtasks array is required.");
            message.reply(response);
            return;
        }

        String createSubtaskSQL = "INSERT INTO [dbo].[Subtasks] ([Id], [ObjectiveId], [Name], [Frequency], [Verification], [CreatedAt], [UpdatedAt]) " +
                "VALUES (NEWID(), ?, ?, ?, ?, GETDATE(), GETDATE())";

        try (PreparedStatement prCreateSubtask = connection.prepareStatement(createSubtaskSQL)) {
            
            boolean isSuccess = true;

            for (int i = 0; i < subtasks.size(); i++) {
                JsonObject subtask = subtasks.getJsonObject(i);

                String name = subtask.getString("name");
                String frequency = subtask.getString("frequency");
                String verification = subtask.getString("verification");

                if (objectiveId == null || name == null) {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Error! ObjectiveId and Name are required for each subtask.");
                    message.reply(response);
                    return;
                }

                prCreateSubtask.setString(1, objectiveId);
                prCreateSubtask.setString(2, name);
                prCreateSubtask.setString(3, frequency);
                prCreateSubtask.setInt(4, Integer.parseInt(verification));
                prCreateSubtask.addBatch();
            }

            int[] batchResults = prCreateSubtask.executeBatch();
            for (int result : batchResults) {
                if (result == 0) { 
                    isSuccess = false;
                    break;
                }
            }

            if (isSuccess) {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! All subtasks created successfully.");
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "Error! One or more subtasks could not be created.");
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
    
    private void fetchSubtasksByObjectives(Message<JsonObject> message) {
    JsonObject response = new JsonObject();
    DBConnection dbConnection = new DBConnection();
    JsonArray result = new JsonArray();

    JsonObject requestBody = message.body();
    JsonArray objectiveIds = requestBody.getJsonArray("ObjectiveId");

    if (objectiveIds == null || objectiveIds.isEmpty()) {
        response.put("responseCode", "999")
                .put("responseDescription", "Error! At least one ObjectiveId is required.");
        message.reply(response);
        return;
    }

    // Convert JsonArray to a comma-separated list of placeholders for SQL query
    String placeholders = objectiveIds.stream()
        .map(obj -> "?") // Create '?' placeholders for each objectiveId
        .collect(Collectors.joining(", "));

    String query = "SELECT * FROM [dbo].[Subtasks] WHERE [ObjectiveId] IN (" + placeholders + ")";

    try (Connection connection = dbConnection.getConnection();
         PreparedStatement prFetch = connection.prepareStatement(query)) {

        // Set ObjectiveIds in PreparedStatement
        for (int i = 0; i < objectiveIds.size(); i++) {
            prFetch.setString(i + 1, objectiveIds.getString(i));
        }

        ResultSet rs = prFetch.executeQuery();
        Map<String, JsonArray> groupedResults = new HashMap<>();

        while (rs.next()) {
            String objectiveId = rs.getString("ObjectiveId");

            JsonObject subtask = new JsonObject()
                    .put("Id", rs.getString("Id"))
                    .put("ObjectiveId", objectiveId)
                    .put("Name", rs.getString("Name"))
                    .put("Frequency", rs.getString("Frequency"))
                    .put("Verification", String.valueOf(rs.getInt("Verification")))
                    .put("CreatedAt", rs.getString("CreatedAt"))
                    .put("UpdatedAt", rs.getString("UpdatedAt"));

            groupedResults
                .computeIfAbsent(objectiveId, k -> new JsonArray())
                .add(subtask);
        }

        JsonArray finalResponseArray = new JsonArray();
        groupedResults.forEach((objectiveId, subtasks) -> {
            JsonObject objectiveData = new JsonObject()
                    .put("ObjectiveId", objectiveId)
                    .put("Subtasks", subtasks);
            finalResponseArray.add(objectiveData);
        });

        if (finalResponseArray.size() > 0) {
            response.put("responseCode", "000")
                    .put("responseDescription", "Subtasks fetched successfully.")
                    .put("data", finalResponseArray);
        } else {
            response.put("responseCode", "999")
                    .put("responseDescription", "No subtasks found for the given ObjectiveIds.");
        }
    } catch (Exception e) {
        response.put("responseCode", "999")
                .put("responseDescription", "Database error: " + e.getMessage());
        e.printStackTrace();
    } finally {
        dbConnection.closeConn();
    }

    message.reply(response);
}

    private void fetchAllSubtasks(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        JsonArray result = new JsonArray();

        String query = "SELECT * FROM [dbo].[Subtasks]";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement prFetchAll = connection.prepareStatement(query)) {

            ResultSet rs = prFetchAll.executeQuery();

            while (rs.next()) {
                JsonObject jo = new JsonObject()
                        .put("Id", rs.getString("Id"))
                        .put("ObjectiveId", rs.getString("ObjectiveId"))
                        .put("Name", rs.getString("Name"))
                        .put("Frequency", rs.getString("Frequency"))
                        .put("Verification", String.valueOf(rs.getInt("Verification")))
                        .put("CreatedAt", rs.getString("CreatedAt"))
                        .put("UpdatedAt", rs.getString("UpdatedAt"));
                result.add(jo);
            }
        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        if (result.size() > 0) {
            response.put("responseCode", "000")
                    .put("responseDescription", "All subtasks fetched successfully.")
                    .put("data", result);
        } else {
            response.put("responseCode", "999")
                    .put("responseDescription", "No subtasks found.");
        }

        message.reply(response);
    } 
       
    private void updateSubtasks(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String id = requestBody.getString("Id");
        String name = requestBody.getString("Name");
        String frequency = requestBody.getString("Frequency");
        String verification = requestBody.getString("Verification");

        if (id == null || id.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! Subtask ID is required for update.");
            message.reply(response);
            return;
        }

        String updateSQL = "UPDATE [dbo].[Subtasks] " +
                "SET [Name] = ?, [Frequency] = ?, [Verification] = ?, [UpdatedAt] = GETDATE() " +
                "WHERE [Id] = ?";

        try (PreparedStatement prUpdateSubtask = connection.prepareStatement(updateSQL)) {
            connection.setAutoCommit(false);

            prUpdateSubtask.setString(1, name);
            prUpdateSubtask.setString(2, frequency);
            prUpdateSubtask.setInt(3, Integer.parseInt(verification));
            prUpdateSubtask.setString(4, id);

            int rowsAffected = prUpdateSubtask.executeUpdate();

            if (rowsAffected > 0) {
                connection.commit();
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Subtask updated successfully.");
            } else {
                connection.rollback();
                response.put("responseCode", "999")
                        .put("responseDescription", "Error! No subtask found with the provided ID.");
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
