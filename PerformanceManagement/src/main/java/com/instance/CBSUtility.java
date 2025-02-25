/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.instance;

import com.aogroup.za.util.Utilities;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Iterator;
import javax.management.StringValueExp;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

/**
 *
 * @author Ronald.Langat
 */
public class CBSUtility {

    public CBSUtility() {
    }

    public JsonObject agentAccounts(String customerResponse) {
        JsonObject result = new JsonObject();

        String response = customerResponse;
        response = removeExtra_ns(response);

        JSONObject data = XML.toJSONObject(response);
        JSONObject envelope = data.getJSONObject("S:Envelope");
        JSONObject body = envelope.getJSONObject("S:Body");
        JSONObject agentDetailsDesponse = body.getJSONObject("SearchAgentFloatAccntsResponse");
        JSONObject agentDetails = agentDetailsDesponse.getJSONObject("USLAGENTFLOATType");
        JSONObject agentDetailsType = agentDetails.getJSONObject("gUSLAGENTFLOATDetailType");

        Object agentArray = agentDetailsType.get("mUSLAGENTFLOATDetailType");

        if (agentArray instanceof JSONObject) {
            String agentArrayString = agentArray.toString();
            JsonObject agentAccounts = new JsonObject(agentArrayString);

            result.put("id", String.valueOf(agentAccounts.getValue("ID")));
            result.put("companyCode", String.valueOf(agentAccounts.getValue("CompanyCode")));
            result.put("category", agentAccounts.getString("CATEGORY"));
        } else if (agentArray instanceof JSONArray) {
            String agentArrayString = agentArray.toString();
            JsonArray agentAccounts = new JsonArray(agentArrayString);

            // Iterate in this way in order to get kyc
            JsonObject jsonObject = null;
            for (Object obj : agentAccounts) {
                jsonObject = (JsonObject) obj;
            }

            if (jsonObject != null) {
                result.put("id", String.valueOf(jsonObject.getValue("ID")));
                result.put("companyCode", String.valueOf(jsonObject.getValue("CompanyCode")));
                result.put("category", jsonObject.getString("CATEGORY"));
            }
            result.put("Accounts", agentAccounts);
        }
        return result;
    }

    public JsonObject getCustomerAccounts(String customerResponse) {
        JsonObject result = new JsonObject();

        String response = customerResponse;
        response = removeExtra_ns(response);

        JSONObject data = XML.toJSONObject(response);
        JSONObject envelope = data.getJSONObject("S:Envelope");
        JSONObject body = envelope.getJSONObject("S:Body");
        JSONObject custDetailsDesponse = body.getJSONObject("AgentCustomerSearchResponse");
        JSONObject status = custDetailsDesponse.getJSONObject("Status");
        if (status.getString("successIndicator").equalsIgnoreCase("T24Error")) {
            result.put("response", "999");
            result.put("responseDescription", "customer details not found");
        } else {
            JSONObject custDetails = custDetailsDesponse.getJSONObject("JSLAGENTSEARCHCUSTOMERType");
            JSONObject custDetailsType = custDetails.getJSONObject("gJSLAGENTSEARCHCUSTOMERDetailType");
            JSONObject mCUSISAIDDetailType = custDetailsType.getJSONObject("mJSLAGENTSEARCHCUSTOMERDetailType");

            String mCUSISAIDDetailTypeStr = mCUSISAIDDetailType.toString();

            JsonObject custObject = new JsonObject(mCUSISAIDDetailTypeStr);

//        result.put("branchCode", String.valueOf(custObject.getValue("BranchID")));
            JSONObject json = XML.toJSONObject(custObject.getString("Accounts"));
            String jsonString = json.toString(4);
            JsonObject jo = new JsonObject(jsonString);
            JsonArray filteredAccounts = new JsonArray();

            Iterator itr = jo.getJsonArray("account").iterator();
            while (itr.hasNext()) {
                JsonObject obj = (JsonObject) itr.next();
                String code = String.valueOf(obj.getInteger("category"));
                if (code.equals("1001") || code.equals("6001") || code.equals("6003")
                        || code.equals("6005") || code.equals("6010") || code.equals("6012")
                        || code.equals("6015")) {
                    filteredAccounts.add(obj);
                }
            }

            if (jo.getJsonArray("account").size() > 0) {
                result.put("accounts", filteredAccounts);
            }
        }

        return result;
    }

    public JsonObject customerDetail(String customerResponse) {
        JsonObject result = new JsonObject();

        String response = customerResponse;
        response = removeExtra_ns(response);

        JSONObject data = XML.toJSONObject(response);
        JSONObject envelope = data.getJSONObject("S:Envelope");
        JSONObject body = envelope.getJSONObject("S:Body");
        JSONObject custDetailsDesponse = body.getJSONObject("ISACUSTOMERDETAILSResponse");
        JSONObject custDetails = custDetailsDesponse.getJSONObject("CUSTDETAILSType");
        JSONObject custDetailsType = custDetails.getJSONObject("gCUSTDETAILSDetailType");

        Object custArray = custDetailsType.get("mCUSTDETAILSDetailType");

        if (custArray instanceof JSONObject) {
            String custArrayString = custArray.toString();
            JsonObject customerAccounts = new JsonObject(custArrayString);

            result.put("CustomerName", customerAccounts.getString("AccountTitle"));
            result.put("IdNumber", String.valueOf(customerAccounts.getValue("IdNumber")));
            result.put("PhoneNumber", String.valueOf(customerAccounts.getValue("PhoneNumber")));
            result.put("CompanyCode", String.valueOf(customerAccounts.getValue("CompanyCode")));
            result.put("AccountBalance", String.valueOf(customerAccounts.getValue("AccountBalance")));
            result.put("response", "000");
            result.put("responseDescription", "success");
        } else if (custArray instanceof JSONArray) {
            String custArrayString = custArray.toString();
            JsonArray customerAccounts = new JsonArray(custArrayString);

            // Iterate in this way in order to get kyc
            JsonObject jsonObject = null;
            for (Object obj : customerAccounts) {
                jsonObject = (JsonObject) obj;
            }

            if (jsonObject != null) {
                result.put("CustomerName", jsonObject.getString("CustomerName"));
                result.put("IdNumber", String.valueOf(jsonObject.getValue("IdNumber")));
                result.put("PhoneNumber", String.valueOf(jsonObject.getValue("PhoneNumber")));
                result.put("CompanyCode", String.valueOf(jsonObject.getValue("CompanyCode")));
                result.put("response", "000");
                result.put("responseDescription", "success");
            }
            result.put("Accounts", customerAccounts);
        }
        return result;
    }

