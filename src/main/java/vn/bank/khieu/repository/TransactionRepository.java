package vn.bank.khieu.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import vn.bank.khieu.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

}
