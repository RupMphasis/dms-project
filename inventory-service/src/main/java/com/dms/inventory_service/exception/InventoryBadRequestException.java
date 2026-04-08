package com.dms.inventory_service.exception;

public class InventoryBadRequestException extends RuntimeException {
    public InventoryBadRequestException(String message) {
        super(message);
    }

    public InventoryBadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
