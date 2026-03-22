package vn.bank.khieu.dto.response.transaction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.bank.khieu.enums.TransactionStatus;
import vn.bank.khieu.enums.TransactionType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponseDTO {

    private UUID id;
    private String transactionCode;
    private String description;
    private TransactionType type;

    // Sử dụng số tài khoản để hiển thị
    private String fromAccountNumber;
    private String toAccountNumber;

    private BigDecimal amount;
    private TransactionStatus status;
    private Instant createdAt;
}
