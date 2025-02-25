package com.co.ke.main;


import com.aogroup.za.Channels.channels;
import com.aogroup.za.DashboardResults.dashboardResults;
import com.aogroup.za.EmployeeTasks.employeeTasks;
//import com.aogroup.za.FetchBalance.fetchbalance;
import com.aogroup.za.UserInteraction.UserInteraction;
import com.aogroup.za.Objectives.objectives;
import com.aogroup.za.Subtasks.subtasks;
import com.aogroup.za.Transactions.SMETransactions;
import com.aogroup.za.adaptors.ESBRouter;
import com.aogroup.za.adaptors.EmailAdaptor;
import com.aogroup.za.adaptors.Integrator;
import com.aogroup.za.adaptors.SMSAdaptor;
import com.aogroup.za.adaptors.Validation;
import com.aogroup.za.user.UserAuth;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import log.Logging;
import com.aogroup.za.util.Prop;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import java.util.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import timetasks.ConfirmDeposits;


/**
 *
 * @author nathan
 */
public class EntryPoint extends AbstractVerticle {
    
    private static final Logger LOG = LoggerFactory.getLogger(EntryPoint.class);

    public static Prop props;
    public static Logging logger;
    public static String LOGS_PATH;
    public static String DATABASE_DRIVER;
    public static String DATABASE_IP;
    public static String DATABASE_PORT;
    public static String DATABASE_NAME;
    public static String DATABASE_USER;
    public static String DATABASE_PASSWORD;
    public static String DATABASE_SERVER_TIME_ZONE;
    public static String SYSTEM_PORT;
    public static String SYSTEM_HOST;
    public static String ESB_ENDPOINT;
    public static String SMS_ENDPOINT;
    public static String SMTP_HOST;
    public static String SMTP_PORT;
    public static String SMTP_EMAIL;
    public static String SMTP_PASSWORD;
    
    
    public static String T24_IP;
    public static String T24_PORT;
    public static String COMPANY_NAME;
    public static String COMPANY_PASSWORD;
    public static String COMPANY_USERNAME;
    public static String AGRI_IP;
    public static String AGRI_PORT;
    
    public static String MOCASH_DATABASE_IP;
    public static String MOCASH_DATABASE_NAME;
    public static String MOCASH_DATABASE_USER;
    public static String MOCASH_DATABASE_PASSWORD;
    public static String ENCRYPTED_SMS_ENDPOINT;

    
    // Hikari Setup
    static int MAX_POOL_SIZE = 3;
    static int MAX_IDLE_TIME = 4;
    static int WORKER_POOL_SIZE = 6;
    static int TIMEOUT_TIME = 90000;
    static int INITIAL_POOL_SIZE = 2;

