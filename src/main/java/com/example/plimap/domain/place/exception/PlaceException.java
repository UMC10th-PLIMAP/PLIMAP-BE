package com.example.plimap.domain.place.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class PlaceException extends BusinessException {

    public PlaceException(PlaceErrorCode errorCode) {
        super(errorCode);
    }

    public PlaceException(PlaceErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
