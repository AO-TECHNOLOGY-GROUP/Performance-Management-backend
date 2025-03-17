package com.aogroup.za.user;

import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.util.Common;
import com.aogroup.za.util.LoginTokens;
import com.aogroup.za.util.ProjectConstants;
import com.aogroup.za.util.Utilities;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import log.Logging;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

public class UserUtil {
    public static final String ERROR_CODE = "999";
    public static final String SUCCESS_CODE = "000";
    Logging logger;

    public UserUtil() {
        logger = new Logging();
    }

    // Method to generate a random alphanumeric password of a specific length
    public String generateRandomPassword(int len)
    {
        // ASCII range – alphanumeric (0-9, a-z, A-Z)
        final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder();

        // each iteration of the loop randomly chooses a character from the given
        // ASCII range and appends it to the `StringBuilder` instance

        for (int i = 0; i < len; i++)
        {
            int randomIndex = random.nextInt(chars.length());
            sb.append(chars.charAt(randomIndex));
        }

        return sb.toString();
    }

    public JsonObject registerUser(JsonObject request) {
        Common common = new Common();
        String firstName, lastName, email, phoneNumber, userType, user, channel;
        JsonArray branches;
        String isRO;
        try {
            firstName = request.getString("firstName");
            lastName = request.getString("lastName");
            email = request.getString("email");
            branches = request.getJsonArray("branches");
            phoneNumber = request.getString("phoneNumber");
            userType = request.getString("userType");
            isRO = request.getString("isRO");
            user = request.getString("user");
            channel = request.getString("channel");
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Error parsing data");
            return request;
        }

        request.clear();
        String password = "";
        
        if (channel.trim().equalsIgnoreCase("APP")) {
            password = common.generateNewPinRandomFour();
        } else {
            password = generateRandomPassword(8);
        }

        phoneNumber = new Utilities().formatPhone(phoneNumber);
        String hashPassword = new Common().generatedHashedPin(password, "A.B.", "12345678");

        JsonObject roleDetails = fetchRoleDetails("id", userType);
        if (!roleDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid Role selected");
            return request;
        }

        String sql = "INSERT INTO users(branch,first_name,last_name,phone_number,email," +
                "password,uuid,type,creator_id,status,created_at,isRO,channel) VALUES(?,?,?,?,? ,?,NEWID(),?,?,'1',GETDATE(),?,?)";

        String insertLoginValidation = "INSERT INTO login_validation([uuid],[password],[login_trials],[change_password],[status],[otp_verified],[otp_hash],[otp_expiry]) \n" +
                "   VALUES(?,?,?,?,?,?,?,DATEADD(Minute,?,GETDATE()))";

        String insertUsersBranches = "INSERT INTO usersBranches(UserId,BranchId) VALUES(?,?)";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             PreparedStatement prLoginVal = connection.prepareStatement(insertLoginValidation);
             PreparedStatement prInsertUsersBranches = connection.prepareStatement(insertUsersBranches)) {

            connection.setAutoCommit(false);

            preparedStatement.setString(1, null);
            preparedStatement.setString(2, firstName);
            preparedStatement.setString(3, lastName);
            preparedStatement.setString(4, phoneNumber);
            preparedStatement.setString(5, email);
            preparedStatement.setString(6, hashPassword);
            preparedStatement.setString(7, userType);
            preparedStatement.setString(8, user);
            preparedStatement.setString(9, isRO);
            preparedStatement.setString(10, channel);
            if (preparedStatement.executeUpdate() == 0) {
                throw new SQLException("Failed to add user to USERS TABLE");
            }

            String userUUID = "";

            String fetchUserUUID = "SELECT [uuid]  FROM [users] WHERE [email] = ? ";
            PreparedStatement prFetchUUID = connection.prepareStatement(fetchUserUUID);
            prFetchUUID.setString(1, email);
            boolean y = prFetchUUID.execute();
            ResultSet resultSet = prFetchUUID.getResultSet();
            if (resultSet.next()) {
                userUUID = resultSet.getString("uuid");
            } else {
                throw new SQLException("Failed to fetch user uuid from USERS TABLE");
            }
            resultSet.close();

            String otp = common.generateRandom(6);
            String otpHash = common.generatedHashedPin(otp, "1", "1");

            prLoginVal.setString(1, userUUID);
            prLoginVal.setString(2, hashPassword);
            prLoginVal.setInt(3, 0); // Trials
            prLoginVal.setInt(4, 1); // Change Password
            prLoginVal.setInt(5, 1); // Status
            prLoginVal.setInt(6, 0); // OTP Verified
            prLoginVal.setString(7, otpHash);
            prLoginVal.setInt(8, 10); // 10 mns
            prLoginVal.executeUpdate();

            // USer Branches
            for (int x = 0; x < branches.size(); x++ ) {
                prInsertUsersBranches.setString(1, userUUID);
                prInsertUsersBranches.setString(2, branches.getString(x));
                prInsertUsersBranches.addBatch();
            }
            int[] insertedBranches = prInsertUsersBranches.executeBatch();
            //System.out.println("\n USer Branches "+ insertedBranches);
                
            String emailBody = "Dear " + firstName.toUpperCase() + " " + lastName.toUpperCase()
                    + ".  You have been registered to use "+ ProjectConstants.COMPANY_NAME +" as a " + roleDetails.getString("description") +
                    " Your password is " + password ;
//                    + " Visit " + ProjectConstants.PORTAL_URL +" to login to the system"
//                    + "  Thank you";

            request
                    .put("emailRecipient", email)
                    .put("emailSubject", "REGISTRATION")
                    .put("emailBody", emailBody);
            
            request
                    .put("phonenumber", phoneNumber)
                    .put("msg", emailBody);

            request
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! User created");

            logger.applicationLog(logger.logPreString() + "PASSWORD - " + email + " - " + password, "", 99);


            request
                    .put("emailRecipient", email)
                    .put("emailSubject", "REGISTRATION")
                    .put("emailBody", emailBody);
            
            request
                    .put("phonenumber", phoneNumber)
                    .put("msg", emailBody);

            request
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! User created");

            logger.applicationLog(logger.logPreString() + "PASSWORD - " + email + " - " + password, "", 99);


            connection.commit();
            
        } catch (SQLException throwables) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed to register User");

