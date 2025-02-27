/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.Checklist;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class checklists extends AbstractVerticle{
    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;
    
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        eventBus.consumer("CREATECHECKLIST", this::createChecklist);
        eventBus.consumer("CREATEAGENTCHECKLIST", this::createAgentChecklistWithNotes);
    }
    
    private void createChecklist(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        JsonObject requestBody = message.body();
        String name = requestBody.getString("name");

        // Validate input
        if (name == null || name.trim().isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Checklist name is required");
            message.reply(response);
            return;
        }

        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        PreparedStatement checkExistenceStmt = null;
        PreparedStatement insertChecklistStmt = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();
            connection.setAutoCommit(false);

            // Query to check if the checklist name already exists
            String checkExistenceQuery = "SELECT COUNT(*) FROM [dbo].[Checklist] WHERE [name] = ?";
            checkExistenceStmt = connection.prepareStatement(checkExistenceQuery);
            checkExistenceStmt.setString(1, name);
            rs = checkExistenceStmt.executeQuery();

            if (rs.next() && rs.getInt(1) > 0) {
                response.put("responseCode", "999")
                        .put("responseDescription", "Error! Checklist name already exists");
                message.reply(response);
                return;
            }

            // Insert new checklist if name doesn't exist
            String insertChecklistQuery = "INSERT INTO [dbo].[Checklist] ([name], [createdAt], [updatedAt]) "
                                        + "VALUES (?, GETDATE(), GETDATE())";

            insertChecklistStmt = connection.prepareStatement(insertChecklistQuery);
            insertChecklistStmt.setString(1, name);

            int rowsAffected = insertChecklistStmt.executeUpdate();

            if (rowsAffected > 0) {
                connection.commit();
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Checklist created successfully");
            } else {
                connection.rollback();
                response.put("responseCode", "999")
                        .put("responseDescription", "Error! Checklist creation failed");
            }

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources to avoid memory leaks
            try {
                if (rs != null) rs.close();
                if (checkExistenceStmt != null) checkExistenceStmt.close();
                if (insertChecklistStmt != null) insertChecklistStmt.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnection.closeConn();
        }

        message.reply(response);
    }
    
    private void createAgentChecklistWithNotes(Message<JsonObject> message) {
        JsonObject response = new JsonObject();

        // Extract user details from headers
        MultiMap headers = message.headers();
        
        if (headers.isEmpty()) {
            message.fail(666, "Unauthenticated User");
            return;
        }

        String user_uuid = headers.get("user_uuid"); // Logged-in user's UUID

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        JsonArray checklists = requestBody.getJsonArray("checklists");
        String agentId = requestBody.getString("agentId");
        String notes = requestBody.getString("notes");
        String escalation = requestBody.getString("escalation"); // UUID of person being escalated to

        if (agentId == null || checklists == null || checklists.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! AgentId and Checklists are required.");
            message.reply(response);
            return;
        }

        // Determine escalationStatus based on logged-in user's UUID
        int escalationStatus = 0; // Default: No escalation
        if (isValidUUID(escalation)) {
            if (escalation.equalsIgnoreCase(user_uuid)) {
                escalationStatus = 1; // Escalated to self
            } else {
                escalationStatus = 2; // Escalated to another user
            }
        }

        String createChecklistSQL = "INSERT INTO [dbo].[Agent_Checklist] ([id], [agentId], [checklistName], [status], [createdAt], [updatedAt]) " +
                "VALUES (NEWID(), ?, ?, ?, GETDATE(), GETDATE())";

        String insertNotesSQL = "INSERT INTO [dbo].[Checklist-Notes] ([id], [agentId], [notes], [escalation], [escalationStatus], [createdAt], [updatedAt]) " +
                "VALUES (NEWID(), ?, ?, ?, ?, GETDATE(), GETDATE())";

        try {
            connection.setAutoCommit(false);

            // Insert checklists
            try (PreparedStatement prCreateChecklist = connection.prepareStatement(createChecklistSQL)) {
                for (int i = 0; i < checklists.size(); i++) {
                    JsonObject checklist = checklists.getJsonObject(i);
                    String checklistName = checklist.getString("checklistName");
                    int status = Integer.parseInt(checklist.getString("status", "0"));

                    prCreateChecklist.setString(1, agentId);
                    prCreateChecklist.setString(2, checklistName);
                    prCreateChecklist.setInt(3, status);
                    prCreateChecklist.addBatch();
                }
                prCreateChecklist.executeBatch();
            }

            // Insert notes, escalation, and escalationStatus
            try (PreparedStatement prInsertNotes = connection.prepareStatement(insertNotesSQL)) {
                prInsertNotes.setString(1, agentId);
                prInsertNotes.setString(2, notes);
                if (isValidUUID(escalation)) {
                    prInsertNotes.setString(3, escalation);
                } else {
                    prInsertNotes.setNull(3, java.sql.Types.OTHER);
                }
                prInsertNotes.setInt(4, escalationStatus);
                prInsertNotes.executeUpdate();
            }

            connection.commit();
            response.put("responseCode", "000");
            response.put("responseDescription", "Success! Checklists and Notes created successfully.");
        } catch (Exception e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackEx) {
                rollbackEx.printStackTrace();
            }
            response.put("responseCode", "999");
            response.put("responseDescription", "Database error: " + e.getMessage());
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

    private boolean isValidUUID(String uuid) {
        if (uuid == null || uuid.trim().isEmpty()) return false;
        try {
            UUID.fromString(uuid);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
