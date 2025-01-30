package com.aogroup.za.makerchecker;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import log.Logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MakerCheckerUtil {
    Logging logger;
    public MakerCheckerUtil() {
        logger = new Logging();
    }

    public boolean createMakerChecker (JsonObject request) {
        boolean isCreated = false;

        String serviceCode = request.getString("serviceCode");
        String module = request.getString("module");
        String payload = request.getString("payload");
        String status = "3";
        String user = request.getString("user");
        String branch = request.getString("branch");

        String sql = "INSERT INTO [dbo].[MakerChecker]([ServiceCode],[Module],[BranchId],[Payload],[Status],[CreatedBy],[CreatedDate])\n" +
                " VALUES(?,?,?,?,?,?,GETDATE())";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, serviceCode);
            preparedStatement.setString(2, module);
            preparedStatement.setString(3, branch);
            preparedStatement.setString(4, payload);
            preparedStatement.setString(5, status);
            preparedStatement.setString(6, user);
            int i = preparedStatement.executeUpdate();

            if (i == 1) { isCreated = true; }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
        }

        return isCreated;
    }

    public JsonArray fetchAllMakerChecker(String column,String word) {
        JsonArray array = new JsonArray();

        String sql = "SELECT TOP (1000) mk.[Id],[ServiceCode],[Module],[BranchId],[Payload],mk.[Status],mk.[CreatedBy],mk.[CreatedDate],[ApprovedBy],[ApprovedDate]\n" +
                ",u.first_name + ' ' + u.last_name AS CreatedByName,uu.first_name + ' ' + uu.last_name AS ApprovedByName,b.[Name] AS BranchName\n" +
                "  FROM [digital_lending].[dbo].[MakerChecker] mk LEFT JOIN users u ON u.id = mk.CreatedBy LEFT JOIN users uu ON uu.id = mk.ApprovedBy \n" +
                "  LEFT JOIN Branches b ON b.Id = mk.BranchId WHERE "+column+" = '"+word+"'  ORDER BY b.[Name],mk.CreatedBy DESC";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                JsonObject obj = new JsonObject();
                obj
                        .put("approval_id",rs.getString("Id"))
                        .put("serviceCode",rs.getString("ServiceCode"))
                        .put("module",rs.getString("Module"))
                        .put("branchId",rs.getString("BranchId"))
                        .put("branchName",rs.getString("BranchName"))
                        .put("status",rs.getString("Status"))
                        .put("payload",new JsonObject(rs.getString("Payload")))
                        .put("createdByName",rs.getString("CreatedByName"))
                        .put("createdBy",rs.getString("CreatedBy"))
                        .put("createdDate",rs.getString("CreatedDate"))
                        .put("approvedByName",rs.getString("ApprovedByName"))
                        .put("approvedBy",rs.getString("ApprovedBy"))
                        .put("approvedDate",rs.getString("ApprovedDate"));

                array.add(obj);
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
        }
        return array;
    }

    public JsonObject validateMakerChecker(JsonObject request) {
//        //System.out.println("MKCKER UTIL "+request);
        String approvalId = request.getString("approval_id");
        String user = request.getString("user");
        String actionFlag = request.getString("action_flag");

        request.clear();
        if (!actionFlag.equals("0") && !actionFlag.equals("1")) {
            return request
                    .put("responseCode","999")
                    .put("responseDescription","Error! Invalid Status Flag");
        }

        String sql = "UPDATE MakerChecker SET ApprovedDate = GETDATE(),ApprovedBy = ?,[Status] = ? WHERE Id = ?";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1,user);
            preparedStatement.setString(2,actionFlag);
            preparedStatement.setString(3,approvalId);
            int i = preparedStatement.executeUpdate();
            if (i == 1) {
                request
                        .put("responseCode","000")
                        .put("responseDescription","Success! Approval Successful");
            } else {
                request
                        .put("responseCode","999")
                        .put("responseDescription","Error! Approval Failed");
            }

        } catch (SQLException throwables) {
            request
                    .put("responseCode","999")
                    .put("responseDescription","Error! Approval Failed Execution");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
        }
        return request;
    }
}