            try {
                connection.rollback();
            } catch (SQLException e) {
                //e.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            }

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }
        return request;
    }

    public JsonObject updateUser(JsonObject request) {
        String firstName, lastName, email, phoneNumber, userType, userUUID,user;
        JsonArray branches;
        String isRO;
        try {
            userUUID = request.getString("uuid_user");
            firstName = request.getString("firstName");
            lastName = request.getString("lastName");
            email = request.getString("email");
            branches = request.getJsonArray("branches");
            
             // Convert JsonArray to List<String> while filtering out duplicates
            Set<String> uniqueIdentifiers = new HashSet<>();

            phoneNumber = request.getString("phoneNumber");
            userType = request.getString("userType");
            isRO = request.getString("isRO");
            user = request.getString("user"); // Login user
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Invalid Request Data");
            return request;
        }

        request.clear();

        phoneNumber = new Utilities().formatPhone(phoneNumber);

        JsonObject roleDetails = fetchRoleDetails("id", userType);
        if (!roleDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid Role selected");
            return request;
        }

        JsonObject userDetails = fetchUserDetails("u.uuid", userUUID);
        if (!userDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Unable to fetch user details");
            return request;
        }

        String sql = "UPDATE users SET first_name = ?, last_name = ?, phone_number = ? , email = ? , [type] = ?,updated_by=?,isRO = ?,updated_at = GETDATE() WHERE uuid = ?";

        String deleteUsersBranches = "DELETE FROM usersBranches WHERE UserId = ?";
        String insertUsersBranches = "INSERT INTO usersBranches(UserId,BranchId) VALUES(?,?)";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             PreparedStatement prDeleteUsersBranches = connection.prepareStatement(deleteUsersBranches);
             PreparedStatement prUpdateUsersBranches = connection.prepareStatement(insertUsersBranches);) {

            connection.setAutoCommit(false);

            preparedStatement.setString(1, firstName);
            preparedStatement.setString(2, lastName);
            preparedStatement.setString(3, phoneNumber);
            preparedStatement.setString(4, email);
            preparedStatement.setString(5, userType);
            preparedStatement.setString(6, user);
            preparedStatement.setString(7, isRO);
            preparedStatement.setString(8, userUUID);
            int insertedRows = preparedStatement.executeUpdate();
            if (insertedRows == 0) {
                throw new SQLException("Failed to update User "+firstName);
            }

            prDeleteUsersBranches.setString(1, userUUID);
            prDeleteUsersBranches.executeUpdate();

            // USer Branches
            for (int x = 0; x < branches.size(); x++ ) {
                prUpdateUsersBranches.setString(1, userUUID);
                prUpdateUsersBranches.setString(2, branches.getString(x));
                prUpdateUsersBranches.addBatch();
            }
            int[] insertedBranches = prUpdateUsersBranches.executeBatch();
//            //System.out.println("\n User Branches "+ Arrays.toString(insertedBranches));

            connection.commit();

            request
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! " + insertedRows + " User updated");

        } catch (SQLException throwables) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed to register User");

            try {
                connection.rollback();
            } catch (SQLException e) {
                //e.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);

            }

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }

        return request;
    }

    public JsonObject activateDeactivateUser(JsonObject request) {
        String userId, user, isRO;
        byte status;
        try {
            userId = request.getString("user_uuid");
            user = request.getString("user");
            status = Byte.parseByte(request.getString("status"));
            isRO = request.getString("isRO");
            
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Invalid Request Data");
            return request;
        }


        JsonObject userDetails = fetchUserDetails("uuid", userId);
        if (!userDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Unable to fetch user details");
            return request;
        }

        String sql = " UPDATE users SET [status] = ?, updated_by = ?, isRO = ?, updated_at = GETDATE() WHERE uuid = ?";
        String lv_sql = " UPDATE login_validation SET [status] = ?,  updated_at = GETDATE() WHERE uuid = ?";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
             PreparedStatement prLoginValidation = connection.prepareStatement(lv_sql)) {

            preparedStatement.setByte(1, status);
            preparedStatement.setString(2, user);
            preparedStatement.setString(3, isRO);
            preparedStatement.setString(4, userId);
            int insertedRows = preparedStatement.executeUpdate();
            if (insertedRows > 0) {
                prLoginValidation.setByte(1, status);
                prLoginValidation.setString(2, userId);
                prLoginValidation.executeUpdate();

                request
                        .put("responseCode", SUCCESS_CODE)
                        .put("responseDescription", "Success! " + insertedRows + " User updated");
            } else {
                request
                        .put("responseCode", ERROR_CODE)
                        .put("responseDescription", "Error! Unable to update user");
            }

        } catch (SQLException throwables) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed to activate User");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }

        return request;
    }

    public JsonObject fetchLoginValidationDetails(String column, String word) {
        JsonObject loginObject = new JsonObject();

        DBConnection conn = new DBConnection();
        try {
            String call = "SELECT [id],[uuid],[password],[login_trials],[change_password],[status],[otp_verified]," +
                    "[otp_hash] ,[otp_expiry],[created_at],[updated_at],[reference] FROM [Performance_Management].[dbo].[login_validation] WHERE " + column + "='" + word + "'";

            ResultSet rs = conn.query_all(call);
            if (rs.next()) {
                loginObject
                        .put("successIndicator", true)
                        .put("id", rs.getString("id"))
                        .put("uuid", rs.getString("uuid"))
                        .put("password", rs.getString("password"))
                        .put("loginTrials", String.valueOf(rs.getInt("login_trials")))
                        .put("changePassword", rs.getString("change_password"))
                        .put("status", rs.getString("status"))
                        .put("otpVerified", rs.getString("otp_verified"))
                        .put("otpHash", rs.getString("otp_hash"))
                        .put("otpExpiry", rs.getString("otp_expiry"))
                        .put("createdAt", rs.getString("created_at"))
                        .put("otpExpiry", rs.getString("updated_at"))
                        .put("reference", rs.getString("reference"));
            } else {
                loginObject.put("successIndicator", false);
            }
        } catch (SQLException ex) {
            loginObject.put("successIndicator", false);
            ex.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error " + ex.getMessage() + "\n", "", 6);
        } finally {
            conn.closeConn();
        }
        return loginObject;
    }

    public JsonArray fetchUsersWithUsersBranchesTable(String column, String value) {
        String sql = " SELECT u.id,ub.BranchId,b.[Name],first_name,last_name,phone_number,email,\n" +
                "  uuid,[type],isRO,u.creator_id,[status],u.created_at,r.[name] AS RoleName FROM users u \n" +
                " INNER JOIN usersBranches ub ON ub.UserId = u.uuid LEFT JOIN Branches b ON b.Id = ub.BranchId\n" +
                " LEFT JOIN roles r ON r.id = u.[type]" +
                " WHERE " + column + " = '" + value + "' ORDER BY first_name ASC";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                JsonObject obj = new JsonObject();
                obj
//                        .put("id",resultSet.getString("id"))
                        .put("branchId", resultSet.getString("BranchId"))
                        .put("branchName", resultSet.getString("Name"))
                        .put("firstName", resultSet.getString("first_name"))
                        .put("lastName", resultSet.getString("last_name"))
                        .put("phoneNumber", resultSet.getString("phone_number"))
                        .put("email", resultSet.getString("email"))
                        .put("uuid", resultSet.getString("uuid"))
                        .put("type", resultSet.getString("type"))
                        .put("roleName", resultSet.getString("RoleName"))
                        .put("maker", resultSet.getString("creator_id"))
                        .put("status", resultSet.getString("status"))
                        .put("isRO", resultSet.getString("isRO"))
                        .put("createdAt", resultSet.getString("created_at"));

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
            dbConnection.closeConn();
        }

        return array;
    }

    public JsonObject fetchUsers(String column, String value) {
        JsonObject res = new JsonObject();
        String sql = "SELECT u.id,first_name,last_name,phone_number,email,\n" +
                "  uuid,[type],isRO,u.creator_id,[status],u.created_at,u.channel,r.[name] AS RoleName FROM users u\n" +
                "  LEFT JOIN roles r ON r.id = u.[type]" +
                " WHERE " + column + " = '" + value + "'  ORDER BY created_at DESC";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                JsonObject obj = new JsonObject();
                obj
                        .put("id",String.valueOf(resultSet.getInt("id")))
                        .put("firstName", resultSet.getString("first_name"))
                        .put("lastName", resultSet.getString("last_name"))
                        .put("phoneNumber", resultSet.getString("phone_number"))
                        .put("email", resultSet.getString("email"))
                        .put("uuid", resultSet.getString("uuid"))
                        .put("type", resultSet.getString("type"))
                        .put("roleName", resultSet.getString("RoleName"))
                        .put("maker", resultSet.getString("creator_id"))
                        .put("status", resultSet.getString("status"))
                        .put("isRO", resultSet.getString("isRO"))
                        .put("createdAt", resultSet.getString("created_at"))
                        .put("channel", resultSet.getString("channel"));

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
            dbConnection.closeConn();
        }

        res.put("data", array);
        return res;
    }
    
    public JsonObject fetchUserDetails(Map<String, Object> searchColumns) {
        JsonObject res = new JsonObject();

        // Construct the base SQL query
        StringBuilder sqlBuilder = new StringBuilder("SELECT u.id,first_name,last_name,phone_number,email,\n" +
                "  uuid,[type],isRO,u.creator_id,[status],u.created_at,u.channel,r.[name] AS RoleName FROM users u\n" +
                "  LEFT JOIN roles r ON r.id = u.[type] "
                + "LEFT JOIN usersBranches ub on u.uuid = ub.UserId WHERE 1 = 1");

        // Dynamic SQL construction based on the columns passed
        int paramIndex = 1;  // To track the position of each parameter in the query
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : searchColumns.entrySet()) {
            sqlBuilder.append(" AND ").append(entry.getKey()).append(" = ?");
            params.add(entry.getValue());
        }

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString())) {
            // Set parameters dynamically based on the type
            for (int i = 0; i < params.size(); i++) {
                Object param = params.get(i);

                if (param instanceof String) {
                    preparedStatement.setString(i + 1, (String) param);
                } else if (param instanceof Integer) {
                    preparedStatement.setInt(i + 1, (Integer) param);
                } else if (param instanceof Boolean) {
                    preparedStatement.setBoolean(i + 1, (Boolean) param);
                } else if (param instanceof Date) {
                    preparedStatement.setDate(i + 1, (java.sql.Date) param);
                } else if (param instanceof Timestamp) {
                    preparedStatement.setTimestamp(i + 1, (Timestamp) param);
                } else {
                    preparedStatement.setObject(i + 1, param);  // Fallback for other types (e.g., Long, Double, etc.)
                }
            }
            
            System.out.println("Executing Query: " + buildFinalQuery(sqlBuilder.toString(), params));

            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                res.put("successIndicator", true)
                    .put("id", resultSet.getString("id"))
                    .put("firstName", resultSet.getString("first_name"))
                    .put("lastName", resultSet.getString("last_name"))
                    .put("phoneNumber", resultSet.getString("phone_number"))
                    .put("email", resultSet.getString("email"))
                    .put("uuid", resultSet.getString("uuid"))
                    .put("type", String.valueOf(resultSet.getInt("type")))
                    .put("roleName", resultSet.getString("RoleName"))
                    .put("maker", resultSet.getString("creator_id"))
                    .put("status", resultSet.getString("status"))
                    .put("isRO", resultSet.getString("isRO"))
                    .put("createdAt", resultSet.getString("created_at"))
                    .put("channel", resultSet.getString("channel"));

                String userUUID = resultSet.getString("uuid");
                JsonArray usersBranchesJsonArray = fetchUsersBranchesAsBranchesJsonArray("UserId", userUUID);
                res.put("userBranches", usersBranchesJsonArray);

                JsonArray usersBranchesDetails = fetchUsersBranchesDetails("UserId", userUUID);
                res.put("userBranchesDetails", usersBranchesDetails);

            } else {
                res.put("successIndicator", false);
            }

        } catch (SQLException e) {
            res.put("successIndicator", false);
            try {
                connection.close();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }

        return res;
    }

    private String buildFinalQuery(String query, List<Object> params) {
        StringBuilder finalQuery = new StringBuilder(query);
        for (Object param : params) {
            int index = finalQuery.indexOf("?");
            if (index != -1) {
                String value = (param instanceof String || param instanceof Date || param instanceof Timestamp)
                        ? "'" + param + "'"  // Add quotes for string and date types
                        : String.valueOf(param);  // No quotes for numbers or booleans
                finalQuery.replace(index, index + 1, value);
            }
        }
        return finalQuery.toString();
    }

    public JsonObject fetchUserDetails(String searchColumn, String searchWord) {
        JsonObject res = new JsonObject();
        String sql = "SELECT u.id,first_name,last_name,phone_number,email,\n" +
                "  uuid,[type],isRO,u.creator_id,[status],u.created_at,u.channel,r.[name] AS RoleName FROM users u\n" +
                "  LEFT JOIN roles r ON r.id = u.[type] "
                + "LEFT JOIN usersBranches ub on u.uuid = ub.UserId"
                + " WHERE " + searchColumn + " = ?";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (
                PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                res
                        .put("successIndicator", true)
                        .put("id", resultSet.getString("id"))
                        .put("firstName", resultSet.getString("first_name"))
                        .put("lastName", resultSet.getString("last_name"))
                        .put("phoneNumber", resultSet.getString("phone_number"))
                        .put("email", resultSet.getString("email"))
                        .put("uuid", resultSet.getString("uuid"))
                        .put("type", resultSet.getString("type"))
                        .put("roleName", resultSet.getString("RoleName"))
                        .put("maker", resultSet.getString("creator_id"))
                        .put("status", resultSet.getString("status"))
                        .put("isRO", resultSet.getString("isRO"))
                        .put("createdAt", resultSet.getString("created_at"))
                        .put("channel", resultSet.getString("channel"));

                String userUUID = resultSet.getString("uuid");
                JsonArray usersBranchesJsonArray = fetchUsersBranchesAsBranchesJsonArray("UserId", userUUID);
                res.put("userBranches", usersBranchesJsonArray);

                JsonArray usersBranchesDetails = fetchUsersBranchesDetails("UserId", userUUID);
                res.put("userBranchesDetails", usersBranchesDetails);

            } else {
                res.put("successIndicator", false);
            }

        } catch (SQLException throwables) {
            res.put("successIndicator", false);
            try {
                connection.close();
            } catch (SQLException e) {
                //e.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
            }

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }
        return res;
    }

    public JsonArray fetchUsersBranchesDetails(String searchColumn, String searchWord) {
        JsonArray array = new JsonArray();
        String sql = "SELECT TOP (1000) ub.[Id],[UserId],[BranchId],ub.[CreatedDate],b.[Name] as BranchName," +
                "b.[Code] AS BranchCode, u.first_name + ' ' + u.last_name AS UserFullName\n" +
                "  FROM [Performance_Management].[dbo].[usersBranches] ub LEFT JOIN Branches b ON b.Id = ub.BranchId " +
                "LEFT JOIN users u ON u.uuid = ub.UserId  WHERE " + searchColumn + " = ? ORDER BY b.[Name] ASC";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                JsonObject obj = new JsonObject();
                obj
                        .put("id", resultSet.getString("Id"))
                        .put("userId", resultSet.getString("UserId"))
                        .put("branchId", resultSet.getString("BranchId"))
                        .put("userFullName", resultSet.getString("UserFullName"))
                        .put("branchName", resultSet.getString("BranchName"))
                        .put("branchCode", resultSet.getString("BranchCode"))
                        .put("createdDate", resultSet.getString("CreatedDate"));
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
            dbConnection.closeConn();
        }
        return array;
    }

    public JsonArray fetchUsersBranchesAsBranchesJsonArray(String searchColumn, String searchWord) {
        JsonArray array = new JsonArray();
        String sql = "SELECT TOP (100) [Id],[UserId],[BranchId],[CreatedDate]"+
                "  FROM [Performance_Management].[dbo].[usersBranches]  WHERE " + searchColumn + " = ?";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                array.add(resultSet.getString("BranchId"));
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
            dbConnection.closeConn();
        }
        return array;
    }

    public JsonObject login(JsonObject request) {
        DBConnection dbConnection = new DBConnection();
        String email, password;
        try {
            email = request.getString("email");
            password = request.getString("user_password");
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Error parsing data");
            return request;
        }
        request.clear();

        JsonObject userDetails = fetchUserDetails("email", email);
        if (!userDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid Email or Password");
//            //System.out.println("FAILED TO FETCH USER DATA " + userDetails);
            return request;
        }
        String uuid = userDetails.getString("uuid");
        String phoneNumber = userDetails.getString("phoneNumber");
        String name = userDetails.getString("firstName") + " " + userDetails.getString("lastName");

        if (userDetails.getString("status").equals("0")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! User has been deactivated. Contact System Administrator.");
            return request;
        }

        JsonObject loginValidationDetails = fetchLoginValidationDetails("uuid", uuid);
        if (!loginValidationDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid Email or Password");
//            //System.out.println("FAILED TO FETCH LOGIN DATA " + loginValidationDetails);
            return request;
        }
        String lv_id = loginValidationDetails.getString("id");
        String lv_password = loginValidationDetails.getString("password");
        int loginTrials = Integer.parseInt(loginValidationDetails.getString("loginTrials"));


        String hashPassword = new Common().generatedHashedPin(password, "A.B.", "12345678");
        
        // Check the number of failed attempts
        if (loginTrials < 3) {
            if (hashPassword.equals(lv_password)) {

                String jsonObjectToken = LoginTokens.generateCustomJsonObjectToken(uuid);
                request.put("json_token", jsonObjectToken);

                String encoded = Base64.getEncoder().encodeToString(uuid.getBytes(StandardCharsets.UTF_8));
                request.put("token", encoded);
                request
                        .put("responseCode", SUCCESS_CODE)
                        .put("responseDescription", "Success! User Authenticated");

                // OTP
                String otp = new Common().generateRandom(6);
                String hashedOTP = new Common().generatedHashedPin(otp, "1", "2");

                String sql = "UPDATE login_validation SET login_trials = '0', otp_verified='0', otp_hash = '" + hashedOTP + "'," +
                        "otp_expiry=DATEADD(Minute,2,GETDATE()),updated_at=GETDATE() WHERE id= '" + lv_id + "'";

                
                int i = dbConnection.update_db(sql);


                String otpSMS = "Dear "+name.toUpperCase()+", your One Time Password is "+otp+".";

                request
                        .put("emailRecipient",email)
                        .put("emailSubject", "OTP")
                        .put("emailBody", otpSMS);

                request
                        .put("phonenumber", phoneNumber)
                        .put("msg", otpSMS);

                System.out.println("\n OTP " + otp + " - " + i);

            } else {
                loginTrials = loginTrials + 1;

                String sql = "UPDATE login_validation SET login_trials='"+ loginTrials +"'" +
                        ",updated_at=GETDATE() WHERE id= '" + lv_id + "'";

                int k = dbConnection.update_db(sql);
                
                request
                        .put("responseCode", ERROR_CODE)
                        .put("responseDescription", "Error! Invalid Email or Password. "+ (3 - loginTrials) +" remaining before account is locked");
            }
            
        } else {
            request
                        .put("responseCode", ERROR_CODE)
                        .put("responseDescription", "Number of login trials exceeded. Contact System Admin!");
        }

        dbConnection.closeConn();
        return request;
    }
    
    public JsonObject otpVerification(JsonObject request) {
        String otp = request.getString("otp");
        String user_uuid = request.getString("user_uuid");
        request.clear();

        String hashedOTP = new Common().generatedHashedPin(otp, "1", "2");

        JsonObject loginValidationDetails = fetchLoginValidationDetails("uuid", user_uuid);
        if (!loginValidationDetails.getBoolean("successIndicator")) {
            return request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed to fetch login details");
        }
        String lv_id = loginValidationDetails.getString("id");
        String lv_otpHash = loginValidationDetails.getString("otpHash");
        String lv_changePassword = loginValidationDetails.getString("changePassword");

        if (!hashedOTP.equals(lv_otpHash)) {
            return request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! OTP is invalid");
        }

        String sql = "UPDATE login_validation SET otp_verified = '1', reference = ?, updated_at = GETDATE() WHERE id = ?";
        DBConnection dbConnection = new DBConnection();
        try (PreparedStatement preparedStatement = dbConnection.getConnection().prepareStatement(sql)) {
            preparedStatement.setString(1, otp);
            preparedStatement.setString(2, lv_id);
            int i = preparedStatement.executeUpdate();

            if (i == 1) {
                request
                        .put("responseCode", SUCCESS_CODE)
                        .put("responseDescription", "Success! OTP Validated");

                JsonObject userDetails = fetchUserDetails("uuid", user_uuid);
                if (!userDetails.getBoolean("successIndicator")) {
                    return request
                            .put("responseCode", ERROR_CODE)
                            .put("responseDescription", "Error! Unable to fetch user details");
                }

                String name = userDetails.getString("firstName") + " " + userDetails.getString("lastName");
                String email = userDetails.getString("email");
                String phoneNumber = userDetails.getString("phoneNumber");
                String branch = userDetails.getJsonArray("userBranches").getString(0);
                String roleName = userDetails.getString("roleName");
                JsonArray userBranches = userDetails.getJsonArray("userBranchesDetails");

                String nextRole = getNextEscalationRole(roleName);
                JsonObject escalationDetails = new JsonObject();

                if (nextRole != null) {
                    Map<String, Object> searchCriteria = new HashMap<>();
                    searchCriteria.put("ub.BranchId", branch);
                    searchCriteria.put("r.name", nextRole);

                    JsonObject escalatedUserDetails = fetchUserDetails(searchCriteria);
                    if (escalatedUserDetails.getBoolean("successIndicator")) {
                        escalationDetails
                                .put("name", escalatedUserDetails.getString("firstName") + " " + escalatedUserDetails.getString("lastName"))
                                .put("uuid", escalatedUserDetails.getString("uuid"))
                                .put("roleName", escalatedUserDetails.getString("roleName"))
                                .put("phoneNumber", escalatedUserDetails.getString("phoneNumber"))
                                .put("email", escalatedUserDetails.getString("email"));
                    }
                }
                // Only one fallback for missing escalation details
                if (escalationDetails.isEmpty()) {
                    escalationDetails
                            .put("name", "")
                            .put("uuid", "")
                            .put("roleName", "")
                            .put("phoneNumber", "")
                            .put("email", "");
                }

                request.put("escalation", escalationDetails);

                request
                        .put("roleName", roleName)
                        .put("changePassword", lv_changePassword)
                        .put("name", name)
                        .put("email", email)
                        .put("phoneNumber", phoneNumber)
                        .put("rolePermission", fetchRolePermissionsArray("r.name", roleName))
                        .put("userBranches", userBranches);
            } else {
                request
                        .put("responseCode", ERROR_CODE)
                        .put("responseDescription", "ERROR! OTP not updated");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Database error: " + e.getMessage());
        } finally {
            dbConnection.closeConn();
        }

        return request;
    }

    public String getNextEscalationRole(String roleName) {
        Map<String, String> escalationMap = new HashMap<>();
        escalationMap.put("RO", "BM");     // Relationship Officer → Branch Manager
        escalationMap.put("BM", "AM");     // Branch Manager → Assistant Manager
        escalationMap.put("AM", "Chief");  // Assistant Manager → Chief
        escalationMap.put("Chief", "CEO"); // Chief → CEO

        return escalationMap.get(roleName); // Returns next role, or null if not found
    }
    
    public JsonObject logout(JsonObject request) {
        request.clear();
        request
                .put("responseCode", SUCCESS_CODE)
                .put("responseDescription", "Success! Logged Out");

        return request;
    }

    public JsonObject forgotPassword(JsonObject request) {
        Common common = new Common();
        String email, channel;
        try {
            email = request.getString("email");
            channel = request.getString("channel");
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Error parsing data");
            return request;
        }
        request.clear();

        String password = "";
        
        System.out.println("Channel - " + channel);
        System.out.println("Channel true - " + common.generateNewPinRandomFour());
        
        if (channel.trim().equalsIgnoreCase("app")) {
            password = common.generateNewPinRandomFour();
        } else {
            password = generateRandomPassword(8);
        }
        
        String hashPassword = new Common().generatedHashedPin(password, "A.B.", "12345678");

        JsonObject userDetails = fetchUserDetails("u.email", email);
        if (!userDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid Email");
            //System.out.println("FAILED TO FETCH USER DATA");
            return request;
        }

        String uuid = userDetails.getString("uuid");
        String name = userDetails.getString("firstName") + " " + userDetails.getString("lastName");
        String phoneNumber = userDetails.getString("phoneNumber");

        String sql = "UPDATE login_validation SET  login_trials='0', change_password='1', updated_at = GETDATE() , password = ? WHERE uuid = ? ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();


        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, hashPassword);
            preparedStatement.setString(2, uuid);
            preparedStatement.executeUpdate();

            request
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! Password changed");

            String emailBody = "Dear " + name.toUpperCase() + " your "+ ProjectConstants.COMPANY_NAME +" password has been reset to " + password +
                    " If you did not initiate a Forgot My Password action, then contact us immediately." ;
//                    "Click <a href=\""+ProjectConstants.PORTAL_URL+"\" >here</a> to login <br> Thanks";
            
            
            String otpSMS = "Dear " + name.toUpperCase() + ", your "+ ProjectConstants.COMPANY_NAME +" password has been reset to " + password +
                    ". If you did not initiate a Forgot My Password action, then contact us immediately.";
            
            request
                        .put("phonenumber", phoneNumber)
                        .put("msg", otpSMS);
            
            request
                    .put("emailRecipient", email)
                    .put("emailSubject", "FORGOT PASSWORD")
                    .put("emailBody", emailBody);


        } catch (SQLException throwables) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! ");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }


        // SEND EMAIL

        return request;
    }
    
    public JsonArray fetchPasswordHistory(String userId) {
        String sql = "SELECT * FROM PasswordHistory WHERE user_id = ?";
        JsonArray results = new JsonArray();
        
        DBConnection conn = new DBConnection();
        Connection connection = conn.getConnection();
        
        try (PreparedStatement prUpdatePassHist = connection.prepareStatement(sql)) {
            prUpdatePassHist.setInt(1, Integer.parseInt(userId));
            
            ResultSet rs = prUpdatePassHist.executeQuery();
            
            while (rs.next()) {
                JsonObject jo = new JsonObject();
                
                jo
                        .put("userId", String.valueOf(rs.getInt("user_id")))
                        .put("previousPassword", rs.getString("previous_password"))
                        .put("createdAt", rs.getString("created_at"));
                
                results.add(jo);
            }
        } catch (Exception e) {
            //e.printStackTrace(); 
            logger.applicationLog(logger.logPreString() + "Error - " + e.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            conn.closeConn();
        }
        
        return results;
    }

    public JsonObject changePassword(JsonObject request) {
        String user, currentPassword, newPassword;
        try {
            user = request.getString("user");
            currentPassword = request.getString("currentPassword");
            newPassword = request.getString("newPassword");
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Error parsing data");
            return request;
        }
        
        request.clear();

        JsonObject userDetails = fetchUserDetails("u.id", user);
        if (!userDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid User details");
            //System.out.println("FAILED TO FETCH USER DATA");
            return request;
        }
        String uuid = userDetails.getString("uuid");
        String email = userDetails.getString("email");
        String name = userDetails.getString("firstName") + " " + userDetails.getString("lastName");

        JsonObject loginValidationDetails = fetchLoginValidationDetails("uuid", uuid);
        if (!loginValidationDetails.getBoolean("successIndicator")) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Invalid Login details");
            //System.out.println("FAILED TO FETCH LOGIN VALIDATION DATA");
            return request;
        }
        String lv_id = loginValidationDetails.getString("id");

        String currentPasswordHash = new Common().generatedHashedPin(currentPassword, "A.B.", "12345678");    
        String newPasswordHash = new Common().generatedHashedPin(newPassword, "A.B.", "12345678");
        
        JsonArray pastPasswords = fetchPasswordHistory(user);
        boolean okayToProceed = true;
        
        for (Object obj: pastPasswords) {
            JsonObject pass = (JsonObject) obj;
            
            String prevPassHash = pass.getString("previousPassword");
            
            if (prevPassHash.equals(newPasswordHash)) {
                okayToProceed = false;
                request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! New password must not have been used before. Try again.");
                break; 
            }
        }

        if (okayToProceed) {
            if (currentPasswordHash.equals(loginValidationDetails.getString("password"))) {

                String sql = "UPDATE login_validation SET updated_at = GETDATE() , change_password = '0',password = ? WHERE id = ? ";
                String sqlPassHist = "INSERT INTO PasswordHistory (user_id,previous_password,created_at) "
                        + "VALUES (?,?,GETDATE())";

                DBConnection dbConnection = new DBConnection();
                Connection connection = dbConnection.getConnection();

                try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                        PreparedStatement prPassHist = connection.prepareStatement(sqlPassHist)
                        ) {
                    preparedStatement.setString(1, newPasswordHash);
                    preparedStatement.setString(2, lv_id);
                    preparedStatement.executeUpdate();

                    prPassHist.setInt(1, Integer.parseInt(user));
                    prPassHist.setString(2, newPasswordHash);
                    prPassHist.executeUpdate();

                    connection.commit();

                    request
                            .put("responseCode", SUCCESS_CODE)
                            .put("responseDescription", "Success! Password changed");

                    String emailBody = "Dear " + name.toUpperCase() + " your " + ProjectConstants.COMPANY_NAME +" password has been reset ." +
                            " If you did not change your password, Contact us immediately. Thanks";
                    

                    request
                            .put("emailRecipient", email)
                            .put("emailSubject", "PASSWORD RESET")
                            .put("emailBody", emailBody);


                } catch (SQLException throwables) {
                    request
                            .put("responseCode", ERROR_CODE)
                            .put("responseDescription", "Error! ");

                    throwables.printStackTrace();
                    logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
                } finally {
                    try {
                        connection.close();
                    } catch (SQLException throwables) {
                        throwables.printStackTrace();
                        logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
                    }
                    dbConnection.closeConn();
                }


            } else {
                request
                        .put("responseCode", ERROR_CODE)
                        .put("responseDescription", "Error! Invalid Password");

            }
        }

        return request;
    }

    public JsonObject fetchUserPermissions(String searchColumn, String searchWord) {
        JsonObject res = new JsonObject();
        String sql = "SELECT TOP (1000) u.[id],r.id AS role_id,p.[name] AS p_name,first_name,last_name,phone_number,[email],[status]\n" +
                "  FROM [Performance_Management].[dbo].[users] u\n" +
                "  LEFT JOIN roles r ON r.[name] = u.[type]\n" +
                "  JOIN [role_has_permissions] rp ON rp.role_id = r.id\n" +
                "  LEFT JOIN [permissions] p ON p.[id] = rp.permission_id\n" +
                "  WHERE " + searchColumn + " = ? ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                JsonObject obj = new JsonObject();
                res
                        .put("successIndicator", true)
                        .put("firstName", resultSet.getString("first_name"))
                        .put("lastName", resultSet.getString("last_name"))
                        .put("phoneNumber", resultSet.getString("phone_number"))
                        .put("email", resultSet.getString("email"))
                        .put("type", resultSet.getString("type"))
                        .put("permissionName", resultSet.getString("p_name"))
                        .put("status", resultSet.getString("status"));
                array.add(obj);
            } else {
                res.put("successIndicator", false);
            }

        } catch (SQLException throwables) {
            res.put("successIndicator", false);

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }
        res.put("data", array);
        return res;
    }

    public boolean checkUserHasPermission(String searchColumn, String searchWord, String permission) {
        boolean hasPermission = false;
        String sql = "SELECT TOP (100) u.[id],r.id AS role_id,p.[name] AS p_name,first_name,last_name,phone_number,[email],[status]\n" +
                "  FROM [Performance_Management].[dbo].[users] u\n" +
                "  LEFT JOIN roles r ON r.[id] = u.[type]\n" +
                "  JOIN [role_has_permissions] rp ON rp.role_id = r.id\n" +
                "  LEFT JOIN [permissions] p ON p.[id] = rp.permission_id\n" +
                "  WHERE " + searchColumn + " = ? AND p.[name] = ? ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();


        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.setString(2, permission);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                //System.out.println("PERM " + resultSet.getString("p_name"));
                hasPermission = true;
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
            dbConnection.closeConn();
            
        }
        return hasPermission;
    }

    public JsonObject approveUser(JsonObject data) {
        String user_id = data.getString("user_id");
        String actionBy = data.getString("user");

        String sql = "UPDATE users SET status = '1', updated_at = GETDATE() , verifier_id = ? WHERE id = ? ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();


        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, user_id);
            preparedStatement.setString(2, actionBy);
            preparedStatement.executeUpdate();

            data
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! ");

        } catch (SQLException throwables) {
            data
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! ");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }

        return data;
    }

    public JsonObject fetchRoleDetails(String searchColumn, String searchWord) {
        JsonObject res = new JsonObject();
        String sql = "SELECT id,[name],[description],created_at,updated_at FROM roles " +
                "  WHERE " + searchColumn + " = ? ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

//        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            if (resultSet.next()) {
                JsonObject obj = new JsonObject();
                res
                        .put("successIndicator", true)
                        .put("id", resultSet.getString("id"))
                        .put("role_name", resultSet.getString("name"))
                        .put("description", resultSet.getString("description"))
                        .put("created_at", resultSet.getString("created_at"))
                        .put("updated_at", resultSet.getString("updated_at"));
//                array.add(obj);
            } else {
                res.put("successIndicator", false);
            }

        } catch (SQLException throwables) {
            res.put("successIndicator", false);

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }
//        res.put("data", array);
        return res;
    }

    public JsonObject fetchRoles() {
        JsonObject res = new JsonObject();
        String sql = "SELECT id,[name],[description],created_at,updated_at FROM roles ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                JsonObject obj = new JsonObject();
                obj
                        .put("id", resultSet.getString("id"))
                        .put("role_name", resultSet.getString("name"))
                        .put("description", resultSet.getString("description"))
                        .put("created_at", resultSet.getString("created_at"))
                        .put("updated_at", resultSet.getString("updated_at"));
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
            dbConnection.closeConn();
        }
        res.put("data", array);
        return res;
    }

    public JsonObject fetchPermissions() {
        JsonObject res = new JsonObject();
        String sql = "SELECT [id],[name],[description],[created_at],[updated_at] FROM permissions ";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                JsonObject obj = new JsonObject();
                obj
                        .put("id", resultSet.getString("id"))
                        .put("role_name", resultSet.getString("name"))
                        .put("description", resultSet.getString("description"))
                        .put("created_at", resultSet.getString("created_at"))
                        .put("updated_at", resultSet.getString("updated_at"));
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
            dbConnection.closeConn();
        }
        res.put("data", array);
        return res;
    }

    public JsonArray fetchRolePermissionsArray(String searchColumn, String searchWord) {
        String sql = "SELECT TOP (1000) rp.[permission_id] ,rp.[role_id],\n" +
                "  r.[name],r.[description],r.[created_at],r.[updated_at],\n" +
                "  p.[name] AS p_name,p.[description] AS p_description,p.[created_at] AS p_created_at,p.[updated_at] AS p_updated_at\n" +
                "FROM [Performance_Management].[dbo].[role_has_permissions] rp\n" +
                "RIGHT JOIN [roles] r ON r.id = rp.role_id\n" +
                "RIGHT JOIN [permissions] p ON p.id = rp.permission_id" +
                " WHERE " + searchColumn + " = ?";

        JsonArray array = new JsonArray();

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {

                array.add(resultSet.getString("p_name"));
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
            dbConnection.closeConn();
        }

        return array;

    }

    public JsonObject fetchRolePermissions(String searchColumn, String searchWord) {
        JsonObject res = new JsonObject();
        String sql = "SELECT TOP (1000) rp.[permission_id] ,rp.[role_id],\n" +
                "  r.[name],r.[description],r.[created_at],r.[updated_at],\n" +
                "  p.[name] AS p_name,p.[description] AS p_description,p.[created_at] AS p_created_at,p.[updated_at] AS p_updated_at\n" +
                "FROM [Performance_Management].[dbo].[role_has_permissions] rp\n" +
                "RIGHT JOIN [roles] r ON r.id = rp.role_id\n" +
                "RIGHT JOIN [permissions] p ON p.id = rp.permission_id" +
                " WHERE " + searchColumn + " = ?";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, searchWord);
            preparedStatement.execute();

            ResultSet resultSet = preparedStatement.getResultSet();
            while (resultSet.next()) {
                JsonObject obj = new JsonObject();
                obj
                        .put("role_id", resultSet.getString("role_id"))
                        .put("role_name", resultSet.getString("name"))
                        .put("role_description", resultSet.getString("description"))
                        .put("role_created_at", resultSet.getString("created_at"))
                        .put("role_updated_at", resultSet.getString("updated_at"))
                        .put("permission_id", resultSet.getString("permission_id"))
                        .put("permission_name", resultSet.getString("p_name"))
                        .put("permission_description", resultSet.getString("p_description"))
                        .put("permission_created_at", resultSet.getString("p_created_at"))
                        .put("permission_updated_at", resultSet.getString("p_updated_at"));
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
            dbConnection.closeConn();
        }
        res.put("data", array);
        return res;
    }

    public JsonObject addRolePermissions(JsonObject data) {
        JsonObject res = new JsonObject();
        String name = data.getString("role_name");
        String description = data.getString("role_description");
        JsonArray permissions = data.getJsonArray("permissions");
        String actionBy = data.getString("user");


        String insertRoles = "INSERT INTO roles([name],[description],[creator_id]) VALUES(?,?,?)";
        String insertRolePermission = "INSERT INTO [dbo].[role_has_permissions]([permission_id],[role_id],[creator_id])  VALUES(?,?,?)";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(insertRoles, PreparedStatement.RETURN_GENERATED_KEYS);
             PreparedStatement preparedStatement1 = connection.prepareStatement(insertRolePermission)) {
            preparedStatement.setString(1, name);
            preparedStatement.setString(2, description);
            preparedStatement.setString(3, actionBy);
            preparedStatement.executeUpdate();

            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if (resultSet.next()) {
                int role_id = resultSet.getInt(1);
                for (byte x = 0; x < permissions.size(); x++) {
                    preparedStatement1.setInt(1, Integer.parseInt(permissions.getString(x)));
                    preparedStatement1.setInt(2, role_id);
                    preparedStatement1.setString(3, actionBy);
                    preparedStatement1.addBatch();
                }
                int[] insertedRows = preparedStatement1.executeBatch();
                //System.out.println("INSERTED " + Arrays.toString(insertedRows));
            }

            res
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! ");

        } catch (SQLException throwables) {
            res
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed Execution to add role and permissions");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }
//        res.put("data", array);
        return res;
    }

    public JsonObject updateRolePermissions(JsonObject data) {
        JsonObject res = new JsonObject();
        String roleId = data.getString("role_id");
        JsonArray permissions = data.getJsonArray("permissions");
        String actionBy = "2";

        JsonObject roleDetails = fetchRoleDetails("[id]", roleId);
        //System.out.println(roleDetails);
        if (!roleDetails.getBoolean("successIndicator")) {
            res
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Unable to fetch role details");
            return res;
        }
        int role_id = Integer.parseInt(roleDetails.getString("id"));


//        String insertRoles = "INSERT INTO roles([name],[description],[creator_id]) VALUES(?,?,?)";
        String deleteRolesPermissions = "DELETE FROM role_has_permissions WHERE role_id = ?";
        String insertRolePermission = "INSERT INTO [dbo].[role_has_permissions]([permission_id],[role_id],[creator_id])  VALUES(?,?,?)";

        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

//        JsonArray array = new JsonArray();

        try (PreparedStatement preparedStatement = connection.prepareStatement(deleteRolesPermissions);
             PreparedStatement preparedStatement1 = connection.prepareStatement(insertRolePermission)) {
            preparedStatement.setInt(1, role_id);
            preparedStatement.executeUpdate();

            for (byte x = 0; x < permissions.size(); x++) {
                preparedStatement1.setInt(1, permissions.getInteger(x));
                preparedStatement1.setInt(2, role_id);
                preparedStatement1.setString(3, actionBy);
                preparedStatement1.addBatch();
            }
            int[] insertedRows = preparedStatement1.executeBatch();
            //System.out.println("INSERTED " + Arrays.toString(insertedRows));

            res
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! ");

        } catch (SQLException throwables) {
            res
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed Execution to add role and permissions");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }
//        res.put("data", array);
        return res;
    }

    public JsonObject changeRelationshipOfficerPortfolio(JsonObject request) {
        String user, currentRelationshipOfficer, newRelationshipOfficer;
        logger.applicationLog(logger.logPreString() + "Error - " + request + "\n\n", "", 6);
        JsonArray customers;
        try {
            user = request.getString("user");
            currentRelationshipOfficer = request.getString("current_relationship_officer");
            newRelationshipOfficer = request.getString("new_relationship_officer");
            customers = request.getJsonArray("customers");
        } catch (Exception e) {
            //e.printStackTrace();
            request.clear();
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Error parsing data");
            return request;
        }

        request.clear();

        // TODO : Check RO's exists
        String sql = "UPDATE customers SET relationship_officer_id = ? WHERE relationship_officer_id = ? AND id=?";
        String loanSql = "UPDATE LoanRequests SET RelationshipManagerId = ? WHERE RelationshipManagerId = ? AND CustomerId = ?";
        
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql);
                PreparedStatement preparedStatementLoan = connection.prepareStatement(loanSql);) {
            
            System.out.println("IMEINGIA");
            
            for (Object obj: customers) {
//                System.out.println(x);
//                System.out.println("customers.getValue(x).toString() :: " +customers.getValue(x).toString());
                preparedStatement.setString(1, newRelationshipOfficer);
                preparedStatement.setString(2, currentRelationshipOfficer);
                preparedStatement.setString(3, obj.toString());
                preparedStatement.addBatch();
                
                preparedStatementLoan.setString(1, newRelationshipOfficer);
                preparedStatementLoan.setString(2, currentRelationshipOfficer);
                preparedStatementLoan.setString(3, obj.toString());
                preparedStatementLoan.addBatch();
            }
            int[] insertedRows = preparedStatement.executeBatch();
            int[] insertedRowsLoan = preparedStatementLoan.executeBatch();
            
            System.out.println("IMEINSERT");
            //System.out.println("INSERTED " + Arrays.toString(insertedRows));
            //System.out.println("INSERTED LOAN " + Arrays.toString(insertedRowsLoan));

            request
//                    .put("res",Arrays.asList(insertedRows))
                    .put("responseCode", SUCCESS_CODE)
                    .put("responseDescription", "Success! ");

        } catch (Exception throwables) {
            request
                    .put("responseCode", ERROR_CODE)
                    .put("responseDescription", "Error! Failed Execution to add role and permissions");

            throwables.printStackTrace();
            logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                logger.applicationLog(logger.logPreString() + "Error - " + throwables.getLocalizedMessage() + "\n\n", "", 6);
            }
            dbConnection.closeConn();
        }


        return request;
    }
}
