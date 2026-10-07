package com.lightning.trading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record LndTradeRequest(
        @NotBlank(message = "Sender username is required")
        String senderUsername,

        @NotBlank(message = "Receiver username is required")
        String receiverUsername,

        @NotNull(message = "Amount in Satoshi is required")
        @DecimalMin(value = "1.0", message = "Amount must be at least 1 satoshi")
        BigDecimal amountSat,

        String memo,

        /**
         * Optional: Specific BOLT11 payment request invoice.
         * If omitted, receiver automatically issues an invoice which the sender pays.
         */
        String paymentRequest
) {}
