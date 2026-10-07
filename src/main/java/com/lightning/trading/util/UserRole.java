package com.lightning.trading.util;

/**
 * 使用者身分權限角色列舉 (UserRole)
 */
public enum UserRole {
    /** 最高管理者（可審核其他角色權限） */
    ROLE_SUPER_ADMIN("最高管理者"),
    /** 管理者 */
    ROLE_ADMIN("管理者"),
    /** 一般使用者 / 交易員 */
    ROLE_USER("一般交易員");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
