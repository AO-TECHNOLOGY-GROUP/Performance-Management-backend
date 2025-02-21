/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.instance;
import com.aogroup.za.FetchBalance.fetchbalance;
import com.co.ke.main.EntryPoint;
import com.aogroup.za.util.ResponseCodes;
import com.aogroup.za.util.Utilities;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.StringValueExp;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import log.Logging;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Ronald.Langat
 */
public class CBSUtility {

    private static Logging logger;

    public CBSUtility() {
    }

    public JsonArray getCustomerAccounts(String response) {
        JsonArray accounts = new JsonArray();
        JSONObject jo = XML.toJSONObject(response);
        JSONObject getLevel2 = jo.getJSONObject("S:Envelope");
        JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
        JSONObject getLevel4 = getLevel3.getJSONObject("ns3:WebServiceAccountDetsResponse");
        JSONObject getLevel5 = getLevel4.getJSONObject("ACCOUNTDETS1Type");
        JSONObject getLevel6 = getLevel5.getJSONObject("ns2:gACCOUNTDETS1DetailType");

        String jsonStr = getLevel6.get("ns2:mACCOUNTDETS1DetailType").toString();
        if (jsonStr.startsWith("[")) {
            accounts = new JsonArray(jsonStr);
        } else {
            String strArr = "[" + jsonStr + "]";
            accounts = new JsonArray(strArr);
        }
        return accounts;
    }

    public JsonArray getCustomerAccountsDestinationAccounts(String response) {
        JsonArray accounts = new JsonArray();
        JSONObject jsonObject = XML.toJSONObject(response);
        JSONObject jo = new JSONObject(jsonObject);
        JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
        JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
        JSONObject getLevel4 = getLevel3.getJSONObject("ns3:ShowNonTransAcctsResponse");
        JSONObject getLevel5 = getLevel4.getJSONObject("OTHERACCTSType");
        JSONObject getLevel6 = getLevel5.getJSONObject("ns2:gOTHERACCTSDetailType");

        String jsonStr = getLevel6.get("ns2:mOTHERACCTSDetailType").toString();
        if (jsonStr.startsWith("[")) {
            accounts = new JsonArray(jsonStr);
        } else {
            String strArr = "[" + jsonStr + "]";
            accounts = new JsonArray(strArr);
        }
        return accounts;
    }

    public JsonObject getCustomerDetails(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = "";
        String messages = null;
        String name = null;
        String phoneNumber = "";
        String idNumber = "";
        String lastName = null;
        String middleName = null;
        String firstName = null;
        String dob = null;

        if (!response.isEmpty()) {
            try {

                JSONObject jo = XML.toJSONObject(removeExtra_ns(response));
                JSONObject getLevel2 = jo.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("WsGetCustomerResponse");
                String status = getLevel4.getJSONObject("Status").getString("successIndicator");
                JSONObject getLevel5 = getLevel4.getJSONObject("GETCUSTOMERType")
                        .getJSONObject("gGETCUSTOMERDetailType").getJSONObject("mGETCUSTOMERDetailType");

                successIndicator = status;
//                messages = breakResponseXML(response, "messages");
                // customerNumber = cbs.breakResponseXML(responseXML, "ns2:CUSTOMER");
                name = getLevel5.getString("Name");
                phoneNumber = getLevel5.get("PhoneNo").toString();
                idNumber = getLevel5.get("IDNo").toString();
                dob = getLevel5.get("DateofBirth").toString();

            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }
            try {
                String[] flName = name.split(" ", 3);
                firstName = flName[0];
                middleName = flName[1];
                if (flName.length > 2) {
                    lastName = flName[2];
                } else {
                    lastName = flName[1];
                }

                if (middleName.equals("")) {
                    middleName = " ";
                }
                if (lastName.equals("")) {
                    lastName = " ";
                }
                if (firstName.equals("")) {
                    firstName = " ";
                }
            } catch (Exception e) {
                logger.applicationLog(logger.logPreString() + " RESPONSE XML: CUSTOMER DETAILS  Err " + e.getMessage() + "\n\n", "", 54);
            }
            if (successIndicator.equalsIgnoreCase("Success")) {

                if (idNumber.length() > 0 && phoneNumber.length() > 0) {

                    if (lastName == null && middleName != null) {
                        lastName = middleName;
                        middleName = null;
                    }
                    res.put("firstName", firstName);
                    res.put("middleName", middleName);
                    res.put("lastName", lastName.trim());
                    res.put("idNumber", idNumber.trim());
                    res.put("phoneNumber", phoneNumber);
                    res.put("dob", dob);

                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("msgType", "1210");
                    res.put("responseDescription", successIndicator);
                    res.put("result", successIndicator);
                } else {
                    res.put("response", ResponseCodes.RECORD_NOT_FOUND);
                    res.put("msgType", "1210");
                    res.put("responseDescription", "Could not get Customer Record");
                    res.put("result", "Could not get Customer Details");
                }

            } else {
                res.put("response", ResponseCodes.RECORD_NOT_FOUND);
                res.put("msgType", "1210");
                res.put("responseDescription", "Could not get Customer Record");
                res.put("result", "Could not get Customer Details");
            }
        } else {
            res.put("response", ResponseCodes.RECORD_NOT_FOUND);
            res.put("msgType", "1210");
            res.put("responseDescription", "Could not get Customer Record");
            res.put("result", "Could not get Customer Details");
        }

        return res;
    }

    public JsonObject checkOffLoanBalance(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = "";
        String loanBal = "";
        if (!response.isEmpty()) {
            try {
                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("GetLoanBalancesResponse");
                String status = getLevel4.getJSONObject("Status").getString("successIndicator");

                successIndicator = status;
                if (successIndicator.equalsIgnoreCase("Success")) {
                    if (loanBal.isEmpty()) {
                        res.put("response", ResponseCodes.RECORD_NOT_FOUND);
                        res.put("responseDescription", "Record not found");
                        res.put("result", "Record not found");
                        res.put("msgType", "1210");
                        res.put("balance", "0");
                    } else {

                        JSONObject getLevel5 = getLevel4.getJSONObject("USLGETLOANBALANCESType")
                                .getJSONObject("gUSLGETLOANBALANCESDetailType")
                                .getJSONObject("mUSLGETLOANBALANCESDetailType");
                        loanBal = getLevel5.get("BALANCE").toString().replace(",", "");
                        res.put("balance", loanBal);
                        res.put("response", ResponseCodes.SUCCESS);
                        res.put("responseDescription", "Record found");
                        res.put("result", "Record found");
                        res.put("msgType", "1210");
                    }
                } else {
                    if (successIndicator.equalsIgnoreCase("T24Override")) {
                        res.put("response", ResponseCodes.TRANSACTION_FAILED);
                        res.put("responseDescription", "Transaction Failed");
                        res.put("result", "Transaction Failed");
                        res.put("msgType", "1210");
                        res.put("balance", "0");
                    } else {
                        res.put("response", ResponseCodes.RECORD_NOT_FOUND);
                        res.put("responseDescription", "Record not found");
                        res.put("result", "Record not found");
                        res.put("msgType", "1210");
                        res.put("balance", "0");

                    }
                }
            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }
        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
            res.put("balance", "0");
            res.put("responseDescription", "System Error");
            res.put("result", "System Error");
        }

        return res;
    }

