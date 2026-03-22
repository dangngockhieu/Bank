package vn.bank.khieu.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.dto.request.user.CreateTellerDTO;
import vn.bank.khieu.dto.request.user.UpdatePasswordDTO;
import vn.bank.khieu.dto.request.user.UpdateUserInforDTO;
import vn.bank.khieu.dto.response.PageResponseDTO;
import vn.bank.khieu.dto.response.ResStringDTO;
import vn.bank.khieu.dto.response.user.ResTellerDTO;
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

    @PostMapping("/create-tellers")
    @ApiMessage("Tạo tài khoản nhân viên mới")
    public ResponseEntity<ResTellerDTO> CreateNewTeller(@Valid @RequestBody CreateTellerDTO dto) {
        ResTellerDTO res = userService.registerNewTeller(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    @GetMapping("/tellers")
    @ApiMessage("Lấy danh sách nhân viên")
    public ResponseEntity<PageResponseDTO<ResTellerDTO>> getAllUsers(
            @RequestParam(value = "current", defaultValue = "1") int current,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "keyword", required = false) String keyword) {
        Pageable pageable = PageRequest.of(current - 1, pageSize, Sort.by(Sort.Direction.ASC, "id"));
        PageResponseDTO<ResTellerDTO> users = this.userService.getAllTellers(pageable, keyword);
        return ResponseEntity.ok(users);
    }

    @PatchMapping("/change-status")
    @ApiMessage("Thay đổi trạng thái hoạt động của nhân viên")
    public ResponseEntity<ResStringDTO> changeUserStatus(@RequestParam Long userId, @RequestParam boolean active) {
        userService.changeUserStatus(userId, active);
        String status = active ? "kích hoạt" : "vô hiệu hóa";
        return ResponseEntity.ok(new ResStringDTO("Tài khoản đã được " + status + " thành công"));
    }

    @PatchMapping("/update-user-info/{userId}")
    @ApiMessage("Cập nhật thông tin người dùng")
    public ResponseEntity<ResStringDTO> updateUserInformation(@PathVariable Long userId,
            @Valid @RequestBody UpdateUserInforDTO dto) {
        userService.updateUser(userId, dto);
        return ResponseEntity.ok(new ResStringDTO("Thông tin người dùng đã được cập nhật thành công"));
    }
}
