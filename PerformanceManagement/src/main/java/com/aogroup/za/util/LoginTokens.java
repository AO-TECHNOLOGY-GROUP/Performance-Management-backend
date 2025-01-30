package com.aogroup.za.util;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class LoginTokens {
    public LoginTokens() {
    }

    public static String generateJWTToken(String subject) {
        JWTAuth provider = JWTAuth.create(Vertx.vertx(), new JWTAuthOptions()
                .addPubSecKey(new PubSecKeyOptions()
                        .setAlgorithm("HS256")
                        .setSecretKey("secretKey")));

        String token = provider.generateToken(new JsonObject()
                    .put("sub",subject)
                    .put("aud","get_cash_capital")
                    .put("iss","18.159.175.168")
                    .put("exp","123456789")
                    .put("nbf","999999")
                    .put("jti","102030"));

//         -----------------------
//            String pinHash = "addhgahgda";
//            JWTAuth provider = JWTAuth.create(Vertx.vertx(), new JWTAuthOptions()
//                    .addPubSecKey(new PubSecKeyOptions()
//                            .setAlgorithm("HS256")
//                            .setPublicKey(pinHash)
//                            .setSymmetric(true)));
//            String jwtToken = provider.generateToken(new JsonObject()
//                    .put("sub",subject)
//                    .put("aud","get_cash_capital")
//                    .put("iss","18.159.175.168")
//                    .put("exp","123456789")
//                    .put("nbf","999999")
//                    .put("jti","102030"));

        return token;
    }

    public static String generateCustomJsonObjectToken(String subject) {
        JsonObject tokenObject = new JsonObject()
                .put("aud", subject)
                .put("nbf","");
        String jsonObjectToken = Base64.getEncoder()
                .encodeToString(tokenObject.toString().getBytes(StandardCharsets.UTF_8));
        return jsonObjectToken;
    }
}
