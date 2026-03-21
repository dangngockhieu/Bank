package vn.bank.khieu.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.transaction.transfer.TransactionOTP;
import vn.bank.khieu.dto.request.transaction.transfer.TransferDTO;
import vn.bank.khieu.dto.response.ResStringDTO;
import vn.bank.khieu.service.TransactionService;
import vn.bank.khieu.utils.SecurityUtil;
import vn.bank.khieu.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer/initiate")
    @ApiMessage("Khởi tạo chuyển tiền và gửi mã OTP")
    public ResponseEntity<ResStringDTO> initiateTransfer(@Valid @RequestBody TransferDTO dto) {
        String senderEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        transactionService.initiateTransfer(senderEmail, dto);

        return ResponseEntity.ok(new ResStringDTO("Mã OTP đã được gửi đến email của bạn. Vui lòng kiểm tra."));
    }

    @PostMapping("/transfer/confirm")
    @ApiMessage("Xác nhận OTP và thực hiện chuyển khoản")
    public ResponseEntity<ResStringDTO> confirmTransfer(@Valid @RequestBody TransactionOTP dto) {
        String senderEmail = SecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> new RuntimeException("Chưa đăng nhập"));

        transactionService.confirmTransfer(senderEmail, dto);

        return ResponseEntity.ok(new ResStringDTO("Chuyển tiền thành công!"));
    }
}
