package com.lightning.trading.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 前端頁面視圖控制器 (WebViewController)
 * 負責將根路徑與儀表板路由轉發 (forward) 給前端單頁應用程式 (index.html)。
 */
@Controller
public class WebViewController {

    /**
     * 前台儀表板與根頁面路由
     *
     * 【端點路徑】：GET / 或 GET /dashboard
     * 【功能邏輯】：使用內部伺服器轉發 (forward) 將請求導向 static/index.html，避免客戶端 URL 改變並保持 SPA 路由正常。
     *
     * @return 轉發至前端靜態 HTML 首頁
     */
    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "forward:/index.html";
    }
}
