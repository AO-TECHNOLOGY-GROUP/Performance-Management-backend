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
        String query
                = "WITH SubtaskTargets AS (\n"
                + "    SELECT \n"
                + "        s.[ObjectiveId],\n"
                + "        s.[Name] AS SubtaskName,\n"
                + "        s.[Frequency],\n"
                + "        o.[Name] AS ObjectiveName,\n"
                + "        et.[Target] AS ExpectedTarget,\n"
                + "        et.[UserId],\n"
                + "        et.[BranchId],\n"
                + "        et.[SubtasksId],\n"
                + "        CASE \n"
                + "            WHEN s.Frequency = 'Daily' THEN et.Target \n"
                + "            WHEN s.Frequency = 'Weekly' THEN CASE WHEN et.Target / 5 < 1 THEN 1 ELSE et.Target / 5 END \n"
                + "            WHEN s.Frequency = 'Monthly' THEN CASE WHEN et.Target / 22 < 1 THEN 1 ELSE et.Target / 22 END \n"
                + "            WHEN s.Frequency = 'Quarterly' THEN CASE WHEN et.Target / 66 < 1 THEN 1 ELSE et.Target / 66 END \n"
                + "            ELSE 0 \n"
                + "        END AS DailyExpectedTarget,\n"
                + "        CASE \n"
                + "            WHEN s.Frequency = 'Daily' THEN et.Target * 5 \n"
                + "            WHEN s.Frequency = 'Weekly' THEN et.Target \n"
                + "            WHEN s.Frequency = 'Monthly' THEN CASE WHEN et.Target / 4.3 < 1 THEN 1 ELSE et.Target / 4.3 END \n"
                + "            WHEN s.Frequency = 'Quarterly' THEN CASE WHEN et.Target / 13 < 1 THEN 1 ELSE et.Target / 13 END \n"
                + "            ELSE 0 \n"
                + "        END AS WeeklyExpectedTarget,\n"
                + "        CASE \n"
                + "            WHEN s.Frequency = 'Daily' THEN et.Target * 22 \n"
                + "            WHEN s.Frequency = 'Weekly' THEN et.Target * 4.3 \n"
                + "            WHEN s.Frequency = 'Monthly' THEN et.Target \n"
                + "            WHEN s.Frequency = 'Quarterly' THEN CASE WHEN et.Target / 3 < 1 THEN 1 ELSE et.Target / 3 END \n"
                + "            ELSE 0 \n"
                + "        END AS MonthlyExpectedTarget,\n"
                + "        CASE \n"
                + "            WHEN s.Frequency = 'Daily' THEN et.Target * 66 \n"
                + "            WHEN s.Frequency = 'Weekly' THEN et.Target * 13 \n"
                + "            WHEN s.Frequency = 'Monthly' THEN et.Target * 3 \n"
                + "            WHEN s.Frequency = 'Quarterly' THEN et.Target \n"
                + "            ELSE 0 \n"
                + "        END AS QuarterlyExpectedTarget,\n"
                + "        COALESCE(SUM(CASE \n"
                + "            WHEN pt.TaskDate >= DATEADD(DAY, -1, GETDATE()) THEN pt.AchievedTarget \n"
                + "            ELSE 0 \n"
                + "        END), 0) AS DailyAchievedTarget,\n"
                + "        COALESCE(SUM(CASE \n"
                + "            WHEN pt.TaskDate >= DATEADD(DAY, -7, GETDATE()) \n"
                + "            AND DATENAME(WEEKDAY, pt.TaskDate) NOT IN ('Saturday', 'Sunday') \n"
                + "            THEN pt.AchievedTarget \n"
                + "            ELSE 0 \n"
                + "        END), 0) AS WeeklyAchievedTarget,\n"
                + "        COALESCE(SUM(CASE \n"
                + "            WHEN pt.TaskDate >= DATEADD(MONTH, -1, GETDATE()) \n"
                + "            AND DATENAME(WEEKDAY, pt.TaskDate) NOT IN ('Saturday', 'Sunday') \n"
                + "            THEN pt.AchievedTarget \n"
                + "            ELSE 0 \n"
                + "        END), 0) AS MonthlyAchievedTarget,\n"
                + "        COALESCE(SUM(CASE \n"
                + "            WHEN pt.TaskDate >= DATEADD(QUARTER, -1, GETDATE()) \n"
                + "            AND DATENAME(WEEKDAY, pt.TaskDate) NOT IN ('Saturday', 'Sunday') \n"
                + "            THEN pt.AchievedTarget \n"
                + "            ELSE 0 \n"
                + "        END), 0) AS QuarterlyAchievedTarget\n"
                + "    FROM [Performance_Management].[dbo].[Subtasks] s\n"
                + "    JOIN [Performance_Management].[dbo].[Objectives] o ON o.Id = s.ObjectiveId\n"
                + "    JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON s.Id = et.SubtasksId\n"
                + "    LEFT JOIN [Performance_Management].[dbo].[ProgressiveTracking] pt \n"
                + "        ON et.Id = pt.EmployeeTaskId \n"
                + "    GROUP BY \n"
                + "        s.[ObjectiveId],\n"
                + "        s.[Name],\n"
                + "        s.[Frequency],\n"
                + "        o.[Name],\n"
                + "        et.[Target],\n"
                + "        et.[UserId],\n"
                + "        et.[BranchId],\n"
                + "        et.[SubtasksId]\n"
                + ")\n"
                + "SELECT \n"
                + "    st.[ObjectiveId],    \n"
                + "    st.[ObjectiveName],\n"
                + "    st.[SubtasksId],\n"
                + "    st.[SubtaskName],\n"
                + "    st.[DailyExpectedTarget],\n"
                + "    st.[DailyAchievedTarget],\n"
                + "    st.[WeeklyExpectedTarget],\n"
                + "    st.[WeeklyAchievedTarget],\n"
                + "    st.[MonthlyExpectedTarget],\n"
                + "    st.[MonthlyAchievedTarget],\n"
                + "    st.[QuarterlyExpectedTarget],\n"
                + "    st.[QuarterlyAchievedTarget]\n"
                + "FROM SubtaskTargets st\n"
                + "WHERE st.[UserId] = ?\n"
                + "ORDER BY st.[ObjectiveId], st.[SubtasksId];";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            JsonArray objectivesArray = new JsonArray();
            while (rs.next()) {
                JsonObject subtaskData = new JsonObject()
                        .put("SubtasksId", rs.getString("SubtasksId"))
                        .put("SubtaskName", rs.getString("SubtaskName"))
                        .put("DailyExpectedTarget", String.valueOf(rs.getInt("DailyExpectedTarget")))
                        .put("DailyAchievedTarget", String.valueOf(rs.getInt("DailyAchievedTarget")))
                        .put("WeeklyExpectedTarget", String.valueOf(rs.getInt("WeeklyExpectedTarget")))
                        .put("WeeklyAchievedTarget", String.valueOf(rs.getInt("WeeklyAchievedTarget")))
                        .put("MonthlyExpectedTarget", String.valueOf(rs.getInt("MonthlyExpectedTarget")))
                        .put("MonthlyAchievedTarget", String.valueOf(rs.getInt("MonthlyAchievedTarget")))
                        .put("QuarterlyExpectedTarget", String.valueOf(rs.getInt("QuarterlyExpectedTarget")))
                        .put("QuarterlyAchievedTarget", String.valueOf(rs.getInt("QuarterlyAchievedTarget")));

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

                double totalAchievedQuarterly = 0;
                double totalExpectedQuarterly = 0;
                double totalAchievedDaily = 0;
                double totalExpectedDaily = 0;

                for (Object subtaskObj : subtasks) {
                    JsonObject subtask = (JsonObject) subtaskObj;

                    // Ensure we get double values instead of string
                    double quarterlyAchievedTarget = Double.parseDouble(subtask.getString("QuarterlyAchievedTarget"));
                    double quarterlyExpectedTarget = Double.parseDouble(subtask.getString("QuarterlyExpectedTarget"));
                    double dailyAchievedTarget = Double.parseDouble(subtask.getString("DailyAchievedTarget"));
                    double dailyExpectedTarget = Double.parseDouble(subtask.getString("DailyExpectedTarget"));

                    totalAchievedQuarterly += quarterlyAchievedTarget;
                    totalExpectedQuarterly += quarterlyExpectedTarget;
                    totalAchievedDaily += dailyAchievedTarget;
                    totalExpectedDaily += dailyExpectedTarget;

                }

                String quarterlyObjectivePercentage = (totalExpectedQuarterly != 0)
                        ? String.format("%.2f", (double) totalAchievedQuarterly / totalExpectedQuarterly * 100) : "0.00";

                // Calculate **Daily** Objective Percentage
                String dailyObjectivePercentage = (totalExpectedDaily != 0)
                        ? String.format("%.2f", (double) totalAchievedDaily / totalExpectedDaily * 100) : "0.00";
    


                // Update the objective JSON object
                objective.put("ObjectivePercentage", quarterlyObjectivePercentage);  // Keep the quarterly percentage
                objective.put("DailyObjectivePercentage", dailyObjectivePercentage); // New daily percentage

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

        String query = "WITH RO_Performance AS (\n"
                + "    SELECT \n"
                + "        u.uuid, \n"
                + "        et.SubtasksId, \n"
                + "        SUM(COALESCE(pt.AchievedTarget, 0)) AS AchievedTarget, \n"
                + "        SUM(COALESCE(et.Target, 0)) AS ExpectedTarget \n"
                + "    FROM [Performance_Management].[dbo].[usersBranches] ub \n"
                + "    JOIN [Performance_Management].[dbo].[users] u ON u.uuid = ub.UserId \n"
                + "    JOIN [Performance_Management].[dbo].[EmployeeTasks] et ON u.uuid = et.UserId \n"
                + "    LEFT JOIN [Performance_Management].[dbo].[ProgressiveTracking] pt ON pt.EmployeeTaskId = et.Id \n"
                + "    WHERE ub.BranchId = ? \n"
                + "      AND u.isRO = 1  \n"
                + "    GROUP BY u.uuid, et.SubtasksId \n"
                + "), \n"
                + "Branch_Performance AS (\n"
                + "    SELECT \n"
                + "        SUM(AchievedTarget) AS TotalAchieved, \n"
                + "        SUM(ExpectedTarget) AS TotalExpected \n"
                + "    FROM RO_Performance\n"
                + ") \n"
                + "SELECT \n"
                + "    bp.TotalAchieved, \n"
                + "    bp.TotalExpected, \n"
                + "    (CASE \n"
                + "        WHEN COALESCE(bp.TotalExpected, 0) > 0 \n"
                + "        THEN (CAST(bp.TotalAchieved AS DECIMAL(10,2)) / CAST(bp.TotalExpected AS DECIMAL(10,2))) * 100 \n"
                + "        ELSE 0 \n"
                + "    END) AS BranchPerformancePercentage \n"
                + "FROM Branch_Performance bp;";

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
