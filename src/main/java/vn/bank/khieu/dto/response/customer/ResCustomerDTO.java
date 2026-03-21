package vn.bank.khieu.dto.response.customer;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResCustomerDTO {
    private Long id;
    private String email;
    private String fullName;
    private String identityCard;
    private String phoneNumber;
    private String address;
    private String accountNumber;
}
