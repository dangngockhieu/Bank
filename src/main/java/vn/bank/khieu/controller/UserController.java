package vn.bank.khieu.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.customer.CreateCustomerDTO;
import vn.bank.khieu.dto.response.customer.ResCreateCustomerDTO;
import vn.bank.khieu.service.CustomerService;
import vn.bank.khieu.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final CustomerService customerService;

    @PostMapping("/register")
    @ApiMessage("Đăng ký khách hàng mới")
    public ResponseEntity<ResCreateCustomerDTO> CreateNewCustomer(@Valid @RequestBody CreateCustomerDTO dto) {
        ResCreateCustomerDTO res = customerService.registerNewCustomer(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

}
