package com.example.plimap.domain.track.repository.exception;

import org.springframework.dao.DataAccessException;

public class CacheSerializationException extends DataAccessException {

    public CacheSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
