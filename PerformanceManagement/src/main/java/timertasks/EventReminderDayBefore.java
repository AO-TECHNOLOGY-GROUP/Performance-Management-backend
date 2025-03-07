/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package timertasks;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.TimerTask;
import log.Logging;

/**
 * Timer task to send SMS reminders a day before scheduled events.
 */
public class EventReminderDayBefore extends TimerTask {
    private final Vertx vertx;

    public EventReminderDayBefore(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void run() {
        sendEventReminders();
    }

    private void sendEventReminders() {
        EventBus eventBus = vertx.eventBus();
        Logging logger = new Logging();

        System.out.println("::::::::::::::::::::::::: SENDING EVENT REMINDERS :::::::::::::::::::::::::");

        String eventQuery = "SELECT e.id, e.event, e.date, e.userId, u.phone_number, u.first_name " +
                            "FROM [Performance_Management].[dbo].[Calender-of-Events] e " +
                            "INNER JOIN users u ON e.userId = u.uuid " +
                            "WHERE DATEDIFF(DAY, GETDATE(), e.date) = 1 " +
                            "AND DATEPART(HOUR, e.date) = DATEPART(HOUR, GETDATE()) " +
                            "AND DATEPART(MINUTE, e.date) = DATEPART(MINUTE, GETDATE()) "; 

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement stmt = connection.prepareStatement(eventQuery);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String eventId = rs.getString("id");
                String eventName = rs.getString("event");
                String eventDate = rs.getString("date");
                String firstName = rs.getString("first_name");
                String phoneNumber = rs.getString("phone_number");

                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                    String msg = "Dear " + firstName + ", this is a reminder for your upcoming event: " + eventName +
                                 " scheduled on " + eventDate + ". Please make necessary preparations.";

                    JsonObject smsObject = new JsonObject();
                    smsObject.put("phonenumber", phoneNumber);
                    smsObject.put("msg", msg);
                    smsObject.put("type", "EVENT_REMINDER");
                    smsObject.put("eventId", eventId);

                    eventBus.send("COMMUNICATION_ADAPTOR", smsObject);
                    System.out.println("Reminder sent to " + firstName + " (" + phoneNumber + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }
    }
}
