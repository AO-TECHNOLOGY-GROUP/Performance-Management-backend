/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.adaptors;

import com.co.ke.main.EntryPoint;
import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.util.Common;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import log.Logging;

/**
 *
 * @author nathan
 */
public class SMSAdaptor extends AbstractVerticle {

    private Logging logger;
    static int TIMEOUT_TIME = 25000;
    EventBus eventBus;

    @Override
    public void start(Future<Void> done) throws Exception {
        //System.out.println("deploymentId SMSAdaptor =" + vertx.getOrCreateContext().deploymentID());
        eventBus = vertx.eventBus();
        logger = new Logging();
        eventBus.consumer("COMMUNICATION_ADAPTOR", this::sendSMS);
    }

    private void sendSMS(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject notificationMap = new JsonObject();
        
        String checkQuery = "SELECT COUNT(1) FROM TextAlerts " +
                        "WHERE TextMessage_Recipient = ? " +
                        "AND TextMessage_Reference = ? " +
                        "AND CONVERT(DATE, CreatedDate) = CONVERT(DATE, GETDATE())";
        
        String smsQuery = "INSERT INTO TextAlerts([Id],[TextMessage_Recipient],[TextMessage_Body],[TextMessage_DLRStatus],"
                + "[TextMessage_Reference],[TextMessage_Origin],[TextMessage_Priority],[TextMessage_SendRetry],[TextMessage_SecurityCritical],[CreatedBy],[CreatedDate]) "
                + "VALUES (NEWID(),?,?,?,?,?,1,0,0,?,GETDATE())";

        //System.out.println("***  SMS proccessing **"+data);
        try {
            String tim = Common.formatDateToday("yyyyMMddHHmmss");
            
            if (data.containsKey("type") && (data.getString("type").equalsIgnoreCase("DISBURSE") || 
                    data.getString("type").equalsIgnoreCase("DEPOSIT") || 
                    data.getString("type").equalsIgnoreCase("COMPLETE") || 
                    data.getString("type").equalsIgnoreCase("REMINDERDUE") ||
                    data.getString("type").equalsIgnoreCase("REMINDERTHREE") ||
                    data.getString("type").equalsIgnoreCase("REMINDERSEVEN"))) {
                
                DBConnection dbConnection = new DBConnection();
                Connection connection = dbConnection.getConnection();
                
                boolean checkForExisting = data.getString("type").equalsIgnoreCase("REMINDERDUE") ||
                               data.getString("type").equalsIgnoreCase("REMINDERONE") ||
                               data.getString("type").equalsIgnoreCase("REMINDERTHREE");
                
                boolean recordExists = false;
                if (data.getString("type").equalsIgnoreCase("REMINDERDUE") ||
                    data.getString("type").equalsIgnoreCase("REMINDERONE") ||
                    data.getString("type").equalsIgnoreCase("REMINDERTHREE")) {
                    
                    try (PreparedStatement checkStmt = connection.prepareStatement(checkQuery)) {
                        checkStmt.setString(1, data.getString("phonenumber"));
                        checkStmt.setString(2, data.getString("type"));

                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next()) {
                                recordExists = rs.getInt(1) > 0;
                            }
                        }
                    }
                }
                
                if (!checkForExisting || !recordExists) {
                    try (PreparedStatement prQuery = connection.prepareStatement(smsQuery)) {
                        prQuery.setString(1, data.getString("phonenumber"));
                        prQuery.setString(2, data.getString("msg"));
                        prQuery.setInt(3, 32); //submitted
                        prQuery.setString(4, data.getString("type"));
                        prQuery.setInt(5, 3); //3:infobip 
                        prQuery.setString(6, "SYSTEM");
                        prQuery.executeUpdate();
                    } catch (Exception exp) {
                        logger.applicationLog(logger.logPreString() + "Error at database insert sms - " + exp.getMessage() + "\n\n", "", 5);
                    } 
                    
                    notificationMap.put("processingCode", "122000");
                    notificationMap.put("phonenumber", data.getString("phonenumber"));
                    notificationMap.put("msg", data.getString("msg"));
                    notificationMap.put("timestamp", tim);

                    // clear Data
                    data.clear();

                    data.put("url", EntryPoint.SMS_ENDPOINT);
                    data.put("json", notificationMap);

                    // consume SMS API
                    eventBus.send("INTEGRATION_TO_3RD_PARTY", data);

                    logger.applicationLog(logger.logPreString() + "To Notification Queue: " + notificationMap + "\n\n", "", 16);
                } else {
                    logger.applicationLog(logger.logPreString() + "SMS already exists for this recipient and type today.\n\n", "", 5);
                }
                
                try {
                    connection.close();
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                dbConnection.closeConn();
            } else if (!data.containsKey("type")) {
                notificationMap.put("processingCode", "122000");
                notificationMap.put("phonenumber", data.getString("phonenumber"));
                notificationMap.put("msg", data.getString("msg"));
                notificationMap.put("timestamp", tim);

                // clear Data
                data.clear();

                data.put("url", EntryPoint.SMS_ENDPOINT);
                data.put("json", notificationMap);

                // consume SMS API
                eventBus.send("INTEGRATION_TO_3RD_PARTY", data);

                logger.applicationLog(logger.logPreString() + "To Notification Queue: " + notificationMap + "\n\n", "", 16);
            }
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error at sendToNotificationQueue - " + e.getMessage() + "\n\n", "", 5);
        }
    }
}
