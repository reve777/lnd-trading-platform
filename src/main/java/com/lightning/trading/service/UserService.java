package com.lightning.trading.service;

import com.lightning.trading.dto.CreateUserRequest;
import com.lightning.trading.entity.UserAccount;
import com.lightning.trading.repo.UserAccountRepository;
import com.lightning.trading.util.AccountStatus;
import com.lightning.trading.util.TradeConstants;
import com.lightning.trading.util.UserRole;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 使用者與錢包帳戶服務 (UserService)
 * 負責處理系統初始預設用戶建立、新帳戶註冊、餘額增減與查詢等業務邏輯。
 * 因無多重實作需求，直接定義為具體服務類別 (@Service)，無需額外建立 Interface 與 Impl。
 */
@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 容器啟動後自動檢查並初始化預設用戶 (super_admin, user_a, user_b)
     */
    @PostConstruct
    public void init() {
        initializeDefaultUsersIfEmpty();
    }

    /**
     * 若資料庫為空，初始化建立 3 組預設演示帳號與初始額度
     */
    @Transactional
    public void initializeDefaultUsersIfEmpty() {
        if (!userAccountRepository.existsByUsername(TradeConstants.USERNAME_SUPER_ADMIN)) {
            UserAccount superAdmin = UserAccount.builder()
                    .username(TradeConstants.USERNAME_SUPER_ADMIN)
                    .email("admin@lightning.trading")
                    .fullName("Platform Super Administrator")
                    .password(passwordEncoder.encode("Admin#Secure2026!"))
                    .role(UserRole.ROLE_SUPER_ADMIN)
                    .balance(new BigDecimal("1000000.00"))
                    .lightningPubkey("02a1b2c3d4e5f67890123456789abcdef0123456789abcdef0123456789abcdef01")
                    .status(AccountStatus.ACTIVE)
                    .build();
            userAccountRepository.save(superAdmin);
            log.info("Initialized default SUPER_ADMIN user: {}", TradeConstants.USERNAME_SUPER_ADMIN);
        }

        if (!userAccountRepository.existsByUsername(TradeConstants.USERNAME_USER_A)) {
            UserAccount userA = UserAccount.builder()
                    .username(TradeConstants.USERNAME_USER_A)
                    .email("usera@lightning.trading")
                    .fullName("Trader Alpha (User A)")
                    .password(passwordEncoder.encode("UserA#Pass2026!"))
                    .role(UserRole.ROLE_USER)
                    .balance(new BigDecimal("500000.00"))
                    .lightningPubkey("02bb11223344556677889900aabbccddeeff00112233445566778899aabbccdde1")
                    .status(AccountStatus.ACTIVE)
                    .build();
            userAccountRepository.save(userA);
            log.info("Initialized default TRADER A: {}", TradeConstants.USERNAME_USER_A);
        }

        if (!userAccountRepository.existsByUsername(TradeConstants.USERNAME_USER_B)) {
            UserAccount userB = UserAccount.builder()
                    .username(TradeConstants.USERNAME_USER_B)
                    .email("userb@lightning.trading")
                    .fullName("Trader Bravo (User B)")
                    .password(passwordEncoder.encode("UserB#Pass2026!"))
                    .role(UserRole.ROLE_USER)
                    .balance(new BigDecimal("500000.00"))
                    .lightningPubkey("03cc11223344556677889900aabbccddeeff00112233445566778899aabbccdde2")
                    .status(AccountStatus.ACTIVE)
                    .build();
            userAccountRepository.save(userB);
            log.info("Initialized default TRADER B: {}", TradeConstants.USERNAME_USER_B);
        }
    }

    /**
     * 註冊建立新使用者帳戶
     *
     * @param request 使用者帳號、信箱、密碼與初始餘額
     * @return 新建的使用者實體物件
     */
    @Transactional
    public UserAccount createUser(CreateUserRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username '" + request.username() + "' is already in use.");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email '" + request.email() + "' is already registered.");
        }

        UserAccount user = UserAccount.builder()
                .username(request.username().trim())
                .email(request.email().trim().toLowerCase())
                .password(passwordEncoder.encode(request.password()))
                .fullName(request.fullName() != null ? request.fullName().trim() : request.username())
                .role(request.role() != null ? request.role() : UserRole.ROLE_USER)
                .balance(request.initialBalance() != null ? request.initialBalance() : new BigDecimal("10000.00"))
                .status(AccountStatus.ACTIVE)
                .build();

        return userAccountRepository.save(user);
    }

    /**
     * 依據使用者名稱查詢帳戶資訊
     *
     * @param username 使用者帳號
     * @return 使用者帳戶物件
     */
    @Transactional(readOnly = true)
    public UserAccount findByUsername(String username) {
        return userAccountRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User '" + username + "' not found."));
    }

    /**
     * 查詢所有使用者清單（依建立時間倒序）
     *
     * @return 使用者清單
     */
    @Transactional(readOnly = true)
    public List<UserAccount> getAllUsers() {
        return userAccountRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 調整指定使用者的帳戶餘額
     *
     * @param username 使用者名稱
     * @param delta    變動金額（可為正數或負數）
     * @return 更新後的使用者帳戶
     */
    @Transactional
    public UserAccount adjustBalance(String username, BigDecimal delta) {
        UserAccount user = findByUsername(username);
        BigDecimal newBalance = user.getBalance().add(delta);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient funds for user " + username + ". Current balance: " + user.getBalance());
        }
        user.setBalance(newBalance);
        return userAccountRepository.save(user);
    }
}
