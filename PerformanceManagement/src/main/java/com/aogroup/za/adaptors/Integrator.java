/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.adaptors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import log.Logging;

/**
 *
 * @author nathan
 */
public class Integrator extends AbstractVerticle {

    private Logging logger;
    static int TIMEOUT_TIME = 25000;
    EventBus eventBus;

    @Override
    public void start(Future<Void> done) throws Exception {
        //System.out.println("deploymentId Integrator =" + vertx.getOrCreateContext().deploymentID());
        eventBus = vertx.eventBus();
        logger = new Logging();

        eventBus.consumer("INTEGRATION_TO_3RD_PARTY", this::sendToThirdParty);
    }

    private void sendToThirdParty(Message<JsonObject> message) {
        JsonObject data = message.body();
        logger.applicationLog(logger.logPreString() + "INTEGRATION_TO_3RD_PARTY: " + data + "\n\n", "", 82);

        String URL = data.getString("url");
        String formattedMessage = data.getJsonObject("json").toString();

        JsonObject result = new JsonObject();
        String response = null;

        try {
            response = requestToThirdParty(URL, formattedMessage);

            result.put("result", new JsonObject(response));
            message.reply(result);

        } catch (Exception e) {
            logger.applicationLog(logger.logPreString() + "Error - " + e.getMessage() + "\n\n", "", 5);
            result.put("responseCode", "999");
            result.put("result", "result");
            result.put("responseDescription", e.getMessage());
            message.reply(result);
        }
    }

    public String requestToThirdParty(String urlto, String json) {
        String response = null;
        logger.applicationLog(logger.logPreString() + "To ESB - " + json + "\n\n", "", 6);
        try {
            String req = json;
            URL ur;
            ur = new URL(urlto);

            HttpURLConnection con = (HttpURLConnection) ur.openConnection();
            con.setRequestMethod("POST");
            con.setConnectTimeout(10000);
            con.setReadTimeout(13000);
            con.setRequestProperty("Accept", "application/json");
            con.setDoOutput(true);
            OutputStream reqStream = con.getOutputStream();

            reqStream.write(req.getBytes());

            String responseString = "";
            String outputString = "";

            InputStreamReader isr = new InputStreamReader(con.getInputStream());

            BufferedReader in = new BufferedReader(isr);
            while ((responseString = in.readLine()) != null) {
                outputString = outputString + responseString;
            }
            con.disconnect();

            response = outputString;
            logger.applicationLog(logger.logPreString() + "From Integration - " + response + "\n\n", "", 7);

        } catch (MalformedURLException ex) {
            logger.applicationLog(logger.logPreString() + "Error - " + ex.getMessage() + "\n\n", "", 5);
            response = null;
        } catch (IOException ex) {
            logger.applicationLog(logger.logPreString() + "Error - " + ex.getMessage() + "\n\n", "", 5);
            response = null;
        }
        return response;
    }
}
