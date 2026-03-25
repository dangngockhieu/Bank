package vn.bank.khieu.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.EmailDTO;
import vn.bank.khieu.dto.request.transaction.deposit_withdrawal.TransactionDTO;
import vn.bank.khieu.dto.request.transaction.deposit_withdrawal.TransactionOTP;
import vn.bank.khieu.dto.request.transaction.transfer.TranferOTP;
import vn.bank.khieu.dto.request.transaction.transfer.TransferDTO;
import vn.bank.khieu.dto.response.PageResponseDTO;
import vn.bank.khieu.dto.response.ResStringDTO;
import vn.bank.khieu.dto.response.transaction.TransactionResponseDTO;
import vn.bank.khieu.service.TransactionService;
import vn.bank.khieu.utils.SecurityUtil;
import vn.bank.khieu.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // Customer chuyển tiền
    @PostMapping("/transfer/initiate")
    @PreAuthorize("hasAnyRole('TELLER')")
    @ApiMessage("Khởi tạo chuyển tiền và gửi mã OTP")
    public ResponseEntity<ResStringDTO> initiateTransfer(@Valid @RequestBody TransferDTO dto) {
        String senderEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        transactionService.initiateTransfer(senderEmail, dto);

        return ResponseEntity.ok(new ResStringDTO("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra."));
    }

    // Customer chuyển tiền xác nhận OTP
    @PostMapping("/transfer/confirm")
    @PreAuthorize("hasAnyRole('TELLER')")
    @ApiMessage("Xác nhận OTP và thực hiện chuyển khoản")
    public ResponseEntity<ResStringDTO> confirmTransfer(@Valid @RequestBody TranferOTP dto) {
        String senderEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        transactionService.confirmTransfer(senderEmail, dto);

        return ResponseEntity.ok(new ResStringDTO("Chuyển tiền thành công!"));
    }

    @PostMapping("/deposit")
    @PreAuthorize("hasAnyRole('TELLER')")
    @ApiMessage("Khởi tạo gửi tiền và gửi mã OTP")
    public ResponseEntity<ResStringDTO> initiateDeposit(@Valid @RequestBody EmailDTO emailDTO) {
        transactionService.initiateDeposit(emailDTO);
        return ResponseEntity.ok(new ResStringDTO("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra."));
    }

    @PostMapping("/deposit/confirm")
    @PreAuthorize("hasAnyRole('TELLER')")
    @ApiMessage("Xác nhận OTP và thực hiện gửi tiền")
    public ResponseEntity<ResStringDTO> confirmDeposit(@Valid @RequestBody TransactionOTP dto) {
        String tellerEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        transactionService.confirmDeposit(tellerEmail, dto);

        return ResponseEntity.ok(new ResStringDTO("Gửi tiền thành công!"));
    }

    @PostMapping("/withdrawal")
    @PreAuthorize("hasAnyRole('TELLER')")
    @ApiMessage("Khởi tạo rút tiền và gửi mã OTP")
    public ResponseEntity<ResStringDTO> initiateWithdrawal(@Valid @RequestBody TransactionDTO dto) {
        transactionService.initiateWithdrawal(dto);
        return ResponseEntity.ok(new ResStringDTO("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra."));
    }

    @PostMapping("/withdrawal/confirm")
    @PreAuthorize("hasAnyRole('TELLER')")
    @ApiMessage("Xác nhận OTP và thực hiện rút tiền")
    public ResponseEntity<ResStringDTO> confirmWithdrawal(@Valid @RequestBody TransactionOTP dto) {
        String tellerEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        transactionService.confirmWithdrawal(tellerEmail, dto);

        return ResponseEntity.ok(new ResStringDTO("Rút tiền thành công!"));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('CUSTOMER')")
    @ApiMessage("Lấy lịch sử giao dịch của khách hàng đang đăng nhập")
    public ResponseEntity<PageResponseDTO<TransactionResponseDTO>> getTransactionHistory(
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        String customerEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        Pageable pageable = PageRequest.of(current - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"));

        PageResponseDTO<TransactionResponseDTO> transactions = transactionService.getMyTransactionHistory(customerEmail,
                pageable);

        return ResponseEntity.ok(transactions);
    }

}
