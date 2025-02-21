/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.instance;

import com.aogroup.za.FetchBalance.fetchbalance;
import com.co.ke.main.EntryPoint;
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
import java.util.Map;
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

    public CBSService() {
        logger = new Logging();
    }

    String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/AGENCY/services";

    public String getCustomerAccounts(String CustomerNumber) {

        String responseString = "";

        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/AccountDetails/services";

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://temenos.com/AccountDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <acc:WebServiceAccountDets>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <ACCOUNTDETS1Type>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </ACCOUNTDETS1Type>\n"
                    + "      </acc:WebServiceAccountDets>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://temenos.com/AccountDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <acc:WebServiceAccountDets>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <ACCOUNTDETS1Type>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </ACCOUNTDETS1Type>\n"
                    + "      </acc:WebServiceAccountDets>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 5);
        }
        return responseString;
    }

    public String getCustomerAccountsDestinationAccounts(String CustomerNumber) {
        String responseString = "";

        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MocashAllAccounts/services";

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MocashAllAccounts\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:ShowNonTransAccts>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OTHERACCTSType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </OTHERACCTSType>\n"
                    + "      </moc:ShowNonTransAccts>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MocashAllAccounts\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:ShowNonTransAccts>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OTHERACCTSType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </OTHERACCTSType>\n"
                    + "      </moc:ShowNonTransAccts>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 5);
        }
        return responseString;
    }

    public String getCustomerDetails(JsonObject request) {

        String responseString = "";
        String CustomerNumber = request.getString("customerNumber");
        //create SOAP message

        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/CustomerDetails/services";

        try {
            // New API for only Registered Mmebers for Mocash
            String messageRequestNw = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://temenos.com/CustomerDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <cus:WsGetCustomer>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <GETCUSTOMERType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </GETCUSTOMERType>\n"
                    + "      </cus:WsGetCustomer>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:cus=\"http://temenos.com/CustomerDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <cus:WsGetCustomer>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <GETCUSTOMERType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </GETCUSTOMERType>\n"
                    + "      </cus:WsGetCustomer>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(messageRequestNw, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;

    }

    public String checkOffLoanBalance(JsonObject request) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        try {
            String getLoanBal = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetLoanBalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANBALANCESType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <criteriaValue>3190</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANBALANCESType>\n"
                    + "      </moc:GetLoanBalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            String getLoanBalToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetLoanBalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANBALANCESType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <criteriaValue>3190</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANBALANCESType>\n"
                    + "      </moc:GetLoanBalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            logger.applicationLog(logger.logPreString() + "Request To T24: " + getLoanBalToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(getLoanBal, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        String finalXml = removeExtra_ns(responseString);
        return finalXml;
        //return responseString;
    }

    public String checkCustomerExists(JsonObject request) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        String docId = request.getString("idNumber");
        String req = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CheckCustomerExists>\n"
                + "         <WebRequestCommon>\n"
                + "            <company></company>\n"
                + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <USLCHECKCUSTEXISTSType>\n"
                + "             <enquiryInputCollection>\n"
                + "                 <columnName>ALT.CUS.NO</columnName>\n"
                + "                 <criteriaValue>" + docId + "</criteriaValue>\n"
                + "                 <operand>EQ</operand>\n"
                + "             </enquiryInputCollection>"
                + "         </USLCHECKCUSTEXISTSType>\n"
                + "      </moc:CheckCustomerExists>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String reqToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CheckCustomerExists>\n"
                + "         <WebRequestCommon>\n"
                + "            <company></company>\n"
                + "            <password></password>\n"
                + "            <userName></userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <USLCHECKCUSTEXISTSType>\n"
                + "             <enquiryInputCollection>\n"
                + "                 <columnName>ALT.CUS.NO</columnName>\n"
                + "                 <criteriaValue>" + docId + "</criteriaValue>\n"
                + "                 <operand>EQ</operand>\n"
                + "             </enquiryInputCollection>"
                + "         </USLCHECKCUSTEXISTSType>\n"
                + "      </moc:CheckCustomerExists>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        try {

            logger.applicationLog(logger.logPreString() + "Request To T24: " + reqToLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(req, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        //return responseString;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String createCustomer(JsonObject request) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";

        String req = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:cus=\"http://temenos.com/CUSTOMERCREATENEWMOCASHCUST\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CreateCustomer>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company/>\n"
                + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction></OfsFunction>\n"
                + "         <CUSTOMERCREATENEWMOCASHCUSTType id=\"\">\n"
                + "            <!--Optional:-->\n"
                + "            <cus:gSHORTNAME g=\"1\">\n"
                + "               <!--Zero or more repetitions:-->\n"
                + "               <cus:LastName>" + request.getString("surname").toUpperCase() + "</cus:LastName>\n"
                + "            </cus:gSHORTNAME>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:gNAME1 g=\"1\">\n"
                + "               <!--Zero or more repetitions:-->\n"
                + "               <cus:FirstName>" + request.getString("firstName").toUpperCase() + "</cus:FirstName>\n"
                + "            </cus:gNAME1>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:NameonID>" + request.getString("firstName").toUpperCase() + " " + request.getString("middleName").toUpperCase() + " " + request.getString("surname").toUpperCase() + "</cus:NameonID>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:DateofIssue>" + request.getString("issueDate") + "</cus:DateofIssue>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:Gender>" + request.getString("gender") + "</cus:Gender>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:DateOfBirth>" + request.getString("dob") + "</cus:DateOfBirth>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:MaritalStatus>" + request.getString("maritalStatus") + "</cus:MaritalStatus>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:SMSPhoneNumber>+" + request.getString("account") + "</cus:SMSPhoneNumber>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:LegalIDNo>" + request.getString("idNumber") + "</cus:LegalIDNo>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:gUNAI.PHONE g=\"1\">\n"
                + "               <!--Zero or more repetitions:-->\n"
                + "               <cus:PhoneNumber>" + request.getString("account") + "</cus:PhoneNumber>\n"
                + "            </cus:gUNAI.PHONE>\n"
                + "         </CUSTOMERCREATENEWMOCASHCUSTType>\n"
                + "      </moc:CreateCustomer>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        String reqToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:cus=\"http://temenos.com/CUSTOMERCREATENEWMOCASHCUST\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CreateCustomer>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company/>\n"
                + "            <password></password>\n"
                + "            <userName></userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction></OfsFunction>\n"
                + "         <CUSTOMERCREATENEWMOCASHCUSTType id=\"\">\n"
                + "            <!--Optional:-->\n"
                + "            <cus:gSHORTNAME g=\"1\">\n"
                + "               <!--Zero or more repetitions:-->\n"
                + "               <cus:LastName>" + request.getString("surname") + "</cus:LastName>\n"
                + "            </cus:gSHORTNAME>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:gNAME1 g=\"1\">\n"
                + "               <!--Zero or more repetitions:-->\n"
                + "               <cus:FirstName>" + request.getString("firstName") + "</cus:FirstName>\n"
                + "            </cus:gNAME1>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:NameonID>" + request.getString("firstName") + " " + request.getString("middleName") + " " + request.getString("surname") + "</cus:NameonID>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:DateofIssue>" + request.getString("issueDate") + "</cus:DateofIssue>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:Gender>" + request.getString("gender") + "</cus:Gender>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:DateOfBirth>" + request.getString("dob") + "</cus:DateOfBirth>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:MaritalStatus>" + request.getString("maritalStatus") + "</cus:MaritalStatus>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:SMSPhoneNumber>" + request.getString("account") + "</cus:SMSPhoneNumber>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:LegalIDNo>" + request.getString("idNumber") + "</cus:LegalIDNo>\n"
                + "            <!--Optional:-->\n"
                + "            <cus:gUNAI.PHONE g=\"1\">\n"
                + "               <!--Zero or more repetitions:-->\n"
                + "               <cus:PhoneNumber>" + request.getString("account") + "</cus:PhoneNumber>\n"
                + "            </cus:gUNAI.PHONE>\n"
                + "         </CUSTOMERCREATENEWMOCASHCUSTType>\n"
                + "      </moc:CreateCustomer>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        try {

            logger.applicationLog(logger.logPreString() + "Request To T24: " + reqToLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(req, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        return responseString;
    }

    public String savingsXMLString(JsonObject map) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        String req = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:acc=\"http://temenos.com/ACCOUNTMOCASHOPENSB\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CreateSavingsAccount>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company/>\n"
                + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction/>\n"
                + "         <ACCOUNTMOCASHOPENSBType id=\"\">\n"
                + "            <acc:CustomerID>" + map.getString("customerNumber") + "</acc:CustomerID>\n"
                + "         </ACCOUNTMOCASHOPENSBType>\n"
                + "      </moc:CreateSavingsAccount>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        String reqToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:acc=\"http://temenos.com/ACCOUNTMOCASHOPENSB\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CreateSavingsAccount>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company/>\n"
                + "            <password></password>\n"
                + "            <userName></userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction/>\n"
                + "         <ACCOUNTMOCASHOPENSBType id=\"\">\n"
                + "            <acc:CustomerID>" + map.getString("customerNumber") + "</acc:CustomerID>\n"
                + "         </ACCOUNTMOCASHOPENSBType>\n"
                + "      </moc:CreateSavingsAccount>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        try {

            logger.applicationLog(logger.logPreString() + "Request To T24: " + reqToLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(req, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        return responseString;
    }

    public String accountXMLString(JsonObject accMap) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";

        String accXML = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:acc=\"http://temenos.com/ACCOUNTOPENNEWMOCASHAC\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CreateCurrentAccount>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company/>\n"
                + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction></OfsFunction>\n"
                + "         <ACCOUNTOPENNEWMOCASHACType id=\"\">\n"
                + "            <acc:CustomerID>" + accMap.getString("customerNumber") + "</acc:CustomerID>\n"
                + "         </ACCOUNTOPENNEWMOCASHACType>\n"
                + "      </moc:CreateCurrentAccount>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        String accXMLToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:acc=\"http://temenos.com/ACCOUNTOPENNEWMOCASHAC\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:CreateCurrentAccount>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company/>\n"
                + "            <password></password>\n"
                + "            <userName></userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction></OfsFunction>\n"
                + "         <ACCOUNTOPENNEWMOCASHACType id=\"\">\n"
                + "            <acc:CustomerID>" + accMap.getString("customerNumber") + "</acc:CustomerID>\n"
                + "         </ACCOUNTOPENNEWMOCASHACType>\n"
                + "      </moc:CreateCurrentAccount>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        try {

            logger.applicationLog(logger.logPreString() + "Request To T24: " + accXMLToLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(accXML, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        return responseString;
    }

    public String checkOffLoanBalance(String custNo) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        try {
            String getLoanBal = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetLoanBalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANBALANCESType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <criteriaValue>" + custNo + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <criteriaValue>3190</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANBALANCESType>\n"
                    + "      </moc:GetLoanBalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            String getLoanBalToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetLoanBalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANBALANCESType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <criteriaValue>" + custNo + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <criteriaValue>3190</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANBALANCESType>\n"
                    + "      </moc:GetLoanBalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            logger.applicationLog(logger.logPreString() + "Request To T24: " + getLoanBalToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(getLoanBal, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        String finalXml = removeExtra_ns(responseString);
        return finalXml;
        //return responseString;
    }

    public String getAccountDetails(String custNo) {
        String responseString = "";
        //String CustomerNumber = "ZZZZZZ";
        //create SOAP message
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/AccountDetails/services";

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://temenos.com/AccountDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <acc:WebServiceAccountDets>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <ACCOUNTDETS1Type>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + custNo + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </ACCOUNTDETS1Type>\n"
                    + "      </acc:WebServiceAccountDets>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://temenos.com/AccountDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <acc:WebServiceAccountDets>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <ACCOUNTDETS1Type>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + custNo + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </ACCOUNTDETS1Type>\n"
                    + "      </acc:WebServiceAccountDets>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String createNewLoan(JsonObject request, String branchCode) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        String accountTo = request.getString("accountTo");
        String amt = request.getString("amount");
//DONE
        String loanXML = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:aaar=\"http://temenos.com/AAARRANGEMENTACTIVITYACDW\">\n"
                + "  <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "   <moc:CreateNewLoan>\n"
                + "      <WebRequestCommon>\n"
                + "        <!--Optional:-->\n"
                + "        <company>" + branchCode + "</company>\n"
                + "        <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                + "       <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                + "    </WebRequestCommon>\n"
                + "   <OfsFunction/>\n"
                + "     <AAARRANGEMENTACTIVITYACDWType id=\"\">\n"
                + "         <!--Optional:-->\n"
                + "         <aaar:ARRANGEMENT>NEW</aaar:ARRANGEMENT>\n"
                + "          <!--Optional:-->\n"
                + "         <aaar:ACTIVITY>LENDING-NEW-ARRANGEMENT</aaar:ACTIVITY>\n"
                + "        <!--Optional:-->\n"
                + "         <aaar:EFFECTIVEDATE/>\n"
                + "         <!--Optional:-->\n"
                + "           <aaar:gCUSTOMER g=\"1\">\n"
                + "            <!--Zero or more repetitions:-->\n"
                + "             <aaar:mCUSTOMER m=\"1\">\n"
                + "                <!--Optional:-->\n"
                + "               <aaar:CUSTOMER>" + request.getString("customerNumber") + "</aaar:CUSTOMER>\n"
                + "               <!--Optional:-->\n"
                + "                <aaar:CUSTOMERROLE/>\n"
                + "             </aaar:mCUSTOMER>\n"
                + "         </aaar:gCUSTOMER>\n"
                + "          <aaar:PRODUCT>MOCASH.LOANS</aaar:PRODUCT>\n "
                + "         <!--Optional:-->\n"
                + "         <aaar:CURRENCY>KES</aaar:CURRENCY>\n"
                + "          <aaar:gPROPERTY g=\"1\">\n"
                + "             <!--Zero or more repetitions:-->\n"
                + "            <aaar:mPROPERTY m=\"1\">\n"
                + "                <!--Optional:-->\n"
                + "               <aaar:PROPERTY>COMMITMENT</aaar:PROPERTY>\n"
                + "                <aaar:EFFECTIVE/>\n"
                + "                <!--Optional:-->\n"
                + "               <aaar:sgFIELDNAME sg=\"1\">\n"
                + "                  <!--Zero or more repetitions:-->\n"
                + "                   <aaar:FIELDNAME s=\"1\">\n"
                + "                     <!--Optional:-->\n"
                + "                     <aaar:FIELDNAME>AMOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + amt + "</aaar:FIELDVALUE>\n"
                + "                </aaar:FIELDNAME>\n"
                + "             </aaar:sgFIELDNAME>\n"
                + "           </aaar:mPROPERTY>\n"
                + "            <aaar:mPROPERTY m=\"1\">\n"
                + "               <!--Optional:-->\n"
                + "               <aaar:PROPERTY>SETTLEMENT</aaar:PROPERTY>\n"
                + "                <aaar:EFFECTIVE/>\n"
                + "               <!--Optional:-->\n"
                + "                <aaar:sgFIELDNAME sg=\"1\">\n"
                + "                  <!--Zero or more repetitions:-->\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                     <!--Optional:-->\n"
                + "                     <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDNAME>PAYOUT.ACCOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "               </aaar:sgFIELDNAME>\n"
                + "            </aaar:mPROPERTY>\n"
                + "           <aaar:mPROPERTY m=\"1\">\n"
                + "              <!--Optional:-->\n"
                + "              <aaar:PROPERTY>OFFICERS</aaar:PROPERTY>\n"
                + "              <aaar:EFFECTIVE/>\n"
                + "              <!--Optional:-->\n"
                + "              <aaar:sgFIELDNAME sg=\"1\">\n"
                + "                 <!--Zero or more repetitions:-->\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                    <!--Optional:-->\n"
                + "                   <aaar:FIELDNAME>PRIMARY.OFFICER</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>113</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "              </aaar:sgFIELDNAME>\n"
                + "           </aaar:mPROPERTY>\n"
                + "        </aaar:gPROPERTY>\n"
                + "     </AAARRANGEMENTACTIVITYACDWType>\n"
                + "  </moc:CreateNewLoan>\n"
                + " </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        //done
        String loanXMLToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:aaar=\"http://temenos.com/AAARRANGEMENTACTIVITYACDW\">\n"
                + "  <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "   <moc:CreateNewLoan>\n"
                + "      <WebRequestCommon>\n"
                + "        <!--Optional:-->\n"
                + "        <company>" + branchCode + "</company>\n"
                + "        <password>*******</password>\n"
                + "       <userName>*******</userName>\n"
                + "    </WebRequestCommon>\n"
                + "   <OfsFunction/>\n"
                + "     <AAARRANGEMENTACTIVITYACDWType id=\"\">\n"
                + "         <!--Optional:-->\n"
                + "         <aaar:ARRANGEMENT>NEW</aaar:ARRANGEMENT>\n"
                + "          <!--Optional:-->\n"
                + "         <aaar:ACTIVITY>LENDING-NEW-ARRANGEMENT</aaar:ACTIVITY>\n"
                + "        <!--Optional:-->\n"
                + "         <aaar:EFFECTIVEDATE/>\n"
                + "         <!--Optional:-->\n"
                + "           <aaar:gCUSTOMER g=\"1\">\n"
                + "            <!--Zero or more repetitions:-->\n"
                + "             <aaar:mCUSTOMER m=\"1\">\n"
                + "                <!--Optional:-->\n"
                + "               <aaar:CUSTOMER>" + request.getString("customerNumber") + "</aaar:CUSTOMER>\n"
                + "               <!--Optional:-->\n"
                + "                <aaar:CUSTOMERROLE/>\n"
                + "             </aaar:mCUSTOMER>\n"
                + "         </aaar:gCUSTOMER>\n"
                + "          <aaar:PRODUCT>MOCASH.LOANS</aaar:PRODUCT>\n "
                + "         <!--Optional:-->\n"
                + "         <aaar:CURRENCY>KES</aaar:CURRENCY>\n"
                + "          <aaar:gPROPERTY g=\"1\">\n"
                + "             <!--Zero or more repetitions:-->\n"
                + "            <aaar:mPROPERTY m=\"1\">\n"
                + "                <!--Optional:-->\n"
                + "               <aaar:PROPERTY>COMMITMENT</aaar:PROPERTY>\n"
                + "                <aaar:EFFECTIVE/>\n"
                + "                <!--Optional:-->\n"
                + "               <aaar:sgFIELDNAME sg=\"1\">\n"
                + "                  <!--Zero or more repetitions:-->\n"
                + "                   <aaar:FIELDNAME s=\"1\">\n"
                + "                     <!--Optional:-->\n"
                + "                     <aaar:FIELDNAME>AMOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + amt + "</aaar:FIELDVALUE>\n"
                + "                </aaar:FIELDNAME>\n"
                + "             </aaar:sgFIELDNAME>\n"
                + "           </aaar:mPROPERTY>\n"
                + "            <aaar:mPROPERTY m=\"1\">\n"
                + "               <!--Optional:-->\n"
                + "               <aaar:PROPERTY>SETTLEMENT</aaar:PROPERTY>\n"
                + "                <aaar:EFFECTIVE/>\n"
                + "               <!--Optional:-->\n"
                + "                <aaar:sgFIELDNAME sg=\"1\">\n"
                + "                  <!--Zero or more repetitions:-->\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                     <!--Optional:-->\n"
                + "                     <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDNAME>PAYOUT.ACCOUNT</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "               </aaar:sgFIELDNAME>\n"
                + "            </aaar:mPROPERTY>\n"
                + "           <aaar:mPROPERTY m=\"1\">\n"
                + "              <!--Optional:-->\n"
                + "              <aaar:PROPERTY>OFFICERS</aaar:PROPERTY>\n"
                + "              <aaar:EFFECTIVE/>\n"
                + "              <!--Optional:-->\n"
                + "              <aaar:sgFIELDNAME sg=\"1\">\n"
                + "                 <!--Zero or more repetitions:-->\n"
                + "                 <aaar:FIELDNAME s=\"1\">\n"
                + "                    <!--Optional:-->\n"
                + "                   <aaar:FIELDNAME>PRIMARY.OFFICER</aaar:FIELDNAME>\n"
                + "                    <!--Optional:-->\n"
                + "                    <aaar:FIELDVALUE>113</aaar:FIELDVALUE>\n"
                + "                 </aaar:FIELDNAME>\n"
                + "              </aaar:sgFIELDNAME>\n"
                + "           </aaar:mPROPERTY>\n"
                + "        </aaar:gPROPERTY>\n"
                + "     </AAARRANGEMENTACTIVITYACDWType>\n"
                + "  </moc:CreateNewLoan>\n"
                + " </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        try {

            logger.applicationLog(logger.logPreString() + "Request To T24: " + loanXMLToLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(loanXML, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        //return responseString;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String disburseLoan(JsonObject request) {
        String responseString = "";
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";

        String disburseCheckOffXML = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERCHECKOFFLOANDISBURSE\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:DisburseNewLoan>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company></company>\n"
                + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction>\n"
                + "         </OfsFunction>\n"
                + "         <FUNDSTRANSFERCHECKOFFLOANDISBURSEType id=\"\">\n"
                + "            <!--Optional:-->\n"
                + "            <fun:ArrangementId>" + request.getString("arrangement") + "</fun:ArrangementId>\n"
                + "            <!--Optional:-->\n"
                + "            <fun:DebitAmount>" + request.getString("amount") + "</fun:DebitAmount>\n"
                + "            <!--Optional:-->\n"
                + "            <fun:CreditAccount>" + request.getString("accountTo") + "</fun:CreditAccount>\n"
                + "         </FUNDSTRANSFERCHECKOFFLOANDISBURSEType>\n"
                + "      </moc:DisburseNewLoan>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        String disburseCheckOffXMLToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERCHECKOFFLOANDISBURSE\">\n"
                + "   <soapenv:Header/>\n"
                + "   <soapenv:Body>\n"
                + "      <moc:DisburseNewLoan>\n"
                + "         <WebRequestCommon>\n"
                + "            <!--Optional:-->\n"
                + "            <company></company>\n"
                + "            <password></password>\n"
                + "            <userName></userName>\n"
                + "         </WebRequestCommon>\n"
                + "         <OfsFunction>\n"
                + "         </OfsFunction>\n"
                + "         <FUNDSTRANSFERCHECKOFFLOANDISBURSEType id=\"\">\n"
                + "            <!--Optional:-->\n"
                + "            <fun:ArrangementId>" + request.getString("arrangement") + "</fun:ArrangementId>\n"
                + "            <!--Optional:-->\n"
                + "            <fun:DebitAmount>" + request.getString("amount") + "</fun:DebitAmount>\n"
                + "            <!--Optional:-->\n"
                + "            <fun:CreditAccount>" + request.getString("accountTo") + "</fun:CreditAccount>\n"
                + "         </FUNDSTRANSFERCHECKOFFLOANDISBURSEType>\n"
                + "      </moc:DisburseNewLoan>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";

        try {

            logger.applicationLog(logger.logPreString() + "Request To T24: " + disburseCheckOffXMLToLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(disburseCheckOffXML, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        return responseString;
    }

    public String doAdvanceLimitCheck(JsonObject request) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";

        try {
            // New API for only Registered Mmebers for Mocash
            String cal = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetCustomerLimit>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETCUSTOMERLIMITType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETCUSTOMERLIMITType>\n"
                    + "      </moc:GetCustomerLimit>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String calToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetCustomerLimit>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETCUSTOMERLIMITType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETCUSTOMERLIMITType>\n"
                    + "      </moc:GetCustomerLimit>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            logger.applicationLog(logger.logPreString() + "Request To T24: " + calToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(cal, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseString;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String advanceLoanBalance(JsonObject request) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        try {
            String getLoanBal = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetLoanBalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANBALANCESType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <criteriaValue>3130</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANBALANCESType>\n"
                    + "      </moc:GetLoanBalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            String getLoanBalToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetLoanBalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANBALANCESType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <criteriaValue>3130</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANBALANCESType>\n"
                    + "      </moc:GetLoanBalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            logger.applicationLog(logger.logPreString() + "Request To T24: " + getLoanBalToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(getLoanBal, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseString;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String getAccountDetails(JsonObject request) {
        String responseString = "";
        String CustomerNumber = request.getString("customerNumber");
        //String CustomerNumber = "ZZZZZZ";
        //create SOAP message
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/AccountDetails/services";

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://temenos.com/AccountDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <acc:WebServiceAccountDets>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <ACCOUNTDETS1Type>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </ACCOUNTDETS1Type>\n"
                    + "      </acc:WebServiceAccountDets>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:acc=\"http://temenos.com/AccountDetails\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <acc:WebServiceAccountDets>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <ACCOUNTDETS1Type>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + CustomerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </ACCOUNTDETS1Type>\n"
                    + "      </acc:WebServiceAccountDets>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String doAdvanceLoanArrangement(JsonObject request, String branchCode) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String amt = request.getString("amount");
        String accountTo = request.getString("accountTo");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ADVMOCASH/services";

        try {
            // New API for only Registered Mmebers for Mocash
            String ala = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\" xmlns:aaar=\"http://temenos.com/AAARRANGEMENTACTIVITYUNSECUREDMOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "  <soapenv:Body>\n"
                    + "     <adv:CreateAdvanceMocashLoan>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "        </WebRequestCommon>\n"
                    + "         <OfsFunction/>\n"
                    + "        <AAARRANGEMENTACTIVITYUNSECUREDMOCASHType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "           <aaar:ARRANGEMENT>NEW</aaar:ARRANGEMENT>\n"
                    + "           <!--Optional:-->\n"
                    + "            <aaar:ACTIVITY>LENDING-NEW-ARRANGEMENT</aaar:ACTIVITY>\n"
                    + "            <!--Optional:-->\n"
                    + "            <aaar:EFFECTIVEDATE/>\n"
                    + "            <!--Optional:-->\n"
                    + "           <aaar:gCUSTOMER g=\"1\">\n"
                    + "               <!--Zero or more repetitions:-->\n"
                    + "               <aaar:mCUSTOMER m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:CUSTOMER>" + customerNumber + "</aaar:CUSTOMER>\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:CUSTOMERROLE/>\n"
                    + "              </aaar:mCUSTOMER>\n"
                    + "            </aaar:gCUSTOMER>\n"
                    + "            <!--Optional:-->\n"
                    + "            <aaar:PRODUCT>MO.CASH</aaar:PRODUCT>\n"
                    + "            <!--Optional:-->\n"
                    + "            <aaar:CURRENCY>KES</aaar:CURRENCY>\n"
                    + "            <aaar:gPROPERTY g=\"1\">\n"
                    + "               <!--Zero or more repetitions:-->\n"
                    + "               <aaar:mPROPERTY m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:PROPERTY>COMMITMENT</aaar:PROPERTY>\n"
                    + "                  <aaar:EFFECTIVE/>\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                     <!--Zero or more repetitions:-->\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>AMOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                       <aaar:FIELDVALUE>" + amt + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                  </aaar:sgFIELDNAME>\n"
                    + "               </aaar:mPROPERTY>\n"
                    + "              <aaar:mPROPERTY m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:PROPERTY>SETTLEMENT</aaar:PROPERTY>\n"
                    + "                  <aaar:EFFECTIVE/>\n"
                    + "                 <!--Optional:-->\n"
                    + "                 <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                    <!--Zero or more repetitions:-->\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                       <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                    <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PAYOUT.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                  </aaar:sgFIELDNAME>\n"
                    + "               </aaar:mPROPERTY>\n"
                    + "               <aaar:mPROPERTY m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:PROPERTY>OFFICERS</aaar:PROPERTY>\n"
                    + "                  <aaar:EFFECTIVE/>\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                     <!--Zero or more repetitions:-->\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PRIMARY.OFFICER</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>113</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                  </aaar:sgFIELDNAME>\n"
                    + "               </aaar:mPROPERTY>\n"
                    + "            </aaar:gPROPERTY>\n"
                    + "         </AAARRANGEMENTACTIVITYUNSECUREDMOCASHType>\n"
                    + "      </adv:CreateAdvanceMocashLoan>\n"
                    + "   </soapenv:Body>\n"
                    + " </soapenv:Envelope>";

            String alaToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\" xmlns:aaar=\"http://temenos.com/AAARRANGEMENTACTIVITYUNSECUREDMOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "  <soapenv:Body>\n"
                    + "     <adv:CreateAdvanceMocashLoan>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password>*******</password>\n"
                    + "            <userName>*******</userName>\n"
                    + "        </WebRequestCommon>\n"
                    + "         <OfsFunction/>\n"
                    + "        <AAARRANGEMENTACTIVITYUNSECUREDMOCASHType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "           <aaar:ARRANGEMENT>NEW</aaar:ARRANGEMENT>\n"
                    + "           <!--Optional:-->\n"
                    + "            <aaar:ACTIVITY>LENDING-NEW-ARRANGEMENT</aaar:ACTIVITY>\n"
                    + "            <!--Optional:-->\n"
                    + "            <aaar:EFFECTIVEDATE/>\n"
                    + "            <!--Optional:-->\n"
                    + "           <aaar:gCUSTOMER g=\"1\">\n"
                    + "               <!--Zero or more repetitions:-->\n"
                    + "               <aaar:mCUSTOMER m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:CUSTOMER>" + customerNumber + "</aaar:CUSTOMER>\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:CUSTOMERROLE/>\n"
                    + "              </aaar:mCUSTOMER>\n"
                    + "            </aaar:gCUSTOMER>\n"
                    + "            <!--Optional:-->\n"
                    + "            <aaar:PRODUCT>MO.CASH</aaar:PRODUCT>\n"
                    + "            <!--Optional:-->\n"
                    + "            <aaar:CURRENCY>KES</aaar:CURRENCY>\n"
                    + "            <aaar:gPROPERTY g=\"1\">\n"
                    + "               <!--Zero or more repetitions:-->\n"
                    + "               <aaar:mPROPERTY m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:PROPERTY>COMMITMENT</aaar:PROPERTY>\n"
                    + "                  <aaar:EFFECTIVE/>\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                     <!--Zero or more repetitions:-->\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>AMOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                       <aaar:FIELDVALUE>" + amt + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                  </aaar:sgFIELDNAME>\n"
                    + "               </aaar:mPROPERTY>\n"
                    + "              <aaar:mPROPERTY m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:PROPERTY>SETTLEMENT</aaar:PROPERTY>\n"
                    + "                  <aaar:EFFECTIVE/>\n"
                    + "                 <!--Optional:-->\n"
                    + "                 <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                    <!--Zero or more repetitions:-->\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                       <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PAYIN.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                    <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PAYOUT.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>" + accountTo + "</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                  </aaar:sgFIELDNAME>\n"
                    + "               </aaar:mPROPERTY>\n"
                    + "               <aaar:mPROPERTY m=\"1\">\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:PROPERTY>OFFICERS</aaar:PROPERTY>\n"
                    + "                  <aaar:EFFECTIVE/>\n"
                    + "                  <!--Optional:-->\n"
                    + "                  <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                     <!--Zero or more repetitions:-->\n"
                    + "                     <aaar:FIELDNAME s=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDNAME>PRIMARY.OFFICER</aaar:FIELDNAME>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:FIELDVALUE>113</aaar:FIELDVALUE>\n"
                    + "                     </aaar:FIELDNAME>\n"
                    + "                  </aaar:sgFIELDNAME>\n"
                    + "               </aaar:mPROPERTY>\n"
                    + "            </aaar:gPROPERTY>\n"
                    + "         </AAARRANGEMENTACTIVITYUNSECUREDMOCASHType>\n"
                    + "      </adv:CreateAdvanceMocashLoan>\n"
                    + "   </soapenv:Body>\n"
                    + " </soapenv:Envelope>";
            logger.applicationLog(logger.logPreString() + "Request To T24: " + alaToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(ala, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseString;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String doAdvanceLoanApplication(JsonObject request) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String amt = request.getString("amount");
        String creditAcc = request.getString("accountTo");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ADVMOCASH/services";

        try {
            // New API for only Registered Mmebers for Mocash
            String ala = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERUNSECUREDLOANDISBURSE\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:DisburseAdvanceMocashLoan>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction></OfsFunction>\n"
                    + "         <FUNDSTRANSFERUNSECUREDLOANDISBURSEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ArrangementId>" + request.getString("arrangement") + "</fun:ArrangementId>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAmount>" + amt + "</fun:DebitAmount>\n"
                    + "            <fun:CreditAccount>" + creditAcc + "</fun:CreditAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "         </FUNDSTRANSFERUNSECUREDLOANDISBURSEType>\n"
                    + "      </adv:DisburseAdvanceMocashLoan>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String alaToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERUNSECUREDLOANDISBURSE\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:DisburseAdvanceMocashLoan>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction></OfsFunction>\n"
                    + "         <FUNDSTRANSFERUNSECUREDLOANDISBURSEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ArrangementId>" + request.getString("arrangement") + "</fun:ArrangementId>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAmount>" + amt + "</fun:DebitAmount>\n"
                    + "            <fun:CreditAccount>" + creditAcc + "</fun:CreditAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "         </FUNDSTRANSFERUNSECUREDLOANDISBURSEType>\n"
                    + "      </adv:DisburseAdvanceMocashLoan>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + alaToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(ala, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseString;
    }

    public String getBalanceInquiry(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/balanceenquiry/services";

        String responseString = "";
        String AccountNumber = request.getString("account");
        //create SOAP message
        try {
            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bal=\"http://temenos.com/balanceenquiry\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <bal:BalanceEnquiry>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <MCACCTBALType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ACCOUNT.NUMBER</columnName>\n"
                    + "               <criteriaValue>" + AccountNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </MCACCTBALType>\n"
                    + "      </bal:BalanceEnquiry>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            // Log
            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bal=\"http://temenos.com/balanceenquiry\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <bal:BalanceEnquiry>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <MCACCTBALType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>ACCOUNT.NUMBER</columnName>\n"
                    + "               <criteriaValue>" + AccountNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </MCACCTBALType>\n"
                    + "      </bal:BalanceEnquiry>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String chargeBalanceEnquiry(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/BalanceEnquiryCharges/services";

        String responseString = "";
        String AccountNumber = request.getString("account");

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bal=\"http://temenos.com/BalanceEnquiryCharges\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERCHARGEBALENQ\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <bal:requesttoChargeBalEnq>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "            \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERCHARGEBALENQType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAC>" + AccountNumber + "</fun:DebitAC>\n"
                    + "         </FUNDSTRANSFERCHARGEBALENQType>\n"
                    + "      </bal:requesttoChargeBalEnq>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:bal=\"http://temenos.com/BalanceEnquiryCharges\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERCHARGEBALENQ\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <bal:requesttoChargeBalEnq>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "            \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERCHARGEBALENQType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAC>" + AccountNumber + "</fun:DebitAC>\n"
                    + "         </FUNDSTRANSFERCHARGEBALENQType>\n"
                    + "      </bal:requesttoChargeBalEnq>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24 Charges Balance Enquiry : " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);
            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String getMinistatement(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MocashMiniStatement/services";

        String responseString = "";
        String AccountNumber = request.getString("account");
        String tranId = request.getString("tranId");
        Map<String, Object> responseMap = new HashMap<String, Object>();

        logger.applicationLog(logger.logPreString() + "Request To T24: " + request + "\n\n", "", 30);

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MocashMiniStatement\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:MinisatementforMocash>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <MOCASHMINISTMTType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NO</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + AccountNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </MOCASHMINISTMTType>\n"
                    + "      </moc:MinisatementforMocash>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MocashMiniStatement\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:MinisatementforMocash>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <MOCASHMINISTMTType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NO</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + AccountNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </MOCASHMINISTMTType>\n"
                    + "      </moc:MinisatementforMocash>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);
            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseString;
//        String finalXml = removeExtra_ns(responseTring);
//        return finalXml;
    }

    public String chargeMinistatement(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ChargeMiniStatement/services";

        String responseString = "";
        String AccountNumber = request.getString("account");

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:char=\"http://temenos.com/ChargeMiniStatement\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERSLKMOCASHMINI\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <char:ChargeMiniStatement>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction></OfsFunction>\n"
                    + "         <FUNDSTRANSFERSLKMOCASHMINIType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "         </FUNDSTRANSFERSLKMOCASHMINIType>\n"
                    + "      </char:ChargeMiniStatement>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:char=\"http://temenos.com/ChargeMiniStatement\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERSLKMOCASHMINI\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <char:ChargeMiniStatement>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction></OfsFunction>\n"
                    + "         <FUNDSTRANSFERSLKMOCASHMINIType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "         </FUNDSTRANSFERSLKMOCASHMINIType>\n"
                    + "      </char:ChargeMiniStatement>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24 Charge Ministatement : " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String doFundstransfer(JsonObject request) {
        String T24_URL = null;

        String responseString = "";
        String accountTo = request.getString("accountTo");
        String accountFrom = request.getString("accountFrom");
        String amount = request.getString("amount");
        String tranId = request.getString("tranId");
        String transferType = request.getString("transferType");
        String messageRequest = "";

        try {

            if (transferType.equalsIgnoreCase("own")) {

                T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/OwnAccountTransfer/services";

                messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:own=\"http://temenos.com/OwnAccountTransfer\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFEROWNACCTTRANSFER\">\n"
                        + "   <soapenv:Header/>\n"
                        + "   <soapenv:Body>\n"
                        + "      <own:OwnAccountTransfer>\n"
                        + "         <WebRequestCommon>\n"
                        + "            <!--Optional:-->\n"
                        + "            <company></company>\n"
                        + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                        + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                        + "         </WebRequestCommon>\n"
                        + "         <OfsFunction>\n"
                        + "           \n"
                        + "         </OfsFunction>\n"
                        + "         <FUNDSTRANSFEROWNACCTTRANSFERType id=\"\">\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:DebitAccount>" + accountFrom + "</fun:DebitAccount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:DebitAmount>" + amount + "</fun:DebitAmount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun:CreditAccount>" + accountTo + "</fun:CreditAccount>\n"
                        + "            <!--Optional:-->\n"
                        + "         </FUNDSTRANSFEROWNACCTTRANSFERType>\n"
                        + "      </own:OwnAccountTransfer>\n"
                        + "   </soapenv:Body>\n"
                        + "</soapenv:Envelope>";

            } else {

                T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/FundsTransfer/services";
                messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fun=\"http://temenos.com/FundsTransfer\" xmlns:fun1=\"http://temenos.com/FUNDSTRANSFERACCTTRANSFER\">\n"
                        + "   <soapenv:Header/>\n"
                        + "   <soapenv:Body>\n"
                        + "      <fun:TransferBtwnAccounts>\n"
                        + "         <WebRequestCommon>\n"
                        + "            <company></company>\n"
                        + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                        + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                        + "         </WebRequestCommon>\n"
                        + "         <OfsFunction>\n"
                        + "         </OfsFunction>\n"
                        + "         <FUNDSTRANSFERACCTTRANSFERType id=\"\">\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun1:DebitAccount>" + accountFrom + "</fun1:DebitAccount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun1:DebitCurrency>KES</fun1:DebitCurrency>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun1:DebitAmount>" + amount + "</fun1:DebitAmount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun1:CreditAccount>" + accountTo + "</fun1:CreditAccount>\n"
                        + "            <!--Optional:-->\n"
                        + "            <fun1:ThirdpartyID>" + tranId + "</fun1:ThirdpartyID>\n"
                        + "         </FUNDSTRANSFERACCTTRANSFERType>\n"
                        + "      </fun:TransferBtwnAccounts>\n"
                        + "   </soapenv:Body>\n"
                        + "</soapenv:Envelope>";

            }

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:fun=\"http://temenos.com/FundsTransfer\" xmlns:fun1=\"http://temenos.com/FUNDSTRANSFERACCTTRANSFER\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <fun:TransferBtwnAccounts>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERACCTTRANSFERType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun1:DebitAccount>" + accountFrom + "</fun1:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun1:DebitCurrency>KES</fun1:DebitCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun1:DebitAmount>" + amount + "</fun1:DebitAmount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun1:CreditAccount>" + accountTo + "</fun1:CreditAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun1:ThirdpartyID>" + tranId + "</fun1:ThirdpartyID>\n"
                    + "         </FUNDSTRANSFERACCTTRANSFERType>\n"
                    + "      </fun:TransferBtwnAccounts>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String rejectFundsTransfer(String branchCode, String transactionId) {
        String responseString = "";

        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/OwnAccountTransfer/services";
        try {

            String rejectFundsTransfer = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/OwnAccountTransfer\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:RejectFundsTransfer>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password></password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <!--Optional:-->\n"
                    + "         <FUNDSTRANSFERREJECTOWNTFRType>\n"
                    + "            <!--Optional:-->\n"
                    + "            <transactionId>" + transactionId + "</transactionId>\n"
                    + "         </FUNDSTRANSFERREJECTOWNTFRType>\n"
                    + "      </adv:RejectFundsTransfer>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String rejectFundsTransferToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/OwnAccountTransfer\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:RejectFundsTransfer>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <!--Optional:-->\n"
                    + "         <FUNDSTRANSFERREJECTOWNTFRType>\n"
                    + "            <!--Optional:-->\n"
                    + "            <transactionId>" + transactionId + "</transactionId>\n"
                    + "         </FUNDSTRANSFERREJECTOWNTFRType>\n"
                    + "      </adv:RejectFundsTransfer>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + rejectFundsTransferToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(rejectFundsTransfer, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String getRecipientFTDetail(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/GetCustomerName/services";

        String responseString = "";
        String accountTo = request.getString("accountTo");

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:get=\"http://temenos.com/GetCustomerName\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <get:GetCustomerName>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <RETURNNAMEType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NUMBER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + accountTo + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </RETURNNAMEType>\n"
                    + "      </get:GetCustomerName>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";
            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:get=\"http://temenos.com/GetCustomerName\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <get:GetCustomerName>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <RETURNNAMEType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NUMBER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + accountTo + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </RETURNNAMEType>\n"
                    + "      </get:GetCustomerName>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String purchaseAirtime(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/AirtimePurchase/services";

        String responseString = "";
        String AccountNumber = request.getString("account");
        String amount = request.getString("amount");
        String tranId = request.getString("tranId");
        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:air=\"http://temenos.com/AirtimePurchase\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERBUYAIRTIME\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <air:AirtimePurchase>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "           \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERBUYAIRTIMEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditCurrency>KES</fun:CreditCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAmount>" + amount + "</fun:CreditAmount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ThirdPartyID>" + tranId + "</fun:ThirdPartyID>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:PaymentDescription>Airtime Purchase</fun:PaymentDescription>\n"
                    + "         </FUNDSTRANSFERBUYAIRTIMEType>\n"
                    + "      </air:AirtimePurchase>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:air=\"http://temenos.com/AirtimePurchase\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERBUYAIRTIME\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <air:AirtimePurchase>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "           \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERBUYAIRTIMEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditCurrency>KES</fun:CreditCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAmount>" + amount + "</fun:CreditAmount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ThirdPartyID>" + tranId + "</fun:ThirdPartyID>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:PaymentDescription>Airtime Purchase</fun:PaymentDescription>\n"
                    + "         </FUNDSTRANSFERBUYAIRTIMEType>\n"
                    + "      </air:AirtimePurchase>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseString;
    }

    public String sendC2BToT24(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/mocashdeposit/services";

        String responseString = "";
        String AccountNumber = request.getString("account");
        String amount = request.getString("amount");
        String tranId = request.getString("tranId");
        String mpesaRef = "";
        if (request.getString("mpesaRef").length() > 23) {
            mpesaRef = request.getString("mpesaRef").substring(0, 16);
        } else {
            mpesaRef = request.getString("mpesaRef");
        }
//        String uniqueMobileRef = request.getString("uniqueRef");

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/mocashdeposit\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERSLKMOCASHDEP\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:MocashDeposit>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "          \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERSLKMOCASHDEPType id=\"\">\n"
                    + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    + "            <fun:DebitAmount>" + amount + "</fun:DebitAmount>\n"
                    + "            <fun:CreditAccount>" + AccountNumber + "</fun:CreditAccount>\n"
                    + "            <fun:MobileUniqueRef>" + mpesaRef + "</fun:MobileUniqueRef>\n"
                    + "            <fun:MobileRef>" + mpesaRef + "</fun:MobileRef>\n"
                    + "         </FUNDSTRANSFERSLKMOCASHDEPType>\n"
                    + "      </moc:MocashDeposit>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/mocashdeposit\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERSLKMOCASHDEP\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:MocashDeposit>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "          \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERSLKMOCASHDEPType id=\"\">\n"
                    + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    + "            <fun:DebitAmount>" + amount + "</fun:DebitAmount>\n"
                    + "            <fun:CreditAccount>" + AccountNumber + "</fun:CreditAccount>\n"
                    + "            <fun:MobileUniqueRef>" + mpesaRef + "</fun:MobileUniqueRef>\n"
                    + "            <fun:MobileRef>" + mpesaRef + "</fun:MobileRef>\n"
                    + "         </FUNDSTRANSFERSLKMOCASHDEPType>\n"
                    + "      </moc:MocashDeposit>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseString;
    }

    public String sendC2BToT24ToChargeAccount(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/DepositsChargeService/services";

        String responseString = "";
        String AccountNumber = request.getString("account");
        String mpesaRef = request.getString("mpesaRef");
        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dep=\"http://temenos.com/DepositsChargeService\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERSLKDEPCHARGE\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <dep:ChargeMobileDeposits>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "           <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERSLKDEPCHARGEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:MobileRef>" + mpesaRef + "</fun:MobileRef>\n"
                    + "         </FUNDSTRANSFERSLKDEPCHARGEType>\n"
                    + "      </dep:ChargeMobileDeposits>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:dep=\"http://temenos.com/DepositsChargeService\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERSLKDEPCHARGE\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <dep:ChargeMobileDeposits>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "           <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERSLKDEPCHARGEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:MobileRef>" + mpesaRef + "</fun:MobileRef>\n"
                    + "         </FUNDSTRANSFERSLKDEPCHARGEType>\n"
                    + "      </dep:ChargeMobileDeposits>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24 Charges C2B : " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);
            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseString;
    }

    public String getRecipientC2BDetail(String accountToDeposit) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/GetCustomerName/services";

        String responseString = "";
        String accountTo = accountToDeposit;

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:get=\"http://temenos.com/GetCustomerName\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <get:GetCustomerName>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <RETURNNAMEType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NUMBER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + accountTo + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </RETURNNAMEType>\n"
                    + "      </get:GetCustomerName>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:get=\"http://temenos.com/GetCustomerName\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <get:GetCustomerName>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <RETURNNAMEType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ACCOUNT.NUMBER</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + accountTo + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </RETURNNAMEType>\n"
                    + "      </get:GetCustomerName>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String sendB2CToCore(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/withdrawals/services";
        String responseString = "";
        String AccountNumber = request.getString("account");
        String amount = request.getString("amount");
        String tranId = request.getString("phonenumber");

        logger.applicationLog(logger.logPreString() + "Request To T24: MAP >>>> " + request + "\n\n", "", 52);

        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wit=\"http://temenos.com/withdrawals\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERWITHDRAWMOCASHSLK\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <wit:WebServiceWithdrawal>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "           \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERWITHDRAWMOCASHSLKType id=\"\">\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <fun:CreditCurrency>KES</fun:CreditCurrency>\n"
                    + "            <fun:CreditAmount>" + amount + "</fun:CreditAmount>\n"
                    + "            <fun:MobileRef>" + tranId + "</fun:MobileRef>\n"
                    + "         </FUNDSTRANSFERWITHDRAWMOCASHSLKType>\n"
                    + "      </wit:WebServiceWithdrawal>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:wit=\"http://temenos.com/withdrawals\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERWITHDRAWMOCASHSLK\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <wit:WebServiceWithdrawal>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "           \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERWITHDRAWMOCASHSLKType id=\"\">\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <fun:CreditCurrency>KES</fun:CreditCurrency>\n"
                    + "            <fun:CreditAmount>" + amount + "</fun:CreditAmount>\n"
                    + "            <fun:MobileRef>" + tranId + "</fun:MobileRef>\n"
                    + "         </FUNDSTRANSFERWITHDRAWMOCASHSLKType>\n"
                    + "      </wit:WebServiceWithdrawal>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 52);
            responseString = requestToCoreBanking(messageRequest, T24_URL);
            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseTring;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String makeUtilityPayment(JsonObject request) {
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/UtilityPayment/services";

        String responseString = "";
        String AccountNumber = request.getString("account");
        String amount = request.getString("amount");
        String tranId = request.getString("tranId");
        String biller = request.getString("biller");
        try {

            String messageRequest = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:util=\"http://temenos.com/UtilityPayment\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERUTILITYPURCHASE\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <util:utilitypurchase>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "           <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERUTILITYPURCHASEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditCurrency>KES</fun:CreditCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAmount>" + amount + "</fun:CreditAmount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ThirdPartyID>" + tranId + "</fun:ThirdPartyID>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:PaymentDesciption>" + biller + " Payment</fun:PaymentDesciption>\n"
                    + "         </FUNDSTRANSFERUTILITYPURCHASEType>\n"
                    + "      </util:utilitypurchase>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String messageRequestLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:util=\"http://temenos.com/UtilityPayment\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERUTILITYPURCHASE\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <util:utilitypurchase>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "           <company></company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         \n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERUTILITYPURCHASEType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:DebitAccount>" + AccountNumber + "</fun:DebitAccount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditCurrency>KES</fun:CreditCurrency>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAmount>" + amount + "</fun:CreditAmount>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:ThirdPartyID>" + tranId + "</fun:ThirdPartyID>\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:PaymentDesciption>" + biller + " Payment</fun:PaymentDesciption>\n"
                    + "         </FUNDSTRANSFERUTILITYPURCHASEType>\n"
                    + "      </util:utilitypurchase>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + messageRequestLog + "\n\n", "", 3);
            responseString = requestToCoreBanking(messageRequest, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 3);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return responseString;
    }

    public String getLoanAccount(JsonObject request, String branchCode) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/MOCASH/services";
        try {

            String getLoanAccount = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetMocashAccount>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETMOCASHACType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETMOCASHACType>\n"
                    + "      </moc:GetMocashAccount>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String getLoanAccountLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:moc=\"http://temenos.com/MOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <moc:GetMocashAccount>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password>*******</password>\n"
                    + "            <userName>*******</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETMOCASHACType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <columnName>CUSTOMER</columnName>\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETMOCASHACType>\n"
                    + "      </moc:GetMocashAccount>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + getLoanAccountLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(getLoanAccount, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        //return responseString;
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String getLoanAccountLoanRepayment(JsonObject request) {
        String responseString = "";
        String customerNumber = request.getString("customerNumber");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ADVMOCASH/services";
        try {

            String getLoanAccountLoanRepayment = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:GetLoanAccount>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password/>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANACCTType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + request.getString("loanType") + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANACCTType>\n"
                    + "      </adv:GetLoanAccount>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String getLoanAccountLoanRepaymentLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:GetLoanAccount>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company/>\n"
                    + "            <password/>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <USLGETLOANACCTType>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ID</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + customerNumber + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>CATEG</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + request.getString("loanType") + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </USLGETLOANACCTType>\n"
                    + "      </adv:GetLoanAccount>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + getLoanAccountLoanRepaymentLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(getLoanAccountLoanRepayment, T24_URL);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String processPayoffLoan(JsonObject request) {
        String responseString = "";
        String loanRepayAccount = request.getString("loanRepayAccount");
        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ADVMOCASH/services";
        try {

            String getLoanAccountLoanRepayment = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERPAYOFFMOBILOAN\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:PayoffMobiloan>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERPAYOFFMOBILOANType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAccount>" + loanRepayAccount + "</fun:CreditAccount>\n"
                    + "         </FUNDSTRANSFERPAYOFFMOBILOANType>\n"
                    + "      </adv:PayoffMobiloan>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String getLoanAccountLoanRepaymentLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERPAYOFFMOBILOAN\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:PayoffMobiloan>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>*******</password>\n"
                    + "            <userName>*******</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <OfsFunction>\n"
                    + "         </OfsFunction>\n"
                    + "         <FUNDSTRANSFERPAYOFFMOBILOANType id=\"\">\n"
                    + "            <!--Optional:-->\n"
                    + "            <fun:CreditAccount>" + loanRepayAccount + "</fun:CreditAccount>\n"
                    + "         </FUNDSTRANSFERPAYOFFMOBILOANType>\n"
                    + "      </adv:PayoffMobiloan>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + getLoanAccountLoanRepaymentLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(getLoanAccountLoanRepayment, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String rejectPayoffLoan(String branchCode, String transactionId) {
        String responseString = "";

        String T24_URL = "" + EntryPoint.T24_IP + ":" + EntryPoint.T24_PORT + "/ADVMOCASH/services";
        try {

            String rejectLoanRepayment = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:RejectMobiloanRepay>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password></password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <!--Optional:-->\n"
                    + "         <FUNDSTRANSFERREJECTMOBIPAYType>\n"
                    + "            <!--Optional:-->\n"
                    + "            <transactionId>" + transactionId + "</transactionId>\n"
                    + "         </FUNDSTRANSFERREJECTMOBIPAYType>\n"
                    + "      </adv:RejectMobiloanRepay>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String rejectLoanRepaymentToLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:adv=\"http://temenos.com/ADVMOCASH\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <adv:RejectMobiloanRepay>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company>" + branchCode + "</company>\n"
                    + "            <password></password>\n"
                    + "            <userName></userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <!--Optional:-->\n"
                    + "         <FUNDSTRANSFERREJECTMOBIPAYType>\n"
                    + "            <!--Optional:-->\n"
                    + "            <transactionId>" + transactionId + "</transactionId>\n"
                    + "         </FUNDSTRANSFERREJECTMOBIPAYType>\n"
                    + "      </adv:RejectMobiloanRepay>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + rejectLoanRepaymentToLog + "\n\n", "", 52);

            responseString = requestToCoreBanking(rejectLoanRepayment, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + responseString + "\n\n", "", 53);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        String finalXml = removeExtra_ns(responseString);
        return finalXml;
    }

    public String createMajaniLoan(JsonObject req, String branch) {
        String response = "";
        String T24_URL = "" + EntryPoint.AGRI_IP + ":" + EntryPoint.AGRI_PORT + "/agricapis/services";
        try {
            String majaniReq = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://temenos.com/testa\" xmlns:aaar=\"http://temenos.com/AAARRANGEMENTACTIVITYKROPAANEW\">\n"
                    + "    <soapenv:Header/>\n"
                    + "    <soapenv:Body>\n"
                    + "        <tes:CreateKropLoan>\n"
                    + "            <WebRequestCommon>\n"
                    + "                <!--Optional:-->\n"
                    + "                <company>" + branch + "</company>\n"
                    + "                <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "                <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "            </WebRequestCommon>\n"
                    + "            <OfsFunction/>\n"
                    + "            <AAARRANGEMENTACTIVITYKROPAANEWType>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:ARRANGEMENT>NEW</aaar:ARRANGEMENT>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:ACTIVITY>LENDING-NEW-ARRANGEMENT</aaar:ACTIVITY>\n"
                    + "                <!--Optional:-->\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:gCUSTOMER>\n"
                    + "                    <!--Zero or more repetitions:-->\n"
                    + "                    <aaar:mCUSTOMER>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:CUSTOMER>" + req.getString("customerNumber") + "</aaar:CUSTOMER>\n"
                    + "                        <!--Optional:-->\n"
                    + "\n"
                    + "                    </aaar:mCUSTOMER>\n"
                    + "                </aaar:gCUSTOMER>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:PRODUCT>MAJANI.TOPUP</aaar:PRODUCT>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:CURRENCY>KES</aaar:CURRENCY>\n"
                    + "                <aaar:gPROPERTY>\n"
                    + "                    <aaar:mPROPERTY m=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:PROPERTY>COMMITMENT</aaar:PROPERTY>\n"
                    + "                        <aaar:EFFECTIVE/>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                            <!--Zero or more repetitions:-->\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>AMOUNT</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("amount") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                        </aaar:sgFIELDNAME>\n"
                    + "                    </aaar:mPROPERTY>\n"
                    + "                    <aaar:mPROPERTY>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:PROPERTY>SETTLEMENT</aaar:PROPERTY>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:sgFIELDNAME>\n"
                    + "                            <!--Zero or more repetitions:-->\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>PAYIN.ACCOUNT:1:1</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("accountTo") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>PAYIN.ACCOUNT:2:1</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("accountTo") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>PAYOUT.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("accountTo") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                        </aaar:sgFIELDNAME>\n"
                    + "                    </aaar:mPROPERTY>\n"
                    + "                </aaar:gPROPERTY>\n"
                    + "                <aaar:GRPSEQUENCE></aaar:GRPSEQUENCE>\n"
                    + "            </AAARRANGEMENTACTIVITYKROPAANEWType>\n"
                    + "        </tes:CreateKropLoan>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String majaniReqLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://temenos.com/testa\" xmlns:aaar=\"http://temenos.com/AAARRANGEMENTACTIVITYKROPAANEW\">\n"
                    + "    <soapenv:Header/>\n"
                    + "    <soapenv:Body>\n"
                    + "        <tes:CreateKropLoan>\n"
                    + "            <WebRequestCommon>\n"
                    + "                <!--Optional:-->\n"
                    + "                <company></company>\n"
                    + "                <password></password>\n"
                    + "                <userName></userName>\n"
                    + "            </WebRequestCommon>\n"
                    + "            <OfsFunction/>\n"
                    + "            <AAARRANGEMENTACTIVITYKROPAANEWType>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:ARRANGEMENT>NEW</aaar:ARRANGEMENT>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:ACTIVITY>LENDING-NEW-ARRANGEMENT</aaar:ACTIVITY>\n"
                    + "                <!--Optional:-->\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:gCUSTOMER>\n"
                    + "                    <!--Zero or more repetitions:-->\n"
                    + "                    <aaar:mCUSTOMER>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:CUSTOMER>" + req.getString("customerNumber") + "</aaar:CUSTOMER>\n"
                    + "                        <!--Optional:-->\n"
                    + "\n"
                    + "                    </aaar:mCUSTOMER>\n"
                    + "                </aaar:gCUSTOMER>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:PRODUCT>MAJANI.TOPUP</aaar:PRODUCT>\n"
                    + "                <!--Optional:-->\n"
                    + "                <aaar:CURRENCY>KES</aaar:CURRENCY>\n"
                    + "                <aaar:gPROPERTY>\n"
                    + "                    <aaar:mPROPERTY m=\"1\">\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:PROPERTY>COMMITMENT</aaar:PROPERTY>\n"
                    + "                        <aaar:EFFECTIVE/>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:sgFIELDNAME sg=\"1\">\n"
                    + "                            <!--Zero or more repetitions:-->\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>AMOUNT</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("amount") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                        </aaar:sgFIELDNAME>\n"
                    + "                    </aaar:mPROPERTY>\n"
                    + "                    <aaar:mPROPERTY>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:PROPERTY>SETTLEMENT</aaar:PROPERTY>\n"
                    + "                        <!--Optional:-->\n"
                    + "                        <aaar:sgFIELDNAME>\n"
                    + "                            <!--Zero or more repetitions:-->\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>PAYIN.ACCOUNT:1:1</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("accountTo") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>PAYIN.ACCOUNT:2:1</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("accountTo") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                            <aaar:FIELDNAME s=\"1\">\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDNAME>PAYOUT.ACCOUNT</aaar:FIELDNAME>\n"
                    + "                                <!--Optional:-->\n"
                    + "                                <aaar:FIELDVALUE>" + req.getString("accountTo") + "</aaar:FIELDVALUE>\n"
                    + "                            </aaar:FIELDNAME>\n"
                    + "                        </aaar:sgFIELDNAME>\n"
                    + "                    </aaar:mPROPERTY>\n"
                    + "                </aaar:gPROPERTY>\n"
                    + "                <aaar:GRPSEQUENCE></aaar:GRPSEQUENCE>\n"
                    + "            </AAARRANGEMENTACTIVITYKROPAANEWType>\n"
                    + "        </tes:CreateKropLoan>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + majaniReqLog + "\n\n", "", 52);

            response = requestToCoreBanking(majaniReq, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + response + "\n\n", "", 53);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return response;
    }

    public String disburseMajaniLoan(JsonObject jo) {
        String response = "";
        String T24_URL = "" + EntryPoint.AGRI_IP + ":" + EntryPoint.AGRI_PORT + "/agricapis/services";
        try {
            String majaniDisburse = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://temenos.com/testa\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERAGRICAAACDI\">\n"
                    + "    <soapenv:Header/>\n"
                    + "    <soapenv:Body>\n"
                    + "        <tes:Disburseloan>\n"
                    + "            <WebRequestCommon>\n"
                    + "                <!--Optional:-->\n"
                    + "                <company></company>\n"
                    + "                <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "                <userName>" + EntryPoint.COMPANY_USERNAME + "</userName>\n"
                    + "            </WebRequestCommon>\n"
                    + "            <OfsFunction>\n"
                    + "                <!--Optional:-->\n"
                    + "                <noOfAuth>0</noOfAuth>\n"
                    + "            </OfsFunction>\n"
                    + "            <FUNDSTRANSFERAGRICAAACDIType id=\"\">\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:ArrangementId>" + jo.getString("arrangement") + "</fun:ArrangementId>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:DebitAmount>" + jo.getString("amount") + "</fun:DebitAmount>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:DebitValueDate></fun:DebitValueDate>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:CreditAccount>" + jo.getString("accountTo") + "</fun:CreditAccount>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:CreditValueDate></fun:CreditValueDate>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:TreasuryRate></fun:TreasuryRate>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:CustomerSpread></fun:CustomerSpread>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:Customerrate></fun:Customerrate>\n"
                    + "            </FUNDSTRANSFERAGRICAAACDIType>\n"
                    + "        </tes:Disburseloan>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String majaniDisburseLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://temenos.com/testa\" xmlns:fun=\"http://temenos.com/FUNDSTRANSFERAGRICAAACDI\">\n"
                    + "    <soapenv:Header/>\n"
                    + "    <soapenv:Body>\n"
                    + "        <tes:Disburseloan>\n"
                    + "            <WebRequestCommon>\n"
                    + "                <!--Optional:-->\n"
                    + "                <company></company>\n"
                    + "                <password></password>\n"
                    + "                <userName></userName>\n"
                    + "            </WebRequestCommon>\n"
                    + "            <OfsFunction>\n"
                    + "                <!--Optional:-->\n"
                    + "                <noOfAuth>0</noOfAuth>\n"
                    + "            </OfsFunction>\n"
                    + "            <FUNDSTRANSFERAGRICAAACDIType id=\"\">\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:ArrangementId>" + jo.getString("arrangement") + "</fun:ArrangementId>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:DebitCurrency>KES</fun:DebitCurrency>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:DebitAmount>" + jo.getString("amount") + "</fun:DebitAmount>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:DebitValueDate></fun:DebitValueDate>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:CreditAccount>" + jo.getString("accountTo") + "</fun:CreditAccount>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:CreditValueDate></fun:CreditValueDate>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:TreasuryRate></fun:TreasuryRate>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:CustomerSpread></fun:CustomerSpread>\n"
                    + "                <!--Optional:-->\n"
                    + "                <fun:Customerrate></fun:Customerrate>\n"
                    + "            </FUNDSTRANSFERAGRICAAACDIType>\n"
                    + "        </tes:Disburseloan>\n"
                    + "    </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + majaniDisburseLog + "\n\n", "", 52);

            response = requestToCoreBanking(majaniDisburse, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + response + "\n\n", "", 53);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }
        return response;
    }

    public String getKropLoanBalances(JsonObject jo) {
        String response = "";
        String T24_URL = "";
        try {
            String loanBalReq = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://temenos.com/testa\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <tes:GetOutstandingloanbalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_NAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <GETOUTSTANDINGBALType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ARRANGEMENT.ID</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + jo.getString("arrangement") + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </GETOUTSTANDINGBALType>\n"
                    + "      </tes:GetOutstandingloanbalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            String loanBalReqLog = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tes=\"http://temenos.com/testa\">\n"
                    + "   <soapenv:Header/>\n"
                    + "   <soapenv:Body>\n"
                    + "      <tes:GetOutstandingloanbalances>\n"
                    + "         <WebRequestCommon>\n"
                    + "            <!--Optional:-->\n"
                    + "            <company></company>\n"
                    + "            <password>" + EntryPoint.COMPANY_PASSWORD + "</password>\n"
                    + "            <userName>" + EntryPoint.COMPANY_NAME + "</userName>\n"
                    + "         </WebRequestCommon>\n"
                    + "         <GETOUTSTANDINGBALType>\n"
                    + "            <!--Zero or more repetitions:-->\n"
                    + "            <enquiryInputCollection>\n"
                    + "               <!--Optional:-->\n"
                    + "               <columnName>ARRANGEMENT.ID</columnName>\n"
                    + "               <!--Optional:-->\n"
                    + "               <criteriaValue>" + jo.getString("arrangement") + "</criteriaValue>\n"
                    + "               <!--Optional:-->\n"
                    + "               <operand>EQ</operand>\n"
                    + "            </enquiryInputCollection>\n"
                    + "         </GETOUTSTANDINGBALType>\n"
                    + "      </tes:GetOutstandingloanbalances>\n"
                    + "   </soapenv:Body>\n"
                    + "</soapenv:Envelope>";

            logger.applicationLog(logger.logPreString() + "Request To T24: " + loanBalReqLog + "\n\n", "", 52);

            response = requestToCoreBanking(loanBalReq, T24_URL);

            logger.applicationLog(logger.logPreString() + "Response From T24: " + response + "\n\n", "", 53);
        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error Sending To T24: " + e.getMessage() + "\n\n", "", 54);
        }

        return response;
    }

    //
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

            InputStreamReader isr = new InputStreamReader(con.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String response = "";

            while ((response = in.readLine()) != null) {
                response_complete = response_complete + response;
            }

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
