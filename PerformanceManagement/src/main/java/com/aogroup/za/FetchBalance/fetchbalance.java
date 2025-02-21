/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.FetchBalance;

import com.aogroup.za.util.Common;
import com.aogroup.za.util.ResponseCodes;
import com.aogroup.za.util.Utilities;
import com.instance.CBSService;
import com.instance.CBSUtility;
import static com.oracle.webservices.internal.api.EnvelopeStyle.Style.XML;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import log.Logging;

/**
 *
 * @author Best Point
 */
public class fetchbalance extends AbstractVerticle{
    
    private Logging logger;
    EventBus eventBus;
    static int TIMEOUT_TIME = 120000;

    @Override
    public void start(Future<Void> start) throws Exception {
        System.out.println("deploymentId TransactionsAdaptor =" + vertx.getOrCreateContext().deploymentID());

        eventBus = vertx.eventBus();
        logger = new Logging();

        eventBus.consumer("310000", this::returnAccountBalance);
     }

    public void returnAccountBalance(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject res = new JsonObject();
        Utilities util = new Utilities();
        CBSService cbs = new CBSService();
        CBSUtility cbsUtil = new CBSUtility();
        String responseXML = cbs.getBalanceInquiry(data);
        JsonObject accountBal = cbsUtil.getBalanceInquiry(responseXML);
        if (accountBal.getString("response").equalsIgnoreCase("000")) {
            String respXML = cbs.chargeBalanceEnquiry(data);
            JsonObject balCharge = cbsUtil.chargeBalanceEnquiry(respXML, data);
            if (balCharge.getString("response").equalsIgnoreCase("000")) {
                data.put("result", accountBal.toString());
                data.put("response", ResponseCodes.SUCCESS);
                data.put("responseDescription", "successful");
                message.reply(data);

                JsonObject sms = new JsonObject();
                String tim = Common.formatDateToday("yyyyMMddHHmmss");
                String corrId = Common.generateRandom(6);
                String msgStr = "Dear Member, your available balance in A/C " + data.getString("account").toString() + " is KES " + accountBal.getString("availableBalance").toString() + ".Transaction cost is KES " + balCharge.getString("totalCharge") + ".For help 0709253000.";

                sms.put("processingCode", "122000");
                sms.put("phonenumber", data.getString("phonenumber"));
                sms.put("msg", msgStr);
                sms.put("timestamp", tim);

                eventBus.send("COMMUNICATION_ADAPTOR", sms);
            } else {
                data.put("response", ResponseCodes.TRANSACTION_FAILED);
                message.reply(data);
            }
        } else if (accountBal.getString("response").equalsIgnoreCase("909") && accountBal.getString("messages").contains("22 BY CUSTOMER")) {
            data.put("response", ResponseCodes.INSUFFICIENT_FUNDS);
            data.put("responseDescription", "Canot process due to insufficient funds.");
            message.reply(data);
        } else {
            data.put("response", ResponseCodes.TRANSACTION_FAILED);
            message.reply(data);
        }

    }



}
