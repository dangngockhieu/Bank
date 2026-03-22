package vn.bank.khieu.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import vn.bank.khieu.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    @Query("SELECT t FROM Transaction t WHERE " +
            "(t.fromAccount.customer.user.email = :email OR t.toAccount.customer.user.email = :email) " +
            "ORDER BY t.createdAt DESC")
    Page<Transaction> findHistoryByUserEmail(@Param("email") String email, Pageable pageable);
}
