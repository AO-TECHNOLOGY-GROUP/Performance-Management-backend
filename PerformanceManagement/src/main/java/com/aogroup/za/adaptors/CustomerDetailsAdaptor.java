package com.aogroup.za.adaptors;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class CustomerDetailsAdaptor extends AbstractVerticle {

    private EventBus eventBus;
    private final OkHttpClient httpClient = new OkHttpClient();

    @Override
    public void start(Future<Void> done) throws Exception {
        eventBus = vertx.eventBus();
        eventBus.consumer("FETCH_CUSTOMER_DETAILS", this::fetchCustomerDetails);
    }

    private void fetchCustomerDetails(Message<JsonObject> message) {
        JsonObject data = message.body();
        String customerNumber = data.getString("customerNumber");

        if (customerNumber == null || customerNumber.isEmpty()) {
            message.fail(999, "Customer number is required");
            return;
        }

        // Generate transaction ID and timestamp
        String tranId = UUID.randomUUID().toString().replace("-", "").substring(0, 14);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        JsonObject requestPayload = new JsonObject()
            .put("transactionType", "CUD")
            .put("tranId", tranId)
            .put("password", "esb@payk0nn1")
            .put("msgType", "1200")
            .put("processingCode", "111000")
            .put("channel", "PERFORMANCE")
            .put("customerNumber", customerNumber)
            .put("account", customerNumber)
            .put("username", "esb")
            .put("timestamp", timestamp)
            .put("logId", UUID.randomUUID().toString().replace("-", "").substring(0, 10));

        // Convert request payload to JSON string
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            requestPayload.encode()
        );

        // Create the HTTP Request
        Request request = new Request.Builder()
            .url("http://10.1.6.60:8190/channelinterface/req") // API URL
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build();

        // Send the HTTP request asynchronously
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                message.fail(999, "Failed to fetch customer details: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    message.fail(999, "Error from API: " + response.message());
                    return;
                }

                // Parse response
                String responseBody = response.body().string();
                JsonObject jsonResponse = new JsonObject(responseBody);

                // Extract customer details
                String firstName = jsonResponse.getString("firstName", "");
                String lastName = jsonResponse.getString("lastName", "");
                String phoneNumber = jsonResponse.getString("phoneNumber", "");

                // Create a new JSON object for further processing
                JsonObject nextStepPayload = new JsonObject()
                    .put("firstName", firstName)
                    .put("lastName", lastName)
                    .put("phoneNumber", phoneNumber);
                    System.out.println(nextStepPayload);
                // Send to next event bus step
                eventBus.send("NEXT_PROCESSING_STEP", nextStepPayload);

                // Reply with response
                message.reply(jsonResponse);
            }
        });
    }
}
