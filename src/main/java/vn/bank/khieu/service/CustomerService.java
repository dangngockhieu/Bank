package vn.bank.khieu.service;

import java.math.BigDecimal;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.customer.CreateCustomerDTO;
import vn.bank.khieu.dto.response.customer.ResCustomerDTO;
import vn.bank.khieu.entity.Account;
import vn.bank.khieu.entity.Customer;
import vn.bank.khieu.entity.Role;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.enums.RoleName;
import vn.bank.khieu.repository.AccountRepository;
import vn.bank.khieu.repository.CustomerRepository;
import vn.bank.khieu.repository.RoleRepository;
import vn.bank.khieu.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class CustomerService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    public String createUniqueAccountNumber() {
        Long nextId = stringRedisTemplate.opsForValue().increment("bank:account:next_id");
        if (nextId == null || nextId == 1) {
            stringRedisTemplate.opsForValue().set("bank:account:next_id", "1000000001");
            return "1000000001";
        }
        return String.valueOf(nextId);
    }

    @Transactional
    public ResCustomerDTO registerNewCustomer(CreateCustomerDTO dto) {
        // Tạo và lưu User (Đăng nhập)
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Lấy Role mặc định là CUSTOMER
        Role customerRole = roleRepository.findByName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new RuntimeException("Lỗi: Role CUSTOMER chưa được khởi tạo trong Database!"));
        user.setRole(customerRole);
        userRepository.save(user);

        // Tạo và lưu Customer
        Customer customer = new Customer();
        customer.setPhoneNumber(dto.getPhoneNumber());
        customer.setIdentityCard(dto.getIdentityCard());
        customer.setAddress(dto.getAddress());
        customer.setUser(user);
        customerRepository.save(customer);

        // Tạo và lưu Account
        Account account = new Account();
        account.setAccountNumber(createUniqueAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setCustomer(customer);
        accountRepository.save(account);

        return new ResCustomerDTO(
                customer.getId(),
                user.getEmail(),
                user.getFullName(),
                customer.getIdentityCard(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                account.getAccountNumber());

    }

}
