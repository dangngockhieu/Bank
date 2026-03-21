package vn.bank.khieu.dto.response.customer;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResBalanceDTO {
    private Long id;
    private String accountNumber;
    private BigDecimal balance;
}