    public JsonObject checkCustomerExists(String response) {
        JsonObject jo = new JsonObject();
        if (!response.isEmpty()) {
            String successIndicator = "", messages = "";
            try {
                successIndicator = breakResponseXML(response, "successIndicator");
                messages = breakResponseXML(response, "messages");
                if (successIndicator.equalsIgnoreCase("Success")) {
                    /**
                     * Customer Exists 1. Get Customer Number Update employee
                     * status table 2. Create the customer in { 1. Customer
                     * table; 2. LoginValidation table} 3. Send Notification
                     * informing the customer of the new loan service
                     */
                    if (!messages.equals("")) {
                        /**
                         * Customer Doesn't Exist 1. Create the customer in core
                         * banking 2. Create an account in core banking 3.
                         * Update employee status table 4. Send Notification
                         */
//                        createT24Customer();
                        jo.put("response", "347");
                        jo.put("msgType", "1210");

                    } else {
                        String customerNumber = breakResponseXML(response, "Customer");
                        jo.put("customerNumber", customerNumber);
                        jo.put("response", ResponseCodes.SUCCESS);
                        jo.put("msgType", "1210");
                    }
                } else {
                    jo.put("response", ResponseCodes.CBS_TIMEOUT);
                    jo.put("msgType", "1210");
                }
            } catch (Exception e) {
                logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
            }
        } else {
            jo.put("response", ResponseCodes.SYSTEM_ERROR);
            jo.put("msgType", "1210");
        }
        return jo;
    }

    public JsonObject createCustomer(String response) {
        JsonObject res = new JsonObject();
        if (!response.isEmpty()) {
            String successIndicator = "", messages = "";
            try {
                successIndicator = breakResponseXML(response, "successIndicator");
                messages = breakResponseXML(response, "messages");
                if (successIndicator.equalsIgnoreCase("Success")) {
                    if (!messages.equals("")) {
                        // Do nothing
                        res.put("response", ResponseCodes.REGISTERED);
                        res.put("msgType", "1210");
                    } else {
                        String customerNumber = breakResponseXML(response, "transactionId");
                        res.put("customerNumber", customerNumber);
                        res.put("response", "348");

                    }
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("msgType", "1210");
                }
            } catch (Exception e) {
                logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
            }
        } else {
        }
        return res;
    }

