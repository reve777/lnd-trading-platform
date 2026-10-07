package com.lightning.trading.client;

public record LndPaymentResponse(
        String paymentPreimage,
        String paymentHash,
        long feeSat,
        int hopCount,
        boolean success,
        String failureReason
) {}
