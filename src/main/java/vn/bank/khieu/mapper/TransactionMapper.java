package vn.bank.khieu.mapper;

import org.springframework.stereotype.Component;

import vn.bank.khieu.dto.response.transaction.TransactionResponseDTO;
import vn.bank.khieu.entity.Transaction;

@Component
public class TransactionMapper {
    public TransactionResponseDTO toDto(Transaction transaction) {
        if (transaction == null) {
            return null;
        }
        return TransactionResponseDTO.builder()
                .id(transaction.getId())
                .transactionCode(transaction.getTransactionCode())
                .description(transaction.getDescription())
                .type(transaction.getType())
                // Kiểm tra null cẩn thận ở đây
                .fromAccountNumber(
                        transaction.getFromAccount() != null ? transaction.getFromAccount().getAccountNumber() : null)
                .toAccountNumber(
                        transaction.getToAccount() != null ? transaction.getToAccount().getAccountNumber() : null)
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
