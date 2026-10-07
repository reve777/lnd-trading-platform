package com.lightning.trading.util;

/**
 * 系統公用常數與中文字訊息定義 (TradeConstants)
 * 將全專案常用字串、審計動作標籤、中文字提示訊息與模板統一收斂至此類別。
 */
public final class TradeConstants {

    private TradeConstants() {
    }

    // ==========================================
    // 審計動作常數 (Audit Actions)
    // ==========================================
    public static final String AUDIT_TRADE_STANDARD_EXECUTE = "TRADE_STANDARD_EXECUTE";
    public static final String AUDIT_TRADE_LND_EXECUTE = "TRADE_LND_EXECUTE";
    public static final String AUDIT_TRADE_BENCHMARK_COMPARISON = "TRADE_BENCHMARK_COMPARISON";
    public static final String AUDIT_ACCOUNT_CREATE = "ACCOUNT_CREATE";
    public static final String AUDIT_ACCOUNT_BALANCE_ADJUST = "ACCOUNT_BALANCE_ADJUST";
    public static final String AUDIT_ROLE_APPLICATION_SUBMIT = "ROLE_APPLICATION_SUBMIT";
    public static final String AUDIT_ROLE_APPLICATION_REVIEW = "ROLE_APPLICATION_REVIEW";

    // ==========================================
    // 執行結果狀態字串
    // ==========================================
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILURE = "FAILURE";

    // ==========================================
    // 預設帳號與操作者常數
    // ==========================================
    public static final String DEFAULT_OPERATOR_ANONYMOUS = "ANONYMOUS";
    public static final String DEFAULT_CLIENT_IP = "127.0.0.1";
    public static final String DEFAULT_HTTP_METHOD_INTERNAL = "INTERNAL";
    public static final String USERNAME_SUPER_ADMIN = "super_admin";
    public static final String USERNAME_USER_A = "user_a";
    public static final String USERNAME_USER_B = "user_b";

    // ==========================================
    // 中文字交易備註與提示訊息 (Chinese Messages)
    // ==========================================
    public static final String MEMO_STANDARD_TRANSFER = "一般後端轉帳記帳";
    public static final String MEMO_LND_OFFCHAIN_PAYMENT = "LND 閃電網路離鏈支付";
    public static final String MSG_OPERATION_SUCCESS = "操作成功";
    public static final String MSG_BENCHMARK_PREFIX_STANDARD = "基準測試一般交易: ";
    public static final String MSG_BENCHMARK_PREFIX_LND = "基準測試 LND 交易: ";

    /**
     * 基準測試分析中文格式化模板
     */
    public static final String BENCHMARK_ANALYSIS_TEMPLATE =
            "一般後端交易耗時 %d ms（本地 ACID 資料庫讀寫），LND 閃電網路交易耗時 %d ms（包含加密發票簽章、HTLC 路由與結算），差距 %d ms（比率 %.2fx）。";

    // ==========================================
    // 中文字異常與驗證提示訊息 (Chinese Error Messages)
    // ==========================================
    public static final String ERR_INSUFFICIENT_FUNDS = "帳戶可用餘額不足";
    public static final String ERR_SENDER_NOT_FOUND = "發起方帳戶不存在";
    public static final String ERR_RECEIVER_NOT_FOUND = "收款方帳戶不存在";
    public static final String ERR_SAME_ACCOUNT_TRADE = "發起方與收款方帳戶不能為同一帳號";
    public static final String ERR_APPLICATION_ALREADY_REVIEWED = "該角色申請單已完成審核，無法重複處理";
    public static final String ERR_SUPER_ADMIN_ONLY_REVIEW = "僅有最高管理者 (SUPER_ADMIN) 具備審核權限";
}
