package vn.bank.khieu.enums;

public enum RoleName {
    ROLE_CUSTOMER, // Khách hàng: Chỉ xem số dư, chuyển khoản của mình
    ROLE_TELLER, // Giao dịch viên: Nộp/Rút tiền tại quầy cho khách
    ROLE_CHECKER, // Kiểm soát viên: Duyệt các giao dịch lớn, quản lý User
    ROLE_ADMIN // Quản trị viên: Cấu hình hệ thống, phân quyền
}
