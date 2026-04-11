package com.dms.user_service.entity;

public enum ApprovalStatus {
    PENDING(0),
    APPROVED(1),
    DENIED(2);

    private final int value;

    ApprovalStatus(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static ApprovalStatus fromValue(int value) {
        for (ApprovalStatus status : values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown ApprovalStatus value: " + value);
    }
}
