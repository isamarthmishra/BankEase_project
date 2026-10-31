package com.bankease.repository;

import com.bankease.entity.Investment;
import com.bankease.entity.InvestmentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {

    Optional<Investment> findByInvestmentNumber(String investmentNumber);

    @Query("SELECT i FROM Investment i " +
           "JOIN FETCH i.account a " +
           "WHERE i.user.email = :email " +
           "ORDER BY i.createdAt DESC")
    List<Investment> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT i FROM Investment i " +
           "JOIN FETCH i.account a " +
           "WHERE i.user.email = :email AND i.status = :status " +
           "ORDER BY i.createdAt DESC")
    List<Investment> findByUserEmailAndStatus(@Param("email") String email, @Param("status") InvestmentStatus status);

    @Query("SELECT i FROM Investment i " +
           "JOIN FETCH i.account a " +
           "WHERE i.id = :id AND i.user.email = :email")
    Optional<Investment> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);
}
