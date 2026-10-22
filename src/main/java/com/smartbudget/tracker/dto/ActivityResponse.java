package com.smartbudget.tracker.dto;

import com.smartbudget.tracker.entity.ActivityEntity;
import java.time.Instant;

public record ActivityResponse(Long id, String action, String description, String relatedType,
                               Long relatedId, Instant createdAt) {
    public static ActivityResponse from(ActivityEntity entity) {
        return new ActivityResponse(entity.getId(), entity.getAction(), entity.getDescription(),
                entity.getRelatedType(), entity.getRelatedId(), entity.getCreatedAt());
    }
}
