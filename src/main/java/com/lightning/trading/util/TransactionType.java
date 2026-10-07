package com.lightning.trading.util;

/**
 * 交易類型列舉 (TransactionType)
 */
public enum TransactionType {
    /** 一般標準記帳交易（本地資料庫 ACID 劃轉） */
    STANDARD("一般標準內部記帳"),
    /** 比特幣二層閃電網路交易（LND 離鏈支付） */
    LND_LIGHTNING("LND 閃電網路交易");

    private final String description;

    TransactionType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
