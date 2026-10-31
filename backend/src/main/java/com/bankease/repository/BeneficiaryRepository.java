package com.bankease.repository;

import com.bankease.entity.Beneficiary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {

    List<Beneficiary> findByUserId(Long userId);

    @Query("SELECT b FROM Beneficiary b WHERE b.user.email = :email ORDER BY b.createdAt DESC")
    List<Beneficiary> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT b FROM Beneficiary b WHERE b.user.email = :email AND b.accountNumber = :accountNumber")
    Optional<Beneficiary> findByUserEmailAndAccountNumber(@Param("email") String email, @Param("accountNumber") String accountNumber);

    @Query("SELECT b FROM Beneficiary b WHERE b.id = :id AND b.user.email = :email")
    Optional<Beneficiary> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);
}
