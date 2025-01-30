package com.aogroup.za.branch;

import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.user.UserUtil;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import log.Logging;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BranchUtil {
    Logging logger;
    public BranchUtil() {
        logger = new Logging();
    }

    public JsonObject fetchBranch(String searchKey, String searchWord) {
        String sqlQuery ="SELECT Id,Code,Name,Location,Latitude,Longitude,IsEnabled,ClusteredId,CreatedAt,UpdatedAt" +
                " FROM Branches WHERE " + searchKey + " = '" + searchWord + "' ORDER BY Name ASC";
        

        DBConnection conn = new DBConnection();
        JsonObject data = new JsonObject();

        try {
            ResultSet rs = conn.query_all(sqlQuery);
            if (rs.next()) {
                data.put("successIndicator", true);

                data
                        .put("id", rs.getString("Id"))
                        .put("code", rs.getString("Code"))
                        .put("name", rs.getString("Name"))
                        .put("location", rs.getString("Location"))
                        .put("latitude", rs.getString("Latitude"))
                        .put("longitude", rs.getString("Longitude"))
                        .put("isEnabled", rs.getString("IsEnabled"))
                        .put("createdAt", rs.getString("CreatedAt"))
                        .put("updatedAt", rs.getString("UpdatedAt"))
                        .put("clusteredId", rs.getString("ClusteredId"));

            } else {
                data.put("successIndicator", false);
            }

        } catch (Exception e) {
            data.put("successIndicator", false);
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            conn.closeConn();
        }

        return data;
    }

    public JsonObject fetchBranches() {
        String sqlQuery = "SELECT Id,Code,Name,Location,Latitude,Longitude,IsEnabled,ClusteredId,CreatedAt,UpdatedAt" +
                " FROM Branches ORDER BY Name ASC";
        DBConnection conn = new DBConnection();
        JsonObject data = new JsonObject();
        JsonArray array = new JsonArray();

        try {
            ResultSet rs = conn.query_all(sqlQuery);
            if (rs.next()) {
                data.put("successIndicator", true);

                do {
                    JsonObject result = new JsonObject();
                    result
                            .put("id", rs.getString("Id"))
                            .put("code", rs.getString("Code"))
                            .put("name", rs.getString("Name"))
                            .put("location", rs.getString("Location"))
                            .put("latitude", rs.getString("Latitude"))
                            .put("longitude", rs.getString("Longitude"))
                            .put("isEnabled", rs.getString("IsEnabled"))
                            .put("createdAt", rs.getString("CreatedAt"))
                            .put("updatedAt", rs.getString("UpdatedAt"))
                            .put("clusteredId", rs.getString("ClusteredId"));

                    array.add(result);
                } while (rs.next());

            } else {
                data.put("successIndicator", false);
            }

        } catch (Exception e) {
            data.put("successIndicator", false);
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            conn.closeConn();
        }

        data.put("data", array);
        return data;
    }

    public JsonObject createBranch(JsonObject request) {
        String name = request.getString("name");
        String location = request.getString("location");
        String latitude = request.getString("latitude");
        String longitude = request.getString("longitude");
        String code = request.getString("code");
        String user = request.getString("user");
        
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "create_branch");
        if (!hasPermission) {
            return request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
        }
        
        String createBranch = "INSERT INTO Branches(Id,Code,Name,Location,Latitude,Longitude,IsEnabled)"
                + " VALUES(NEWID(),?,?,?,?,?,1)";

//        String insertGlobalSetting = "INSERT INTO GlobalSetting([Name],[Value]) VALUES(?,?)";
//        PreparedStatement prInsertGlobalSetting = connection.prepareStatement(insertGlobalSetting)

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(createBranch, PreparedStatement.RETURN_GENERATED_KEYS);) {
            preparedStatement.setString(1, code);
            preparedStatement.setString(2, name);
            preparedStatement.setString(3, location);
            preparedStatement.setString(4, latitude);
            preparedStatement.setString(5, longitude);
            int i = preparedStatement.executeUpdate();
            if (i == 1) {
                request
                        .put("responseCode", "000")
                        .put("responseDescription", "Success! Created");
            } else {
                request
                        .put("responseCode", "999")
                        .put("responseDescription", "Error! Creation Failed");
            }

        } catch (SQLException throwables) {
            request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Execution Failed");

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

    public JsonObject updateBranch(JsonObject request) {
        String branchId = request.getString("branch_id");
        String name = request.getString("name");
        String location = request.getString("location");
        String latitude = request.getString("latitude");
        String longitude = request.getString("longitude");
        String code = request.getString("code");
        String user = request.getString("user");

        request.clear();

        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "update_branch");
        if (!hasPermission) {
            return request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
        }

        String sql = " UPDATE Branches SET [Name] = ?, [Location] = ?, [Latitude] = ?, [Longitude] = ?, [Code] = ?\n" +
                " WHERE Id = ?";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, location);
            preparedStatement.setString(3, latitude);
            preparedStatement.setString(4, longitude);
            preparedStatement.setString(5, code);
            preparedStatement.setString(6, branchId);
            int i = preparedStatement.executeUpdate();

            if (i == 1) {
                request
                        .put("responseCode", "000")
                        .put("responseDescription", "Success! Updated");
            } else {
                request
                        .put("responseCode", "999")
                        .put("responseDescription", "Error! Failed to insert data");
            }

        } catch (SQLException throwables) {
            request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Could not insert data");

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
