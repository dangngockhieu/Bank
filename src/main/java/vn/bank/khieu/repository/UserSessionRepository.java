package vn.bank.khieu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

}