    public JsonObject statement(String customerResponse) {
        JsonObject result = new JsonObject();

        String response = customerResponse;
        response = removeExtra_ns(response);

        JSONObject data = XML.toJSONObject(response);
        JSONObject envelope = data.getJSONObject("S:Envelope");
        JSONObject body = envelope.getJSONObject("S:Body");

        JSONObject custDetailsDesponse = body.getJSONObject("ACSTATEMENTResponse");
        JSONObject status = custDetailsDesponse.getJSONObject("Status");

        if (status.getString("successIndicator").equalsIgnoreCase("T24Error")) {
            result.put("response", "999");
            result.put("responseDescription", "Transactions not found.");
        } else {
            JSONObject transactionsDetailType = custDetailsDesponse.getJSONObject("CUSTACCTSTMTType");
            JSONObject transactionsOb = transactionsDetailType.getJSONObject("gCUSTACCTSTMTDetailType");
            JSONArray stmtArray = transactionsOb.getJSONArray("mCUSTACCTSTMTDetailType");
            String custMihiArray = stmtArray.toString();
            JsonArray statement = new JsonArray(custMihiArray);
            result.put("transactions", statement);
            result.put("response", "000");
            result.put("responseDescription", "Transactions fetched successfully.");
        }

        return result;
    }

    public JsonObject deposits(String customerResponse) {
        String response = customerResponse;
        response = removeExtra_ns(response);

        JsonObject result = new JsonObject();

        JSONObject data = XML.toJSONObject(response);
        System.out.println("data ::: " + data);
        JSONObject envelope = data.getJSONObject("S:Envelope");
        JSONObject body = envelope.getJSONObject("S:Body");
        JSONObject custDetailsDesponse = body.getJSONObject("MPESACollectionsResponse");
        JSONObject status = custDetailsDesponse.getJSONObject("Status");

        if (status.getString("successIndicator").equalsIgnoreCase("success")) {

            result.put("successIndicator", status.getString("successIndicator"));
            result.put("transactionId", status.getString("transactionId"));
            result.put("branchCode", custDetailsDesponse.getJSONObject("FUNDSTRANSFERType").get("CREDITCOMPCODE"));

        } else {

            System.out.println("custDetailsDesponse ::  " + custDetailsDesponse);
            if (status.has("messages")) {
                String messages = status.getJSONArray("messages").toString();
//                result.put("messages", messages);
                if (messages.contains("Message Reference already used")) {
                    result.put("successIndicator", "duplicate");
                    result.put("transactionId", status.getString("transactionId"));
                } else {
                    result.put("successIndicator", status.getString("successIndicator"));
                    result.put("transactionId", status.getString("transactionId"));
                }

            } else {
                result.put("successIndicator", status.getString("successIndicator"));
                result.put("transactionId", status.getString("transactionId"));
                
            }

        }
        System.out.println("RESPONSE ::  " + result);
        return result;
    }

    public JsonObject minstat(String customerResponse) {
        String response = removeExtra_ns(customerResponse);

        JSONObject data = XML.toJSONObject(response);

        JSONObject envelope = data.getJSONObject("S:Envelope");
        JSONObject body = envelope.getJSONObject("S:Body");
        JSONObject custDetailsDesponse = body.getJSONObject("AgencyMinistatementResponse");
        JSONObject status = custDetailsDesponse.getJSONObject("Status");
        String statusStr = status.toString();
        JsonObject result = new JsonObject(statusStr);

        JSONObject ISAMINISTMTType = custDetailsDesponse.getJSONObject("AGENTCUSTMINISTATEMENTType");
        JSONObject gISAMINISTMTDetailType = ISAMINISTMTType.getJSONObject("gAGENTCUSTMINISTATEMENTDetailType");
        Object stmtObject = gISAMINISTMTDetailType.get("mAGENTCUSTMINISTATEMENTDetailType");

        JSONArray stmtArray = new JSONArray();
        if (stmtObject instanceof JSONArray) {
            stmtArray = gISAMINISTMTDetailType.getJSONArray("mAGENTCUSTMINISTATEMENTDetailType");
        }

        // JSONArray stmtArray = gISAMINISTMTDetailType.getJSONArray("mISAMINISTMTDetailType");
        String custMihiArrayString = stmtArray.toString();
        JsonArray statement = new JsonArray(custMihiArrayString);

        result.put("transactions", statement);

        return result;
    }

    public static String removeExtra_ns(String received) {
        String sendBack = null;
        sendBack = received.replaceAll("<ns.*?:", "<");
        sendBack = sendBack.replaceAll("</ns.*?:", "</");
        return sendBack;
    }

}
