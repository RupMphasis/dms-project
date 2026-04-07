package com.dms.order_service.client;

import com.dms.order_service.dto.AuditEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuditClient {
    private final RestTemplate restTemplate;
    private final String auditServiceUrl;

    public AuditClient(RestTemplate restTemplate, @Value("${audit.service.url:http://localhost:8084}") String auditServiceUrl) {
        this.restTemplate = restTemplate;
        this.auditServiceUrl = auditServiceUrl;
    }

    public void logEvent(AuditEventDto event) {
        try {
            restTemplate.postForObject(auditServiceUrl + "/api/audit/events", event, AuditEventDto.class);
        } catch (RestClientException ex) {
            log.warn("Failed to send audit event to audit-service: {}", ex.getMessage());
        }
    }
}
