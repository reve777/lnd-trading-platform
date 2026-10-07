package com.lightning.trading.dto;

import com.lightning.trading.util.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewRoleApplicationRequest(
        @NotNull(message = "Application ID is required")
        Long applicationId,

        @NotNull(message = "Reviewer username is required")
        String reviewerUsername,

        @NotNull(message = "Decision is required")
        ApplicationStatus decision,

        String reviewNotes
) {}
