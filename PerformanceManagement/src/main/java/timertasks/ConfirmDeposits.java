package timertasks;

///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package timetasks;
//
//import com.aogroup.za.datasource.DBConnection;
//import io.vertx.core.AsyncResult;
//import io.vertx.core.Handler;
//import io.vertx.core.Vertx;
//import io.vertx.core.eventbus.EventBus;
//import io.vertx.core.eventbus.Message;
//import io.vertx.core.json.JsonArray;
//import io.vertx.core.json.JsonObject;
//import java.sql.Connection;
//import java.sql.PreparedStatement;
//import java.sql.ResultSet;
//import java.sql.SQLException;
//import java.time.LocalDate;
//import java.time.format.DateTimeFormatter;
//import java.util.TimerTask;
//
///**
// *
// * @author Best Point
// */
//public class ConfirmDeposits extends TimerTask {
//    Vertx vertx;
//
//    public ConfirmDeposits(Vertx vertx) {
//        this.vertx = vertx;
//    }
//
//    @Override
//    public void run() {
//        startSchedule();
//    }
//
//    public void startSchedule() {
//        EventBus eventBus = vertx.eventBus();
//        DBConnection conn = new DBConnection();
//        Connection connection = null;
//
//        try {
//           connection = conn.getConnection();
//           String todayDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
//
//           // Fetch deposits from TaskBulkUpload
//           String fetchBulkDeposits = "SELECT Id, AccountNumber, Deposit FROM TaskBulkUpload WHERE ConfirmationStatus = 0 AND CONVERT(date, TaskDate) = ?";
//           PreparedStatement bulkStmt = connection.prepareStatement(fetchBulkDeposits);
//           bulkStmt.setString(1, todayDate);
//           ResultSet bulkResultSet = bulkStmt.executeQuery();
//
//           while (bulkResultSet.next()) {
//               String bulkId = bulkResultSet.getString("Id");
//               String accountNumber = bulkResultSet.getString("AccountNumber");
//               double expectedDeposit = bulkResultSet.getDouble("Deposit");
//
//               processDeposit(eventBus, accountNumber, expectedDeposit, () -> {
//                   try {
//                       updateConfirmationStatus(bulkId, "TaskBulkUpload");
//                   } catch (SQLException e) {
//                       e.printStackTrace();
//                   }
//            });
//        }
//            String fetchBusinessRealizedDeposits = 
//                "SELECT br.Id, uts.AccountNumber, br.deposit " +
//                "FROM BusinessRealized br " +
//                "JOIN UserTaskSubmissions uts ON br.id = uts.BusinessRealizedId " +
//                "WHERE br.ConfirmationStatus = 0 AND CONVERT(date, br.depositDate) = ?";
//
//            PreparedStatement businessStmt = connection.prepareStatement(fetchBusinessRealizedDeposits);
//            businessStmt.setString(1, todayDate);
//            ResultSet businessResultSet = businessStmt.executeQuery();
//
//            while (businessResultSet.next()) {
//                String businessId = businessResultSet.getString("Id");
//                String accountNumber = businessResultSet.getString("AccountNumber"); // Now using AccountNumber from UserTaskSubmissions
//                double expectedDeposit = businessResultSet.getDouble("deposit");
//
//                processDeposit(eventBus, accountNumber, expectedDeposit, () -> {
//                    try {
//                        updateConfirmationStatus(businessId, "BusinessRealized");
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
//                });
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            try {
//                if (connection != null && !connection.isClosed()) {
//                    connection.close();
//                }
//            } catch (SQLException e) {
//                e.printStackTrace();
//            }
//            conn.closeConn();
//        }
//    }
//
//    private void processDeposit(EventBus eventBus, String accountNumber, double expectedAmount, Runnable onSuccess) {
//        String todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));
//        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
//        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//        String formattedDate = LocalDate.parse(todayFormatted, inputFormatter).format(outputFormatter);
//
//        JsonObject fetchRequest = new JsonObject()
//                .put("account", accountNumber)
//                .put("start", formattedDate);
//
//        eventBus.send("310000", fetchRequest, (Handler<AsyncResult<Message<JsonObject>>>) reply -> {
//            if (reply.succeeded()) {
//                JsonObject response = reply.result().body();
//                JsonArray transactions = response.getJsonArray("transactions");
//                double totalDeposits = 0.0;
//
//                for (int i = 0; i < transactions.size(); i++) {
//                    JsonObject txn = transactions.getJsonObject(i);
//                    String description = txn.getString("description", "");
//                    if ("+".equals(txn.getString("sign")) && !description.contains("AA")) {
//                        totalDeposits += Double.parseDouble(txn.getString("amount"));
//                    }
//                }
//
//                if (Double.compare(totalDeposits, expectedAmount) == 0) {
//                    onSuccess.run();
//                }
//            } else {
//                System.out.println("Failed to fetch transactions for account: " + accountNumber);
//            }
//        });
//    }
//
//    private void updateConfirmationStatus(String id, String tableName) throws SQLException {
//        DBConnection conn = new DBConnection();
//        try (Connection connection = conn.getConnection()) {
//            String updateQuery = "UPDATE " + tableName + " SET ConfirmationStatus = 1 WHERE Id = ?";
//            PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
//            updateStmt.setString(1, id);
//            int rowsUpdated = updateStmt.executeUpdate();
//
//            if (rowsUpdated > 0) {
//                System.out.println("Confirmation status updated for ID: " + id + " in table " + tableName);
//            }
//        } finally {
//            conn.closeConn();
//        }
//    }
//}
