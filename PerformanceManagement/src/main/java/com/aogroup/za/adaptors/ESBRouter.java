/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.adaptors;

import com.aogroup.za.branch.BranchUtil;
import com.aogroup.za.datasource.DBConnection;
import com.aogroup.za.makerchecker.MakerCheckerUtil;
import com.aogroup.za.reports.ReportsUtil;
import com.aogroup.za.user.UserUtil;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import log.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author nathan
 */
public class ESBRouter extends AbstractVerticle {

    private Logging logger;
    private static final Logger LOG = LoggerFactory.getLogger(ESBRouter.class);
    static int TIMEOUT_TIME = 25000;
    EventBus eventBus;

    @Override
    public void start(Future<Void> done) throws Exception {
        //System.out.println("deploymentId ESBRouter = " + vertx.getOrCreateContext().deploymentID());
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        eventBus.consumer("110500", this::createBranch);
        eventBus.consumer("117500", this::updateBranch);
        eventBus.consumer("118500", this::activateBranch);
        eventBus.consumer("119500", this::deactivateBranch);
        eventBus.consumer("111000", this::fetchBranches);
        eventBus.consumer("113000", this::fetchBranch);
       
        
        eventBus.consumer("501000", this::createReportCategories);
        eventBus.consumer("502000", this::createReports);
        eventBus.consumer("503000", this::fetchAllReports);
        eventBus.consumer("504000", this::fetchAllReportCategories);
    }

    private void createBranch(Message<JsonObject> message) {
        JsonObject data = message.body();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");

        data.put("user",user);

        new BranchUtil().createBranch(data);

        message.reply(data);
    }

    private void updateBranch (Message<JsonObject> message) {
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");

        data.put("user", user);

        new BranchUtil().updateBranch(data);
        message.reply(data);
    }

    private void activateBranch(Message<JsonObject> message) {
        JsonObject data = message.body();
        String branchId = data.getString("branch_id");

        data.clear();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");


        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "activate_branch");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        String sql = "UPDATE Branches SET IsEnabled = ? WHERE Id = ?";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, "1");
            preparedStatement.setString(2, branchId);
            preparedStatement.executeUpdate();

            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success! Activated");

        } catch (SQLException throwables) {
            data
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
        message.reply(data);
    }

    private void deactivateBranch(Message<JsonObject> message) {
        JsonObject data = message.body();
        String branchId = data.getString("branch_id");

        data.clear();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");


        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "deactivate_branch");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        String sql = "UPDATE Branches SET IsEnabled = ? WHERE Id = ?";
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            preparedStatement.setString(1, "0");
            preparedStatement.setString(2, branchId);
            preparedStatement.executeUpdate();

            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success! Deactivated");

        } catch (SQLException throwables) {
            data
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
        message.reply(data);
    }

    private void fetchBranches(Message<JsonObject> message) {
        JsonObject input = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "view_branch");
        if (!hasPermission) {
            input
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(input);
            return;
        }

        JsonObject data = new BranchUtil().fetchBranches();

        if (data.getBoolean("successIndicator")) {
            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success!");
        } else {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unable to fetch data");
        }

        data.remove("successIndicator");

        message.reply(data);
    }

    private void fetchBranch(Message<JsonObject> message) {
        JsonObject data = message.body();
        String uuid = data.getString("branch_id");

        data.clear();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "view_branch");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        JsonObject response = new BranchUtil().fetchBranch("B.Id", uuid);

        if (response.getBoolean("successIndicator")) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Success!");
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Failed to fetch branch");
        }

        response.remove("successIndicator");

        data.put("data", response);

        message.reply(response);
    }
    
    private void createReportCategories(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject response = new JsonObject();
        ReportsUtil rUtil = new ReportsUtil();
        
        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        
        String user = headers.get("user");
        
        data.put("user", user);
        
        int i = rUtil.createReportCategory(data);
        
        if (i != 0) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Successfully created report category.");
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to create report category.");
        }
 
        message.reply(response);
    }

    private void createReports(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject response = new JsonObject();
        ReportsUtil rUtil = new ReportsUtil();
        
        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        
        String user = headers.get("user");
        
        data.put("user", user);
        
        int i = rUtil.createReport(data);
        
        if (i != 0) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Successfully created report.");
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to create report.");
        }
        
        message.reply(response);
    }

    private void fetchAllReports(Message<JsonObject> message) {
        JsonObject data = message.body();
        ReportsUtil rUtil = new ReportsUtil();
        JsonObject response = new JsonObject();
        
        JsonArray reports = rUtil.fetchReports();
        
        if (reports.size() > 0) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Successfully fetched all reports.")
                    .put("data", reports);
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to fetch reports.");
        }
        
        message.reply(response);
    }

    private void fetchAllReportCategories(Message<JsonObject> message) {
        JsonObject data = message.body();
        ReportsUtil rUtil = new ReportsUtil();
        JsonObject response = new JsonObject();
        
        JsonArray reportCategories = rUtil.fetchReportCategories();
        
        if (reportCategories.size() > 0) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Successfully fetched all report categories.")
                    .put("data", reportCategories);
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Failed to fetch report categories.");
        }
        
        message.reply(response);
    }

}
