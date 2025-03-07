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
        eventBus.consumer("FETCHALLACHECKLISTS", this::fetchAllChecklists);
        eventBus.consumer("FETCH_AGENT_DETAILS", this::fetchAgentDetailsByCustomerNumber);

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
//        String escalation = requestBody.getString("escalation"); // UUID of person being escalated to

        String escalatedToUserUUID = requestBody.getString("EscalatedToUserUUID", null);
        String escalatedToEmail = requestBody.getString("EscalatedToEmail", null);
        String escalatedToPhoneNumber = requestBody.getString("EscalatedToPhoneNumber", null);
        String escalatedToName = requestBody.getString("EscalatedToName", null);

        String userId = requestBody.getString("userId");
        String branchId = requestBody.getString("branchId");
        // Validate agentId
        if (agentId == null || agentId.trim().isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! AgentId is required.");
            message.reply(response);
            return;
        }

        // Validate checklists array
        if (checklists == null || checklists.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! At least one checklist is required.");
            message.reply(response);
            return;
        }

        // Determine escalationStatus
        int escalationStatus = 0;
        if (isValidUUID(escalatedToUserUUID)) {
            escalationStatus = escalatedToUserUUID.equals(user_uuid) ? 1 : 2;
        }

        // SQL queries
        String fetchChecklistNameSQL = "SELECT name FROM [dbo].[Checklist] WHERE id = ?";
        String createChecklistSQL = "INSERT INTO [dbo].[Agent_Checklist] ([id], [agentId], [checklistName], [status], [userId], [branchId], [createdAt], [updatedAt]) " +
                "VALUES (NEWID(), ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
        String insertNotesSQL = "INSERT INTO [dbo].[Checklist-Notes] ([id], [agentId], [notes], [userId], [branchId], [escalation], [escalationStatus], [createdAt], [updatedAt]) " +
                "VALUES (NEWID(), ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";

        try {
            connection.setAutoCommit(false);

            try (PreparedStatement prFetchChecklistName = connection.prepareStatement(fetchChecklistNameSQL);
                 PreparedStatement prCreateChecklist = connection.prepareStatement(createChecklistSQL)) {

                for (int i = 0; i < checklists.size(); i++) {
                    JsonObject checklist = checklists.getJsonObject(i);
                    int checklistId = Integer.parseInt(checklist.getString("checklistId"));
                    int status = Integer.parseInt(checklist.getString("status", "0"));

                    // Fetch checklistName using checklistId
                    prFetchChecklistName.setInt(1, checklistId);
                    ResultSet rs = prFetchChecklistName.executeQuery();

                    if (!rs.next()) {
                        response.put("responseCode", "999")
                                .put("responseDescription", "Error! Checklist ID " + checklistId + " not found.");
                        message.reply(response);
                        return;
                    }

                    String checklistName = rs.getString("name");

                    // Insert into Agent_Checklist
                    prCreateChecklist.setString(1, agentId);
                    prCreateChecklist.setString(2, checklistName);
                    prCreateChecklist.setInt(3, status);
                    prCreateChecklist.setString(4, userId);
                    prCreateChecklist.setString(5, branchId);
                    prCreateChecklist.addBatch();
                }

                prCreateChecklist.executeBatch();
            }

            // Insert notes and escalation
            try (PreparedStatement prInsertNotes = connection.prepareStatement(insertNotesSQL)) {
                prInsertNotes.setString(1, agentId);
                prInsertNotes.setString(2, notes);
                prInsertNotes.setString(3, userId);
                prInsertNotes.setString(4, branchId);
                 if (isValidUUID(escalatedToUserUUID)) {
                        prInsertNotes.setString(5, escalatedToUserUUID);
                    } else {
                        prInsertNotes.setNull(5, java.sql.Types.OTHER);
                    }
                   
                prInsertNotes.setInt(6, escalationStatus);
               
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
            try {
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
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

    private void fetchAllChecklists(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        JsonArray checklistArray = new JsonArray();

        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        PreparedStatement psChecklists = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();

            // Query to fetch all checklists
            String fetchQuery = "SELECT * FROM [dbo].[Checklist]";
            psChecklists = connection.prepareStatement(fetchQuery);
            rs = psChecklists.executeQuery();

            // Loop through results and build the checklist array
            while (rs.next()) {
                JsonObject checklist = new JsonObject()
                        .put("id", rs.getInt("id"))
                        .put("name", rs.getString("name"))
                        .put("createdAt", rs.getString("createdAt"))
                        .put("updatedAt", rs.getString("updatedAt"));
                checklistArray.add(checklist);
            }

            response.put("responseCode", "000")
                    .put("responseDescription", "Success")
                    .put("checklists", checklistArray);

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources to prevent memory leaks
            try {
                if (rs != null) rs.close();
                if (psChecklists != null) psChecklists.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnection.closeConn();
        }

        message.reply(response);
    }
    
    private void fetchAgentDetailsByCustomerNumber(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        JsonObject requestBody = message.body();

        // Extract customerNumber from request
        String customerNumber = requestBody.getString("customerNumber");

        // Validate input
        if (customerNumber == null || customerNumber.trim().isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! Customer Number is required.");
            message.reply(response);
            return;
        }

        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = dbConnection.getConnection();

            // Query to fetch agent details based on customerNumber
            String query = "SELECT phone_number, full_name, id_number, location, branch_code " +
                           "FROM [unaitas_agency].[dbo].[agents] WHERE customer_number = ?";
            preparedStatement = connection.prepareStatement(query);
            preparedStatement.setString(1, customerNumber);
            resultSet = preparedStatement.executeQuery();

            // Check if agent details exist
            if (resultSet.next()) {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success")
                        .put("phoneNumber", resultSet.getString("phone_number"))
                        .put("fullName", resultSet.getString("full_name"))
                        .put("idNumber", resultSet.getString("id_number"))
                        .put("location", resultSet.getString("location"))
                        .put("branchCode", resultSet.getString("branch_code"));
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "Error! No agent found for the given Customer Number.");
            }

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnection.closeConn();
        }

        // Send response
        message.reply(response);
    }


}
