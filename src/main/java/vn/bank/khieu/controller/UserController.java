package vn.bank.khieu.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.user.CreateEmployeeDTO;
import vn.bank.khieu.dto.request.user.UpdatePasswordDTO;
import vn.bank.khieu.dto.response.PageResponseDTO;
import vn.bank.khieu.dto.response.ResStringDTO;
import vn.bank.khieu.dto.response.user.ResEmployeeDTO;
import vn.bank.khieu.service.UserService;
import vn.bank.khieu.utils.SecurityUtil;
import vn.bank.khieu.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PatchMapping("/change-password")
    @ApiMessage("Thay đổi mật khẩu")
    public ResponseEntity<ResStringDTO> changePassword(@Valid @RequestBody UpdatePasswordDTO updatePasswordDTO) {
        String email = SecurityUtil.getCurrentUserLogin().orElse(null);
        userService.changePassword(email, updatePasswordDTO);
        return ResponseEntity.ok().body(new ResStringDTO("Mật khẩu đã được thay đổi thành công"));
    }

    @PostMapping("/create-employee")
    @ApiMessage("Tạo tài khoản nhân viên mới")
    public ResponseEntity<ResEmployeeDTO> CreateNewEmployee(@Valid @RequestBody CreateEmployeeDTO dto) {
        ResEmployeeDTO res = userService.registerNewEmployee(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/employees")
    @ApiMessage("Lấy danh sách nhân viên")
    public ResponseEntity<PageResponseDTO<ResEmployeeDTO>> getAllUsers(
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Pageable pageable = PageRequest.of(current - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        PageResponseDTO<ResEmployeeDTO> users = this.userService.getAllEmployees(pageable, keyword);
        return ResponseEntity.ok(users);
    }

}
