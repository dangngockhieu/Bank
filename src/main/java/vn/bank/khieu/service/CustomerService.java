package vn.bank.khieu.service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;
import vn.bank.khieu.dto.request.customer.CreateCustomerDTO;
import vn.bank.khieu.dto.response.customer.ResBalanceDTO;
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
import vn.bank.khieu.utils.error.NotFindException;

@RequiredArgsConstructor
@Service
@Slf4j
public class CustomerService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RoleRepository roleRepository;
    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

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
                .orElseThrow(() -> new NotFindException("Lỗi: Role CUSTOMER chưa được khởi tạo trong Database!"));
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
        account.setBalance(dto.getInitialBalance() != null ? dto.getInitialBalance() : BigDecimal.ZERO);
        account.setCustomer(customer);
        accountRepository.save(account);

        return new ResCustomerDTO(
                customer.getId(),
                user.getEmail(),
                user.getFullName(),
                customer.getIdentityCard(),
                customer.getPhoneNumber(),
                customer.getAddress(),
                account.getAccountNumber(),
                true);

    }

    private ResCustomerDTO convertToResCustomerDTO(Customer customer) {
        User user = customer.getUser();
        ResCustomerDTO dto = new ResCustomerDTO();

        dto.setId(customer.getId());
        dto.setIdentityCard(customer.getIdentityCard());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setAddress(customer.getAddress());

        if (user != null) {
            dto.setEmail(user.getEmail());
            dto.setFullName(user.getFullName());
            dto.setActive(user.isActive());
        }

        if (customer.getAccount() != null) {
            dto.setAccountNumber(customer.getAccount().getAccountNumber());
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public ResCustomerDTO getMyAccountCustomer(String email) {
        String cacheKey = "bank:customer:profile:" + email;

        // Kiểm tra trong Redis trước
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedJson != null) {
                return objectMapper.readValue(cachedJson, ResCustomerDTO.class);
            }
        } catch (Exception e) {
            log.error("Redis error: ", e);
        }

        // Nếu Redis không có, lấy từ DB
        ResCustomerDTO dto = customerRepository.findByUserEmail(email)
                .map(this::convertToResCustomerDTO)
                .orElseThrow(() -> new NotFindException("Không tìm thấy khách hàng"));

        // Lưu lại vào Redis
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    objectMapper.writeValueAsString(dto),
                    30,
                    TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("Failed to cache to Redis: ", e);
        }

        return dto;
    }

    @Transactional(readOnly = true)
    public ResCustomerDTO findCustomer(String keyword) {
        return customerRepository.findCustomerWithUserByKeyword(keyword)
                .map(this::convertToResCustomerDTO)
                .orElseThrow(() -> new NotFindException("Không tìm thấy khách hàng"));
    }

    @Transactional(readOnly = true)
    public ResBalanceDTO getMyBalance(String email) {

        ResBalanceDTO dto = accountRepository.findBalanceByEmail(email)
                .orElseThrow(() -> new NotFindException("Không tìm thấy tài khoản"));

        return dto;
    }
}
