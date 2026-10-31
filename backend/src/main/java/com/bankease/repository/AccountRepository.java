package com.bankease.repository;

import com.bankease.entity.Account;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    @Query("SELECT a FROM Account a WHERE a.user.email = :email")
    List<Account> findByUserEmail(@Param("email") String email);

    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber AND a.user.email = :email")
    Optional<Account> findByAccountNumberAndUserEmail(@Param("accountNumber") String accountNumber, @Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);
}
