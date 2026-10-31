package com.bankease.repository;

import com.bankease.entity.Transaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceNumber(String referenceNumber);

    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.sourceAccount sa " +
           "LEFT JOIN FETCH t.targetAccount ta " +
           "WHERE (sa IS NOT NULL AND sa.accountNumber = :accountNumber) OR (ta IS NOT NULL AND ta.accountNumber = :accountNumber) " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByAccountNumberOrderByCreatedAtDesc(@Param("accountNumber") String accountNumber);

    @Query("SELECT t FROM Transaction t " +
           "LEFT JOIN FETCH t.sourceAccount sa " +
           "LEFT JOIN sa.user sau " +
           "LEFT JOIN FETCH t.targetAccount ta " +
           "LEFT JOIN ta.user tau " +
           "WHERE (sau.email = :email OR tau.email = :email) " +
           "ORDER BY t.createdAt DESC")
    List<Transaction> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);
}
