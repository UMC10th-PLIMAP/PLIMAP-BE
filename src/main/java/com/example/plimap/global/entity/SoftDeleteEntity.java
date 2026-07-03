package com.example.plimap.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class SoftDeleteEntity extends BaseEntity {

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void delete() {
        if (deletedAt == null) {
            deletedAt = Instant.now();
        }
    }

    public void restore() {
        deletedAt = null;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
