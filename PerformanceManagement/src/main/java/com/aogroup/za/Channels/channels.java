/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.Channels;

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
public class channels extends AbstractVerticle{
    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;
    
    
     @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        eventBus.consumer("CREATECHANNEL", this::createChannel);
        eventBus.consumer("FETCHALLCHANNELS", this::fetchChannel);
    }
    
    private void createChannel(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();

        String name = requestBody.getString("name");
        String code = requestBody.getString("code");

        String insertchannel = "INSERT INTO [dbo].[Channels] ([name], [code], [created_at], [updated_at]) "
                              + "VALUES (?, ?, GETDATE(), GETDATE())";

        
         try (PreparedStatement prInsertChannel = connection.prepareStatement(insertchannel, PreparedStatement.RETURN_GENERATED_KEYS)) {
            connection.setAutoCommit(false);

            prInsertChannel.setString(1, name);
            prInsertChannel.setString(2, code);
          
         int rowsAffected = prInsertChannel.executeUpdate();

        if (rowsAffected > 0) {
            connection.commit();
            response.put("responseCode", "000");
            response.put("responseDescription", "Success! Channel created successfully");

        } else {
            connection.rollback();
            response.put("responseCode", "999");
            response.put("responseDescription", "Error! Channel creation failed");
        }

    } catch (Exception e) {
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

    private void fetchChannel (Message<JsonObject> message){
        DBConnection dbConnection = new DBConnection();
        JsonObject response = new JsonObject();

        JsonArray result = new JsonArray();

        String query = "SELECT * FROM [Performance_Management].[dbo].[Channels]";
        try {
             ResultSet allChannels = dbConnection.query_all(query);
            
            while (allChannels.next()) {
                JsonObject jo = new JsonObject();
                jo
                           .put("Id", allChannels.getString("Id"))
                           .put("name", allChannels.getString("name"))
                           .put("code", allChannels.getString("code"))
                           .put("created_at", allChannels.getString("created_at"))
                           .put("updated_at", allChannels.getString("updated_at"));
                                
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
                    .put("responseDescription", "All channels fetched successfully.")
                    .put("data", result);
        }else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to fetch channels.");
        }
        message.reply(response);

       
    }
        
}
