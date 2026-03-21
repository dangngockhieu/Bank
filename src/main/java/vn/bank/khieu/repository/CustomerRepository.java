package vn.bank.khieu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import io.lettuce.core.dynamic.annotation.Param;
import vn.bank.khieu.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdentityCard(String identityCard);

    Optional<Customer> findByPhoneNumber(String phoneNumber);

    @Query("SELECT c FROM Customer c JOIN FETCH c.user WHERE c.phoneNumber = :keyword OR c.identityCard = :keyword")
    Optional<Customer> findCustomerWithUserByKeyword(@Param("keyword") String keyword);

    @Query("SELECT c FROM Customer c JOIN FETCH c.user JOIN FETCH c.account WHERE c.user.email = :email")
    Optional<Customer> findByUserEmail(@Param("email") String email);
}
