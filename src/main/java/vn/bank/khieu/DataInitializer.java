package vn.bank.khieu;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.bank.khieu.entity.Role;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.enums.RoleName;
import vn.bank.khieu.repository.RoleRepository;
import vn.bank.khieu.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Khởi tạo Roles nếu chưa có
        if (roleRepository.count() == 0) {
            roleRepository.save(new Role(RoleName.ROLE_CUSTOMER));
            roleRepository.save(new Role(RoleName.ROLE_TELLER));
            roleRepository.save(new Role(RoleName.ROLE_ADMIN));
        }

        // Khởi tạo duy nhất User Admin nếu chưa có
        if (!userRepository.existsByEmail("admin@bank.vn")) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Role not found"));

            User admin = new User();
            admin.setEmail("admin@bank.vn");
            admin.setFullName("Quản trị viên");
            admin.setPassword(passwordEncoder.encode("123456"));
            admin.setActive(true);
            admin.setRole(adminRole);

            userRepository.save(admin);

            System.out.println(">>> Seed Admin thành công!");
        }
    }
}
