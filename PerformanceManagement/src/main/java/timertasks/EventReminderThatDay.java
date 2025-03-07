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
import java.sql.ResultSet;
import java.util.TimerTask;
import log.Logging;

/**
 * Timer task to send event reminders on the same day the event is happening.
 * This runs once a day and checks for events happening today.
 *
 * @author Best Point
 */
public class EventReminderThatDay extends TimerTask {
    private final Vertx vertx;

    public EventReminderThatDay(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void run() {
        sendEventReminders();
    }

    private void sendEventReminders() {
        EventBus eventBus = vertx.eventBus();
        Logging logger = new Logging();

        System.out.println("::::::::::::::::::::::::::: SENDING EVENT REMINDERS FOR TODAY :::::::::::::::::::::::::");

        // Query to get today's events and associated user details
        String sql = "SELECT e.id, e.event, e.date, e.userId, u.phone_number, u.first_name \n" +
                    "FROM [Performance_Management].[dbo].[Calender-of-Events] e \n" +
                    "INNER JOIN users u ON e.userId = u.uuid \n" +
                     "WHERE DATEADD(HOUR, -1, e.date) = GETDATE()"; // Events happening today

        DBConnection conn = new DBConnection();

        try {
            ResultSet rs = conn.query_all(sql);

            while (rs.next()) {
                String eventId = rs.getString("id");
                String eventName = rs.getString("event");
                String eventDate = rs.getString("date");
                String firstName = rs.getString("first_name");
                String phoneNumber = rs.getString("phone_number");

                // Capitalize first letter of first name
                firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1).toLowerCase();

                String msg = "Dear " + firstName + ", this is a reminder that your event '" + eventName +
                             "' will start in one hour. Please be prepared.";

                JsonObject smsObject = new JsonObject();
                smsObject.put("phonenumber", phoneNumber)
                         .put("msg", msg);

                eventBus.send("COMMUNICATION_ADAPTOR", smsObject);
                System.out.println("Reminder sent to: " + firstName + " (" + phoneNumber + ")");
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            conn.closeConn();
        }
    }
}
