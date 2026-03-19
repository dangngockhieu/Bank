package vn.bank.khieu.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import vn.bank.khieu.enums.TransactionStatus;
import vn.bank.khieu.enums.TransactionType;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String transactionCode; // Vd: VCB123456

    @Enumerated(EnumType.STRING)
    private TransactionType type; // DEPOSIT, WITHDRAW, TRANSFER

    @ManyToOne
    private Account fromAccount; // Null nếu nộp tiền mặt

    @ManyToOne
    private Account toAccount; // Null nếu rút tiền mặt

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status; // PENDING , SUCCESS, REJECTED

    private Instant createdAt = Instant.now();

    @ManyToOne
    private User maker; // Teller lập lệnh tại quầy

    @ManyToOne
    private User checker; // Manager duyệt lệnh
}
