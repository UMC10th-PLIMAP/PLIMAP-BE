package com.example.plimap.domain.track.exception;

import com.example.plimap.global.apiPayload.exception.BusinessException;

public class TrackException extends BusinessException {

    public TrackException(TrackErrorCode errorCode) {
        super(errorCode);
    }

    public TrackException(TrackErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
