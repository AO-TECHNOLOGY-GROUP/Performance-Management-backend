package com.aogroup.za.adaptors;

import com.aogroup.za.util.Common;
import com.aogroup.za.util.ResponseCodes;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import log.Logging;

public class Validation extends AbstractVerticle {
    String time;
    String username;
    String pass;

    private Logging logger;
    static int TIMEOUT_TIME = 25000;
    EventBus eventBus;

    @Override
    public void start(Future<Void> done) throws Exception {
        //System.out.println("deploymentId EsbEngine =" + vertx.getOrCreateContext().deploymentID());
        eventBus = vertx.eventBus();
        logger = new Logging();

        eventBus.consumer("VALIDATION", this::doValidation);
    }

    private void doValidation(Message<JsonObject> message) {
        JsonObject data = message.body();
        JsonObject result = new JsonObject();

        time = Common.formatDateToday("yyyyMMddHHmmss");
        data.put("esbTimestamp", time);

        if (data.containsKey("username") && data.containsKey("password")) {

            username = data.getString("username");
            pass = data.getString("password");

            if (username.equals("performance") && pass.equals("pm@ao2025")) {
                result.put("validation", "pass");
                message.reply(result);
            } else {
                result.put("validation", "fail");
                result.put("msgType", "1210");
                result.put("responseCode", ResponseCodes.ERROR);
                result.put("responseDescription", "invalid system username or password");
                result.put("result", "invalid system username & password");

                message.reply(result);
            }
        } else {
            result.put("validation", "fail");
            result.put("msgType", "1210");
            result.put("responseCode", ResponseCodes.ERROR);
            result.put("responseDescription", "invalid system username or password");
            result.put("result", "invalid system username & password");

            message.reply(result);
        }
    }
}
