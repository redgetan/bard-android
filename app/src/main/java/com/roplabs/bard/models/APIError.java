package com.roplabs.bard.models;

public class APIError {

    private int statusCode;
    private String error;

    public APIError() {
    }

    public int status() {
        return statusCode;
    }

    public String message() {
        return error;
    }
}
