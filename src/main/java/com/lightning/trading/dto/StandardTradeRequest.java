package com.lightning.trading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StandardTradeRequest(
        @NotBlank(message = "Sender username is required")
        String senderUsername,

        @NotBlank(message = "Receiver username is required")
        String receiverUsername,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.0001", message = "Amount must be strictly positive")
        BigDecimal amount,

        String memo
) {}
