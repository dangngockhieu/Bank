package vn.bank.khieu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.Notification;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

}