    static {
        props = new Prop();
        logger = new Logging();
        LOGS_PATH = "";
        DATABASE_DRIVER = "";
        DATABASE_IP = "";
        DATABASE_PORT = "";
        DATABASE_NAME = "";
        DATABASE_USER = "";
        DATABASE_PASSWORD = "";
        DATABASE_SERVER_TIME_ZONE = "";
        SYSTEM_PORT = "";
        SYSTEM_HOST = "";
        ESB_ENDPOINT = "";
        SMS_ENDPOINT = "";
        SMTP_HOST = "";
        SMTP_PORT = "";
        SMTP_PASSWORD = "";
        SMTP_EMAIL = "";
        
        
        T24_IP = "";
        T24_PORT = "";
        COMPANY_NAME = "";
        COMPANY_PASSWORD = "";
        COMPANY_USERNAME = "";
        
        //Mocash
        MOCASH_DATABASE_IP = "";
        MOCASH_DATABASE_NAME = "";
        MOCASH_DATABASE_USER = "";
        MOCASH_DATABASE_PASSWORD = "";
        ENCRYPTED_SMS_ENDPOINT = "";
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        // instatiate Properties and Logging classes
        props = new Prop();
        logger = new Logging();

        // Get properties from property file
        LOGS_PATH = props.getLogsPath();
        DATABASE_DRIVER = props.getDATABASE_DRIVER();
        DATABASE_IP = props.getDATABASE_IP();
        DATABASE_PORT = props.getDATABASE_PORT();
        DATABASE_NAME = props.getDATABASE_NAME();
        DATABASE_USER = props.getDATABASE_USER();
        DATABASE_PASSWORD = props.getDATABASE_PASSWORD();
        DATABASE_SERVER_TIME_ZONE = props.getDATABASE_SERVER_TIME_ZONE();
        SYSTEM_PORT = props.getSYSTEM_PORT();
        SYSTEM_HOST = props.getSYSTEM_HOST();
        ESB_ENDPOINT = props.getESB_ENDPOINT();
        SMS_ENDPOINT = props.getSMS_ENDPOINT();
        SMTP_HOST = props.getEMAIL_HOST();
        SMTP_PORT = props.getEMAIL_PORT();
        SMTP_EMAIL = props.getEMAIL_SENDER();
        SMTP_PASSWORD = props.getEMAIL_PASSWORD();

        
        T24_IP = props.getT24_IP();
        T24_PORT = props.getT24_PORT();
        COMPANY_NAME = props.getCOMPANY_NAME();
        COMPANY_PASSWORD = props.getCOMPANY_PASSWORD();
        COMPANY_USERNAME = props.getCOMPANY_USERNAME();
        AGRI_IP = props.getAGRI_IP();
        AGRI_PORT = props.getAGRI_PORT();
        
        MOCASH_DATABASE_IP = props.getMOCASH_DATABASE_IP();
        MOCASH_DATABASE_NAME = props.getMOCASH_DATABASE_NAME();
        MOCASH_DATABASE_USER = props.getMOCASH_DATABASE_USER();
        MOCASH_DATABASE_PASSWORD = props.getMOCASH_DATABASE_PASSWORD();

        
    
        // Deployment options
        DeploymentOptions options = new DeploymentOptions()
                .setInstances(10)
                .setWorker(true)
                .setWorkerPoolSize(40)
                .setHa(true);
      
        // deploy Vertices Here 
        vertx.deployVerticle(EntryPoint.class.getName(), options);
        vertx.deployVerticle(employeeTasks.class.getName(), options);
        vertx.deployVerticle(objectives.class.getName(), options);
        vertx.deployVerticle(subtasks.class.getName(), options);
        vertx.deployVerticle(Validation.class.getName(), options);
        vertx.deployVerticle(SMSAdaptor.class.getName(), options);
        vertx.deployVerticle(Integrator.class.getName(), options);
        vertx.deployVerticle(EmailAdaptor.class.getName(), options);
        vertx.deployVerticle(ESBRouter.class.getName(), options);
        vertx.deployVerticle(UserAuth.class.getName(), options);
        vertx.deployVerticle(UserInteraction.class.getName(), options);
        vertx.deployVerticle(dashboardResults.class.getName(), options);
        vertx.deployVerticle(SMETransactions.class.getName(), options);
        vertx.deployVerticle(channels.class.getName(), options);
//        vertx.deployVerticle(fetchbalance.class.getName(), options);

        ConfirmDeposits diodays = new ConfirmDeposits (vertx);
        Timer diodaysSched = new Timer();
        diodaysSched.schedule(diodays, 0, 24 * 60 * 60 * 1000); // Runs every 24 hours
          }

