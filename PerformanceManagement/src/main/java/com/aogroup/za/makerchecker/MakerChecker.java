package com.aogroup.za.makerchecker;

import com.aogroup.za.adaptors.ESBRouter;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import log.Logging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MakerChecker extends AbstractVerticle {

    private Logging logger;
    private static final Logger LOG = LoggerFactory.getLogger(ESBRouter.class);
    EventBus eventBus;

    @Override
    public void start(Future<Void> done) throws Exception {
        //System.out.println("deploymentId ESBRouter = " + vertx.getOrCreateContext().deploymentID());
        eventBus = vertx.eventBus();
        logger = new Logging();

        eventBus.consumer("800000", this::createMakerChecker);
        eventBus.consumer("801000", this::validateMakerChecker);
        eventBus.consumer("802000", this::fetchAllMakerChecker);
//        eventBus.consumer("802000", this::fetchAllMakerChecker);
    }

    private void createMakerChecker(Message<JsonObject> message) {
        JsonObject data = message.body();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");
//        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "view_maker_checker");
//        if (!hasPermission) {
//            data
//                    .put("responseCode", "999")
//                    .put("responseDescription", "Error! Unauthorised permission");
//            message.reply(data);
//            return;
//        }

        data
                .put("user", user)
                .put("branch", "5A86B167-AADB-458C-A675-8E8621A00EF0");

//        JsonObject response = new MakerChekerUtil().createMakerChecker(data);
        if (new MakerCheckerUtil().createMakerChecker(data)) {
            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success! Request Pending approval");
        } else {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Failed to execute data");
        }

        message.reply(data);
    }

    private void validateMakerChecker(Message<JsonObject> message) {
        JsonObject data = message.body();
        String approval_id = data.getString("approval_id");
        String actionFlag = data.getString("action_flag");

        if (!actionFlag.equals("0") && !actionFlag.equals("1")) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Invalid Action Flag ");
            message.reply(data);
            return;
        }

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");
//        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "view_maker_checker");
//        if (!hasPermission) {
//            data
//                    .put("responseCode", "999")
//                    .put("responseDescription", "Error! Unauthorised permission");
//            message.reply(data);
//            return;
//        }

        data.put("user", user);

        JsonArray array = new MakerCheckerUtil().fetchAllMakerChecker("mk.Id", approval_id);
//        //System.out.println(array);
        if (array.size() == 0) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Invalid Approval Id ");
            message.reply(data);
            return;
        }

        JsonObject approvalDetails = array.getJsonObject(0);
        String serviceCode = approvalDetails.getString("serviceCode");
        String status = approvalDetails.getString("status");
        String createdBy = approvalDetails.getString("createdBy");
        String approvedBy = approvalDetails.getString("approvedBy");
        JsonObject payload = approvalDetails.getJsonObject("payload");
//        JsonObject payload = new JsonObject(approvalDetails.getString("payload"));

        // OR check status
        if (approvedBy != null) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Request has been approved ");
            message.reply(data);
            return;
        }

        if (user.equals(createdBy)) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Cannot approve what you initiated ");
            message.reply(data);
            return;
        }

        DeliveryOptions deliveryOptions = new DeliveryOptions()
                .addHeader("user", user);

        if (actionFlag.equals("0")) {
            if (serviceCode.equals("830000")) {
                eventBus.send("825000", payload, deliveryOptions, responseMessage -> {
                    if (responseMessage.failed()) {
                        data
                                .put("responseCode", "999")
                                .put("responseDescription", "Error! Execution failed ");
                        message.reply(data);
                    } else {
                        JsonObject replyData = (JsonObject) responseMessage.result().body();
                        if (replyData.getString("responseCode").equals("000")) {
                            new MakerCheckerUtil().validateMakerChecker(data);
                        }
                        message.reply(replyData);
                    }
                });
            } else if (serviceCode.equals("267000")) {
                eventBus.send("267500", payload, deliveryOptions, responseMessage -> {
                    if (responseMessage.failed()) {
                        data
                                .put("responseCode", "999")
                                .put("responseDescription", "Error! Execution failed ");
                        message.reply(data);
                    } else {
                        JsonObject replyData = (JsonObject) responseMessage.result().body();
                        if (replyData.getString("responseCode").equals("000")) {
                            new MakerCheckerUtil().validateMakerChecker(data);
                        }
                        message.reply(replyData);
                    }
                });
            }
            // REJECTED
            new MakerCheckerUtil().validateMakerChecker(data);
//            //System.out.println(" Rejected "+ data);
            message.reply(data);
            return;
        }

