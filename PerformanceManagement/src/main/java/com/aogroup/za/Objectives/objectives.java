/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.Objectives;

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
import log.Logging;

/**
 *
 * @author Best Point
 */
public class objectives extends AbstractVerticle{
    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;
    
    
     @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        
        //apis
        
        eventBus.consumer("CREATEOBJ", this::creatingObjectives);
        eventBus.consumer("FETCHALLOBJ", this::fetchAllObjectives);
        eventBus.consumer("UPDATEOBJECTIVES", this::updateObjectives);
        eventBus.consumer("FETCHOBJBYROLE", this::fetchObjectivesByRole);
        
    }
    
    private void creatingObjectives(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();

        
        JsonArray name = requestBody.getJsonArray("Name");
        String role = requestBody.getString("Role");
        String periodStart = requestBody.getString("PeriodStart");
        String periodEnd = requestBody.getString("PeriodEnd");

        String createObjectiveSQL = "INSERT INTO [dbo].[Objectives] ([Id], [Name], [Role]"
            + ",[PeriodStart],[PeriodEnd], [CreatedAt], [UpdatedAt])"
            + " VALUES (NEWID(),?,?,?,?,GETDATE(),GETDATE())";
        
        
        try (PreparedStatement prCreateObjectives = connection.prepareStatement(createObjectiveSQL, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ;
            
            for (int x = 0; x < name.size(); x++ ) {

               prCreateObjectives.setString(1, name.getString(x));
               prCreateObjectives.setString(2, role);
               prCreateObjectives.setString(3, periodStart);
               prCreateObjectives.setString(4, periodEnd);
               prCreateObjectives.addBatch();
            }
        
            int[] insertednames = prCreateObjectives.executeBatch();

                response.put("responseCode", "000");
                response.put("responseDescription", "Success! Objective created successfully");
            

        } catch (Exception e) {
            // Log the exact SQL error message for easier debugging
            response.put("responseCode", "999");
            response.put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();  // Log the stack trace to understand where the error occurred
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
    
    private void fetchAllObjectives(Message<JsonObject> message) {
        DBConnection dbConnection = new DBConnection();
        JsonObject response = new JsonObject();


        JsonArray result = new JsonArray();

        String query = "SELECT * FROM [Performance_Management].[dbo].[Objectives]";
        
        try {
             ResultSet allObjectives = dbConnection.query_all(query);
            
            while (allObjectives.next()) {
                JsonObject jo = new JsonObject();
                jo
                           .put("Id", allObjectives.getString("Id"))
                           .put("Name", allObjectives.getString("Name"))
                           .put("Role", allObjectives.getString("Role"))
                           .put("PeriodStart", allObjectives.getString("PeriodStart"))
                           .put("PeriodEnd", allObjectives.getString("PeriodEnd"))
                           .put("CreatedAt", allObjectives.getString("CreatedAt"))
                           .put("UpdatedAt", allObjectives.getString("UpdatedAt"));                        
                result.add(jo);
                
            }
        } catch(Exception e) {
            e.getMessage();
        } finally {
            dbConnection.closeConn();
        }
        
        if (result.size() > 0) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Objectives fetched successfully.")
                    .put("data", result);
        }else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to fetch Objectives.");
        }
        message.reply(response);
        
    }
    
    private void updateObjectives(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();

        String id = requestBody.getString("Id");
        String name = requestBody.getString("name");

        if (id == null || id.isEmpty() || name == null || name.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! Both Id and name are required for update.");
            message.reply(response);
            return;
        }

        String singleUpdateSQL = "UPDATE [dbo].[Objectives] "
                + "SET [Name] = ?, [UpdatedAt] = GETDATE() "
                + "WHERE [Id] = ?";

        try (PreparedStatement prSingleUpdate = connection.prepareStatement(singleUpdateSQL)) {
            connection.setAutoCommit(false);

            prSingleUpdate.setString(1, name);
            prSingleUpdate.setString(2, id);

            int result = prSingleUpdate.executeUpdate();

            if (result > 0) {
                connection.commit();
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Objective updated successfully.");
            } else {
                connection.rollback();
                response.put("responseCode", "999")
                        .put("responseDescription", "Error! Objective with given Id not found.");
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
        return;

    }

    private void fetchObjectivesByRole(Message<JsonObject> message){
        DBConnection dbConnection = new DBConnection();
        JsonObject response = new JsonObject();
        
        JsonObject requestBody = message.body();
        String role = requestBody.getString("Role");

        JsonArray result = new JsonArray();
        
        if (role == null || role.isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error! Role is required.");
            message.reply(response);
            return;
        }

        String query = "SELECT * FROM [Performance_Management].[dbo].[Objectives] where Role = ? ";
        
       try (Connection connection = dbConnection.getConnection();
             PreparedStatement prFetch = connection.prepareStatement(query)) {

            prFetch.setString(1, role);
            ResultSet rs = prFetch.executeQuery();

            while (rs.next()) {
                JsonObject jo = new JsonObject()
                           .put("Id", rs.getString("Id"))
                           .put("Name", rs.getString("Name"))
                           .put("Role", rs.getString("Role"))
                           .put("PeriodStart", rs.getString("PeriodStart"))
                           .put("PeriodEnd", rs.getString("PeriodEnd"))
                           .put("CreatedAt", rs.getString("CreatedAt"))
                           .put("UpdatedAt", rs.getString("UpdatedAt"));                        
                result.add(jo);
                
            }
        } catch(Exception e) {
            e.getMessage();
        } finally {
            dbConnection.closeConn();
        }
        
        if (result.size() > 0) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Objectives fetched successfully.")
                    .put("data", result);
        }else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to fetch Objectives.");
        }
        message.reply(response);
    }
}
