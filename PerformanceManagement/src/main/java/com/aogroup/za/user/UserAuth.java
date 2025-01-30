package com.aogroup.za.user;

import com.aogroup.za.adaptors.ESBRouter;
import com.aogroup.za.branch.BranchUtil;
import com.aogroup.za.makerchecker.MakerCheckerUtil;
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

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class UserAuth extends AbstractVerticle {

    private Logging logger;
    private static final Logger LOG = LoggerFactory.getLogger(ESBRouter.class);
    EventBus eventBus;

    @Override
    public void start(Future<Void> done) throws Exception {
        //System.out.println("deploymentId ESBRouter = " + vertx.getOrCreateContext().deploymentID());
        eventBus = vertx.eventBus();
        logger = new Logging();

        eventBus.consumer("900000", this::registration);
        eventBus.consumer("901000", this::login);
        eventBus.consumer("901500", this::otpVerification);
        eventBus.consumer("902000", this::forgotPassword);
        eventBus.consumer("903000", this::changePassword);
        eventBus.consumer("904000", this::fetchUsers);
        eventBus.consumer("905000", this::fetchUser);
        eventBus.consumer("906000", this::activateUser);
        eventBus.consumer("906500", this::deactivateUser);
        eventBus.consumer("907000", this::logout);
        eventBus.consumer("908000", this::updateUser);
        eventBus.consumer("910000", this::createRoleAndItsPermissions);
        eventBus.consumer("911000", this::updateRolePermissions);
        eventBus.consumer("912000", this::fetchRoles);
        eventBus.consumer("913000", this::fetchPermissions);
        eventBus.consumer("914000", this::fetchRolePermissions);
        eventBus.consumer("915000", this::fetchRelationshipOfficers);
        eventBus.consumer("916000", this::updateRelationshipOfficerPortfolio);
        eventBus.consumer("916500", this::approveUpdateRelationshipOfficerPortfolio);
        eventBus.consumer("999000", this::userValidator);
    }


    private void userValidator(Message<JsonObject> message){
        JsonObject data = message.body();
        String token = data.getString("token").trim();
        String serviceCode = data.getString("serviceCode").trim();

        byte[] tokenBytes =  Base64.getDecoder().decode(token.getBytes(StandardCharsets.UTF_8));
        String uuid = new String(tokenBytes, StandardCharsets.UTF_8);
        //System.out.println("TOKEN UUID \t "+uuid);

//        JsonObject jsonObject = new JsonObject(uuid);
//        //System.out.println("TOKEN Json \n"+jsonObject);


        UserUtil userUtil = new UserUtil();
        JsonObject userDetails = userUtil.fetchUserDetails("uuid",uuid);
//        //System.out.println("\n USER "+userDetails);
        if (!userDetails.getBoolean("successIndicator")){
//            //System.out.println("invalid User Token");
            userDetails.clear();
            userDetails
                    .put("responseCode","999")
                    .put("responseDescription","Error! User has been deactivated ");
            message.reply(userDetails);
            return;
        }


        if (userDetails.getString("status").equals("0")) {
            userDetails.clear();
            userDetails
                    .put("responseCode","999")
                    .put("responseDescription","Error! User has been deactivated ");
            message.reply(userDetails);
            return;
        }

        final JsonObject loginValidationDetails = userUtil.fetchLoginValidationDetails("uuid", uuid);
//        //System.out.println("Login Val "+loginValidationDetails);
        if ( !loginValidationDetails.getBoolean("successIndicator")) {
            userDetails.clear();
            userDetails
                    .put("responseCode","999")
                    .put("responseDescription","Error! User login details ");
            message.reply(userDetails);
            return;
        }

        if (loginValidationDetails.getString("changePassword").equals("1") && !serviceCode.equals("903000") && !serviceCode.equals("901500")) {
            userDetails.clear();
            userDetails
                    .put("responseCode","999")
                    .put("responseDescription","Error! Change Password to proceed");
            message.reply(userDetails);
            return;
        }

        if (loginValidationDetails.getString("otpVerified").equals("0") && !serviceCode.equals("901500") && !serviceCode.equals("901550")) {
            userDetails.clear();
            userDetails
                    .put("responseCode","999")
                    .put("responseDescription","Error! OTP not verified to proceed");
            message.reply(userDetails);
            return;
        }

        JsonArray usersBranchesDetails = userDetails.getJsonArray("userBranchesDetails");
        JsonArray usersBranchesJsonArray = userDetails.getJsonArray("userBranches");

        if (usersBranchesJsonArray.size() == 0) {
            userDetails.clear();
            userDetails
                    .put("responseCode","999")
                    .put("responseDescription","Error! User "+userDetails.getString("firstName")
                            + userDetails.getString("lastName") +" has not been allocated a branch");
            message.reply(userDetails);
            return;
        }

        String userBranchesString = "";
        for (int x = 0; x < usersBranchesJsonArray.size(); x++ ) {
            userBranchesString = userBranchesString + "'" + usersBranchesJsonArray.getString(x) + "',";
        }

        if (userBranchesString.endsWith(",")) {
            userBranchesString = userBranchesString.substring(0,userBranchesString.length() - 1);
        }


        String user_fullname = userDetails.getString("firstName") + " " +userDetails.getString("lastName");

        DeliveryOptions deliveryOptions = new DeliveryOptions()
                .addHeader("user", userDetails.getString("id"))
                .addHeader("user_uuid", uuid)
                .addHeader("user_name", user_fullname)
                .addHeader("user_branch_id", usersBranchesJsonArray.getString(0))
//                .addHeader("user_branch_name", userDetails.getString("branchName"))
                .addHeader("user_branches", userBranchesString);

        data.remove("username");
        data.remove("password");
        data.remove("token");
        data.remove("processingCode");
        data.remove("serviceCode");
        eventBus.send(serviceCode,data,deliveryOptions, sendToBus -> {
            if (sendToBus.succeeded()){
                //System.out.println("SUCCEEDED");
                JsonObject resObj = (JsonObject) sendToBus.result().body();
                message.reply(resObj);
            } else {
                System.err.println("Failed");
                data.clear();
                data
                        .put("responseCode", "999")
                        .put("responseDescription", "Error! Failed to process service "+sendToBus.result().address());
                message.reply(data);
//                message.fail(120,"Error! Failed to process service "+sendToBus.result().address());
            }
        });
//        message.reply(quickResponse);
    }

    private void registration(Message<JsonObject> message){
        JsonObject data = message.body();

        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"create_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        data.put("user",user);

        JsonObject quickResponse = new UserUtil().registerUser(data);

        if (quickResponse.containsKey("emailBody")) {
            DeliveryOptions deliveryOptions = new DeliveryOptions()
                    .addHeader("emailRecipient", quickResponse.getString("emailRecipient"))
                    .addHeader("emailSubject", quickResponse.getString("emailSubject"))
                    .addHeader("emailBody", quickResponse.getString("emailBody"));

            eventBus.send("SEND_EMAIL", quickResponse,deliveryOptions);
            quickResponse.remove("emailRecipient");
            quickResponse.remove("emailSubject");
            quickResponse.remove("emailBody");
        }
        message.reply(quickResponse);
    }

    private void updateUser(Message<JsonObject> message){
        JsonObject data = message.body();

        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"manage_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        data.put("user",user);

        JsonObject quickResponse = new UserUtil().updateUser(data);
        message.reply(quickResponse);
    }

    private void login(Message<JsonObject> message){
        JsonObject data = message.body();
        JsonObject quickResponse = new UserUtil().login(data);
        if (quickResponse.getString("responseCode").equals("000")) {
            eventBus.send("COMMUNICATION_ADAPTOR",quickResponse);
            quickResponse.remove("phonenumber");
            quickResponse.remove("msg");

            DeliveryOptions deliveryOptions = new DeliveryOptions()
                    .addHeader("emailRecipient", quickResponse.getString("emailRecipient"))
                    .addHeader("emailSubject", quickResponse.getString("emailSubject"))
                    .addHeader("emailBody", quickResponse.getString("emailBody"));

//            eventBus.send("SEND_EMAIL", quickResponse,deliveryOptions);
            quickResponse.remove("emailRecipient");
            quickResponse.remove("emailSubject");
            quickResponse.remove("emailBody");
        }
        message.reply(quickResponse);
    }

    private void otpVerification(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        String user_uuid = headers.get("user_uuid");

        data.put("user_uuid",user_uuid);
        JsonObject quickResponse = new UserUtil().otpVerification(data);
        quickResponse.put("userId", user);
        quickResponse.put("user_uuid", user_uuid);
        message.reply(quickResponse);

        eventBus.send("ACTIVITY_LOG",
                new JsonObject()
                .put("logName",message.address())
                        .put("description","OTP Validation")
                        .put("subject_type","Scope Branch")
                        .put("subject_id","1")
                        .put("description","OTP Validation")
                        .put("user",user)
                        .put("properties",data.getString("ip")));
    }
    
    private void logout(Message<JsonObject> message){
        JsonObject data = message.body();
        JsonObject quickResponse = new UserUtil().logout(data);
        message.reply(quickResponse);
    }

    private void forgotPassword(Message<JsonObject> message){
        JsonObject data = message.body();
        JsonObject quickResponse = new UserUtil().forgotPassword(data);
        if (quickResponse.getString("responseCode").equals("000")) {
            eventBus.send("COMMUNICATION_ADAPTOR",quickResponse);
            quickResponse.remove("phonenumber");
            quickResponse.remove("msg");
            
            DeliveryOptions deliveryOptions = new DeliveryOptions()
                    .addHeader("emailRecipient", quickResponse.getString("emailRecipient"))
                    .addHeader("emailSubject", quickResponse.getString("emailSubject"))
                    .addHeader("emailBody", quickResponse.getString("emailBody"));
            eventBus.send("SEND_EMAIL",quickResponse,deliveryOptions);
            quickResponse.remove("emailRecipient");
            quickResponse.remove("emailSubject");
            quickResponse.remove("emailBody");
        }
        message.reply(quickResponse);
    }

    private void changePassword(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");

        data.put("user",user);
        JsonObject quickResponse = new UserUtil().changePassword(data);
        if (quickResponse.getString("responseCode").equals("000")) {
            DeliveryOptions deliveryOptions = new DeliveryOptions()
                    .addHeader("emailRecipient", quickResponse.getString("emailRecipient"))
                    .addHeader("emailSubject", quickResponse.getString("emailSubject"))
                    .addHeader("emailBody", quickResponse.getString("emailBody"));
            eventBus.send("SEND_EMAIL",quickResponse,deliveryOptions);
            quickResponse.remove("emailRecipient");
            quickResponse.remove("emailSubject");
            quickResponse.remove("emailBody");
        }
        message.reply(quickResponse);
    }

    private void activateUser(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"manage_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        data.put("user", user);
        data.put("status", "1");
        data.put("isRO", "1");

        JsonObject quickResponse = new UserUtil().activateDeactivateUser(data);
        message.reply(quickResponse);
    }

    private void deactivateUser(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"manage_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        data.put("user", user);
        data.put("status", "0");
        data.put("isRO", "0");

        JsonObject quickResponse = new UserUtil().activateDeactivateUser(data);
        message.reply(quickResponse);
    }

    private void fetchUsers(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        String user_branches = headers.get("user_branches");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        data.clear();

//        JsonArray array = new UserUtil().fetchUsersWithUsersBranchesTable("ub.BranchId IN ("+user_branches+") AND 1","1");
//        if (array.size() == 0) {
//            data
//                    .put("responseCode", "999")
//                    .put("responseDescription", "Error");
//        } else {
//            data
//                    .put("responseCode", "000")
//                    .put("responseDescription", "Success");
//        }
//        data.put("data",array);
        // message.reply(data);

        JsonObject result = new UserUtil().fetchUsers("1","1");
        if (result.getJsonArray("data").size() == 0) {
            result
                    .put("responseCode", "999")
                    .put("responseDescription", "Error");
        } else {
            result
                    .put("responseCode", "000")
                    .put("responseDescription", "Success");
        }
        message.reply(result);
    }

    private void fetchRelationshipOfficers(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        String user_branches = headers.get("user_branches");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        data.clear();
        JsonArray array = new UserUtil().fetchUsersWithUsersBranchesTable("ub.BranchId IN ("+user_branches+") AND isRO","1");
        data.put("data",array);
        if (array.size() == 0) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error");
        } else {
            data
                    .put("responseCode", "000")
                    .put("responseDescription", "Success");
        }
        message.reply(data);
    }

    private void updateRelationshipOfficerPortfolio(Message<JsonObject> message){
        JsonObject data = message.body();
        String current_relationship_officer = data.getString("current_relationship_officer");
        String new_relationship_officer = data.getString("new_relationship_officer");
        JsonArray customers = data.getJsonArray("customers");

        MultiMap headers = message.headers();
        if (headers.isEmpty()) {
            //System.out.println("empty Header");
            message.fail(666, "Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        String user_branch_id = headers.get("user_branch_id");

        String[] fields = {"current_relationship_officer","new_relationship_officer"};
        for (String field: fields) {
            if (!data.containsKey(field) || data.getString(field) == null) {
                data
                        .put("responseCode", "999")
                        .put("responseDescription", "Error! Field "+field+" is invalid");
            }
        }

        if (!data.containsKey("customers") || data.getJsonArray("customers") == null) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Field customers is invalid");
        }

        JsonObject currentRO = new UserUtil().fetchUserDetails("uuid",current_relationship_officer);
        if (!currentRO.getBoolean("successIndicator")) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Current RO is invalid");
            message.reply(data);
            return;
        }
        String currentROName = currentRO.getString("firstName")+" "+currentRO.getString("lastName");

        JsonObject newRO = new UserUtil().fetchUserDetails("uuid",new_relationship_officer);
        if (!newRO.getBoolean("successIndicator")) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! New RO is invalid");
            message.reply(data);
            return;
        }

        String newROName = newRO.getString("firstName")+" "+newRO.getString("lastName");

        JsonObject payload = new JsonObject()
                .put("current_relationship_officer", current_relationship_officer)
                .put("current_relationship_officer_name", currentROName)
                .put("new_relationship_officer", new_relationship_officer)
                .put("new_relationship_officer_name", newROName)
                .put("customers", customers);
