package com.lightning.trading.client;

public record LndInvoiceResponse(
        String rHash,
        String paymentRequest,
        long addIndex
) {}
