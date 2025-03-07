/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.CalenderofEvents;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class events extends AbstractVerticle{
    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;
    
    
     @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        eventBus.consumer("CREATEEVENTS", this::insertEvent);
        eventBus.consumer("FETCHBYBRANCHID", this::fetchEventsByBranchId);
        eventBus.consumer("UPDATEEVENTSACTUALCOST", this::updateEventsById);
        eventBus.consumer("UPDATEEVENTDATEWITHREASON", this::updateEventsByDate);
    }
    
    private void insertEvent(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();

        String event = requestBody.getString("event");
        String eventDate = requestBody.getString("date");
        String resourcesRequired = requestBody.getString("resourcesRequired", ""); // Default empty if missing
        String branchId = requestBody.getString("branchId");
        String deliverables = requestBody.getString("deliverables", ""); // Default empty if missing
        String userId = requestBody.getString("userId");

        // Ensure expectedCost exists before retrieving
        if (!requestBody.containsKey("expectedCost")) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Missing required field: expectedCost");
            message.reply(response);
            return;
        }

          String expectedCostStr = requestBody.getString("expectedCost");
            double expectedCost = (expectedCostStr != null && !expectedCostStr.isEmpty()) ? Double.parseDouble(expectedCostStr) : 0.0;

          

        // Validate required fields
        if (event == null || event.isEmpty() || eventDate == null || eventDate.isEmpty() ||
            branchId == null || branchId.isEmpty() || userId == null || userId.isEmpty()) {

            response.put("responseCode", "999")
                    .put("responseDescription", "Missing required fields.");
            message.reply(response);
            return;
        }

        String insertQuery = "INSERT INTO [Calender-of-Events] " +
                             "(id, event, date, resourcesRequired, branchId, deliverables, expectedCost, userId, Status, createdAt, updatedAt) " +
                             "VALUES (NEWID(), ?, ?, ?, ?, ?, ?, ?, 0, GETDATE(), GETDATE())";

        try (PreparedStatement insertStmt = connection.prepareStatement(insertQuery, Statement.RETURN_GENERATED_KEYS)) {
            insertStmt.setString(1, event);
            insertStmt.setString(2, eventDate);
            insertStmt.setString(3, resourcesRequired);
            insertStmt.setString(4, branchId);
            insertStmt.setString(5, deliverables);
            insertStmt.setDouble(6, expectedCost); 
            insertStmt.setString(7, userId);

            int affectedRows = insertStmt.executeUpdate();
            ResultSet generatedKeys = insertStmt.getGeneratedKeys();

            if (affectedRows > 0 && generatedKeys.next()) {
                response.put("responseCode", "000")
                        .put("responseDescription", "Event inserted successfully.");
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "Failed to insert event.");
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

    private void fetchEventsByBranchId(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String branchId = requestBody.getString("branchId");
        String status = requestBody.getString("Status");
                
        if (branchId == null || branchId.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Branch ID is required.");
            message.reply(response);
            return;
        }

        String fetchQuery = "SELECT id, event, date, resourcesRequired, branchId, deliverables, expectedCost, actualCost, Status, userId, comments, createdAt, updatedAt " +
                            "FROM [Calender-of-Events] WHERE branchId = ?";
       

        if (status != null && !status.isEmpty()) {
            fetchQuery += " AND status = ?";
        }
        
        try (PreparedStatement fetchStmt = connection.prepareStatement(fetchQuery)) {
            fetchStmt.setString(1, branchId);
            
            if (status != null && !status.isEmpty()) {
                fetchStmt.setInt(2, Integer.parseInt(status));
            }

            ResultSet resultSet = fetchStmt.executeQuery();
            JsonArray eventsArray = new JsonArray();

            while (resultSet.next()) {
                JsonObject event = new JsonObject();
                event.put("id", resultSet.getString("id"));
                event.put("event", resultSet.getString("event"));
                event.put("date", resultSet.getString("date"));
                event.put("resourcesRequired", resultSet.getString("resourcesRequired"));
                event.put("branchId", resultSet.getString("branchId"));
                event.put("deliverables", resultSet.getString("deliverables"));
                event.put("expectedCost", String.valueOf(resultSet.getDouble("expectedCost")));
                event.put("actualCost", String.valueOf(resultSet.getDouble("actualCost")));
                event.put("Status", String.valueOf(resultSet.getInt("Status")));
                event.put("userId", resultSet.getString("userId"));
                event.put("comments", resultSet.getString("comments"));
                event.put("createdAt", resultSet.getString("createdAt"));
                event.put("updatedAt", resultSet.getString("updatedAt"));

                eventsArray.add(event);
            }

            if (eventsArray.isEmpty()) {
                response.put("responseCode", "999")
                        .put("responseDescription", "No events found for the provided branch ID.");
            } else {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Events fetched successfully.")
                        .put("events", eventsArray);
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

    private void updateEventsById(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String eventId = requestBody.getString("id");
        String actualCostStr = requestBody.getString("actualCost");

        // Validate required fields
        if (eventId == null || eventId.isEmpty() || actualCostStr == null || actualCostStr.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Missing required fields: id and actualCost");
            message.reply(response);
            return;
        }

        try {
            double actualCost = Double.parseDouble(actualCostStr); // Convert actualCost to double

            String updateQuery = "UPDATE [Calender-of-Events] SET actualCost = ?, Status = 1, updatedAt = GETDATE() WHERE id = ?";

            try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
                updateStmt.setDouble(1, actualCost);
                updateStmt.setString(2, eventId);

                int affectedRows = updateStmt.executeUpdate();

                if (affectedRows > 0) {
                    response.put("responseCode", "000")
                            .put("responseDescription", "Event updated successfully.");
                } else {
                    response.put("responseCode", "999")
                            .put("responseDescription", "No event found with the provided id.");
                }
            }
        } catch (NumberFormatException e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Invalid actualCost format. Must be a valid number.");
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

    private void updateEventsByDate(Message<JsonObject> message){      
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String eventId = requestBody.getString("id");
        String newEventDate = requestBody.getString("newEventDate");
        String previousDate = requestBody.getString("previousDate");
        String comments = requestBody.getString("comments");

        if (eventId == null || eventId.isEmpty() || newEventDate == null || newEventDate.isEmpty() || comments == null || comments.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Missing required fields: id, newEventDate, and comments");
            message.reply(response);
            return;
        }

        String updateQuery = "UPDATE [Calender-of-Events] SET date = ?, comments = ?, updatedAt = GETDATE() WHERE id = ?";

        try (PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {
            updateStmt.setString(1, newEventDate);
            updateStmt.setString(2, comments);
            updateStmt.setString(3, eventId);

            int affectedRows = updateStmt.executeUpdate();

            if (affectedRows > 0) {
                response.put("responseCode", "000")
                        .put("responseDescription", "Event updated successfully.");

                // Fetch event details for SMS
                String fetchQuery = "SELECT event FROM [Calender-of-Events] WHERE id = ?";
                try (PreparedStatement fetchStmt = connection.prepareStatement(fetchQuery)) {
                    fetchStmt.setString(1, eventId);
                    ResultSet resultSet = fetchStmt.executeQuery();
                    if (resultSet.next()) {
                        String eventName = resultSet.getString("event");
                        sendEventUpdateNotification(eventId, eventName, newEventDate, previousDate, comments);
                    }
                }
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "No event found with the provided id.");
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

    private void sendEventUpdateNotification(String eventId, String eventName, String newEventDate, String previousDate, String comments) { 
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        String adminRoleIdQuery = "SELECT id FROM roles WHERE name = 'Admin'";
        String adminPhoneQuery = "SELECT u.phone_number, u.first_name, b.Name AS branch_name " +
                                "FROM users u " +
                                "INNER JOIN usersBranches Y on Y.UserId = u.uuid " +
                                "INNER JOIN Branches b ON Y.BranchId = b.Id " +
                                "INNER JOIN [Calender-of-Events] e ON e.branchId = b.Id " +                
                                "WHERE u.type = ? AND e.id = ?";

        
        try (PreparedStatement roleStmt = connection.prepareStatement(adminRoleIdQuery);
             ResultSet roleRs = roleStmt.executeQuery()) {

            if (roleRs.next()) {
                String adminRoleId = roleRs.getString("id");

                try (PreparedStatement phoneStmt = connection.prepareStatement(adminPhoneQuery)) {
                    phoneStmt.setString(1, adminRoleId);
                    phoneStmt.setString(2, eventId);
                    ResultSet phoneRs = phoneStmt.executeQuery();

                    while (phoneRs.next()) {
                        String adminPhoneNumber = phoneRs.getString("phone_number");
                        String firstName = phoneRs.getString("first_name");
                        String branchName = phoneRs.getString("branch_name");

                        String smsMessage = "Dear " + firstName + ", the " + eventName + " event of " + branchName +
                            " branch has been rescheduled from " + previousDate + " to " + newEventDate + " due to " + comments + ".";

                        JsonObject smsData = new JsonObject();
                        smsData.put("phonenumber", adminPhoneNumber);
                        smsData.put("msg", smsMessage);
                        
                        System.out.println(smsMessage);

                        eventBus.send("COMMUNICATION_ADAPTOR", smsData);
                    }
                }
            }
        } catch (Exception e) {
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
    }


}
