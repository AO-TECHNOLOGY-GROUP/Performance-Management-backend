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
    public static final String TRANSACTION_FAILED = "800";
    public static final String INSUFFICIENT_FUNDS = "02";
    public static final String SYSTEM_ERROR = "909";
    public static final String RECORD_NOT_FOUND = "801";
    public static final String CBS_TIMEOUT = "802";
    public static final String REGISTERED = "001";
    public static final String TRANSACTION_NOT_ALLOWED = "19";
    public static final String FAILED_LOAN_REPAYMENT = "502";
    public static final String FAILED_REJECT_TRANSFER = "504";
    public static final String FAILED_REJECT_REPAYMENT = "503";
    public static final String REJECT_REPAYMENT_SUCCESS = "506";
    public static final String OVERRID_OVERDRAFT= "400";


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
