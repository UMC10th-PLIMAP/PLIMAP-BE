package com.example.plimap.global.external.kakao;

public class KakaoClientException extends RuntimeException {

    public KakaoClientException(String message) {
        super(message);
    }

    public KakaoClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
