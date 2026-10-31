package com.bankease.repository;

import com.bankease.entity.Bill;
import com.bankease.entity.BillStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    @Query("SELECT b FROM Bill b WHERE b.user.email = :email ORDER BY b.dueDate ASC")
    List<Bill> findByUserEmail(@Param("email") String email);

    @Query("SELECT b FROM Bill b WHERE b.user.email = :email AND b.status = :status ORDER BY b.dueDate ASC")
    List<Bill> findByUserEmailAndStatus(@Param("email") String email, @Param("status") BillStatus status);

    @Query("SELECT b FROM Bill b WHERE b.id = :id AND b.user.email = :email")
    Optional<Bill> findByIdAndUserEmail(@Param("id") Long id, @Param("email") String email);
}
