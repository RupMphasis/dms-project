package com.dms.user_service.dto;

import java.time.LocalDateTime;

public class AuditEventDto {
    private Long id;
    private String eventType;
    private String actor;
    private String targetType;
    private String targetId;
    private String details;
    private LocalDateTime timestamp;

    public AuditEventDto() {
    }

    public AuditEventDto(Long id, String eventType, String actor, String targetType, String targetId, String details, LocalDateTime timestamp) {
        this.id = id;
        this.eventType = eventType;
        this.actor = actor;
        this.targetType = targetType;
        this.targetId = targetId;
        this.details = details;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
