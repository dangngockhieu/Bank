package vn.bank.khieu.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.bank.khieu.dto.request.transaction.transfer.TransactionOTP;
import vn.bank.khieu.dto.request.transaction.transfer.TransferDTO;
import vn.bank.khieu.entity.Account;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.enums.TransactionStatus;
import vn.bank.khieu.enums.TransactionType;
import vn.bank.khieu.repository.AccountRepository;
import vn.bank.khieu.utils.OtpUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final OtpUtil otpUtil;
    private final TransactionHistoryService transactionHistoryService;
    private final NotificationService notificationService;

    public void initiateTransfer(String senderEmail, TransferDTO dto) {
        Account senderAccount = accountRepository.findByCustomerUserEmail(senderEmail)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người gửi"));

        if (senderAccount.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Số dư không đủ để thực hiện giao dịch");
        }

        boolean recipientExists = accountRepository.existsByAccountNumber(dto.getRecipientAccountNumber());
        if (!recipientExists) {
            throw new IllegalArgumentException("Tài khoản người nhận không tồn tại");
        }

        if (senderAccount.getAccountNumber().equals(dto.getRecipientAccountNumber())) {
            throw new IllegalArgumentException("Không thể tự chuyển tiền cho chính mình");
        }

        // Gửi OTP
        otpUtil.sendEmailOTP(senderEmail);
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmTransfer(String senderEmail, TransactionOTP dto) {
        String transactionCode = "TRF-" + System.currentTimeMillis();
        Account senderAccount = null;
        Account recipientAccount = null;

        try {
            // Xác thực OTP
            if (!otpUtil.verifyOTP(senderEmail, dto.getOtpCode())) {
                throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
            }

            // Lấy thông tin tài khoản
            senderAccount = accountRepository.findByCustomerUserEmail(senderEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người gửi"));

            recipientAccount = accountRepository.findByAccountNumber(dto.getRecipientAccountNumber())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản người nhận"));

            // Kiểm tra lại số dư
            if (senderAccount.getBalance().compareTo(dto.getAmount()) < 0) {
                throw new IllegalArgumentException("Số dư không đủ để thực hiện giao dịch");
            }

            // Cộng/Trừ tiền (BigDecimal)
            senderAccount.setBalance(senderAccount.getBalance().subtract(dto.getAmount()));
            recipientAccount.setBalance(recipientAccount.getBalance().add(dto.getAmount()));

            // Lưu vào DB (Kích hoạt @Version Optimistic Locking)
            accountRepository.save(senderAccount);
            accountRepository.save(recipientAccount);

            // Ghi log SUCCESS
            transactionHistoryService.logTransaction(
                    transactionCode, senderEmail, dto.getDescription(), TransactionType.TRANSFER,
                    senderAccount, recipientAccount, dto.getAmount(), TransactionStatus.SUCCESS);

            // Tạo thông báo biến động số dư cho cả sender và recipient
            User senderUser = senderAccount.getCustomer().getUser();
            User recipientUser = recipientAccount.getCustomer().getUser();

            // Tạo thông báo cho NGƯỜI GỬI (Trừ tiền)
            String senderContent = String.format(
                    "Tài khoản %s bị trừ -%s VND. Nội dung: %s",
                    senderAccount.getAccountNumber(),
                    dto.getAmount().toString(),
                    dto.getDescription());
            notificationService.createNotification(senderUser, "Biến động số dư", senderContent);

            // Tạo thông báo cho NGƯỜI NHẬN (Cộng tiền)
            String recipientContent = String.format(
                    "Tài khoản %s được cộng +%s VND từ tài khoản %s. Nội dung: %s",
                    recipientAccount.getAccountNumber(),
                    dto.getAmount().toString(),
                    senderAccount.getAccountNumber(),
                    dto.getDescription());
            notificationService.createNotification(recipientUser, "Biến động số dư", recipientContent);

        } catch (Exception e) {
            log.error("Giao dịch chuyển tiền thất bại: {}", e.getMessage());

            // Ghi log REJECTED nếu đã lấy được thông tin tài khoản
            transactionHistoryService.logTransaction(
                    transactionCode, senderEmail, e.getMessage(), TransactionType.TRANSFER,
                    senderAccount, recipientAccount, dto.getAmount(), TransactionStatus.REJECTED);

            throw new RuntimeException(e.getMessage());
        }
    }
}