//                .addHeader("user_uuid", uuid)
//                .addHeader("user_name", user_fullname)
//                .addHeader("user_branch_id", userDetails.getString("branch"))
//                .addHeader("user_branch_name", userDetails.getString("branchName"))
//                .addHeader("scope", scopeLevel)
//                .addHeader("scope_id", scopeID);
        logger.applicationLog(logger.logPreString() + "Error - " + payload + "\n\n", "", 6);
        eventBus.send(serviceCode, payload, deliveryOptions, responseMessage -> {
            if (responseMessage.failed()) {
                data
                        .put("responseCode", "999")
                        .put("responseDescription", "Error! Execution failed ");
                message.reply(data);
            } else {
                JsonObject replyData = (JsonObject) responseMessage.result().body();
                if (replyData.getString("responseCode").equals("000")) {
                    new MakerCheckerUtil().validateMakerChecker(data);
                }
                message.reply(replyData);
            }
        });

    }

    private void fetchAllMakerChecker(Message<JsonObject> message) {
        JsonObject data = message.body();
        String serviceCode = data.getString("service").trim();

        String serviceCodeSearch = "";
        if (serviceCode.isEmpty()) {
            serviceCodeSearch = "1";
            serviceCode = "1";
        } else {
            serviceCodeSearch = "ServiceCode";
        }

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        String user_branches = headers.get("user_branches");
//        String scope = headers.get("scope");
//        String scopeId = headers.get("scope_id");
//        //System.out.println("SCOPE "+ scope + " id "+scopeId);
//        if (scope.equals("ALL")) {
//            scope = "1";
//            scopeId = "1";
//        } else if (scope.equals("REGION")) {
//            scope = "b.[RegionId]";
//        } else {
//            scope = "mk.[BranchId]";
//        }
//        String scopeMsg = " "+scope+" = '"+scopeId+"'";
//        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id", user, "view_maker_checker");
//        if (!hasPermission) {
//            data
//                    .put("responseCode", "999")
//                    .put("responseDescription", "Error! Unauthorised permission");
//            message.reply(data);
//            return;
//        }

        JsonArray array = new MakerCheckerUtil().fetchAllMakerChecker(" ApprovedBy IS NULL AND mk.BranchId IN (" + user_branches + ") AND " + serviceCodeSearch, serviceCode);
        if (array.size() > 0) {
            data.clear();
            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success! ");
        } else {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! No pending approvals");
        }

        data.put("data", array);
        message.reply(data);
    }

    private void updateRelationshipPortfolio(Message<JsonObject> message) {
        JsonObject data = message.body();

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");

        String[] fields = {"current_relationship_officer", "new_relationship_officer", "customers"};
        for (String field : fields) {
            if (!data.containsKey(field) || data.getString(field) == null) {
                data
                        .put("responseCode", "999")
                        .put("responseDescription", "Error! Field " + field + " is invalid");
            }
        }

        JsonObject payload = new JsonObject()
                .put("current_relationship_officer", data.getString("current_relationship_officer"))
                .put("new_relationship_officer", data.getString("new_relationship_officer"))
                .put("customers", new JsonArray(data.getValue("customers").toString()));

        JsonObject mkchkr = new JsonObject()
                .put("serviceCode", message.replyAddress() + "->" + message.address())
                .put("module", "UPDATE_RO")
                .put("payload", payload)
                .put("user", user);

        if (new MakerCheckerUtil().createMakerChecker(mkchkr)) {
            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success! Request Pending approval");
        } else {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Failed to execute data");
        }

        message.reply(data);
    }
}