    public JsonObject createSavings(String response) {
        JsonObject res = new JsonObject();
        if (!res.isEmpty()) {
            String successIndicator = "";
            try {
                successIndicator = breakResponseXML(response, "successIndicator");
                if (successIndicator.equalsIgnoreCase("Success")) {
                    res.put("response", "349");
                    res.put("msgType", "1210");
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("msgType", "1210");
                }
            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + ex.getMessage() + "\n\n", "", 54);
            }
        } else {
        }
        return res;
    }

    public JsonObject createCurrentAccount(String responseXML) {
        JsonObject res = new JsonObject();
        if (!responseXML.isEmpty()) {
            String successIndicator = "";
            try {
                successIndicator = breakResponseXML(responseXML, "successIndicator");
                if (successIndicator.equalsIgnoreCase("Success")) {

                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("msgType", "1210");

                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("msgType", "1210");
                }
            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + ex.getMessage() + "\n\n", "", 54);
            }
        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
        }
        return res;
    }

    public JsonObject checkOffBalance(String response) {
        JsonObject jo = new JsonObject();
        try {
            String loanBal = breakResponseXML(response, "BALANCE");
            if (!loanBal.isEmpty()) {
                jo.put("response", ResponseCodes.TRANSACTION_NOT_ALLOWED);
                jo.put("responseDescription", "Transaction is not allowed. Similar loan exists");
                jo.put("result", loanBal);
                jo.put("msgType", "1210");

            } else {
                jo.put("response", ResponseCodes.SUCCESS);
                jo.put("responseDescription", "Transaction successful");
                jo.put("result", loanBal);
                jo.put("msgType", "1210");
            }
        } catch (Exception e) {
        }
        return jo;
    }

    public JsonObject getAccountDetails(String response) {
        JsonObject jo = new JsonObject();
        JsonArray accs = new JsonArray();

        if (!response.isEmpty()) {
            String branchCode = "";
            try {
                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("WebServiceAccountDetsResponse");
                String status = getLevel4.getJSONObject("Status").getString("successIndicator");
                JSONObject getLevel5 = new JSONObject();
                Object custArray = getLevel4.getJSONObject("ACCOUNTDETS1Type").getJSONObject("gACCOUNTDETS1DetailType")
                        .get("mACCOUNTDETS1DetailType");

                if (custArray instanceof JSONObject) {
                    getLevel5 = getLevel4.getJSONObject("ACCOUNTDETS1Type")
                            .getJSONObject("gACCOUNTDETS1DetailType").getJSONObject("mACCOUNTDETS1DetailType");
                    branchCode = getLevel5.get("CompanyCode").toString();
                } else if (custArray instanceof JSONArray) {
                    branchCode = getLevel4.getJSONObject("ACCOUNTDETS1Type")
                            .getJSONObject("gACCOUNTDETS1DetailType").getJSONArray("mACCOUNTDETS1DetailType")
                            .getJSONObject(0).get("CompanyCode").toString();

                }

                if (!branchCode.isEmpty()) {
                    jo.put("response", ResponseCodes.SUCCESS);
                    jo.put("msgType", "1210");
                    jo.put("responseDescription", "Request scuccessful");
                    jo.put("branchCode", branchCode);
                } else {
                    jo.put("response", ResponseCodes.SYSTEM_ERROR);
                    jo.put("msgType", "1210");
                    jo.put("responseDescription", "Corebanking System Error");
                    jo.put("result", "Corebanking System Error");
                }

            } catch (Exception e) {
            }
        } else {
            jo.put("response", ResponseCodes.SYSTEM_ERROR);
            jo.put("msgType", "1210");
            jo.put("responseDescription", "Corebanking System Error");
            jo.put("result", "Corebanking System Error");

        }
        return jo;
    }

    public JsonObject createLoan(String response) {
        JsonObject res = new JsonObject();
        if (!response.isEmpty()) {
            String successIndicator = "", messages = "", arrangement = "";
            try {

                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("CreateNewLoanResponse");
                successIndicator = getLevel4.getJSONArray("Status").getJSONObject(0).getString("successIndicator");

                if (!successIndicator.equalsIgnoreCase("success")) {
                    String msg = getLevel4.getJSONArray("Status").getJSONObject(0).getString("messages");
                    messages = msg;
                    res.put("messages", messages);
                } else {
                    messages = "";
                }

                if (successIndicator.equalsIgnoreCase("Success")) {

                    arrangement = getLevel4.getJSONObject("AAARRANGEMENTACTIVITY").get("ARRANGEMENT").toString();

                    if (!arrangement.equals("")) {
                        res.put("arrangement", arrangement);
                        res.put("response", ResponseCodes.SUCCESS);
                    } else {
                        res.put("response", ResponseCodes.CBS_TIMEOUT);
                        res.put("msgType", "1210");
                        res.put("responseDescription", "Core banking timed out");
                        res.put("result", "Core banking timed out");

                    }
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("responseDescription", "Core banking timed out");
                    res.put("result", "Core banking timed out");
                    res.put("msgType", "1210");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
        }
        return res;
    }

    public JsonObject disburseLoan(String response) {
        JsonObject res = new JsonObject();
        try {
            if (!response.isEmpty()) {
                String successIndicator = "", transactionId = "";
                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("ns16:DisburseNewLoanResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                if (successIndicator.equalsIgnoreCase("Success")) {

                    transactionId = getLevel4.getJSONObject("Status").getString("transactionId");
                    if (!transactionId.equals("")) {
                        res.put("transactionId", transactionId);
                        res.put("response", ResponseCodes.SUCCESS);
                        res.put("msgType", "1210");
                    } else {
                        res.put("response", ResponseCodes.CBS_TIMEOUT);
                        res.put("msgType", "1210");
                    }
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("msgType", "1210");
                }
            } else {
                res.put("response", ResponseCodes.SYSTEM_ERROR);
                res.put("msgType", "1210");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public JsonObject advanceLimitCheck(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = "";
        String totalAmount = "";
        if (!response.isEmpty()) {
            try {

                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("GetCustomerLimitResponse");
                String status = getLevel4.getJSONObject("Status").getString("successIndicator");
                JSONObject getLevel5 = getLevel4.getJSONObject("USLGETCUSTOMERLIMITType")
                        .getJSONObject("gUSLGETCUSTOMERLIMITDetailType").getJSONObject("mUSLGETCUSTOMERLIMITDetailType");

                successIndicator = status;
                totalAmount = getLevel5.get("LIMIT").toString();

                // messages = cbs.breakResponseXML(responseXML, "messages");
            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }

            if (successIndicator.equalsIgnoreCase("Success")) {

                if (totalAmount.isEmpty()) {
                    res.put("response", ResponseCodes.RECORD_NOT_FOUND);
                    res.put("responseDescription", "Record not found");
                    res.put("msgType", "1210");
                } else {

                    res.put("advanceLoanLimit", totalAmount);
                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("msgType", "1210");
                }
            } else {

                if (successIndicator.equalsIgnoreCase("T24Override")) {
                    res.put("response", ResponseCodes.TRANSACTION_FAILED);
                    res.put("responseDescription", "Transaction Failed");
                    res.put("msgType", "1210");
                } else {
                    res.put("response", ResponseCodes.RECORD_NOT_FOUND);
                    res.put("responseDescription", "Record not found");
                    res.put("msgType", "1210");
                }

            }
        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
        }
        return res;
    }

    public JsonObject advanceLoanBalance(String response) {
        JsonObject res = new JsonObject();
        String messages = "";
        try {
            String loanBal = "";
            JSONObject o = XML.toJSONObject(response);
            JSONObject getLevel2 = o.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("GetLoanBalancesResponse");
            String status = getLevel4.getJSONObject("Status").getString("successIndicator");

            if (status.equalsIgnoreCase("success")) {
                JSONObject getLevel5 = getLevel4.getJSONObject("USLGETLOANBALANCESType")
                        .getJSONObject("gUSLGETLOANBALANCESDetailType")
                        .getJSONObject("mUSLGETLOANBALANCESDetailType");
                loanBal = getLevel5.get("BALANCE").toString();
                res.put("response", ResponseCodes.SUCCESS);
                res.put("result", loanBal);
                res.put("msgType", "1210");
            } else {
                if (getLevel4.getJSONObject("Status").has("messages")) {
                    if (status.equalsIgnoreCase("T24error")) {
                        String msg = getLevel4.getJSONObject("Status").getJSONArray("messages").toString();
                        messages = msg;
                        if (messages.contains("Bucket Error E-168054")) {
                            loanBal = "0";
                            res.put("response", ResponseCodes.SUCCESS);
                            res.put("result", loanBal);
                            res.put("msgType", "1210");
                        }
                    }
                } else {
                    loanBal = "";
                    res.put("response", ResponseCodes.TRANSACTION_NOT_ALLOWED);
                    res.put("responseDescription", "Transaction is not allowed. Similar loan exists");
                    res.put("result", loanBal);
                    res.put("msgType", "1210");
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public JsonObject advanceLoanArrangement(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String arrangement = null;
        String messages = null;
        String responseCode = "";

        if (!response.isEmpty()) {
            try {

                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("CreateAdvanceMocashLoanResponse");
                String status = getLevel4.getJSONArray("Status").getJSONObject(0).get("successIndicator").toString();
                successIndicator = status;
                //arrangement = cbs.breakResponseXML(responseXML, "ns6:ARRANGEMENT");
                if (successIndicator.equalsIgnoreCase("success")) {
                    arrangement = getLevel4.getJSONObject("AAARRANGEMENTACTIVITY").get("ARRANGEMENT").toString();
                }

                if (arrangement != null || !arrangement.equalsIgnoreCase("T24Error")) {
                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("responseDescription", "Transaction success");
                    res.put("msgType", "1210");
                    res.put("arrangement", arrangement);
                } else {
                    res.put("response", ResponseCodes.TRANSACTION_FAILED);
                    res.put("responseDescription", "Transaction Failed");
                    res.put("msgType", "1210");
                }
            } catch (Exception e) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + e.getMessage() + "\n\n", "", 54);

            }
        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("responseDescription", "System Error");
            res.put("msgType", "1210");
        }
        return res;
    }

    public JsonObject advanceLoanApplication(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String transactionId = null;
        String messages = null;
        String responseCode = "";

        if (!response.isEmpty()) {
            try {

                JSONObject o = XML.toJSONObject(response);
                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("ns10:DisburseAdvanceMocashLoanResponse");
                String status = getLevel4.getJSONObject("Status").get("successIndicator").toString();
                successIndicator = status;
                transactionId = getLevel4.getJSONObject("Status").get("transactionId").toString();

                if (!transactionId.isEmpty()) {
                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("msgType", "1210");
                    res.put("transactionId", transactionId);
                    res.put("responseDescription", "Transaction successful");
                } else {
                    res.put("msgType", "1210");
                    res.put("response", ResponseCodes.TRANSACTION_FAILED);
                    res.put("responseDescription", "Transaction Failed");
                }

                // messages = cbs.breakResponseXML(responseXML, "messages");
            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }
        } else {
            res.put("responseDescription", "System Error");
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");

        }
        return res;
    }

    public JsonObject getBalanceInquiry(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String workingBalance = null;
        String product = null;
        String accNo = null;
        String WMessages = null;

        BigDecimal BigWorkingBalance = BigDecimal.ZERO;

        if (!response.isEmpty()) {

            try {

                JSONObject jo = XML.toJSONObject(response);
                JSONObject getLevel2 = jo.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("BalanceEnquiryResponse");
                String status = getLevel4.getJSONObject("Status").getString("successIndicator");
                if (getLevel4.has("MCACCTBALType")) {
                    JSONObject getLevel5 = getLevel4.getJSONObject("MCACCTBALType")
                            .getJSONObject("gMCACCTBALDetailType").getJSONObject("mMCACCTBALDetailType");

                    successIndicator = status;
                    workingBalance = getLevel5.get("WorkingBal").toString();
                    product = getLevel5.getString("Product");
                    accNo = getLevel5.get("AcctNo").toString();
                    res.put("availableBalance", workingBalance);
                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("msgType", "1210");
                } else {
                    String msg = getLevel4.getJSONObject("Status").getJSONArray("messages").toString();
                    WMessages = msg;
                    res.put("messages", WMessages);
                    res.put("availableBalance", workingBalance);
                    res.put("response", ResponseCodes.SYSTEM_ERROR);
                    res.put("msgType", "1210");
                }

//                WMessages = breakResponseXML(response, "messages");
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }

        } else {
            res.put("availableBalance", workingBalance);
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");

        }
        return res;
    }

    public JsonObject chargeBalanceEnquiry(String response, JsonObject data) {
        JsonObject res = new JsonObject();
        String c_successIndicator = null;
        String c_myPhoneNumber = data.getString("phonenumber");
        String c_totalCharge = "0";
        String COMMISSION_AMT = "0";
        String DEBIT_AMOUNT = "0";
        try {
            JSONObject jo = XML.toJSONObject(response);
            JSONObject getLevel2 = jo.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("requesttoChargeBalEnqResponse");
            String status = getLevel4.getJSONObject("Status").getString("successIndicator");

            if (status.equalsIgnoreCase("success")) {
                JSONObject getLevel5 = getLevel4.getJSONObject("FUNDSTRANSFERType");

                c_successIndicator = status;
                DEBIT_AMOUNT = getLevel5.get("DEBITAMOUNT").toString();
                COMMISSION_AMT = getLevel5.get("LOCALCHARGEAMT").toString();
                if (DEBIT_AMOUNT != null && COMMISSION_AMT != null) {
                    c_totalCharge = String.valueOf(new BigDecimal(COMMISSION_AMT).add(new BigDecimal(DEBIT_AMOUNT)));
                }
                res.put("response", ResponseCodes.SUCCESS);
                res.put("totalCharge", c_totalCharge);
            } else {
                res.put("response", ResponseCodes.TRANSACTION_FAILED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public JsonObject getMinistatement(String response) {
        JsonObject result = new JsonObject();
        String f_result = "";
        String responseXML = response;
        JSONObject jsonObject = XML.toJSONObject(responseXML);
        JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
        JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
        JSONObject getLevel4 = getLevel3.getJSONObject("ns3:MinisatementforMocashResponse");
        JSONObject getLevel5 = getLevel4.getJSONObject("MOCASHMINISTMTType");
        JSONObject getLevel6 = getLevel5.getJSONObject("ns2:gMOCASHMINISTMTDetailType");

        JSONArray arrLevel = (JSONArray) getLevel6.get("ns2:mMOCASHMINISTMTDetailType");
        String statusCode = "901";
        List<String> toReturn = new ArrayList<String>();
        for (int i = 0, size = arrLevel.length(); i < size; i++) {
            JSONObject objectInArray = arrLevel.getJSONObject(i);
            String transaction = objectInArray.get("ns2:Details").toString();
            String res = transaction;
            String dateJ = res.substring(0, Math.min(res.length(), 9));
            String des = "";

            String sign = "";
            if (res.contains("+") || res.contains("-")) {
                sign = res.substring(res.length() - 1);
                if (sign.equalsIgnoreCase("+")) {
                    sign = "+";
                } else {
                    sign = "-";
                }
                des = res.substring(10, Math.min(res.length(), 14));
            }
            if (dateJ.equalsIgnoreCase("Available")) {
                dateJ = "Bal:";
            }
            String withNoDate = res.replaceAll(dateJ, "");
            String amount = withNoDate.replaceAll("[^0-9.]", "");
            if (transaction.equalsIgnoreCase("0")) {
                transaction = "success";
                statusCode = "000";
            } else if (transaction.equalsIgnoreCase("2")) {
                transaction = "fail";
                statusCode = "901";
            }

            dateJ = dateJ.replaceAll(" ", "");
            transaction = dateJ + " " + des + " " + amount + "" + sign;

            toReturn.add(transaction);
        }

        if (statusCode.equalsIgnoreCase("000")) {
            f_result = toReturn.toString();//
            f_result = f_result.replaceAll("\\[", "").replaceAll("\\]", "");
            result.put("result", f_result);
            result.put("response", ResponseCodes.SUCCESS);
            result.put("msgType", "1210");

        } else {
            result.put("result", "Transactions not found");
            result.put("status", "fail");
            result.put("response", ResponseCodes.SYSTEM_ERROR);
        }

        return result;
    }

    public JsonObject chargeMinistatement(String response) {
        JsonObject res = new JsonObject();
        String c_totalCharge = "0";
        String COMMISSION_AMT = "0";
        String DEBIT_AMOUNT = "0";

        String c_successIndicator = null;

        try {
            JSONObject jsonObject = XML.toJSONObject(response);
            JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("ChargeMiniStatementResponse");
            String status = getLevel4.getJSONObject("Status").getString("successIndicator");
//            if (DEBIT_AMOUNT != null && COMMISSION_AMT != null) {
//                c_totalCharge = String.valueOf(new BigDecimal(COMMISSION_AMT).add(new BigDecimal(DEBIT_AMOUNT)));
//            }

            if (status.equalsIgnoreCase("success")) {
                JSONObject getLevel5 = getLevel4.getJSONObject("FUNDSTRANSFERType");

                c_successIndicator = status;
                DEBIT_AMOUNT = getLevel5.get("DEBITAMOUNT").toString();
                COMMISSION_AMT = getLevel5.get("LOCALCHARGEAMT").toString();
                res.put("response", ResponseCodes.SUCCESS);
                res.put("charge", c_totalCharge);
            } else {
                res.put("response", ResponseCodes.SYSTEM_ERROR);
            }

            //send Message Here
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return res;
    }

    public JsonObject fundsTransfer(String response, JsonObject jo) {
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String transactionId = null;
        String transactionCost = "0.0";
        String transferType = jo.getString("transferType");
        String messages = "";

        String responseCode = "";

        if (!response.isEmpty()) {
            try {

                JSONObject jsonObject = XML.toJSONObject(response);
                JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLeve14 = new JSONObject();
                if (transferType.equalsIgnoreCase("other")) {
                    getLeve14 = getLevel3.getJSONObject("TransferBtwnAccountsResponse");
                } else {
                    getLeve14 = getLevel3.getJSONObject("OwnAccountTransferResponse");
                }

                String status = getLeve14.getJSONObject("Status").getString("successIndicator");
                String tranId = getLeve14.getJSONObject("Status").getString("transactionId");
                if (!status.equalsIgnoreCase("success")) {
                    String msg = getLeve14.getJSONObject("Status").getString("messages");
                    messages = msg;
                    res.put("messages", messages);
                }
                JSONObject getLevel5 = getLeve14.getJSONObject("FUNDSTRANSFERType");

                successIndicator = status;
                transactionId = tranId;

                if (transferType.equalsIgnoreCase("other")) {
                    transactionCost = getLevel5.get("TOTALCHARGEAMT").toString();
                } else {
                    transactionCost = "0.00";
                }

                res.put("response", ResponseCodes.SUCCESS);
                res.put("msgType", "1210");
                res.put("cost", transactionCost);
                res.put("transactionId", transactionId);
                // messages = cbs.breakResponseXML(responseXML, "messages");
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }
        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
            res.put("responseDescription", "System Error");
        }
        return res;
    }

    public JsonObject rejectFundsTransfer(String response) {
        JsonObject res = new JsonObject();
        try {
            String rejectSuccessIndicator = breakResponseXML(response, "successIndicator");
            String rejectMessages = breakResponseXML(response, "messages");
            if (rejectSuccessIndicator.equalsIgnoreCase("Success")) {
                if (!rejectMessages.isEmpty()) {
                    //failed to rollback
                    res.put("response", ResponseCodes.FAILED_REJECT_TRANSFER);
                    res.put("responseDescription", "rollback failed");
                    res.put("msgType", "1210");
                } else {
                    res.put("response", ResponseCodes.TRANSACTION_NOT_ALLOWED);
                    res.put("responseDescription", "transaction failed");
                    res.put("msgType", "1210");
                }
            } else {

                res.put("response", ResponseCodes.FAILED_REJECT_TRANSFER);
                res.put("responseDescription", "transaction failed");
                res.put("msgType", "1210");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public JsonObject getRecipientFTDetail(String response) {
        JsonObject res = new JsonObject();
        String cusName = "";
        try {
            JSONObject jsonObject = XML.toJSONObject(response);
            JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("GetCustomerNameResponse");
            String status = getLevel4.getJSONObject("Status").getString("successIndicator");

            cusName = getLevel4.getJSONObject("RETURNNAMEType")
                    .getJSONObject("gRETURNNAMEDetailType")
                    .getJSONObject("mRETURNNAMEDetailType").getString("ACCOUNTTITLE1");
            res.put("customerName", cusName);
            res.put("response", ResponseCodes.SUCCESS);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return res;
    }

    public JsonObject purchaseAirtime(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String transactionId = null;

        // request.put("accounts", response.get("accounts").toString());
        if (!response.isEmpty()) {
            try {
                JSONObject jsonObject = XML.toJSONObject(response);
                JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("ns6:AirtimePurchaseResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                transactionId = getLevel4.getJSONObject("Status").getString("transactionId");

            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }

            if (successIndicator.equalsIgnoreCase("Success")) {
                res.put("transactionId", transactionId);
                res.put("response", ResponseCodes.SUCCESS);
                res.put("msgType", "1210");
            } else {
                res.put("response", ResponseCodes.SYSTEM_ERROR);
                res.put("msgType", "1210");
            }

        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
        }
        return res;
    }

    public JsonObject sendC2BToT24(String response, JsonObject jo) {
        JsonObject res = new JsonObject();
        String successIndicator = "";
        String transactionId = null;
        String messages = null;

        String AccountNumberToCharge = jo.getString("account");

        if (!response.isEmpty()) {
            try {
                JSONObject jsonObject = XML.toJSONObject(response);
                JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("ns4:MocashDepositResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                transactionId = getLevel4.getJSONObject("Status").getString("transactionId");
                if (!successIndicator.equalsIgnoreCase("success")) {
                    messages += getLevel4.getJSONObject("Status").getString("messages");
                }
                

            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }
            if (successIndicator.equalsIgnoreCase("success")) {
                
                res.put("transactionId", transactionId);
                res.put("response", ResponseCodes.SUCCESS);
                res.put("msgType", "1210");
                res.put("responseDescription", successIndicator);
                res.put("result", successIndicator);
            } else {
                res.put("transactionId", transactionId);
                res.put("response", "999");
                res.put("msgType", "1210");
                res.put("responseDescription", successIndicator);
                res.put("result", messages);
            }

        } else {
            res.put("transactionId", transactionId);
                res.put("response", "999");
                res.put("msgType", "1210");
                res.put("responseDescription", successIndicator);
                res.put("result", messages);
        }

        return res;
    }

    public JsonObject getRecipeintDetailC2B(String response) {
        JsonObject res = new JsonObject();
        String cusName = "";
        try {
            JSONObject jsonObject = XML.toJSONObject(removeExtra_ns(response));
            JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("GetCustomerNameResponse");
            String successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
            if (successIndicator.equalsIgnoreCase("success")) {
                cusName = getLevel4.getJSONObject("RETURNNAMEType")
                        .getJSONObject("gRETURNNAMEDetailType")
                        .getJSONObject("mRETURNNAMEDetailType")
                        .get("ACCOUNTTITLE1").toString();
            } else {
                cusName = "";
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        res.put("cusName", cusName);
        return res;
    }

    public JsonObject sendB2CToCore(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = "";
        String transactionId = null;
        String messages = "";
        String chargeAmount = "0";
        String tranId = "";
        if (!response.equalsIgnoreCase(ResponseCodes.CBS_TIMEOUT)) {
            try {
                JSONObject jsonObject = XML.toJSONObject(response);
                JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("WebServiceWithdrawalResponse");
                String status = getLevel4.getJSONObject("Status").getString("successIndicator");
                tranId = getLevel4.getJSONObject("Status").getString("transactionId");

                if (!status.equalsIgnoreCase("success")) {
                    messages += getLevel4.getJSONObject("Status").getString("messages");
                } else {
                    JSONObject getLevel5 = getLevel4.getJSONObject("FUNDSTRANSFERType");
                    transactionId = tranId;
                    chargeAmount = getLevel5.get("TOTALCHARGEAMT").toString();
                    messages = "";
                }

                successIndicator = status;

            } catch (Exception ex) {
                ex.printStackTrace();
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }

            if (successIndicator.equalsIgnoreCase("Success")) {

                if (!messages.equals("")) {
                    //errors pulled
                    res.put("transactionId", transactionId);
                    res.put("response", ResponseCodes.TRANSACTION_FAILED);
                    res.put("msgType", "1210");
                    res.put("responseDescription", messages);
                    res.put("result", messages);
                } else {
                    res.put("transactionId", transactionId);
                    res.put("chargeAmount", chargeAmount);
                    res.put("response", ResponseCodes.SUCCESS);
                    res.put("msgType", "1210");
                    res.put("responseDescription", successIndicator);
                    res.put("result", successIndicator);
                }
            } else {
                res.put("transactionId", transactionId);
                res.put("response", ResponseCodes.TRANSACTION_FAILED);
                res.put("msgType", "1210");
                if (!messages.equals("")) {
                    res.put("responseDescription", messages);
                    res.put("result", messages);
                } else {
                    res.put("responseDescription", "Transaction Failed");
                    res.put("result", "Transaction Failed");
                }
            }
        } else {
            res.put("response", ResponseCodes.CBS_TIMEOUT);
            res.put("msgType", "1210");
            res.put("responseDescription", "Transaction Failed");
            res.put("result", "Transaction Failed");
        }
        return res;
    }

    public JsonObject makeUtilityPayament(String response) {
        logger.applicationLog(logger.logPreString() + "Response From T24: " + response + "\n\n", "", 3);
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String transactionId = null;

        // request.put("accounts", response.get("accounts").toString());
        if (!response.isEmpty()) {
            try {
                JSONObject jsonObject = XML.toJSONObject(response);
                JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("ns7:utilitypurchaseResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                transactionId = getLevel4.getJSONObject("Status").getString("transactionId");

            } catch (Exception ex) {
                logger.applicationLog(logger.logPreString() + "ERROR BREAKING XML: " + ex.getMessage() + "\n\n", "", 54);
            }

            if (successIndicator.equalsIgnoreCase("Success")) {
                res.put("transactionId", transactionId);
                res.put("response", ResponseCodes.SUCCESS);
                res.put("msgType", "1210");
            } else {
                res.put("response", ResponseCodes.SYSTEM_ERROR);
                res.put("msgType", "1210");
            }

        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
        }
        logger.applicationLog(logger.logPreString() + "Response From T24: " + res + "\n\n", "", 3);
        return res;
    }

    public JsonObject loanAccounts(String response) {
        JsonObject res = new JsonObject();

        try {
            JSONObject jsonObject = XML.toJSONObject(response);
            JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("GetMocashAccountResponse");
            String status = getLevel4.getJSONObject("Status").getString("successIndicator");
//            tranId = getLevel4.getJSONObject("Status").getString("transactionId");
            JSONObject getLevel5 = getLevel4.getJSONObject("USLGETMOCASHACType")
                    .getJSONObject("gUSLGETMOCASHACDetailType")
                    .getJSONObject("mUSLGETMOCASHACDetailType");

            String type = getLevel5.get("CATEGORY").toString();
            String accountNumber = getLevel5.get("ACCOUNTNO").toString();

            res.put("ns2:CATEGORY", type);
            res.put("ns2:ID", accountNumber);
            res.remove("msgType");
            res.put("msgType", "1210");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return res;
    }

    public String getLoanAccountRepayment(String response) {
        String acc = "";
        try {
            JSONObject jsonObject = XML.toJSONObject(response);
            JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
            JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
            JSONObject getLevel4 = getLevel3.getJSONObject("GetLoanAccountResponse");
            String status = getLevel4.getJSONObject("Status").getString("successIndicator");
            if (status.equalsIgnoreCase("success")) {
                JSONObject getLevel5 = getLevel4.getJSONObject("USLGETLOANACCTType")
                        .getJSONObject("gUSLGETLOANACCTDetailType")
                        .getJSONObject("mUSLGETLOANACCTDetailType");
                String loanRepayAccount = getLevel5.get("LOANACCT").toString();
                acc = loanRepayAccount;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return acc;
    }

    public JsonObject processPayoffLoan(String response) {
        JsonObject res = new JsonObject();
        String successIndicator = null;
        String responseCode = "";
        String amountPaid = "";
        String chargedCustomer = "";
        String messages = "";
        String transactionId = "";
        if (!response.isEmpty()) {
            try {
                JSONObject jsonObject = XML.toJSONObject(response);
                JSONObject getLevel2 = jsonObject.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("PayoffMobiloanResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                JSONObject getLevel5 = getLevel4.getJSONObject("FUNDSTRANSFERType");

                amountPaid = getLevel5.get("AMOUNTDEBITED").toString();
                chargedCustomer = getLevel5.get("CHARGEDCUSTOMER").toString();
                messages = getLevel4.getJSONObject("Status").getJSONArray("messages").toString();
                transactionId = getLevel4.getJSONObject("Status").getString("transactionId");
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (successIndicator.equalsIgnoreCase("success")) {
                res.put("amountPaid", amountPaid);
                res.put("chargedCustomer", chargedCustomer);
                res.remove("msgType");
                res.put("msgType", "1210");
                res.put("response", ResponseCodes.SUCCESS);
                res.put("msgType", "1210");
                res.put("responseDescription", "loan repayed");
                res.put("messages", messages);
                res.put("tranId", transactionId);
                responseCode = "000";

            } else {
                if (messages.contains("YOU DONT HAVE SUFFICIENT BALANCE TO PAYOFF THE LOAN")) {
                    res.put("response", ResponseCodes.FAILED_LOAN_REPAYMENT);
                    res.put("responseDescription", "Transaction Failed");
                    res.put("msgType", "1210");
                    res.put("insufficientFunds", "1");
                    responseCode = "502";
                } else if (messages.contains("HOLD - OVERRIDE Unauthorised overdraft")) {
                    res.put("response", ResponseCodes.OVERRID_OVERDRAFT);
                    res.put("responseDescription", "Transaction Failed");
                    res.put("msgType", "1210");
                    res.put("insufficientFunds", "1");
                    responseCode = "400";
                }
            }

        } else {
            res.put("response", ResponseCodes.SYSTEM_ERROR);
            res.put("msgType", "1210");
            res.put("responseDescription", "System Error");
        }
        return res;
    }

    public JsonObject createMajaniLoan(String response) {
        JsonObject res = new JsonObject();
        if (!response.isEmpty()) {
            String successIndicator = "", messages = "", arrangement = "";
            try {

                JSONObject o = XML.toJSONObject(removeExtra_ns(response));

                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("CreateKropLoanResponse");
                successIndicator = getLevel4.getJSONArray("Status").getJSONObject(0).getString("successIndicator");

                if (!successIndicator.equalsIgnoreCase("success")) {
                    String msg = getLevel4.getJSONArray("Status").getJSONObject(0).getString("messages");
                    messages = msg;
                    res.put("messages", messages);
                } else {
                    messages = "";
                }

                if (successIndicator.equalsIgnoreCase("Success")) {

                    arrangement = getLevel4.getJSONObject("AAARRANGEMENTACTIVITY").get("ARRANGEMENT").toString();

                    if (!arrangement.equals("")) {
                        res.put("arrangement", arrangement);
                        res.put("response", ResponseCodes.SUCCESS);
                    } else {
                        res.put("response", ResponseCodes.CBS_TIMEOUT);
                        res.put("msgType", "1210");
                        res.put("responseDescription", "Core banking timed out");
                        res.put("result", "Core banking timed out");

                    }
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("responseDescription", "Core banking timed out");
                    res.put("result", "Core banking timed out");
                    res.put("msgType", "1210");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
        }
        return res;
    }

    public JsonObject disburseMajaniLoan(String response) {
        JsonObject res = new JsonObject();
        try {
            if (!response.isEmpty()) {
                String successIndicator = "", transactionId = "";
                JSONObject o = XML.toJSONObject(removeExtra_ns(response));

                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("DisburseloanResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                if (successIndicator.equalsIgnoreCase("Success")) {

                    transactionId = getLevel4.getJSONObject("Status").getString("transactionId");
                    if (!transactionId.equals("")) {
                        res.put("transactionId", transactionId);
                        res.put("response", ResponseCodes.SUCCESS);
                        res.put("msgType", "1210");
                    } else {
                        res.put("response", ResponseCodes.CBS_TIMEOUT);
                        res.put("msgType", "1210");
                    }
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("msgType", "1210");
                }
            } else {
                res.put("response", ResponseCodes.SYSTEM_ERROR);
                res.put("msgType", "1210");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    public JsonObject rejectPayoffLoan(String response) {
        JsonObject res = new JsonObject();
        String rejectSuccessIndicator = "";
        String rejectMessages = "";
        try {
            rejectSuccessIndicator = breakResponseXML(response, "successIndicator");
            rejectMessages = breakResponseXML(response, "messages");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (rejectSuccessIndicator.equalsIgnoreCase("Success")) {
            if (!rejectMessages.isEmpty()) {
                //failed to rollback
                res.put("response", ResponseCodes.FAILED_REJECT_REPAYMENT);
                res.put("responseDescription", "rollback failed");
                res.put("msgType", "1210");
            } else {
                //successful rollback
                res.put("response", ResponseCodes.REJECT_REPAYMENT_SUCCESS);
                res.put("responseDescription", "successful rollback");
                res.put("msgType", "1210");
            }
        } else {
            res.put("response", ResponseCodes.FAILED_LOAN_REPAYMENT);
            res.put("responseDescription", "loan repayment failed");
            res.put("msgType", "1210");
        }
        return res;
    }

    public JsonObject getKropLoanBalances(String response) {
        JsonObject res = new JsonObject();
        try {
            if (!response.isEmpty()) {
                String successIndicator = "", transactionId = "";
                JSONObject o = XML.toJSONObject(removeExtra_ns(response));

                JSONObject getLevel2 = o.getJSONObject("S:Envelope");
                JSONObject getLevel3 = getLevel2.getJSONObject("S:Body");
                JSONObject getLevel4 = getLevel3.getJSONObject("GetOutstandingloanbalancesResponse");
                successIndicator = getLevel4.getJSONObject("Status").getString("successIndicator");
                if (successIndicator.equalsIgnoreCase("Success")) {

                    transactionId = getLevel4.getJSONObject("Status").getString("transactionId");
                    if (!transactionId.equals("")) {
                        res.put("transactionId", transactionId);
                        res.put("response", ResponseCodes.SUCCESS);
                        res.put("msgType", "1210");
                    } else {
                        res.put("response", ResponseCodes.CBS_TIMEOUT);
                        res.put("msgType", "1210");
                    }
                } else {
                    res.put("response", ResponseCodes.CBS_TIMEOUT);
                    res.put("msgType", "1210");
                }
            } else {
                res.put("response", ResponseCodes.SYSTEM_ERROR);
                res.put("msgType", "1210");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    //
    public JsonObject validateDeposit(JsonObject request) {
        Utilities util = new Utilities();
        request.put("reqValidation", "fail");

        if (request.containsKey("creditAccount")) {

            if (request.containsKey("floatAccount")) {
                if (request.containsKey("amount")) {
                    if (request.containsKey("agentUserName")) {
                        request.put("reqValidation", "pass");
                    } else {
                        request.put("response", "999");
                        request.put("responseDescription", "Bad request. Agent username is missing");
                    }

                } else {
                    request.put("response", "999");
                    request.put("responseDescription", "Bad request. Amount is missing");
                }
            } else {
                request.put("response", "999");
                request.put("responseDescription", "Bad request. Float Account is missing");
            }
        } else {
            request.put("response", "999");
            request.put("responseDescription", "Bad request. Credit Account is missing");
        }
        return request;
    }

    public JsonObject validateWithdrawal(JsonObject request) {
        Utilities util = new Utilities();
        request.put("reqValidation", "fail");

        if (request.containsKey("debitAccount")) {

            if (request.containsKey("floatAccount")) {
                if (request.containsKey("amount")) {
                    if (request.containsKey("phoneNumber")) {
                        if (request.containsKey("agentUserName")) {
                            request.put("reqValidation", "pass");
                        } else {
                            request.put("response", "999");
                            request.put("responseDescription", "Bad request. Agent username is missing");
                        }
                    } else {
                        request.put("response", "999");
                        request.put("responseDescription", "Bad request. Phone number is missing");
                    }
                } else {
                    request.put("response", "999");
                    request.put("responseDescription", "Bad request. Amount is missing");
                }
            } else {
                request.put("response", "999");
                request.put("responseDescription", "Bad request. Float Account is missing");
            }
        } else {
            request.put("response", "999");
            request.put("responseDescription", "Bad request. Credit Account is missing");
        }
        return request;
    }

    public JsonObject validateFT(JsonObject request) {
        Utilities util = new Utilities();
        request.put("reqValidation", "fail");

        if (request.containsKey("debitAccount")) {

            if (request.containsKey("accountTo")) {
                if (request.containsKey("amount")) {
                    if (request.containsKey("phoneNumber")) {
                        if (request.containsKey("agentUserName")) {
                            request.put("reqValidation", "pass");
                        } else {
                            request.put("response", "999");
                            request.put("responseDescription", "Bad request. Agent username is missing");
                        }
                    } else {
                        request.put("response", "999");
                        request.put("responseDescription", "Bad request. Phone number is missing");
                    }
                } else {
                    request.put("response", "999");
                    request.put("responseDescription", "Bad request. Amount is missing");
                }
            } else {
                request.put("response", "999");
                request.put("responseDescription", "Bad request. Destination Account is missing");
            }
        } else {
            request.put("response", "999");
            request.put("responseDescription", "Bad request. Credit Account is missing");
        }
        return request;
    }

    public static String removeExtra_ns(String received) {
        String sendBack = null;
        sendBack = received.replaceAll("<ns.*?:", "<");
        sendBack = sendBack.replaceAll("</ns.*?:", "</");
        return sendBack;
    }

    public HashMap breakXML(String xml) {

        HashMap hm = new HashMap();
        hm.clear();
        InputStream file = new ByteArrayInputStream(xml.getBytes(Charset.defaultCharset()));
        if (!xml.contains("ERR@")) {
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();
                NodeList nodes = doc.getElementsByTagName("*");
                Node node;
                int nodeCount = nodes.getLength();
                for (int i = 0; i < nodeCount; i++) {
                    node = nodes.item(i);
                    if (node.hasChildNodes()) {
                        hm.put(node.getNodeName(), node.hasChildNodes() ? node.getFirstChild().getNodeValue() : "NULL");
                    }
                }
            } catch (ParserConfigurationException ex) {
                logger.applicationLog(logger.logPreString() + "Error PARSERCONFIG breakXML: " + ex.getMessage() + "\n\n", "", 54);
            } catch (SAXException ex) {
                logger.applicationLog(logger.logPreString() + "Error SAX breakXML: " + ex.getMessage() + "\n\n", "", 54);
            } catch (IOException ex) {
                logger.applicationLog(logger.logPreString() + "Error breakXML: " + ex.getMessage() + "\n\n", "", 54);
            }
        }
        return hm;
    }

    public String breakResponseXML(String strResp, String dynamicNode) throws ParserConfigurationException, SAXException, IOException {
        String setResultMsg = "";
        if (!strResp.contains("ERR@")) {
            InputStream file = new ByteArrayInputStream(strResp.getBytes(Charset.defaultCharset()));
            try {
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                dbFactory.setCoalescing(true);
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(file);
                doc.getDocumentElement().normalize();

                NodeList resultList = doc.getElementsByTagName("*");
                int size = resultList.getLength();

                for (int i = 0; i < size; i++) {
                    Element element = (Element) resultList.item(i);
                    String nodeName = element.getNodeName();
                    if (nodeName.equals(dynamicNode)) {
                        String setResultMsgx = "";
                        try {
                            if (element.getChildNodes().getLength() > 0) // then it has text
                            {
                                setResultMsgx = element.getChildNodes().item(0).getNodeValue();
                            } else {
                                setResultMsgx = "";
                            }
                            setResultMsg = setResultMsgx;
                        } catch (DOMException e) {
                            //log
                        }
                    }
                }
            } catch (IOException e) {
                //log
                return setResultMsg;
            } catch (ParserConfigurationException e) {
                //log
                return setResultMsg;
            } catch (SAXException e) {
                //log
                return setResultMsg;
            }
        } else {
            return setResultMsg;
        }
        return setResultMsg;
    }

}
