package com.bankease.repository;

import com.bankease.entity.Loan;
import com.bankease.entity.LoanStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    Optional<Loan> findByLoanNumber(String loanNumber);

    @Query("SELECT l FROM Loan l " +
           "JOIN FETCH l.account a " +
           "WHERE l.user.email = :email " +
           "ORDER BY l.createdAt DESC")
    List<Loan> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT l FROM Loan l " +
           "JOIN FETCH l.account a " +
           "WHERE l.user.email = :email AND l.status = :status " +
           "ORDER BY l.createdAt DESC")
    List<Loan> findByUserEmailAndStatus(@Param("email") String email, @Param("status") LoanStatus status);

    @Query("SELECT l FROM Loan l " +
           "JOIN FETCH l.account a " +
           "WHERE l.id = :id AND l.user.email = :email")
    Optional<Loan> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);
}
