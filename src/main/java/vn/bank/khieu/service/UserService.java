package vn.bank.khieu.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.user.UpdatePasswordDTO;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.repository.UserRepository;

@RequiredArgsConstructor
@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
