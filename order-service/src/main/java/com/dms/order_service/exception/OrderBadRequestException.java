package com.dms.order_service.exception;

public class OrderBadRequestException extends RuntimeException {
    public OrderBadRequestException(String message) {
        super(message);
    }

    public OrderBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
