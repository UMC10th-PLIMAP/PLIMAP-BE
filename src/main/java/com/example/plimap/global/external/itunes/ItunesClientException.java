package com.example.plimap.global.external.itunes;

public class ItunesClientException extends RuntimeException {

    public ItunesClientException(String message) {
        super(message);
    }

    public ItunesClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
