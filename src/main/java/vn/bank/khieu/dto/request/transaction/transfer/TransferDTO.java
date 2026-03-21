package vn.bank.khieu.dto.request.transaction.transfer;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransferDTO {
    @NotBlank(message = "Số tài khoản người nhận không được để trống")
    private String recipientAccountNumber;

    @NotNull(message = "Số tiền chuyển không được để trống")
    @DecimalMin(value = "1.0", inclusive = true, message = "Số tiền chuyển phải lớn hơn 0")
    private BigDecimal amount;

    private String description;
}
