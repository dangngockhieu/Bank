package vn.bank.khieu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.Customer;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByIdentityCard(String identityCard);

    Optional<Customer> findByPhoneNumber(String phoneNumber);
}
