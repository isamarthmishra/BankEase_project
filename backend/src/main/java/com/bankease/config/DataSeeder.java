package com.bankease.config;

import com.bankease.dto.CreateAccountRequest;
import com.bankease.entity.Account;
import com.bankease.entity.AccountType;
import com.bankease.entity.Beneficiary;
import com.bankease.entity.Bill;
import com.bankease.entity.BillCategory;
import com.bankease.entity.BillStatus;
import com.bankease.entity.User;
import com.bankease.repository.AccountRepository;
import com.bankease.repository.BeneficiaryRepository;
import com.bankease.repository.BillRepository;
import com.bankease.repository.UserRepository;
import com.bankease.service.AccountService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final BeneficiaryRepository beneficiaryRepository;
    private final BillRepository billRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(
            UserRepository userRepository,
            AccountRepository accountRepository,
            AccountService accountService,
            BeneficiaryRepository beneficiaryRepository,
            BillRepository billRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.beneficiaryRepository = beneficiaryRepository;
        this.billRepository = billRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedDemoUser("live.demo@bankease.com", "SecurePassword@123", "Demo Customer", "9876543210");
        seedDemoUser("test.user@bankease.com", "DemoUser@123", "Test Customer", "9123456780");
    }

    private void seedDemoUser(String email, String rawPassword, String name, String phone) {
        try {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                user = new User();
                user.setName(name);
                user.setEmail(email);
                user.setPassword(passwordEncoder.encode(rawPassword));
                user.setPhone(phone);
                user.setRole("ROLE_USER");
                user = userRepository.save(user);
                log.info("Initialized demo account: {}", email);
            } else {
                // Ensure password hash matches documented credentials
                if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
                    user.setPassword(passwordEncoder.encode(rawPassword));
                    if (user.getRole() == null) {
                        user.setRole("ROLE_USER");
                    }
                    user = userRepository.save(user);
                    log.info("Synchronized password hash for demo account: {}", email);
                }
            }

            // Ensure user has at least one primary account
            List<Account> accounts = accountRepository.findByUserEmail(email);
            if (accounts.isEmpty()) {
                accountService.createAccount(email, new CreateAccountRequest(AccountType.SAVINGS, new BigDecimal("50000.00"), "INR"));
                accountService.createAccount(email, new CreateAccountRequest(AccountType.CHECKING, new BigDecimal("10000.00"), "INR"));
                log.info("Seeded default bank accounts for {}", email);
            }

            // Ensure sample beneficiary exists
            if (beneficiaryRepository.findByUserEmailOrderByCreatedAtDesc(email).isEmpty()) {
                Beneficiary b = new Beneficiary();
                b.setUser(user);
                b.setName("Priya Sharma");
                b.setAccountNumber("987654321098");
                b.setBankName("HDFC Bank");
                b.setIfscCode("HDFC0001234");
                b.setNickname("Landlord");
                beneficiaryRepository.save(b);
            }

            // Ensure sample bills exist
            if (billRepository.findByUserEmail(email).isEmpty()) {
                Bill bill1 = new Bill();
                bill1.setUser(user);
                bill1.setBillerName("Tata Power Mumbai");
                bill1.setCategory(BillCategory.ELECTRICITY);
                bill1.setConsumerNumber("EL-992014-X");
                bill1.setAmount(new BigDecimal("1250.00"));
                bill1.setDueDate(LocalDate.now().plusDays(15));
                bill1.setStatus(BillStatus.PENDING);
                billRepository.save(bill1);

                Bill bill2 = new Bill();
                bill2.setUser(user);
                bill2.setBillerName("Airtel Broadband");
                bill2.setCategory(BillCategory.INTERNET);
                bill2.setConsumerNumber("BB-440192");
                bill2.setAmount(new BigDecimal("999.00"));
                bill2.setDueDate(LocalDate.now().plusDays(20));
                bill2.setStatus(BillStatus.PENDING);
                billRepository.save(bill2);
            }
        } catch (Exception e) {
            log.warn("Notice during data seeding for {}: {}", email, e.getMessage());
        }
    }
}
