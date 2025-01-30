/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.aogroup.za.util;

/**
 * @author ronald.langat
 */
public class ResponseCodes {
    public static final String SUCCESS = "000";
    public static final String ERROR = "999";
    public static final String RESPONSE_CODE = "ResponseCode";
    public static final String RESPONSE_DESCRIPTION = "ResponseDescription";

    public enum RESPONSE {
        CODE("ResponseCode"),
        DESCRIPTION("ResponseCode"),
        SUCCESS("000"),
        ERROR("999")
        ;

        private final String text;

        /**
         * @param text
         */
        RESPONSE(final String text) {
            this.text = text;
        }

    }
}
