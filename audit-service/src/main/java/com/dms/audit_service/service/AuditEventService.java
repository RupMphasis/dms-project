package com.dms.audit_service.service;

import com.dms.audit_service.dto.AuditEventDto;
import com.dms.audit_service.entity.AuditEvent;
import com.dms.audit_service.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditEventService {

    private final AuditEventRepository repository;

    public AuditEventDto create(AuditEventDto dto) {
        AuditEvent event = new AuditEvent(
                dto.getEventType(),
                dto.getActor(),
                dto.getTargetType(),
                dto.getTargetId(),
                dto.getDetails(),
                dto.getTimestamp() != null ? dto.getTimestamp() : LocalDateTime.now()
        );
        AuditEvent saved = repository.save(event);
        return AuditEventDto.fromEntity(saved);
    }

    public List<AuditEventDto> findAll() {
        return repository.findAll().stream()
                .map(AuditEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditEventDto> findByTargetType(String targetType) {
        return repository.findByTargetType(targetType).stream()
                .map(AuditEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditEventDto> findByTargetId(String targetId) {
        return repository.findByTargetId(targetId).stream()
                .map(AuditEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditEventDto> findByTargetTypeAndTargetId(String targetType, String targetId) {
        return repository.findByTargetTypeAndTargetId(targetType, targetId).stream()
                .map(AuditEventDto::fromEntity)
                .collect(Collectors.toList());
    }

    public AuditEventDto findById(Long id) {
        return repository.findById(id)
                .map(AuditEventDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("Audit event not found with id " + id));
    }
}
