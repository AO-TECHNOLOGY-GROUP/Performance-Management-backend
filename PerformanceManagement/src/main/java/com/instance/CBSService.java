/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.instance;

import com.co.ke.main.EntryPoint;
import com.aogroup.za.util.Common;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import log.Logging;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author Ronald.Lang'at
 */
public class CBSService {

    private final Logging logger;
    JsonObject request;

    public CBSService(JsonObject data) {
        logger = new Logging();
        this.request = data;
    }

    String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/AGENCY/services";
    String ISA_T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ISA/services";
    String STATEMENT_T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/STATEMENT/services";

    public String getAgentDetails() {
        String customerNumber = request.getJsonObject("agent").getString("customerNumber");

        String responseString = null;

        try {
            String requestToSend = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:agen=\"http://temenos.com/AGENCY\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <agen:SearchAgentFloatAccnts>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLAGENTFLOATType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLAGENTFLOATType>\n"
                    + "      </agen:SearchAgentFloatAccnts>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String reqLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:agen=\"http://temenos.com/AGENCY\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <agen:SearchAgentFloatAccnts>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLAGENTFLOATType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLAGENTFLOATType>\n"
                    + "      </agen:SearchAgentFloatAccnts>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + " ~T24 Response ~ " + reqLog + "\n\n", "", 7);

            responseString = requestToCoreBanking(requestToSend, T24_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseString;
    }

    public String statement() {
        String account = request.getString("account");
        String date = request.getString("startDate");

        String responseString = null;

        try {
            String requestToSend = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:stat=\"http://temenos.com/STATEMENT\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <stat:ACSTATEMENT>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <CUSTACCTSTMTType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NO</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + account + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>DATE.FROM</columnName>\n"
                    + "               <criteriaValue>" + date + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "\n"
                    + "         </CUSTACCTSTMTType>\n"
                    + "      </stat:ACSTATEMENT>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String reqLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:stat=\"http://temenos.com/STATEMENT\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <stat:ACSTATEMENT>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <CUSTACCTSTMTType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NO</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + account + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>DATE.FROM</columnName>\n"
                    + "               <criteriaValue>" + date + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "\n"
                    + "         </CUSTACCTSTMTType>\n"
                    + "      </stat:ACSTATEMENT>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + " ~T24 Response ~ " + reqLog + STATEMENT_T24_URL + "\n\n", "", 7);

            responseString = requestToCoreBanking(requestToSend, STATEMENT_T24_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseString;
    }

    public String sendC2BToT24() {

        String C2BT24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MpesaIntegration/services";

        String responseTring = "";
        String accountNumber = request.getString("account");
        String amount = request.getString("TransAmount");
        String phone = "";
        if (request.getString("phoneNumber").length() > 12) {
            phone = "";
        } else {
            phone = request.getString("phoneNumber").replace("***", "-");
        }
        String mpesaRef = request.getString("mpesaRef");
        String narration = request.getString("adminNo").replaceAll("[^a-zA-Z0-9]", " ");
        
        System.out.println("TRAN ::: " + request);

        try {
            String messageRequest = "";
            if (request.getString("shortCode").equalsIgnoreCase("544600")) {
                messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mpes=\"http://temenos.com/MpesaIntegration\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERMPESACOLLECTIONS\">\n"
                        + "   <soapenv:Header/>\n"
                        + "   <soapenv:Body>\n"
                        + "      <mpes:MPESACollections>\n"
                        + "         <WebRequestCommon>\n"
                        + "            <!--Optional:-->\n"
                        + "            <company></company>\n"
                        + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                        + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                        + "         </WebRequestCommon>\n"
                        + "                  <OfsFunction>\n"
                        + "            <!--Optional:-->\n"
                        + "            <activityName></activityName>\n"
                        + "            <!--Optional:-->\n"
                        + "            <assignReason></assignReason>\n"
                        + "            <!--Optional:-->\n"
                        + "            <dueDate></dueDate>\n"
                        + "            <!--Optional:-->\n"
                        + "            <extProcess></extProcess>\n"
                        + "            <!--Optional:-->\n"
                        + "            <extProcessID></extProcessID>\n"
                        + "            <!--Optional:-->\n"
                        + "            <gtsControl></gtsControl>\n"
                        + "            <!--Optional:-->\n"
                        + "            <messageId></messageId>\n"
                        + "            <!--Optional:-->\n"
                        + "            <noOfAuth></noOfAuth>\n"
                        + "            <!--Optional:-->\n"
                        + "            <owner></owner>\n"
                        + "            <!--Optional:-->\n"
                        + "            <replace></replace>\n"
                        + "            <!--Optional:-->\n"
                        + "            <startDate></startDate>\n"
                        + "            <!--Optional:-->\n"
                        + "            <user></user>\n"
                        + "         </OfsFunction>\n"
                        + "\n"
                        + "         <FUNDSTRANSFERMPESACOLLECTIONSType id=\"\">\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:DebitAccountNo>KES1375300010001</fun:DebitAccountNo>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:DebitAmount>" + amount + "</fun:DebitAmount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:ReferenceNumber>" + mpesaRef + "</fun:ReferenceNumber>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:PhoneNumber>" + phone + "</fun:PhoneNumber>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:CreditAccount>" + accountNumber + "</fun:CreditAccount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:gPAYMENTDETAILS g=\"1\">\n"
                        + "               <!--Zero or more repetitions:-->\n"
                        + "               <fun:Naration>" + narration + "</fun:Naration>\n"
                        + "            </fun:gPAYMENTDETAILS>\n"
                        + "         </FUNDSTRANSFERMPESACOLLECTIONSType>\n"
                        + "      </mpes:MPESACollections>\n"
                        + "   </soapenv:Body>\n"
                        + "</soapenv:Envelope>";
            } else {
                messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mpes=\"http://temenos.com/MpesaIntegration\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERMPESACOLLECTIONS\">\n"
                        + "   <soapenv:Header/>\n"
                        + "   <soapenv:Body>\n"
                        + "      <mpes:MPESACollections>\n"
                        + "         <WebRequestCommon>\n"
                        + "            <!--Optional:-->\n"
                        + "            <company></company>\n"
                        + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                        + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                        + "         </WebRequestCommon>\n"
                        + "                  <OfsFunction>\n"
                        + "            <!--Optional:-->\n"
                        + "            <activityName></activityName>\n"
                        + "            <!--Optional:-->\n"
                        + "            <assignReason></assignReason>\n"
                        + "            <!--Optional:-->\n"
                        + "            <dueDate></dueDate>\n"
                        + "            <!--Optional:-->\n"
                        + "            <extProcess></extProcess>\n"
                        + "            <!--Optional:-->\n"
                        + "            <extProcessID></extProcessID>\n"
                        + "            <!--Optional:-->\n"
                        + "            <gtsControl></gtsControl>\n"
                        + "            <!--Optional:-->\n"
                        + "            <messageId></messageId>\n"
                        + "            <!--Optional:-->\n"
                        + "            <noOfAuth></noOfAuth>\n"
                        + "            <!--Optional:-->\n"
                        + "            <owner></owner>\n"
                        + "            <!--Optional:-->\n"
                        + "            <replace></replace>\n"
                        + "            <!--Optional:-->\n"
                        + "            <startDate></startDate>\n"
                        + "            <!--Optional:-->\n"
                        + "            <user></user>\n"
                        + "         </OfsFunction>\n"
                        + "\n"
                        + "         <FUNDSTRANSFERMPESACOLLECTIONSType id=\"\">\n"
                        //                    + "            <!--Optional:-->\n"
                        //                    + "            <fun:DebitAccountNo>"+ g +"</fun:DebitAccountNo>\n"
                        //                    + "            <!--Optional:-->\n"
                        //                    + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                        //                    + "            <!--Optional:-->\n"
                        + "            <fun:DebitAmount>" + amount + "</fun:DebitAmount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:ReferenceNumber>" + mpesaRef + "</fun:ReferenceNumber>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:PhoneNumber>" + phone + "</fun:PhoneNumber>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:CreditAccount>" + accountNumber + "</fun:CreditAccount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:gPAYMENTDETAILS g=\"1\">\n"
                        + "               <!--Zero or more repetitions:-->\n"
                        + "               <fun:Naration>" + narration + "</fun:Naration>\n"
                        + "            </fun:gPAYMENTDETAILS>\n"
                        + "         </FUNDSTRANSFERMPESACOLLECTIONSType>\n"
                        + "      </mpes:MPESACollections>\n"
                        + "   </soapenv:Body>\n"
                        + "</soapenv:Envelope>";
            }

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:mpes=\"http://temenos.com/MpesaIntegration\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERMPESACOLLECTIONS\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <mpes:MPESACollections>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "                  <OfsFunction>\n"
                    + "            <!--Optional:-->\n"
                    + "            <activityName></activityName>\n"
                    + "            <!--Optional:-->\n"
                    + "            <assignReason></assignReason>\n"
                    + "            <!--Optional:-->\n"
                    + "            <dueDate></dueDate>\n"
                    + "            <!--Optional:-->\n"
                    + "            <extProcess></extProcess>\n"
                    + "            <!--Optional:-->\n"
                    + "            <extProcessID></extProcessID>\n"
                    + "            <!--Optional:-->\n"
                    + "            <gtsControl></gtsControl>\n"
                    + "            <!--Optional:-->\n"
                    + "            <messageId></messageId>\n"
                    + "            <!--Optional:-->\n"
                    + "            <noOfAuth></noOfAuth>\n"
                    + "            <!--Optional:-->\n"
                    + "            <owner></owner>\n"
                    + "            <!--Optional:-->\n"
                    + "            <replace></replace>\n"
                    + "            <!--Optional:-->\n"
                    + "            <startDate></startDate>\n"
                    + "            <!--Optional:-->\n"
                    + "            <user></user>\n"
                    + "         </OfsFunction>\n"
                    + "\n"
                    + "         <FUNDSTRANSFERMPESACOLLECTIONSType id=\"\">\n"
                    //                    + "            <!--Optional:-->\n"
                    //                    + "            <fun:DebitAccountNo>"+ g +"</fun:DebitAccountNo>\n"
                    //                    + "            <!--Optional:-->\n"
                    //                    + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    //                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAmount>" + amount + "</fun:DebitAmount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ReferenceNumber>" + mpesaRef + "</fun:ReferenceNumber>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:PhoneNumber>" + phone + "</fun:PhoneNumber>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAccount>" + accountNumber + "</fun:CreditAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:gPAYMENTDETAILS g=\"1\">\n"
                    + "               <!--Zero or more repetitions:-->\n"
                    + "               <fun:Naration>" + narration + "</fun:Naration>\n"
                    + "            </fun:gPAYMENTDETAILS>\n"
                    + "         </FUNDSTRANSFERMPESACOLLECTIONSType>\n"
                    + "      </mpes:MPESACollections>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

//            System.out.println("messageRequestLog :: " + messageRequestLog);
            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 7);
            responseTring = requestToCoreBanking(messageRequest, C2BT24_URL);
            logger.applicationLog(logger.logPreString() + "Request From T24: " + responseTring + "\n\n", "", 8);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseTring;
    }

    public String getCustomerAccounts() {
        String identifier = request.getString("custNumber");

        String responseString = null;

        try {
            String requestToSend = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:agen=\"http://temenos.com/AGENCY\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <agen:AgentCustomerSearch>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <JSLAGENTSEARCHCUSTOMERType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CUST.NO</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + identifier + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </JSLAGENTSEARCHCUSTOMERType>\n"
                    + "      </agen:AgentCustomerSearch>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String reqLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:agen=\"http://temenos.com/AGENCY\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <agen:AgentCustomerSearch>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <JSLAGENTSEARCHCUSTOMERType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CUST.NO</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + identifier + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </JSLAGENTSEARCHCUSTOMERType>\n"
                    + "      </agen:AgentCustomerSearch>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + " ~T24 Response ~ " + reqLog + "\n\n", "", 7);

            responseString = requestToCoreBanking(requestToSend, T24_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseString;
    }

    public String getCustomerDetailsOnMemberNumber() {
        String identifier = request.getString("identifier");

        // String END_POINT = T24_URL + "/ISA/services";
        String responseString = null;

        try {
            String requestToSend = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:isa=\"http://temenos.com/ISA\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <isa:ISACUSTOMERDETAILS>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <CUSTDETAILSType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + identifier + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </CUSTDETAILSType>\n"
                    + "      </isa:ISACUSTOMERDETAILS>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String reqLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:isa=\"http://temenos.com/ISA\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <isa:ISACUSTOMERDETAILS>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <CUSTDETAILSType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + identifier + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </CUSTDETAILSType>\n"
                    + "      </isa:ISACUSTOMERDETAILS>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + " ~T24 Response ~ " + reqLog + "\n\n", "", 7);

            responseString = requestToCoreBanking(requestToSend, ISA_T24_URL);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return responseString;
    }

    public String requestToCoreBanking(String request, String transURL) {
        String response_complete = null;

        try {
            URL urlc = new URL(transURL);
            HttpURLConnection con = (HttpURLConnection) urlc.openConnection();

            byte[] buffer = request.getBytes();
            ByteArrayOutputStream request_msg = new ByteArrayOutputStream();
            request_msg.write(buffer);
            byte[] request_msgbyte = request_msg.toByteArray();

            con.setRequestProperty("Content-Length", String.valueOf(request_msgbyte.length));
            con.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            con.setReadTimeout(40000);
            con.setRequestMethod("POST");
            con.setDoOutput(true);
            con.setDoInput(true);

            OutputStream reqstream = con.getOutputStream();
            reqstream.write(request_msgbyte);
            reqstream.close();
            InputStreamReader isr = new InputStreamReader(con.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String response = "";

            while ((response = in.readLine()) != null) {
                response_complete = response_complete + response;
            }
//            reqstream.close();
//            isr.close();

            logger.applicationLog(logger.logPreString() + " ~T24 Response ~ " + response_complete + "\n\n", "", 8);

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            response_complete = null;
            logger.applicationLog(logger.logPreString() + "Error Sending request to T24  CBS - " + ex.getStackTrace() + "\n\n", "", 9);
        } catch (IOException ex) {
            response_complete = null;
            ex.printStackTrace();
//            ex.getStackTrace()
            response_complete = ex.getLocalizedMessage();
            logger.applicationLog(logger.logPreString() + "Error Sending request to T24 CBS - " + ex.getLocalizedMessage() + "\n\n", "", 9);
        } catch (Exception ex) {
            response_complete = null;
            ex.printStackTrace();
//            ex.getStackTrace()
            response_complete = ex.getLocalizedMessage();
            logger.applicationLog(logger.logPreString() + "Error Sending request to T24 CBS - " + ex.getLocalizedMessage() + "\n\n", "", 9);

        }
        return response_complete;
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

    public String breakResponseCurrentLoanXML(String strResp, String dynamicNode) throws ParserConfigurationException, SAXException, IOException {
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
                            if (setResultMsg.equalsIgnoreCase("CURRENT")) {
                                break;
                            }
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

    public String removeExtra_ns(String received) {
        String sendBack = null;
        sendBack = received.replaceAll("<ns.*?:", "<");
        sendBack = sendBack.replaceAll("</ns.*?:", "</");
        return sendBack;
    }
}
