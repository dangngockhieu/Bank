package vn.bank.khieu.dto.request.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordDTO {
    @NotBlank(message = "Email is not empty")
    @Email(message = "Email không đúng định dạng", regexp = "^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$")
    private String email;

    @NotBlank(message = "Code password is not empty")
    private String code;

    @NotBlank(message = "New password is not empty")
    @Size(min = 8, max = 32, message = "Password must be 8-32 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]+$", message = "Password must contain uppercase, lowercase, number, special character")
    private String newPassword;

    @NotBlank(message = "Confirm password is not empty")
    private String confirmPassword;
}
