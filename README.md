# FIDO 交易平台 (LND Lightning & Standard Dual-Track Trading Platform)

基於 **JDK 25** 與 **Spring Boot 3.4.3** 構建的現代化企業級雙軌交易平台。整合 **LND (Lightning Network Daemon) 閃電網路 API** 與傳統資料庫 ACID 交易，支援 **RFC 9562 UUIDv7 去除破折號取 16 碼** 作為唯一交易追蹤鍵，具備嚴格資安規範（OWASP、AOP 稽核軌跡、限流防護、BCrypt 雜湊），並提供即時視覺化對比測試平台。

---

## 🌟 核心功能與特性

1. **雙軌交易架構與耗時對比 (Standard vs LND)**
   - **一般後端交易**：本地 PostgreSQL 可重複讀 (Repeatable Read) 事務，高吞吐量 ACID 結算（通常 < 20ms）。
   - **LND 閃電網路交易**：支援實體 LND REST API（TLS + Macaroon 認證）與內建高保真閃電網路模擬器（BOLT11 發票簽章、多跳 HTLC 路由、Preimage 加密證明、通道餘額即時扣抵與手續費計算）。
   - **一鍵雙軌對比壓測**：同筆負載同時觸發兩者，計算延遲差距與比率，並匯總歷史 P95/平均延遲統計。

2. **RFC 9562 UUIDv7 交易追蹤機制**
   - 採用最新 UUIDv7 標準，時間戳單調遞增（k-sortable），毫秒級序列防碰撞。
   - **去除 `-` 取 16 碼**（前 12 位時間戳 + 1 位版本碼 '7' + 3 位高隨機/序列碼）作為全系統唯一業務交易編號（`txUid`）。
   - 保留完整 36 碼 UUIDv7 與原始傳入 Request Payload、傳出 Response Payload，達成端到端審計追蹤。

3. **帳號管理與分級權限審核**
   - 預設預載交易員：
     - **Trader A (`user_a`)**：初始餘額 500,000 sats/NTD。
     - **Trader B (`user_b`)**：初始餘額 500,000 sats/NTD。
     - **總管理者 (`super_admin`)**：權限 `ROLE_SUPER_ADMIN`。
   - **動態帳號管理**：可即時註冊新交易人員、充值/調帳。
   - **權限申請與審核機制**：一般使用者可於系統中提交申請晉升為管理者（`ROLE_ADMIN`）或一般使用者（`ROLE_USER`），總管理者於後台審核專區一鍵核准（自動變更權限）或駁回。

4. **金融級資安合規規範 (Security Standards)**
   - **AOP 稽核軌跡 (`@AuditAction`)**：自動攔截核心業務操作，記錄操作者、客戶端 IP、HTTP 方法、執行耗時、請求參數遮罩（自動遮蔽密碼與 Macaroon）、執行狀態。
   - **AOP 頻率限流 (`@RateLimit`)**：防範憑證暴力破解與惡意交易重放攻擊（滑動窗口限流）。
   - **憑證防護**：BCrypt 強雜湊加密儲存。
   - **HTTP 安全標頭**：嚴格配置 `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: strict-origin-when-cross-origin`。
   - **防資訊洩漏**：全局例外處理遮蔽內部堆疊細節，統一返回標準錯誤模型。

5. **清晰明確的分層架構**
   - `com.fido.trading.entity`：資料模型 (`UserAccount`, `TransactionRecord`, `RoleApplication`, `AuditLog`)
   - `com.fido.trading.repo`：Spring Data JPA 倉儲與聚合統計
   - `com.fido.trading.service`：業務邏輯與接口分離 (`TradeService`, `UserService`, `RoleApplicationService`, `AuditService`)
   - `com.fido.trading.aop`：AOP 切面審計與防暴力限流 (`TransactionAuditAspect`, `RateLimiterAspect`)
   - `com.fido.trading.client`：LND 閃電網路客戶端 (`LndRestClientImpl`, `MockLndClientImpl`)
   - `com.fido.trading.controller`：REST API 控制器與測試平台視圖
   - `com.fido.trading.util`：RFC 9562 UUIDv7 16碼生成器

---

## 🚀 測試平台儀表板

啟動後直接以瀏覽器訪問測試中心：
👉 **http://localhost:8085/**

### 儀表板四大核心專區：
1. **即時交易紀錄與追蹤 (UUIDv7 16碼)**：查看所有交易、狀態、耗時，點擊「🔍 詳情與追蹤」查看完整 JSON 請求/響應與 LND 加密 Preimage。
2. **歷史統計與平均延遲比較**：即時聚合統計一般後端 vs LND 交易之成功筆數、平均耗時 (ms)、最小與最大延遲。
3. **總管理者審核專區**：查看待審核角色晉升申請，總管理者可一鍵「核准通過」或「駁回」。
4. **資安稽核日誌 (Audit Trail)**：檢視所有 API 操作的資安審計事件。

---

## 🛠️ 環境配置與啟動

### 1. Docker 容器 (PostgreSQL & Redis)
本專案提供根目錄 `docker-compose.yml`：
```bash
docker-compose up -d
```
- PostgreSQL：連接埠 `5432`，資料庫 `fido_trade_db`，帳號 `postgres`，密碼 `password`
- Redis：連接埠 `6380`

### 2. 構建與啟動應用程式 (JDK 25)
```powershell
# 編譯與打包
.\mvnw.cmd clean package -DskipTests

# 執行 Spring Boot 應用程式
java -jar target\fido-lnd-trading-platform-1.0.0-SNAPSHOT.jar
```

---

## 📡 核心 API 端點清單

| 方法 | 端點 | 描述 |
| :--- | :--- | :--- |
| `POST` | `/api/trades/standard` | 發起一般後端交易 (ACID DB) |
| `POST` | `/api/trades/lnd` | 發起 LND 閃電網路交易 (BOLT11 / HTLC) |
| `POST` | `/api/trades/benchmark` | 同步雙軌對比壓測 (返回兩者耗時與比率) |
| `GET`  | `/api/trades/records` | 取得最新交易紀錄清單 |
| `GET`  | `/api/trades/records/{txUid}` | 依 UUIDv7 16碼查詢交易詳情與 Request/Response Payload |
| `GET`  | `/api/trades/benchmark-stats` | 取得歷史統計與平均延遲聚合分析 |
| `GET`  | `/api/trades/lnd-node` | 取得 LND 節點資訊 (通道數、區塊高度、餘額) |
| `GET`  | `/api/accounts` | 取得所有交易人員與帳戶餘額 |
| `POST` | `/api/accounts` | 註冊新交易人員 |
| `POST` | `/api/accounts/{username}/adjust-balance` | 為帳戶充值/調整金額 |
| `POST` | `/api/roles/apply` | 使用者提交管理者或使用者角色申請 |
| `POST` | `/api/roles/review` | 總管理者審核權限申請 (APPROVED / REJECTED) |
| `GET`  | `/api/roles/applications` | 查詢所有角色申請單 |
| `GET`  | `/api/audit/logs` | 查詢資安合規稽核軌跡 |
