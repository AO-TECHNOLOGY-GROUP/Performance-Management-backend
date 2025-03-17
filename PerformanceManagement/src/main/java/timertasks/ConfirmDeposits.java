package timertasks;

import com.aogroup.za.datasource.DBConnection;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

/**
 * @author Best Point
 */
public class ConfirmDeposits extends TimerTask {

    private Vertx vertx;

    public ConfirmDeposits(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void run() {
        startSchedule();
    }

    // Functional interface for Java 8
    public interface DepositConfirmationHandler {
        void handle(double totalDeposits, double expectedAmount);
    }

    public void startSchedule() {
        System.out.println("::::::::::::::::::::::::: CONFIRM DEPOSITS TRANSACTIONS :::::::::::::::::::::::::");
        EventBus eventBus = vertx.eventBus();
        DBConnection conn = new DBConnection();
        Connection connection = null;

        try {
            System.out.println("Attempting to get database connection...");
            connection = conn.getConnection();
            System.out.println("Database connection established.");

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String todayDate = sdf.format(new Date());
            System.out.println("Today’s date: " + todayDate);

            // Fetch deposits from TaskBulkUpload
            String fetchBulkDeposits = "SELECT Id, AccountNumber, Deposit, DepositDate FROM TaskBulkUpload WHERE ConfirmationStatus = 0 AND CAST(DepositDate AS DATE) <= CAST(GETDATE() AS DATE)";
            System.out.println("Preparing bulk deposits query...");
            PreparedStatement bulkStmt = connection.prepareStatement(fetchBulkDeposits);
            System.out.println("Executing bulk deposits query...");
            ResultSet bulkResultSet = bulkStmt.executeQuery();
            System.out.println("Bulk deposits query executed.");

            while (bulkResultSet.next()) {
                String bulkId = bulkResultSet.getString("Id");
                String accountNumber = bulkResultSet.getString("AccountNumber");
                double expectedDeposit = bulkResultSet.getDouble("Deposit");
                String depositDate = bulkResultSet.getString("DepositDate");
                System.out.println("Processing bulk deposit - ID: " + bulkId + ", Account: " + accountNumber + ", Expected: " + expectedDeposit);

                // Run on Vert.x event loop
                vertx.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void v) {
                        processDeposit(eventBus, accountNumber, expectedDeposit, depositDate, new DepositConfirmationHandler() {
                            @Override
                            public void handle(double totalDeposits, double expectedAmount) {
                                try {
                                    if (totalDeposits >= expectedAmount) {
                                        updateConfirmationStatus(bulkId, "TaskBulkUpload", totalDeposits, true);
                                    } else {
                                        updateConfirmationStatus(bulkId, "TaskBulkUpload", totalDeposits, false);
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
            }

            // Fetch deposits from BusinessRealized
            String fetchBusinessRealizedDeposits = "SELECT br.Id, uts.AccountNumber, br.deposit, brdepositDate "
                    + "FROM BusinessRealized br "
                    + "JOIN UserTaskSubmissions uts ON br.id = uts.BusinessRealizedId "
                    + "WHERE br.ConfirmationStatus = 0 AND CAST(br.depositDate AS DATE) <= CAST(GETDATE() AS DATE)";
            System.out.println("Preparing business realized deposits query...");
            PreparedStatement businessStmt = connection.prepareStatement(fetchBusinessRealizedDeposits);
            businessStmt.setString(1, todayDate);
            System.out.println("Executing business realized deposits query...");
            ResultSet businessResultSet = businessStmt.executeQuery();
            System.out.println("Business realized deposits query executed.");

            while (businessResultSet.next()) {
                String businessId = businessResultSet.getString("Id");
                String accountNumber = businessResultSet.getString("AccountNumber");
                double expectedDeposit = businessResultSet.getDouble("deposit");
                String depositDate = businessResultSet.getString("depositDate");
                System.out.println("Processing business deposit - ID: " + businessId + ", Account: " + accountNumber + ", Expected: " + expectedDeposit);

                // Run on Vert.x event loop
                vertx.runOnContext(new Handler<Void>() {
                    @Override
                    public void handle(Void v) {
                        processDeposit(eventBus, accountNumber, expectedDeposit, depositDate, new DepositConfirmationHandler() {
                            @Override
                            public void handle(double totalDeposits, double expectedAmount) {
                                try {
                                    if (totalDeposits >= expectedAmount) {
                                        updateConfirmationStatus(businessId, "BusinessRealized", totalDeposits, true);
                                    } else {
                                        updateConfirmationStatus(businessId, "BusinessRealized", totalDeposits, false);
                                    }
                                } catch (SQLException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                });
            }

        } catch (Exception e) {
            System.out.println("Exception in startSchedule: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (connection != null && !connection.isClosed()) {
                    connection.close();
                    System.out.println("Database connection closed.");
                }
            } catch (SQLException e) {
                System.out.println("Error closing connection: " + e.getMessage());
                e.printStackTrace();
            }
            conn.closeConn();
        }
    }

    private void processDeposit(EventBus eventBus, String accountNumber, double expectedAmount, String depositDate, DepositConfirmationHandler handler) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        String formattedDate = sdf.format(depositDate);
        System.out.println("Sending event bus request for account: " + accountNumber + ", date: " + formattedDate);

        JsonObject fetchRequest = new JsonObject()
                .put("account", accountNumber)
                .put("start", formattedDate);

        eventBus.send("310000", fetchRequest, new Handler<AsyncResult<Message<JsonObject>>>() {
            @Override
            public void handle(AsyncResult<Message<JsonObject>> reply) {
                if (reply.succeeded()) {
                    JsonObject response = reply.result().body();
                    System.out.println("RESPONSE T24: " + response.encodePrettily());

                    if ("000".equals(response.getString("response"))) {
                        JsonArray transactions = response.getJsonArray("transactions");
                        System.out.println("RESPONSE T24 TRANSACTIONS: " + transactions.encodePrettily());
                        double totalDeposits = 0.0;

                        for (int i = 0; i < transactions.size(); i++) {
                            JsonObject txn = transactions.getJsonObject(i);
                            String description = txn.getString("description", "");
                            if ("+".equals(txn.getString("sign")) && !description.contains("AA")) {
                                totalDeposits += Double.parseDouble(txn.getString("amount"));
                            }
                        }
                        System.out.println("Total deposits calculated: " + totalDeposits);
                        handler.handle(totalDeposits, expectedAmount);
                    } else {
                        System.out.println("T24 response code not '000': " + response.getString("response"));
                        handler.handle(0.0, expectedAmount);
                    }
                } else {
                    System.out.println("Failed to fetch transactions for account: " + accountNumber + ", cause: " + reply.cause().getMessage());
                    handler.handle(0.0, expectedAmount);
                }
            }
        });
    }

    private void updateConfirmationStatus(String id, String tableName, double depositAmount, boolean updateStatus) throws SQLException {
        DBConnection conn = new DBConnection();
        Connection connection = null;
        try {
            connection = conn.getConnection();
            String updateQuery = updateStatus
                    ? "UPDATE " + tableName + " SET ConfirmationStatus = 1, DepositAmount = ? WHERE Id = ?"
                    : "UPDATE " + tableName + " SET DepositAmount = ? WHERE Id = ?";
            PreparedStatement updateStmt = connection.prepareStatement(updateQuery);
            updateStmt.setDouble(1, depositAmount);
            updateStmt.setString(2, id);

            int rowsUpdated = updateStmt.executeUpdate();
            if (rowsUpdated > 0) {
                if (updateStatus) {
                    System.out.println("Updated ConfirmationStatus and DepositAmount (" + depositAmount + ") for ID: " + id + " in table " + tableName);
                } else {
                    System.out.println("Updated DepositAmount (" + depositAmount + ") for ID: " + id + " in table " + tableName + " (ConfirmationStatus unchanged)");
                }
            } else {
                System.out.println("No rows updated for ID: " + id + " in table " + tableName);
            }
        } finally {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            conn.closeConn();
        }
    }

    // Test main method for standalone execution
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.eventBus().consumer("310000", new Handler<Message<Object>>() {
            @Override
            public void handle(Message<Object> message) {
                JsonObject request = (JsonObject) message.body();
                System.out.println("Received request on 310000: " + request.encodePrettily());
                JsonObject response = new JsonObject()
                    .put("response", "000")
                    .put("transactions", new JsonArray()
                        .add(new JsonObject().put("sign", "+").put("amount", "100.0").put("description", "Deposit"))
                    );
                message.reply(response);
            }
        });
        java.util.Timer timer = new java.util.Timer();
        timer.scheduleAtFixedRate(new ConfirmDeposits(vertx), 0, 60000);
    }
}