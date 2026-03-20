package vn.bank.khieu.dto.request.customer;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateCustomerDTO {
    @NotBlank(message = "Email không được để trống")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Password không được để trống")
    private String password;

    @NotEmpty(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "CCCD là bắt buộc")
    @Pattern(regexp = "^\\d{12}$", message = "CCCD phải đúng 12 chữ số")
    private String identityCard;

    @NotBlank(message = "Số điện thoại là bắt buộc")
    @Pattern(regexp = "^\\d{10}$", message = "Số điện thoại phải đúng 10 chữ số")
    private String phoneNumber;

    @NotEmpty(message = "Địa chỉ là bắt buộc")
    private String address;

    private BigDecimal initialBalance = BigDecimal.ZERO;
}
