package com.lightning.trading.dto;

import com.lightning.trading.entity.TransactionRecord;
import com.lightning.trading.util.TransactionStatus;
import com.lightning.trading.util.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record TradeResponse(
        String txUid,
        String uuidv7Full,
        TransactionType txType,
        String sender,
        String receiver,
        BigDecimal amount,
        TransactionStatus status,
        Long executionTimeMs,
        String memo,
        String lndPaymentHash,
        String lndPreimage,
        String lndPaymentRequest,
        Long lndFeeSat,
        Integer lndHopCount,
        String errorMessage,
        Instant createdAt
) {
    public static TradeResponse fromEntity(TransactionRecord record) {
        return new TradeResponse(
                record.getTxUid(),
                record.getUuidv7Full(),
                record.getTxType(),
                record.getSender() != null ? record.getSender().getUsername() : "SYSTEM",
                record.getReceiver() != null ? record.getReceiver().getUsername() : "SYSTEM",
                record.getAmount(),
                record.getStatus(),
                record.getExecutionTimeMs(),
                record.getMemo(),
                record.getLndPaymentHash(),
                record.getLndPreimage(),
                record.getLndPaymentRequest(),
                record.getLndFeeSat(),
                record.getLndHopCount(),
                record.getErrorMessage(),
                record.getCreatedAt()
        );
    }
}
