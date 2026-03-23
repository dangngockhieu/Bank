package vn.bank.khieu.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.bank.khieu.dto.request.EmailDTO;
import vn.bank.khieu.dto.request.transaction.deposit_withdrawal.TransactionDTO;
import vn.bank.khieu.dto.request.transaction.deposit_withdrawal.TransactionOTP;
import vn.bank.khieu.dto.request.transaction.transfer.TranferOTP;
import vn.bank.khieu.dto.request.transaction.transfer.TransferDTO;
import vn.bank.khieu.dto.response.PageResponseDTO;
import vn.bank.khieu.dto.response.transaction.TransactionResponseDTO;
import vn.bank.khieu.entity.Account;
import vn.bank.khieu.entity.Transaction;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.enums.TransactionStatus;
import vn.bank.khieu.enums.TransactionType;
import vn.bank.khieu.mapper.TransactionMapper;
import vn.bank.khieu.repository.AccountRepository;
import vn.bank.khieu.repository.TransactionRepository;
import vn.bank.khieu.utils.OtpUtil;
import vn.bank.khieu.utils.error.NotFindException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final AccountRepository accountRepository;
    private final OtpUtil otpUtil;
    private final TransactionHistoryService transactionHistoryService;
    private final NotificationService notificationService;
    private final TransactionMapper transactionMapper;
    private final TransactionRepository transactionRepository;

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
    public void confirmTransfer(String senderEmail, TranferOTP dto) {
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
                    .orElseThrow(() -> new NotFindException("Không tìm thấy tài khoản người gửi"));

            recipientAccount = accountRepository.findByAccountNumber(dto.getRecipientAccountNumber())
                    .orElseThrow(() -> new NotFindException("Không tìm thấy tài khoản người nhận"));

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
                    "Tài khoản %s bị trừ %s VND. Nội dung: %s",
                    senderAccount.getAccountNumber(),
                    dto.getAmount().toString(),
                    dto.getDescription());
            notificationService.createNotification(senderUser, "Biến động số dư", senderContent);

            // Tạo thông báo cho NGƯỜI NHẬN (Cộng tiền)
            String recipientContent = String.format(
                    "Tài khoản %s được cộng %s VND từ tài khoản %s. Nội dung: %s",
                    recipientAccount.getAccountNumber(),
                    dto.getAmount().toString(),
                    senderAccount.getAccountNumber(),
                    dto.getDescription());
            notificationService.createNotification(recipientUser, "Biến động số dư", recipientContent);

        } catch (Exception e) {
            log.error("Giao dịch chuyển tiền thất bại: {}", e.getMessage());

            // Ghi log REJECTED nếu đã lấy được thông tin tài khoản
            transactionHistoryService.logTransaction(
                    transactionCode, null, e.getMessage(), TransactionType.TRANSFER,
                    senderAccount, recipientAccount, dto.getAmount(), TransactionStatus.REJECTED);

            throw new RuntimeException(e.getMessage());
        }
    }

    public void initiateDeposit(EmailDTO emailDTO) {
        accountRepository.findByCustomerUserEmail(emailDTO.getEmail())
                .orElseThrow(() -> new NotFindException("Không tìm thấy tài khoản khách hàng"));
        // Gửi OTP
        otpUtil.sendEmailOTP(emailDTO.getEmail());
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmDeposit(String tellerEmail, TransactionOTP dto) {
        String transactionCode = "TRF-" + System.currentTimeMillis();
        Account account = null;

        try {
            // Xác thực OTP
            if (!otpUtil.verifyOTP(dto.getCustomerEmail(), dto.getOtpCode())) {
                throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
            }

            // Lấy thông tin tài khoản
            account = accountRepository.findByCustomerUserEmail(dto.getCustomerEmail())
                    .orElseThrow(() -> new NotFindException("Không tìm thấy tài khoản khách hàng"));

            // Cộng tiền (BigDecimal)
            account.setBalance(account.getBalance().add(dto.getAmount()));

            // Lưu vào DB (Kích hoạt @Version Optimistic Locking)
            accountRepository.save(account);

            // Ghi log SUCCESS
            transactionHistoryService.logTransaction(
                    transactionCode, tellerEmail, dto.getDescription(), TransactionType.DEPOSIT,
                    null, account, dto.getAmount(), TransactionStatus.SUCCESS);

            // Tạo thông báo biến động số dư cho cả customer
            User user = account.getCustomer().getUser();

            // Tạo thông báo cho Customer (Trừ tiền)
            String content = String.format(
                    "Tài khoản %s đã cộng %s VND. Nội dung: %s",
                    account.getAccountNumber(),
                    dto.getAmount().toString(),
                    dto.getDescription());
            notificationService.createNotification(user, "Biến động số dư", content);

        } catch (Exception e) {
            log.error("Giao dịch nộp tiền thất bại: {}", e.getMessage());

            // Ghi log REJECTED nếu đã lấy được thông tin tài khoản
            transactionHistoryService.logTransaction(
                    transactionCode, tellerEmail, e.getMessage(), TransactionType.DEPOSIT,
                    null, account, dto.getAmount(), TransactionStatus.REJECTED);

            throw new RuntimeException(e.getMessage());
        }
    }

    public void initiateWithdrawal(TransactionDTO dto) {
        Account account = accountRepository.findByCustomerUserEmail(dto.getCustomerEmail())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng"));

        if (account.getBalance().compareTo(dto.getAmount()) < 0) {
            throw new IllegalArgumentException("Số dư không đủ để thực hiện giao dịch");
        }
        // Gửi OTP
        otpUtil.sendEmailOTP(dto.getCustomerEmail());
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirmWithdrawal(String tellerEmail, TransactionOTP dto) {
        String transactionCode = "TRF-" + System.currentTimeMillis();
        Account account = null;

        try {
            // Xác thực OTP
            if (!otpUtil.verifyOTP(dto.getCustomerEmail(), dto.getOtpCode())) {
                throw new IllegalArgumentException("Mã OTP không chính xác hoặc đã hết hạn");
            }

            // Lấy thông tin tài khoản
            account = accountRepository.findByCustomerUserEmail(dto.getCustomerEmail())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy tài khoản khách hàng"));

            // Kiểm tra lại số dư
            if (account.getBalance().compareTo(dto.getAmount()) < 0) {
                throw new IllegalArgumentException("Số dư không đủ để thực hiện giao dịch");
            }

            // Trừ tiền (BigDecimal)
            account.setBalance(account.getBalance().subtract(dto.getAmount()));

            // Lưu vào DB (Kích hoạt @Version Optimistic Locking)
            accountRepository.save(account);

            // Ghi log SUCCESS
            transactionHistoryService.logTransaction(
                    transactionCode, tellerEmail, dto.getDescription(), TransactionType.WITHDRAW,
                    account, null, dto.getAmount(), TransactionStatus.SUCCESS);

            // Tạo thông báo biến động số dư cho cả customer
            User user = account.getCustomer().getUser();

            // Tạo thông báo cho NGƯỜI GỬI (Trừ tiền)
            String content = String.format(
                    "Tài khoản %s đã trừ %s VND. Nội dung: %s",
                    account.getAccountNumber(),
                    dto.getAmount().toString(),
                    dto.getDescription());
            notificationService.createNotification(user, "Biến động số dư", content);

        } catch (Exception e) {
            log.error("Giao dịch rút tiền thất bại: {}", e.getMessage());

            // Ghi log REJECTED nếu đã lấy được thông tin tài khoản
            transactionHistoryService.logTransaction(
                    transactionCode, tellerEmail, e.getMessage(), TransactionType.WITHDRAW,
                    account, null, dto.getAmount(), TransactionStatus.REJECTED);

            throw new RuntimeException(e.getMessage());
        }
    }

    public PageResponseDTO<TransactionResponseDTO> getMyTransactionHistory(String email, Pageable pageable) {
        Page<Transaction> transactionPage = transactionRepository.findHistoryByUserEmail(email, pageable);

        // Gọi hàm toDto từ TransactionMapper
        List<TransactionResponseDTO> dtoList = transactionPage.getContent().stream()
                .map(transactionMapper::toDto)
                .collect(Collectors.toList());
        PageResponseDTO<TransactionResponseDTO> response = new PageResponseDTO<>();
        response.setCurrentPage(pageable.getPageNumber() + 1);
        response.setPageSize(pageable.getPageSize());
        response.setTotalElements(transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());

        response.setData(dtoList);
        return response;
    }
}