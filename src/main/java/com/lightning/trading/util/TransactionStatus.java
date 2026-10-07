package com.lightning.trading.util;

/**
 * 交易處理狀態列舉 (TransactionStatus)
 */
public enum TransactionStatus {
    /** 交易成功 */
    SUCCESS("交易成功"),
    /** 交易失敗 */
    FAILED("交易失敗"),
    /** 處理中 */
    PENDING("處理中"),
    /** 已沖正撤銷 */
    REVERTED("已沖正撤銷");

    private final String description;

    TransactionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
