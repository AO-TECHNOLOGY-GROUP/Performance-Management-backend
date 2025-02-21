/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.instance;

import com.aogroup.za.util.ResponseCodes;
import io.vertx.core.json.JsonObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import log.Logging;
import org.json.JSONObject;

/**
 *
 * @author Benard.Mwangi
 */
public class IPRS {

    JsonObject hmap = new JsonObject();
    private static Logging logger;

    public IPRS(JsonObject empDet) {
        this.hmap = empDet;
        logger = new Logging();
    }

    public boolean checkIPRS() {
        boolean check = false;
        JSONObject checked = requestToIPRS();
        if (checked.has("iprsResponseCode")) {
            if (checked.get("iprsResponseCode").equals("000")) {
                check = true;
            } else {
                check = false;
            }

        } else {
            check = false;
        }

        return check;
    }

    public JSONObject requestToIPRS() {
        String response_complete = "";
        JSONObject outJson = null;

        try {
            URL urlc = new URL("http://10.1.8.30:8190/MocashIPRS/req");
            HttpURLConnection con = (HttpURLConnection) urlc.openConnection();
            JSONObject json = new JSONObject();
            json.put("idNumber", hmap.getString("employee_id_passport"));
            json.put("idType", "1");
            json.put("firstName", hmap.getString("first_name"));
            json.put("middleName", hmap.getString("middle_name"));
            json.put("surName", hmap.getString("sir_name"));
            json.put("gender", hmap.getString("gender"));
            String request = json.toString();

            logger.applicationLog(logger.logPreString() + " ~IPRS Request ~ " + request + "\n\n", "", 53);

            byte[] buffer = request.getBytes();
            ByteArrayOutputStream request_msg = new ByteArrayOutputStream();
            request_msg.write(buffer);
            byte[] request_msgbyte = request_msg.toByteArray();

            con.setRequestProperty("Content-Length", String.valueOf(request_msgbyte.length));
            con.setRequestProperty("Content-Type", "application/json; charset=utf-8");
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
            if (!response_complete.isEmpty()) {
                outJson = new JSONObject(response_complete);
            }
            logger.applicationLog(logger.logPreString() + " ~IPRS Response ~ " + response_complete + "\n\n", "", 53);

        } catch (MalformedURLException ex) {
            response_complete = ResponseCodes.SYSTEM_ERROR;
            logger.applicationLog(logger.logPreString() + "Error Sending request to T24  CBS - " + ex.getMessage() + "\n\n", "", 54);
        } catch (IOException ex) {
            response_complete = ResponseCodes.CBS_TIMEOUT;
            logger.applicationLog(logger.logPreString() + "Error Sending request to T24 CBS - " + ex.getMessage() + "\n\n", "", 54);
        }

        return outJson;

    }

    private static void disableSslVerification() {
        try {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };

            // Install the all-trusting trust manager
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

            // Create all-trusting host name verifier
            HostnameVerifier allHostsValid = new HostnameVerifier() {
                @Override
                public boolean verify(String hostname, SSLSession session) {
                    return true;
                }
            };

            // Install the all-trusting host verifier
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (NoSuchAlgorithmException e) {
            logger.applicationLog(logger.logPreString() + "Error at disableSslVerification - " + e.getMessage() + "\n\n", "", 5);
        } catch (KeyManagementException e) {
            logger.applicationLog(logger.logPreString() + "Error at disableSslVerification - " + e.getMessage() + "\n\n", "", 5);
        }
    }
}
