
package com.aogroup.za.DashboardResults;

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
import log.Logging;

public class dashboardResults extends AbstractVerticle {

    private Logging logger;
    EventBus eventBus;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        // Registering the dashboard API endpoint
        eventBus.consumer("GET_DASHBOARD_PERFORMANCE", this::getPerformanceTargets);
        eventBus.consumer("GET_BRANCH_PERFORMANCE", this::getBranchPerformance);
    }

    private void getPerformanceTargets(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String userId = requestBody.getString("UserId");
        
        // SQL query with the correct use of UserId and handling of the results
        String query = 
            "WITH SubtaskTargets AS (" +
            "    SELECT " +
            "        s.[ObjectiveId], " +
            "        s.[Name] AS SubtaskName, " +
            "        s.[Frequency], " +
            "        o.[Name] AS ObjectiveName, " +
            "        et.[Target] AS ExpectedTarget, " +
            "        et.[UserId], " +
            "        et.[BranchId], " +
            "        et.[SubtasksId], " +
            "        CASE " +
            "            WHEN s.Frequency = 'Daily' THEN et.Target * 7 " +
            "            WHEN s.Frequency = 'Weekly' THEN et.Target " +
            "            WHEN s.Frequency = 'Monthly' THEN et.Target / 4 " +
            "            WHEN s.Frequency = 'Quarterly' THEN et.Target / 12 " +
            "            ELSE 0 " +
            "        END AS WeeklyExpectedTarget, " +
            "        CASE " +
            "            WHEN s.Frequency = 'Daily' THEN et.Target * 30 " +
            "            WHEN s.Frequency = 'Weekly' THEN et.Target * 4 " +
            "            WHEN s.Frequency = 'Monthly' THEN et.Target " +
            "            WHEN s.Frequency = 'Quarterly' THEN et.Target / 3 " +
            "            ELSE 0 " +
            "        END AS MonthlyExpectedTarget, " +
            "        CASE " +
            "            WHEN s.Frequency = 'Daily' THEN et.Target * 90 " +
            "            WHEN s.Frequency = 'Weekly' THEN et.Target * 12 " +
            "            WHEN s.Frequency = 'Monthly' THEN et.Target * 3 " +
            "            WHEN s.Frequency = 'Quarterly' THEN et.Target " +
            "            ELSE 0 " +
            "        END AS QuarterlyExpectedTarget, " +
            "        COALESCE(SUM(CASE " +
            "            WHEN pt.TaskDate >= DATEADD(DAY, -7, GETDATE()) THEN pt.AchievedTarget " +
            "            ELSE 0 " +
            "        END), 0) AS WeeklyAchievedTarget, " +
            "        COALESCE(SUM(CASE " +
            "            WHEN pt.TaskDate >= DATEADD(MONTH, -1, GETDATE()) THEN pt.AchievedTarget " +
            "            ELSE 0 " +
            "        END), 0) AS MonthlyAchievedTarget, " +
            "        COALESCE(SUM(CASE " +
            "            WHEN pt.TaskDate >= DATEADD(QUARTER, -1, GETDATE()) THEN pt.AchievedTarget " +
            "            ELSE 0 " +
            "        END), 0) AS QuarterlyAchievedTarget " +
            "    FROM [Performance_Management].[dbo].[Subtasks] s " +
            "    JOIN [Performance_Management].[dbo].[Objectives] o ON o.Id = s.ObjectiveId " +
            "    JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON s.Id = et.SubtasksId " +
            "    LEFT JOIN [Performance_Management].[dbo].[ProgressiveTracking] pt ON et.Id = pt.EmployeeTaskId " +
            "    GROUP BY " +
            "        s.[ObjectiveId], " +
            "        s.[Name], " +
            "        s.[Frequency], " +
            "        o.[Name], " +
            "        et.[Target], " +
            "        et.[UserId], " +
            "        et.[BranchId], " +
            "        et.[SubtasksId] " +
            ") " +
            "SELECT " +
            "    st.[ObjectiveId], " +    
            "    st.[ObjectiveName]," +
            "    st.[SubtasksId], " +
            "    st.[SubtaskName], " +
            "    st.[WeeklyExpectedTarget], " +
            "    st.[WeeklyAchievedTarget], " +
            "    st.[MonthlyExpectedTarget], " +
            "    st.[MonthlyAchievedTarget], " +
            "    st.[QuarterlyExpectedTarget], " +
            "    st.[QuarterlyAchievedTarget] " +
            "FROM SubtaskTargets st " +
            "WHERE St.[UserId] = ? " +
            "ORDER BY st.[ObjectiveId], st.[SubtasksId];";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            JsonArray objectivesArray = new JsonArray();
            while (rs.next()) {
                JsonObject subtaskData = new JsonObject()
                        .put("SubtasksId", rs.getString("SubtasksId"))
                        .put("SubtaskName", rs.getString("SubtaskName"))
                        .put("WeeklyExpectedTarget", rs.getString("WeeklyExpectedTarget"))
                        .put("WeeklyAchievedTarget", rs.getString("WeeklyAchievedTarget"))
                        .put("MonthlyExpectedTarget", rs.getString("MonthlyExpectedTarget"))
                        .put("MonthlyAchievedTarget", rs.getString("MonthlyAchievedTarget"))
                        .put("QuarterlyExpectedTarget", rs.getString("QuarterlyExpectedTarget"))
                        .put("QuarterlyAchievedTarget", rs.getString("QuarterlyAchievedTarget"));

                String objectiveId = rs.getString("ObjectiveId");
                String objectiveName = rs.getString("ObjectiveName");
                JsonObject objective = objectivesArray.stream()
                        .filter(obj -> ((JsonObject) obj).getString("ObjectiveId").equals(objectiveId))
                        .map(obj -> (JsonObject) obj)
                        .findFirst()
                        .orElseGet(() -> {
                            JsonObject newObjective = new JsonObject()
                                    .put("ObjectiveId", objectiveId)
                                    .put("ObjectiveName", objectiveName)
                                    .put("ObjectivePercentage", "0.00") // Default to "0.00"
                                    .put("Subtasks", new JsonArray());
                            objectivesArray.add(newObjective);
                            return newObjective;
                        });

                ((JsonArray) objective.getValue("Subtasks")).add(subtaskData);
            }

            // Calculate ObjectivePercentage
            for (Object obj : objectivesArray) {
                JsonObject objective = (JsonObject) obj;
                JsonArray subtasks = objective.getJsonArray("Subtasks");

                double totalAchieved = 0;
                double totalExpected = 0;

                for (Object subtaskObj : subtasks) {
                    JsonObject subtask = (JsonObject) subtaskObj;

                    // Ensure we get double values instead of string
                    double quarterlyAchievedTarget = Double.parseDouble(subtask.getString("QuarterlyAchievedTarget"));
                    double quarterlyExpectedTarget = Double.parseDouble(subtask.getString("QuarterlyExpectedTarget"));

                    totalAchieved += quarterlyAchievedTarget;
                    totalExpected += quarterlyExpectedTarget;
                }

                // Calculate ObjectivePercentage
                String objectivePercentage = (totalExpected != 0) ? 
                    String.format("%.2f", (totalAchieved / totalExpected) * 100) : "0.00";

                objective.put("ObjectivePercentage", objectivePercentage); // Now directly under objective
            }

            // Prepare the response
            response.put("responseCode", "000")
                    .put("responseDescription", "Performance targets fetched successfully")
                    .put("objectives", objectivesArray);

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
    }

    private void getBranchPerformance(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonObject requestBody = message.body();
        String branchId = requestBody.getString("BranchId");

        String query = "WITH RO_Performance AS (\n" +
            "    SELECT \n" +
            "        u.uuid, \n" +
            "        et.SubtasksId, \n" +
            "        SUM(COALESCE(pt.AchievedTarget, 0)) AS AchievedTarget, \n" +
            "        SUM(COALESCE(et.Target, 0)) AS ExpectedTarget \n" +
            "    FROM [Performance_Management].[dbo].[usersBranches] ub \n" +
            "    JOIN [Performance_Management].[dbo].[users] u ON u.uuid = ub.UserId \n" +
            "    JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON u.uuid = et.UserId \n" +
            "    LEFT JOIN [Performance_Management].[dbo].[ProgressiveTracking] pt ON pt.EmployeeTaskId = et.Id \n" +
            "    WHERE ub.BranchId = ? \n" +
            "      AND u.isRO = 1  \n" +
            "    GROUP BY u.uuid, et.SubtasksId \n" +
            "), \n" +
            "Branch_Performance AS (\n" +
            "    SELECT \n" +
            "        SUM(AchievedTarget) AS TotalAchieved, \n" +
            "        SUM(ExpectedTarget) AS TotalExpected \n" +
            "    FROM RO_Performance\n" +
            ") \n" +
            "SELECT \n" +
            "    bp.TotalAchieved, \n" +
            "    bp.TotalExpected, \n" +
            "    (CASE \n" +
            "        WHEN COALESCE(bp.TotalExpected, 0) > 0 \n" +
            "        THEN (CAST(bp.TotalAchieved AS DECIMAL(10,2)) / CAST(bp.TotalExpected AS DECIMAL(10,2))) * 100 \n" +
            "        ELSE 0 \n" +
            "    END) AS BranchPerformancePercentage \n" +
            "FROM Branch_Performance bp;";


        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, branchId);  // Set BranchId

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                double totalAchieved = rs.getDouble("TotalAchieved");
                double totalExpected = rs.getDouble("TotalExpected");
                double branchPerformance = rs.getDouble("BranchPerformancePercentage");

                response.put("responseCode", "000")
                        .put("responseDescription", "Branch performance fetched successfully")
                        .put("averagePercentage", String.format("%.2f", branchPerformance));  // Format the percentage

            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "No data found for the provided BranchId");
            }

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            dbConnection.closeConn();
        }

        message.reply(response);
    }

}