    @Override
    public void start(Future<Void> start_application) {

        EventBus eventBus = vertx.eventBus();
        int port = Integer.parseInt(SYSTEM_PORT);
        String host = SYSTEM_HOST;
        HttpServer ovHttpServer;

        ovHttpServer = vertx.createHttpServer();
        ovHttpServer.requestHandler(request -> {
            HttpServerResponse response = request.response();
            response.headers()
                    .add("Content-Type", "application/json")
                    .add("Access-Control-Allow-Origin", "*")
                    .add("Access-Control-Allow-Headers", "*")
                    .add("Access-Control-Allow-Methods", "*")
                    .add("Access-Control-Allow-Credentials", "true");
            String method = request.rawMethod();
            String path = request.path();
            String ip = request.remoteAddress().host();
            //System.out.println("Request IP Address "+ip);

            request.bodyHandler(bodyHandler -> {
                String body = bodyHandler.toString();
                JsonObject responseOBJ = new JsonObject();
                if ("POST".equalsIgnoreCase(method)) {
                    JsonObject data = new JsonObject(body);
                    data
                            .put("ip",ip)
                            .put("ip_address",ip);


                    logger.applicationLog(logger.logPreString() + "Channel Request  - " + data + "\n\n", "", 2);
                    if (path.endsWith("/performance/req")) {
                        try {
                            DeliveryOptions deliveryOptions = new DeliveryOptions()
                                    .setSendTimeout(TIMEOUT_TIME);
                            String processingCode = data.getString("processingCode");
                            // check validation
                            eventBus.send("VALIDATION", data, deliveryOptions, sendToBus -> {
                                if (sendToBus.succeeded()) {
                                    JsonObject resobj = (JsonObject) sendToBus.result().body();
                                    if ("pass".equalsIgnoreCase(resobj.getString("validation"))) {

                                        data.put("validation", resobj.getString("validation"));
                                        eventBus.send(processingCode, data, deliveryOptions, sToBus -> {
                                            if (sToBus.succeeded()) {
                                                JsonObject resobject = (JsonObject) sToBus.result().body();
                                                //send response
                                                logger.applicationLog(logger.logPreString() + "Response to channel - " + resobject + "\n\n", "", 3);
                                                response.end(resobject.toString());
                                            } else {
                                                // error
                                                responseOBJ.put("responseCode", "999")
                                                        .put("responseDescription", processingCode + " failed")
                                                        .put("error_data", sToBus.cause().getLocalizedMessage());
                                                logger.applicationLog(logger.logPreString() + "Response to channel - " + responseOBJ + "\n\n", "", 5);
                                                response.end(responseOBJ.toString());
                                                
                                            }
                                        });

                                    } else {
                                        responseOBJ.put("responseCode", resobj.getString("response"))
                                                .put("responseDescription", resobj.getString("responseDescription"));
                                        logger.applicationLog(logger.logPreString() + "Response to channel - " + responseOBJ + "\n\n", "", 5);
                                        response.end(responseOBJ.toString());
                                    }
                                } else {
                                    // error
                                    responseOBJ.put("responseCode", "999")
                                            .put("responseDescription", "failed to process request")
                                            .put("error_data", sendToBus.cause().getLocalizedMessage());
                                    logger.applicationLog(logger.logPreString() + "Response to channel - " + responseOBJ + "\n\n", "", 5);
                                    response.end(responseOBJ.toString());
                                }
                            });

                        } catch (Exception ex) {
                            logger.applicationLog(logger.logPreString() + "Channel Request  - " + ex.getLocalizedMessage() + "\n\n", "", 5);
                            responseOBJ.put("responseCode", "901")
                                    .put("responseDescription", ex.getLocalizedMessage());
                            response.end(responseOBJ.toString());
                        }
                       } else {
                        // Unknown path
                        responseOBJ.put("responseCode", "404")
                                .put("responseDescription", "Invalid path");
                        response.end(responseOBJ.toString());
                    }
                } else {
                    // wrong request method
                    responseOBJ.put("responseCode", "901")
                            .put("responseDescription", "Bad Request");
                    response.end(responseOBJ.toString());
                }
            });
        });

        ovHttpServer.listen(port, resp -> {
            if (resp.succeeded()) {
                LOG.info("System listening at " + host + ":" + port);
            } else {
                System.out.println("System failed to start !!" + resp.failed());
            }
        });
    }
}
