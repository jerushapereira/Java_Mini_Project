package com.smartbudget.tracker.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "activity")
public class ActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(nullable = false, length = 300)
    private String description;

    @Column(name = "related_type", length = 20)
    private String relatedType;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ActivityEntity() {}

    public ActivityEntity(String action, String description, String relatedType, Long relatedId) {
        this.action = action;
        this.description = description;
        this.relatedType = relatedType;
        this.relatedId = relatedId;
    }

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getDescription() { return description; }
    public String getRelatedType() { return relatedType; }
    public Long getRelatedId() { return relatedId; }
    public Instant getCreatedAt() { return createdAt; }
}