//                .put("customersDetails", new CustomerUtil().fetchCustomersFromJsonArray("c.id",customers));

        JsonObject mkchkr = new JsonObject()
                .put("serviceCode","916500")
                .put("module","UPDATE_PORTFOLIO")
                .put("payload",payload.toString())
                .put("user",user)
                .put("branch",user_branch_id);

//        data.clear();
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

    private void approveUpdateRelationshipOfficerPortfolio(Message<JsonObject> message){
        JsonObject data = message.body();
        logger.applicationLog(logger.logPreString() + "Error - " + data + "\n\n", "", 6);
//        String approval_id = data.getString("approval_id");
//        //System.out.println("XXX "+data);
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");

//        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"portfolio_transfer");
//        if (!hasPermission) {
//            data
//                    .put("responseCode", "999")
//                    .put("responseDescription", "Error! Unauthorised permission");
//            message.reply(data);
//            return;
//        }

        data.put("user", user);

//        JsonArray array = new MakerCheckerUtil().fetchAllMakerChecker("mk.Id",approval_id);
//        if (array.size() == 0) {
//            data
//                    .put("responseCode", "999")
//                    .put("responseDescription", "Error! Invalid Approval Id ");
//            message.reply(data);
//            return;
//        }

        JsonObject response = new JsonObject();
        response
                .put("responseCode", "000")
                .put("responseDescription", "Service is being processed.");
        message.reply(response);
        
        JsonObject quickResponse = new UserUtil().changeRelationshipOfficerPortfolio(data);
//        message.reply(data);
    }

    private void fetchUser(Message<JsonObject> message){
        JsonObject data = message.body();
        String user_email = data.getString("email");

        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        JsonObject userDetails = new UserUtil().fetchUserDetails("u.email", user_email);
        if (userDetails.getBoolean("successIndicator")) {
            userDetails.remove("uuid");
            userDetails
                    .put("responseCode", "000")
                    .put("responseDescription", "Success");
        } else {
            userDetails
                    .put("responseCode", "999")
                    .put("responseDescription", "Error");
        }
        userDetails.remove("successIndicator");

        message.reply(userDetails);
    }

    private void fetchRoles(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_roles");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        JsonObject response = new UserUtil().fetchRoles();
        if (response.getJsonArray("data").size() > 0 ) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Success");
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Failed to retrieve roles");
        }
        message.reply(response);
    }

    private void fetchPermissions(Message<JsonObject> message){
        JsonObject data = message.body();
        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_permissions");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        JsonObject response = new UserUtil().fetchPermissions();
        if (response.getJsonArray("data").size() > 0 ) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Success");
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Failed to retrieve permissions");
        }
        message.reply(response);
    }

    private void fetchRolePermissions(Message<JsonObject> message){
        JsonObject data = message.body();
        String role_id = data.getString("identifier");

        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_roles");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        JsonObject response = new UserUtil().fetchRolePermissions("rp.[role_id]",role_id);
        if (response.getJsonArray("data").size() > 0 ) {
            response
                    .put("responseCode", "000")
                    .put("responseDescription", "Success");
        } else {
            response
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Failed to retrieve permissions");
        }
        message.reply(response);
    }

    private void createRoleAndItsPermissions(Message<JsonObject> message){
        JsonObject data = message.body();

        MultiMap headers = message.headers();
        if (headers.isEmpty()){
            //System.out.println("empty Header");
            message.fail(666,"Unauthenticated User");
            return;
        }
        String user = headers.get("user");
        boolean hasPermission = new UserUtil().checkUserHasPermission("u.id",user,"view_users");
        if (!hasPermission) {
            data
                    .put("responseCode", "999")
                    .put("responseDescription", "Error! Unauthorised permission");
            message.reply(data);
            return;
        }

        JsonObject response = new UserUtil().addRolePermissions(data);
        message.reply(response);
    }

    private void updateRolePermissions(Message<JsonObject> message){
        JsonObject data = message.body();

        JsonObject response = new UserUtil().updateRolePermissions(data);
        message.reply(response);
    }
}
