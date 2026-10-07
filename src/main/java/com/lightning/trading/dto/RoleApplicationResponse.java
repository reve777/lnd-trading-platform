package com.lightning.trading.dto;

import com.lightning.trading.entity.RoleApplication;
import com.lightning.trading.util.ApplicationStatus;
import com.lightning.trading.util.UserRole;

import java.time.Instant;

public record RoleApplicationResponse(
        Long id,
        String applicantUsername,
        String applicantFullName,
        UserRole requestedRole,
        String reason,
        ApplicationStatus status,
        String reviewedByUsername,
        String reviewNotes,
        Instant createdAt,
        Instant reviewedAt
) {
    public static RoleApplicationResponse fromEntity(RoleApplication app) {
        return new RoleApplicationResponse(
                app.getId(),
                app.getApplicant() != null ? app.getApplicant().getUsername() : null,
                app.getApplicant() != null ? app.getApplicant().getFullName() : null,
                app.getRequestedRole(),
                app.getReason(),
                app.getStatus(),
                app.getReviewedBy() != null ? app.getReviewedBy().getUsername() : null,
                app.getReviewNotes(),
                app.getCreatedAt(),
                app.getReviewedAt()
        );
    }
}
