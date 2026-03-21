package vn.bank.khieu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

}
