package com.dms.audit_service.controller;

import com.dms.audit_service.dto.AuditEventDto;
import com.dms.audit_service.service.AuditEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditEventService auditEventService;

    @PostMapping("/events")
    public ResponseEntity<AuditEventDto> createEvent(@RequestBody AuditEventDto eventDto) {
        return ResponseEntity.ok(auditEventService.create(eventDto));
    }

    @GetMapping("/events")
    public ResponseEntity<List<AuditEventDto>> getAllEvents() {
        return ResponseEntity.ok(auditEventService.findAll());
    }

    @GetMapping("/events/{id}")
    public ResponseEntity<AuditEventDto> getEventById(@PathVariable Long id) {
        return ResponseEntity.ok(auditEventService.findById(id));
    }
}
