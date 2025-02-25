/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.Transactions;

import com.aogroup.za.util.Utilities;
import com.instance.CBSService;
import com.instance.CBSUtility;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class SMETransactions extends AbstractVerticle {
    
    private Logging logger;
    EventBus eventBus;
    static int TIMEOUT_TIME = 45000;
    
    @Override
    public void start(Future<Void> start) throws Exception {
        System.out.println("deploymentId CustomerKYC =" + vertx.getOrCreateContext().deploymentID());
        
        eventBus = vertx.eventBus();
        logger = new Logging();
        
        eventBus.consumer("310000", this::fetchTransactions);
        eventBus.consumer("400000", this::postUploadedTransactions);
        eventBus.consumer("910000", this::validateTransactionDetails);
        eventBus.consumer("920000", this::validateInstitution);
        eventBus.consumer("930000", this::fetchInstitutions);
        eventBus.consumer("940000", this::fetchUsers);
    }
    
    private void fetchTransactions(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject response = new JsonObject();
        Utilities util = new Utilities();
        String type = "";
        JsonObject smeUserDetails = new JsonObject();
        
        smeUserDetails = util.fetchSmeUserDetails(data);
        if (!smeUserDetails.isEmpty()) {
            type = smeUserDetails.getString("type");
        } else {
            type = "";
        }
        
        String start = data.getString("start");
        
        String[] startSplit = start.split("-");
        if (startSplit[0].length() == 1) {
            start = "0" + startSplit[0] + "-" + startSplit[1] + "-" + startSplit[2];
        }
        
        String[] startSplit2 = start.split("-");
        if (startSplit2[1].length() == 1) {
            start = startSplit2[0] + "-" + "0" + startSplit2[1] + "-" + startSplit2[2];
        }
        
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        LocalDate startDateTime = LocalDate.parse(start, dateTimeFormatter);
        
        DateTimeFormatter outputDateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String startDateFormattedString = startDateTime.format(outputDateTimeFormatter);

//        data.put("startDate",startDateFormattedString);
        data.put("startDate", startDateFormattedString);
        CBSService cbs = new CBSService(data);
        String responseString = cbs.statement();
        
        CBSUtility cbsUtil = new CBSUtility();
        //format the transactions in orderly manner
        response = cbsUtil.statement(responseString);
        
        if (response.getString("response").equalsIgnoreCase("999")) {
            message.reply(response);
        } else {
            JsonObject jo = null;
            
            JsonArray stmnt = new JsonArray();
            
            JsonArray transactions = response.getJsonArray("transactions");
            String fullOpeningBal = "";
            String fullClosingBal = "";
            if (transactions.size() > 0) {
                String openingBalance = transactions.getJsonObject(0).getString("Details");
                
                int closingBalanceId = transactions.size() - 1;
                String closingBalance = transactions.getJsonObject(closingBalanceId).getString("Details");
                
                String[] splitOpeningBalance = openingBalance.split("\\s+");
                String[] splitClosingBalance = closingBalance.split("\\s+");
                
                fullOpeningBal = splitOpeningBalance[0] + " " + splitOpeningBalance[1] + "    " + splitOpeningBalance[splitOpeningBalance.length - 1];
                fullClosingBal = splitClosingBalance[0] + " " + splitClosingBalance[1] + "    " + splitClosingBalance[splitClosingBalance.length - 1];
                
                for (int i = 1; i < closingBalanceId; i++) {
                    JsonObject res = new JsonObject();
                    JsonObject obj = transactions.getJsonObject(i);
                    
                    String[] arrOfStr = obj.getString("Details").split(" ", obj.getString("Details").length());
                    
                    String desc = "";
                    for (int x = 1; x < arrOfStr.length - 2; x++) {
                        desc += arrOfStr[x];
                    }
                    
                    res.put("amount", arrOfStr[arrOfStr.length - 2]);
                    res.put("description", desc);
                    res.put("date", arrOfStr[0]);
                    res.put("sign", arrOfStr[arrOfStr.length - 1]);
                    stmnt.add(res);
                    
                }
            }

//        String[] openingBalanceSplit = openingBalance.split("\\s+");
//        String[] closingBalanceSplit = closingBalance.split("\\s+");
            JsonArray debits = new JsonArray();
            if (type.trim().equalsIgnoreCase("institution staff")) {
                
                JsonObject jsonObject = null;
                for (Object obj : stmnt) {
                    jsonObject = (JsonObject) obj;
                    
                    if (jsonObject.getString("sign").trim().equalsIgnoreCase("+")) {
                        debits.add(jsonObject);
                        
                    }
                }
            }
            if (type.equalsIgnoreCase("institution staff")) {
                response
                        .put("transactions", debits)
                        .put("opening_balance", fullOpeningBal)
                        .put("closing_balance", fullClosingBal);
                
                message.reply(response);
            } else {
                response
                        .put("transactions", stmnt)
                        .put("opening_balance", fullOpeningBal)
                        .put("closing_balance", fullClosingBal);
                message.reply(response);
            }
            
        }
        
    }
    
    private void postUploadedTransactions(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject response = new JsonObject();
        Utilities util = new Utilities();
        if (!data.getString("batchId").isEmpty()) {
            response.put("response", "000");
            response.put("responseDescription", "batch is being processed.");
            message.reply(response);
            JsonObject obj = new JsonObject();
            JsonArray uploadedTransactions = util.fetchBatchTransactions(data.getString("batchId"));
            
            for (Object o : uploadedTransactions) {
                
                JsonObject ob = (JsonObject) o;
                
                boolean exists = false;
                exists = util.checkIfExists("mpesa_c2b", "TransID", ob.getString("transID"));
                if (!exists) {
                    obj = util.fetchAccountUsingCode(ob.getString("code"));
                    ob.put("billRefNumber", obj.getString("account"));
                    int save = util.saveUploadRequest(ob);
                    
                    if (save == 1) {
                        //send to cbs
                        String adminNo = ob.getString("narration");
                        String account = ob.getString("billRefNumber");
                        String phoneNumber = ob.getString("phoneNumber");
                        String TransAmount = ob.getString("amount");
                        String mpesaRef = ob.getString("transID");
                        JsonObject tran = new JsonObject();
                        tran.put("adminNo", adminNo).put("phoneNumber", phoneNumber)
                                .put("TransAmount", TransAmount)
                                .put("mpesaRef", mpesaRef)
                                .put("shortCode", ob.getString("shortCode"));
                        
                        tran.put("account", obj.getString("account"));
                        
                        System.out.println("TRAN ::: " + tran);
                        
                        util.updateTransaction("mpesa_c2b", "status", "3", "TransID", ob.getString("transID"));
                        CBSService cbs = new CBSService(tran);
                        String t24response = cbs.sendC2BToT24();
                        
                        CBSUtility cbsUtil = new CBSUtility();
                        JsonObject t24Result = cbsUtil.deposits(t24response);
                        
                        if (t24Result.getString("successIndicator").equalsIgnoreCase("success")) {
                            
                            util.updateTransactionAsIs("mpesa_c2b", "FTReference", t24Result.getString("transactionId"),
                                    "status", "1", "ConfirmationStatus", t24Result.getString("successIndicator"), "TransID", ob.getString("transID"));
                            
                            String time = util.getTodayStringTime();
                            JsonObject sender = new JsonObject();
                            String accNo = "";
                            try {
                                accNo = util.maskString(account, 2, 8, 'x');
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            
                            String msg = "";
                            JsonObject recepient = new JsonObject();
                            if (!obj.getString("phone").contains("*")) {
                                
                                if (ob.getString("narration").equalsIgnoreCase("not provided")) {
                                    if (obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    } else {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " " + accNo + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    }
                                    
                                    if (!obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        String rsms = "Dear Member, You have received Ksh. " + tran.getString("TransAmount") + " from " + tran.getString("phoneNumber") + "  "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        recepient.put("phonenumber", obj.getString("phone"));
                                        recepient.put("msg", rsms);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                        recepient.put("phonenumber", obj.getString("altPhone"));
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                    }
                                } else {
                                    if (obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " " + " for " + ob.getString("narration") + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    } else {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " " + accNo + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    }
                                    
                                    if (!obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        String rsms = "Dear Member, You have received Ksh. " + tran.getString("TransAmount") + " for " + ob.getString("narration") + " from " + tran.getString("phoneNumber") + "  "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        recepient.put("phonenumber", obj.getString("phone"));
                                        recepient.put("msg", rsms);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                        recepient.put("phonenumber", obj.getString("altPhone"));
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                    }
                                }
                                
                            }
                            
                        } else {
                            if (t24Result.containsKey("messages") && t24Result.getString("messages").contains("Maximum T24 users already signed on (250) DURING SIGN ON PROCESS")) {
                                util.updateTransactionMocash("bulk_deposits", "batch_status", "4", "trans_id", ob.getString("transID"), "batch_status", "1");
                                
                            } else {
                                util.updateTransactionAsIs("mpesa_c2b", "FTReference", t24Result.getString("transactionId"),
                                        "status", "2", "ConfirmationStatus", t24Result.getString("successIndicator"), "TransID", ob.getString("transId"));
                                
                            }
                            
                        }
                        
                    } else {
                        
                    }
                } else {
                    boolean multiple = false;
                    multiple = util.checkIfExistsMultiple("mpesa_c2b", "TransID", ob.getString("transID"),
                            "ConfirmationStatus", "T24error", "status", "2");
                    
                    if (multiple) {
                        //send to cbs
                        obj = util.fetchAccountUsingCode(ob.getString("code"));
                        
                        String adminNo = ob.getString("narration");
                        String account = obj.getString("account");
                        String phoneNumber = ob.getString("phoneNumber");
                        String TransAmount = ob.getString("amount");
                        String mpesaRef = ob.getString("transID");
                        JsonObject tran = new JsonObject();
                        tran.put("adminNo", adminNo).put("account", account)
                                .put("phoneNumber", phoneNumber)
                                .put("TransAmount", TransAmount)
                                .put("transID", mpesaRef);

//                        obj = util.fetchAccount(account);
                        util.updateTransaction("mpesa_c2b", "status", "3", "TransID", ob.getString("transID"));
                        CBSService cbs = new CBSService(tran);
                        String t24response = cbs.sendC2BToT24();
                        
                        CBSUtility cbsUtil = new CBSUtility();
                        JsonObject t24Result = cbsUtil.deposits(t24response);
                        
                        if (t24Result.getString("successIndicator").equalsIgnoreCase("success")) {
                            
                            util.updateTransactionAsIs("mpesa_c2b", "FTReference", t24Result.getString("transactionId"),
                                    "status", "1", "ConfirmationStatus", t24Result.getString("successIndicator"), "TransID", ob.getString("transID"));
                            
                            String time = util.getTodayStringTime();
                            JsonObject sender = new JsonObject();
                            String accNo = "";
                            try {
                                accNo = util.maskString(account, 2, 8, 'x');
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            
                            String msg = "";
                            JsonObject recepient = new JsonObject();
                            if (!obj.getString("phone").contains("*")) {
                                if (ob.getString("narration").equalsIgnoreCase("not provided")) {
                                    if (obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("transID") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    } else {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " " + accNo + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("transID") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    }
                                    
                                    if (!obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        String rsms = "Dear Member, You have received Ksh. " + tran.getString("TransAmount") + " from " + tran.getString("phoneNumber") + "  "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("transID") + "";
                                        recepient.put("phonenumber", obj.getString("phone"));
                                        recepient.put("msg", rsms);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                        recepient.put("phonenumber", obj.getString("altPhone"));
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                    }
                                } else {
                                    if (obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " " + " for " + ob.getString("narration") + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("transID") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    } else {
                                        msg = "Dear Member, you have sent Ksh. " + tran.getString("TransAmount") + " to " + obj.getString("name") + " " + accNo + " "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("mpesaRef") + "";
                                        sender.put("phonenumber", tran.getString("phoneNumber"));
                                        sender.put("msg", msg);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", sender);
                                    }
                                    
                                    if (!obj.getString("phone").equalsIgnoreCase("254709253000")) {
                                        String rsms = "Dear Member, You have received Ksh. " + tran.getString("TransAmount") + " for " + ob.getString("narration") + " from " + tran.getString("phoneNumber") + "  "
                                                + "on " + time + ". Mpesa Ref " + tran.getString("transID") + "";
                                        recepient.put("phonenumber", obj.getString("phone"));
                                        recepient.put("msg", rsms);
                                        
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                        recepient.put("phonenumber", obj.getString("altPhone"));
                                        eventBus.send("COMMUNICATION_ADAPTOR", recepient);
                                    }
                                }
                                
                            }
                            
                        } else {
                            if (t24Result.containsKey("messages") && t24Result.getString("messages").contains("Maximum T24 users already signed on (250) DURING SIGN ON PROCESS")) {
                                util.updateTransactionMocash("bulk_deposits", "batch_status", "4", "trans_id", ob.getString("transID"), "batch_status", "1");
                                
                            } else {
                                util.updateTransactionAsIs("mpesa_c2b", "FTReference", t24Result.getString("transactionId"),
                                        "status", "2", "ConfirmationStatus", t24Result.getString("successIndicator"), "TransID", ob.getString("transId"));
                                
                            }
                        }
                    }
                }
                
            }
            util.updateTransactionMocash(
                    "bulk_deposits", "batch_status", "2", "batch_id", data.getString("batchId"), "batch_status", "1");
            
        }
        int countRecs = util.fetchRemainingCount(data.getString("batchId"));
        if (countRecs
                == 0) {
            util.updateTransactionMocash("deposit_file_upload", "batch_status", "2", "batch_id", data.getString("batchId"), "batch_status", "1");
            String msg = "";
            JsonObject sms = new JsonObject();
            String rsms = "Dear user, batch processing is complete.";
            sms.put("phonenumber", data.getString("phoneNumber"));
            sms.put("msg", rsms);
            
            eventBus.send("COMMUNICATION_ADAPTOR", sms);
        } else {
            response.put("response", "999");
            response.put("responseDescription", "batch not supplied.");
            message.reply(response);
            
        }
    }
    
    private void validateTransactionDetails(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject result = new JsonObject();
        Utilities util = new Utilities();
        boolean exists = false;
        boolean code = false;
        exists = util.checkIfExists("mpesa_c2b", "TransID", data.getString("mpesaRef"), "status", "1");
//        code = util.checkIfExists("institution", "code", data.getString("code"));
        if (exists) {
            result.put("response", "000");
            result.put("reaponseDescription", "Transaction exists.");
            message.reply(result);
        } else {
            result.put("response", "999");
            result.put("reaponseDescription", "Institution doesn't exists.");
            message.reply(result);
//            System.out.println("CODE :::: " +code);
//            if (code) {
//                result.put("response", "999");
//                result.put("reaponseDescription", "Institution doesn't exists.");
//                message.reply(result);
//            } else {
//                result.put("response", "007");
//                result.put("reaponseDescription", "Institution doesn't exists.");
//                message.reply(result);
//            }

        }
        
    }
    
    private void validateInstitution(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject result = new JsonObject();
        Utilities util = new Utilities();
        boolean exists = false;
        exists = util.checkIfExists("institution", "code", data.getString("code"), "status", "1");
        if (exists) {
            result.put("response", "000");
            result.put("responseDescription", "Institution exists.");
            message.reply(result);
        } else {
            result.put("response", "999");
            result.put("responseDescription", "Institution does not exists.");
            message.reply(result);
        }
    }
    
    private void fetchInstitutions(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject response = new JsonObject();
        Utilities util = new Utilities();
        response = util.fetchInstitutions();
        message.reply(response);
    }
    
    private void fetchUsers(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject response = new JsonObject();
        Utilities util = new Utilities();
        response = util.fetchUsers();
        message.reply(response);
    }
    
}
