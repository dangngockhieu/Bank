package vn.bank.khieu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import vn.bank.khieu.dto.response.customer.ResBalanceDTO;
import vn.bank.khieu.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByAccountNumber(String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    @Query("SELECT a.id as id, a.accountNumber as accountNumber, a.balance as balance FROM Account a JOIN a.customer c JOIN c.user u WHERE u.email = :email")
    Optional<ResBalanceDTO> findBalanceByEmail(@Param("email") String email);

    @Query("SELECT a FROM Account a JOIN a.customer c JOIN c.user u WHERE u.email = :email")
    Optional<Account> findByCustomerUserEmail(@Param("email") String email);
}
