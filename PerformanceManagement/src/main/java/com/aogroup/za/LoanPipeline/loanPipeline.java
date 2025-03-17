/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.LoanPipeline;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.SQLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class loanPipeline extends AbstractVerticle {

    private Logging logger;
    static int TIMEOUT_TIME = 120000;
    EventBus eventBus;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        eventBus = vertx.eventBus();
        logger = new Logging();

        eventBus.consumer("CREATELOANPIPELINE", this::createloanPipeline);
        eventBus.consumer("FETCHALLLOANPIPELINE", this::fetchallloanPipeline);
    }

    private void createloanPipeline(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        JsonObject requestBody = message.body();

        // Extract request parameters
        String productName = requestBody.getString("productName");
        String approvedAmountStr = requestBody.getString("approvedAmount"); // Passed as String
        String dateLOOIssued = requestBody.getString("dateLOOIssued");
        String perfectionStatus = requestBody.getString("perfectionStatus");
        String disbursementDate = requestBody.getString("disbursmentDate");
        String userId = requestBody.getString("userId");
        String branchId = requestBody.getString("branchId");
        String customerNumber = requestBody.getString("customerNumber");

        // Validate required fields
        if (productName == null || productName.trim().isEmpty()
                || approvedAmountStr == null || approvedAmountStr.trim().isEmpty()
                || userId == null || userId.trim().isEmpty()
                || branchId == null || branchId.trim().isEmpty()
                || customerNumber == null || customerNumber.trim().isEmpty()) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Required fields are missing.");
            message.reply(response);
            return;
        }

        // Convert approvedAmount to Double
        Double approvedAmount;
        try {
            approvedAmount = Double.parseDouble(approvedAmountStr);
        } catch (NumberFormatException e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Invalid approvedAmount format.");
            message.reply(response);
            return;
        }

        // Prepare request to fetch customer details
        JsonObject requestPayload = new JsonObject().put("customerNumber", customerNumber);

        // Call the CustomerDetailsAdaptor
        eventBus.send("FETCH_CUSTOMER_DETAILS", requestPayload, reply -> {
            if (reply.succeeded()) {
                JsonObject customerResponse = (JsonObject) reply.result().body();

                if (!"000".equals(customerResponse.getString("response"))) {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Customer not found.");
                    message.reply(response);
                    return;
                }

                // Extract customer details
                String firstName = customerResponse.getString("firstName", "");
                String lastName = customerResponse.getString("lastName", "");
                String phoneNumber = customerResponse.getString("phoneNumber", "");
                String customerName = firstName + " " + lastName;

                // Insert loan details into the database
                DBConnection dbConnection = new DBConnection();
                Connection connection = null;
                PreparedStatement insertLoanStmt = null;

                try {
                    connection = dbConnection.getConnection();
                    connection.setAutoCommit(false);

                    String insertLoanQuery = "INSERT INTO [dbo].[Loan-pipeline] "
                            + "(id, productName, approvedAmount, dateLOOIssued, perfectionStatus, disbursmentDate, userId, branchId, customerName, customerNumber, CustomerPhoneNumber, createdAt, updatedAt) "
                            + "VALUES (NEWID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";

                    insertLoanStmt = connection.prepareStatement(insertLoanQuery);
                    insertLoanStmt.setString(1, productName);
                    insertLoanStmt.setDouble(2, approvedAmount);
                    insertLoanStmt.setString(3, dateLOOIssued);
                    insertLoanStmt.setString(4, perfectionStatus);
                    insertLoanStmt.setString(5, disbursementDate);
                    insertLoanStmt.setString(6, userId);
                    insertLoanStmt.setString(7, branchId);
                    insertLoanStmt.setString(8, customerName);
                    insertLoanStmt.setString(9, customerNumber);
                    insertLoanStmt.setString(10, phoneNumber);

                    int rowsAffected = insertLoanStmt.executeUpdate();

                    if (rowsAffected > 0) {
                        connection.commit();
                        response.put("responseCode", "000")
                                .put("responseDescription", "Loan pipeline entry created successfully");
                    } else {
                        connection.rollback();
                        response.put("responseCode", "999")
                                .put("responseDescription", "Loan pipeline creation failed");
                    }
                } catch (Exception e) {
                    response.put("responseCode", "999")
                            .put("responseDescription", "Database error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    try {
                        if (insertLoanStmt != null) {
                            insertLoanStmt.close();
                        }
                        if (connection != null) {
                            connection.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    dbConnection.closeConn();
                }

                message.reply(response);
            } else {
                response.put("responseCode", "999")
                        .put("responseDescription", "Failed to fetch customer details.");
                message.reply(response);
            }
        });
    }

//    private void createloanPipeline(Message<JsonObject> message) {
//        JsonObject response = new JsonObject();
//        JsonObject requestBody = message.body();
//
//        // Extract request parameters
//        String productName = requestBody.getString("productName");
//        String approvedAmountStr = requestBody.getString("approvedAmount"); // Passed as String
//        String dateLOOIssued = requestBody.getString("dateLOOIssued");
//        String perfectionStatus = requestBody.getString("perfectionStatus");
//        String disbursementDate = requestBody.getString("disbursmentDate");
//        String userId = requestBody.getString("userId"); 
//        String branchId = requestBody.getString("branchId"); 
//        String customerNumber = requestBody.getString("customerNumber");
//
//        // Validate required fields
//        if (productName == null || productName.trim().isEmpty() || 
//            approvedAmountStr == null || approvedAmountStr.trim().isEmpty() || 
//            userId == null || userId.trim().isEmpty() || 
//            branchId == null || branchId.trim().isEmpty() || 
//            customerNumber == null || customerNumber.trim().isEmpty()) {
//            response.put("responseCode", "999")
//                    .put("responseDescription", "Required fields are missing.");
//            message.reply(response);
//            return;
//        }
//
//        // Convert approvedAmount to Double
//        Double approvedAmount;
//        try {
//            approvedAmount = Double.parseDouble(approvedAmountStr);
//        } catch (NumberFormatException e) {
//            response.put("responseCode", "999")
//                    .put("responseDescription", "Invalid approvedAmount format.");
//            message.reply(response);
//            return;
//        }
//
//        DBConnection dbConnection = new DBConnection();
//        Connection connection = null;
//        PreparedStatement insertLoanStmt = null;
//        PreparedStatement fetchCustomerStmt = null;
//        ResultSet customerRs = null;
//
//        try {
//            connection = dbConnection.getConnection();
//            connection.setAutoCommit(false);
//
//            // Fetch customer details from customer_details_stub
//            String fetchCustomerQuery = "SELECT first_name, last_name, phone_number FROM [Dfa].[dbo].[customer_details_stub] WHERE customer_number = ?";
//            fetchCustomerStmt = connection.prepareStatement(fetchCustomerQuery);
//            fetchCustomerStmt.setString(1, customerNumber);
//            customerRs = fetchCustomerStmt.executeQuery();
//
//            String customerName = null;
//            String customerPhoneNumber = null;
//
//            if (customerRs.next()) {
//                customerName = customerRs.getString("first_name") + " " + customerRs.getString("last_name");
//                customerPhoneNumber = customerRs.getString("phone_number");
//            } else {
//                response.put("responseCode", "999")
//                        .put("responseDescription", "Customer not found.");
//                message.reply(response);
//                return;
//            }
//
//            // Insert loan entry
//            String insertLoanQuery = "INSERT INTO [dbo].[Loan-pipeline] " +
//                    "(id, productName, approvedAmount, dateLOOIssued, perfectionStatus, disbursmentDate, userId, branchId, customerName, customerNumber, CustomerPhoneNumber, createdAt, updatedAt) " +
//                    "VALUES (NEWID(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, GETDATE(), GETDATE())";
//
//            insertLoanStmt = connection.prepareStatement(insertLoanQuery);
//            insertLoanStmt.setString(1, productName);
//            insertLoanStmt.setDouble(2, approvedAmount);
//            insertLoanStmt.setString(3, dateLOOIssued);
//            insertLoanStmt.setString(4, perfectionStatus);
//            insertLoanStmt.setString(5, disbursementDate);
//            insertLoanStmt.setString(6, userId);
//            insertLoanStmt.setString(7, branchId);
//            insertLoanStmt.setString(8, customerName); 
//            insertLoanStmt.setString(9, customerNumber);
//            insertLoanStmt.setString(10, customerPhoneNumber); 
//
//            int rowsAffected = insertLoanStmt.executeUpdate();
//
//            if (rowsAffected > 0) {
//                connection.commit();
//                response.put("responseCode", "000")
//                        .put("responseDescription", "Loan pipeline entry created successfully");
//                        
//            } else {
//                connection.rollback();
//                response.put("responseCode", "999")
//                        .put("responseDescription", "Loan pipeline creation failed");
//            }
//
//        } catch (Exception e) {
//            response.put("responseCode", "999")
//                    .put("responseDescription", "Database error: " + e.getMessage());
//            e.printStackTrace();
//        } finally {
//            try {
//                if (customerRs != null) customerRs.close();
//                if (fetchCustomerStmt != null) fetchCustomerStmt.close();
//                if (insertLoanStmt != null) insertLoanStmt.close();
//                if (connection != null) connection.close();
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            dbConnection.closeConn();
//        }
//
//        message.reply(response);
//    }
    private void fetchallloanPipeline(Message<JsonObject> message) {
        JsonObject response = new JsonObject();
        DBConnection dbConnection = new DBConnection();
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            connection = dbConnection.getConnection();

            // Include CustomerPhoneNumber in the SELECT query
            String query = "SELECT id, productName, approvedAmount, dateLOOIssued, perfectionStatus, disbursmentDate, userId, branchId, customerName, customerNumber, CustomerPhoneNumber, createdAt, updatedAt FROM [dbo].[Loan-pipeline]";
            stmt = connection.prepareStatement(query);
            rs = stmt.executeQuery();

            JsonArray loanPipelines = new JsonArray();

            while (rs.next()) {
                JsonObject loan = new JsonObject()
                        .put("id", rs.getString("id"))
                        .put("productName", rs.getString("productName"))
                        .put("approvedAmount", String.valueOf(rs.getDouble("approvedAmount")))
                        .put("dateLOOIssued", rs.getString("dateLOOIssued"))
                        .put("perfectionStatus", rs.getString("perfectionStatus"))
                        .put("disbursmentDate", rs.getString("disbursmentDate"))
                        .put("userId", rs.getString("userId"))
                        .put("branchId", rs.getString("branchId"))
                        .put("customerName", rs.getString("customerName"))
                        .put("customerNumber", rs.getString("customerNumber"))
                        .put("customerPhoneNumber", rs.getString("CustomerPhoneNumber")) // Newly added field
                        .put("createdAt", rs.getString("createdAt"))
                        .put("updatedAt", rs.getString("updatedAt"));

                loanPipelines.add(loan);
            }

            if (loanPipelines.isEmpty()) {
                response.put("responseCode", "999")
                        .put("responseDescription", "No loan pipeline records found.");
            } else {
                response.put("responseCode", "000")
                        .put("responseDescription", "Success! Loan pipelines fetched successfully")
                        .put("data", loanPipelines);
            }

        } catch (Exception e) {
            response.put("responseCode", "999")
                    .put("responseDescription", "Database error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close resources
            try {
                if (rs != null) {
                    rs.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            dbConnection.closeConn();
        }

        message.reply(response);
    }

}
