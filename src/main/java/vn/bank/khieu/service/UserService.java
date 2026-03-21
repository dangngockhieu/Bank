package vn.bank.khieu.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.user.CreateEmployeeDTO;
import vn.bank.khieu.dto.request.user.UpdatePasswordDTO;
import vn.bank.khieu.dto.response.PageResponseDTO;
import vn.bank.khieu.dto.response.user.ResEmployeeDTO;
import vn.bank.khieu.entity.Role;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.enums.RoleName;
import vn.bank.khieu.mapper.UserMapper;
import vn.bank.khieu.repository.RoleRepository;
import vn.bank.khieu.repository.UserRepository;
import vn.bank.khieu.utils.GenericSpecification;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;
    private final UserMapper userMapper;

    public Optional<User> findUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void changePassword(String email, UpdatePasswordDTO updatePasswordDTO) {
        if (!updatePasswordDTO.getNewPassword().equals(updatePasswordDTO.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        User existingUser = this.userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy User với ID: " + email));
        boolean isMatch = passwordEncoder.matches(updatePasswordDTO.getOldPassword(), existingUser.getPassword());
        if (!isMatch) {
            throw new IllegalArgumentException("Mật khẩu không chính xác");
        }
        String hashPassword = passwordEncoder.encode(updatePasswordDTO.getNewPassword());
        existingUser.setPassword(hashPassword);
        this.userRepository.save(existingUser);
    }

    public ResEmployeeDTO registerNewEmployee(CreateEmployeeDTO dto) {
        // Tạo và lưu User (Đăng nhập)
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Lấy Role mặc định là TELLER
        Role tellerRole = roleRepository.findByName(RoleName.ROLE_TELLER)
                .orElseThrow(() -> new RuntimeException("Lỗi: Role TELLER chưa được khởi tạo trong Database!"));
        user.setRole(tellerRole);
        userRepository.save(user);

        return new ResEmployeeDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt());

    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ResEmployeeDTO> getAllEmployees(Pageable pageable, String keyword) {
        Specification<User> spec = GenericSpecification.<User>equal("active", true);
        spec = spec.and(GenericSpecification.<User>equal("role.name", RoleName.ROLE_TELLER.name()));
        if (keyword != null && !keyword.isBlank()) {
            Specification<User> keywordSpec = GenericSpecification.<User>like("email", keyword)
                    .or(GenericSpecification.<User>like("fullName", keyword));

            spec = spec.and(keywordSpec);
        }
        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<ResEmployeeDTO> users = userPage.getContent().stream()
                .map(userMapper::toEmployeeDTO)
                .collect(Collectors.toList());
        return new PageResponseDTO<ResEmployeeDTO>(
                users,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber() + 1,
                userPage.getSize());
    }
}
