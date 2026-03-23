package vn.bank.khieu.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.user.CreateTellerDTO;
import vn.bank.khieu.dto.request.user.UpdatePasswordDTO;
import vn.bank.khieu.dto.request.user.UpdateUserInforDTO;
import vn.bank.khieu.dto.response.PageResponseDTO;
import vn.bank.khieu.dto.response.user.ResTellerDTO;
import vn.bank.khieu.entity.Role;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.enums.RoleName;
import vn.bank.khieu.mapper.UserMapper;
import vn.bank.khieu.repository.RoleRepository;
import vn.bank.khieu.repository.UserRepository;
import vn.bank.khieu.utils.GenericSpecification;
import vn.bank.khieu.utils.error.NotFindException;

@RequiredArgsConstructor
@Service
public class UserService {
    private final StringRedisTemplate stringRedisTemplate;
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
                .orElseThrow(() -> new NotFindException("Không tìm thấy User với ID: " + email));
        boolean isMatch = passwordEncoder.matches(updatePasswordDTO.getOldPassword(), existingUser.getPassword());
        if (!isMatch) {
            throw new IllegalArgumentException("Mật khẩu không chính xác");
        }
        String hashPassword = passwordEncoder.encode(updatePasswordDTO.getNewPassword());
        existingUser.setPassword(hashPassword);
        this.userRepository.save(existingUser);
    }

    public ResTellerDTO registerNewTeller(CreateTellerDTO dto) {
        // Tạo và lưu User (Đăng nhập)
        User user = new User();
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // Lấy Role mặc định là TELLER
        Role tellerRole = roleRepository.findByName(RoleName.ROLE_TELLER)
                .orElseThrow(() -> new NotFindException("Lỗi: Role TELLER chưa được khởi tạo trong Database!"));
        user.setRole(tellerRole);
        userRepository.save(user);

        return new ResTellerDTO(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getCreatedAt());

    }

    @Transactional(readOnly = true)
    public PageResponseDTO<ResTellerDTO> getAllTellers(Pageable pageable, String keyword) {
        Specification<User> spec = GenericSpecification.<User>equal("active", true);
        spec = spec.and((root, query, cb) -> cb.equal(root.join("role").get("name"), RoleName.ROLE_TELLER));
        if (keyword != null && !keyword.isBlank()) {
            Specification<User> keywordSpec = GenericSpecification.<User>like("email", keyword)
                    .or(GenericSpecification.<User>like("fullName", keyword));

            spec = spec.and(keywordSpec);
        }
        Page<User> userPage = userRepository.findAll(spec, pageable);
        List<ResTellerDTO> users = userPage.getContent().stream()
                .map(userMapper::toTellerDTO)
                .collect(Collectors.toList());
        return new PageResponseDTO<ResTellerDTO>(
                users,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                userPage.getNumber() + 1,
                userPage.getSize());
    }

    public void changeUserStatus(Long userId, boolean active) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFindException("Không tìm thấy User với ID: " + userId));
        user.setActive(active);
        userRepository.save(user);
    }

    public void updateUser(Long userId, UpdateUserInforDTO dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFindException("Không tìm thấy User với ID: " + userId));
        user.setFullName(dto.getFullName());
        user.setEmail(dto.getEmail());
        userRepository.save(user);
        String email = user.getEmail();
        String loginFailKey = "bank:auth:fail_count:login:" + email;
        stringRedisTemplate.delete(loginFailKey);
    }
}
