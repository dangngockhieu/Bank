package vn.bank.khieu.dto.request.transaction.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TranferOTP extends TransferDTO {
    @NotBlank(message = "Mã OTP không được để trống")
    @Size(min = 6, max = 6, message = "Mã OTP phải bao gồm đúng 6 chữ số")
    private String otpCode;
}
