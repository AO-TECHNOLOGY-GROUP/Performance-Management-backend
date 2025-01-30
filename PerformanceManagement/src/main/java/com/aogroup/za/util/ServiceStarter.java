/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.util;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import java.util.TimerTask;

/**
 *
 * @author ronald.langat
 */
public class ServiceStarter extends TimerTask {

    Vertx vertx;

    public ServiceStarter(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void run() {
        startService();
    }

    public void startService() {
        EventBus eventBus = vertx.eventBus();

        JsonObject starterObject = new JsonObject();
        starterObject.put("start", "start");
        eventBus.send("SERVICES", starterObject);
        eventBus.send("BALANCEQWORKER", starterObject);
    }

}
