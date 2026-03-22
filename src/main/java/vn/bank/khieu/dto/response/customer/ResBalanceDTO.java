package vn.bank.khieu.dto.response.customer;

import java.math.BigDecimal;

public interface ResBalanceDTO {
    Long getId();

    String getAccountNumber();

    BigDecimal getBalance();
}