package com.example.plimap.global.external.youtube;

public class YoutubeClientException extends RuntimeException {

    public YoutubeClientException(String message) {
        super(message);
    }

    public YoutubeClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
