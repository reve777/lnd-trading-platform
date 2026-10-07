package com.lightning.trading.dto;

import com.lightning.trading.util.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoleApplicationRequest(
        @NotBlank(message = "Applicant username is required")
        String applicantUsername,

        @NotNull(message = "Requested role is required")
        UserRole requestedRole,

        @NotBlank(message = "Reason is required")
        String reason
) {}
