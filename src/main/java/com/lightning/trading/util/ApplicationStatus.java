package com.lightning.trading.util;

/**
 * 角色權限申請狀態列舉 (ApplicationStatus)
 */
public enum ApplicationStatus {
    /** 待審核 */
    PENDING("待審核"),
    /** 已核准 */
    APPROVED("已核准"),
    /** 已駁回 */
    REJECTED("已駁回");

    private final String description;

    ApplicationStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
