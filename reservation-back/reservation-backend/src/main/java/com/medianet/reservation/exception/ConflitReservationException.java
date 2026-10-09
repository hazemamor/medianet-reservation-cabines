package com.medianet.reservation.exception;

import org.springframework.http.HttpStatus;

public class ConflitReservationException extends BusinessException {
    public ConflitReservationException(String message) {
        super(message, HttpStatus.CONFLICT);
    }
}
