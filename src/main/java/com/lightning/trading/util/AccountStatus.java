package com.lightning.trading.util;

/**
 * 帳戶啟用狀態列舉 (AccountStatus)
 */
public enum AccountStatus {
    /** 正常啟用中 */
    ACTIVE("正常啟用中"),
    /** 凍結暫停 */
    SUSPENDED("凍結暫停"),
    /** 等待審核開通 */
    PENDING_APPROVAL("等待審核開通");

    private final String description;

    AccountStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
