package vn.bank.khieu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.bank.khieu.entity.Account;
import vn.bank.khieu.entity.Transaction;
import vn.bank.khieu.enums.TransactionStatus;
import vn.bank.khieu.enums.TransactionType;
import vn.bank.khieu.repository.TransactionRepository;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryService {

    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logTransaction(String code, String createdBy, String description,
            TransactionType type, Account from, Account to,
            BigDecimal amount, TransactionStatus status) {
        try {
            Transaction transaction = new Transaction();
            transaction.setTransactionCode(
                    code != null ? code : "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            transaction.setCreatedBy(createdBy);
            transaction.setDescription(description);
            transaction.setType(type);
            transaction.setFromAccount(from);
            transaction.setToAccount(to);
            transaction.setAmount(amount);
            transaction.setStatus(status);

            transactionRepository.save(transaction);
            log.info("Đã ghi log giao dịch {}: Trạng thái {}", transaction.getTransactionCode(), status);
        } catch (Exception e) {
            log.error("Lỗi khi ghi log giao dịch bằng REQUIRES_NEW: ", e);
        }
    }
}