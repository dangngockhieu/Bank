# Mini Banking System - High Consistency & Security

## Giới thiệu

Dự án tập trung vào việc xây dựng các logic nghiệp vụ ngân hàng cốt lõi như **Chuyển tiền (Transfer)**, **Nạp/Rút tiền (Deposit/Withdrawal)** và **Quản lý bảo mật**.

Hệ thống được tối ưu hóa để xử lý các vấn đề **tranh chấp dữ liệu** và **phân quyền đa cấp**, đảm bảo tính **nhất quán (consistency)** và **an toàn (security)** trong môi trường giao dịch tài chính.

---

## Tính năng

### 1. Ngăn chặn Double Spending bằng Optimistic Locking

Trong các giao dịch tài chính, việc hai giao dịch xảy ra đồng thời có thể dẫn đến sai lệch số dư.

- Triển khai **Optimistic Locking** với `@Version` của JPA
- Đảm bảo **Consistency** mà không cần khóa toàn bộ bảng
- Tối ưu hiệu năng cho hệ thống có **tần suất đọc cao**

---

### 2. Phân quyền đa vai trò (RBAC) với Method Security

Hệ thống hỗ trợ 3 nhóm người dùng:

- **CUSTOMER**
  - Chuyển tiền
  - Xem lịch sử giao dịch

- **TELLER**
  - Nạp / Rút tiền tại quầy
  - Thu hồi quyền truy cập (Revoke)

- **ADMIN**
  - Quản trị người dùng
  - Quản lý hệ thống

**Kiến trúc:**

- Sử dụng `@PreAuthorize` tại tầng Controller
- Đảm bảo **tính đóng gói (encapsulation)** và **dễ bảo trì**

---

### 3. Quản lý phiên đăng nhập & Revoke Token

- Sử dụng **JWT** cho xác thực
- Kết hợp cơ chế **Blacklist (Redis / Database)**
- Cho phép **Teller/Admin thu hồi quyền truy cập ngay lập tức** khi phát hiện bất thường

---

## 🛠 Tech Stack

- **Spring Boot 3**
- **Spring Security (OAuth2 Resource Server)**
- **Spring Data JPA (Hibernate)**
- **PostgreSQL**
- **REDIS**
- **Lombok**
- **MapStruct**
- **Bean Validation**

---
