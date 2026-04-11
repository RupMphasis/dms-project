package com.dms.user_service.entity;

import com.dms.user_service.entity.ApprovalStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class ApprovalStatusConverter implements AttributeConverter<ApprovalStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(ApprovalStatus approvalStatus) {
        if (approvalStatus == null) {
            return ApprovalStatus.PENDING.getValue();
        }
        return approvalStatus.getValue();
    }

    @Override
    public ApprovalStatus convertToEntityAttribute(Integer dbData) {
        if (dbData == null) {
            return ApprovalStatus.PENDING;
        }
        return ApprovalStatus.fromValue(dbData);
    }
}
