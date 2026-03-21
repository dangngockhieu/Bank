package vn.bank.khieu.controller;

import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import vn.bank.khieu.service.NotificationService;
import vn.bank.khieu.utils.annotation.ApiMessage;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @PatchMapping("/{id}")
    @ApiMessage("Đánh dấu thông báo đã đọc")
    public void markAsRead(@PathVariable Long id) {
        notificationService.markAsRead(id);
    }
}
