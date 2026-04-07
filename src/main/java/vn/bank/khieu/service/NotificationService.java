package vn.bank.khieu.service;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import vn.bank.khieu.entity.Notification;
import vn.bank.khieu.entity.User;
import vn.bank.khieu.repository.NotificationRepository;
import vn.bank.khieu.utils.error.NotFindException;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    private final SimpMessagingTemplate messagingTemplate;

    @Async
    public void createNotification(User user, String title, String content) {
        if (user == null)
            return;

        // Lưu vào Database
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setReaded(false);

        notification = notificationRepository.save(notification);

        // Format dữ liệu để gửi đi
        String messagePayload = String.format("{\"id\": %d, \"title\": \"%s\", \"content\": \"%s\"}",
                notification.getId(), title, content);

        // Bắn thẳng qua WebSocket cho User đó
        messagingTemplate.convertAndSendToUser(
                user.getFullName(), // Đích danh người nhận
                "/queue/notifications", // Kênh nhận
                messagePayload // Dữ liệu
        );
    }

    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFindException("Notification not found with ID: " + notificationId));
        notification.setReaded(true);
        notificationRepository.save(notification);
    }
}