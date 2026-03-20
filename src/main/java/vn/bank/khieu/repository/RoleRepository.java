package vn.bank.khieu.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.Role;
import vn.bank.khieu.enums.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);
}
