# FRUIT SHOP MANAGEMENT

Ứng dụng quản lý cửa hàng trái cây trên nền tảng Android, phục vụ đồ án môn học / bài thực hành (BTH2).

---

## 👥 Danh sách thành viên

1. **Nguyễn Trung Nam** (Nhóm trưởng)
2. **Lê Duy Anh**
3. **Phan Nguyễn Việt Dũng**
4. **Vũ Hoàng Anh**
5. **Trương Gia Bảo**

---

## 📌 Tổng quan dự án

Hệ thống quản lý cửa hàng trái cây hỗ trợ 2 vai trò người dùng (**Admin** và **Customer**), lưu trữ dữ liệu cục bộ an toàn qua **SQLite / Room Database** và duy trì phiên đăng nhập với **SharedPreferences**.

### 🗄️ Cấu trúc Cơ sở dữ liệu (4 bảng chính)

1. **`Account`**: `(id, username, password, role)`
   - Lưu tài khoản đăng nhập hệ thống, phân quyền `Admin` hoặc `Customer`.
2. **`Fruit`**: `(id, name, category, price, quantity, expiryDate, description)`
   - Lưu thông tin trái cây trong cửa hàng, theo dõi số lượng và ngày hết hạn.
3. **`Customer`**: `(id, fullname, phone, email, accountId)`
   - Lưu thông tin chi tiết khách hàng, liên kết khóa ngoại với `Account(id)`.
4. **`Order`**: `(id, customerId, orderDate, totalAmount, status)`
   - Lưu đơn đặt hàng, liên kết với `Customer(id)`. Trạng thái đơn hàng: `Pending`, `Confirmed`, `Completed`.

---

## ⚖️ Phân công công việc (Chia đều cho 5 thành viên)

Tiêu chí đánh giá dựa trên tốc độ và thời gian hoàn thành (càng nhanh điểm càng cao), do đó các module được phân rã độc lập để 5 thành viên có thể code song song (parallel) mà không bị nghẽn (bottleneck).

| STT | Thành viên | Vai trò / Module phụ trách | Chi tiết công việc & Deliverables |
|:---:|---|---|---|
| **1** | **Nguyễn Trung Nam** *(Lead)* | **Database & Architecture Core** | • Thiết kế & khởi tạo SQLite / Room Database với 4 bảng: `Account`, `Fruit`, `Customer`, `Order`.<br>• Xây dựng các Entity, DAO / DatabaseHelper, Repository mẫu.<br>• Viết hàm khởi tạo dữ liệu mẫu (Dummy/Seed data) về tài khoản admin/user, trái cây để cả nhóm test.<br>• Quản lý GitHub repo, quy chuẩn git branch, merge code và giải quyết conflict. |
| **2** | **Lê Duy Anh** | **Auth & Session Management** | • Xây dựng UI/UX màn hình Đăng nhập (Login), Đăng ký (Register), Đăng xuất (Logout).<br>• Xử lý xác thực tài khoản cho 2 vai trò: `Admin` và `Customer`.<br>• Lưu trữ thông tin tài khoản đăng nhập và phiên làm việc bằng `SharedPreferences`.<br>• Điều hướng phân quyền (Role-based Navigation / Routing) chuyển đúng màn hình tương ứng với từng vai trò. |
| **3** | **Phan Nguyễn Việt Dũng** | **Admin: Fruit & Expiry Management** | • Xây dựng UI/UX giao diện Admin: Danh sách trái cây, form Thêm/Sửa/Xóa (CRUD).<br>• Triển khai các tính năng CRUD trái cây: Tên, danh mục, giá, số lượng, hạn sử dụng, mô tả.<br>• Chức năng tìm kiếm và lọc danh sách trái cây cho Admin.<br>• **Quản lý hạn sử dụng (Expiry Date Management)**: Hiển thị trực quan hạn sử dụng, tự động đánh dấu và cảnh báo trái cây đã hết hạn (badge cảnh báo/màu sắc nổi bật). |
| **4** | **Vũ Hoàng Anh** | **Customer: Browse & Place Order** | • Xây dựng UI/UX cho Khách hàng: Màn hình danh sách trái cây còn hạn, xem chi tiết trái cây.<br>• Chức năng tìm kiếm, lọc trái cây theo danh mục, kiểm tra thông tin và hạn sử dụng.<br>• **Nghiệp vụ chặn mua hàng hết hạn**: Khóa nút đặt hàng / không cho phép khách hàng đặt các mặt hàng đã quá hạn sử dụng hoặc hết số lượng (`quantity = 0`).<br>• Chức năng chọn mua, chọn số lượng và tiến hành Đặt hàng (`Place Order`). |
| **5** | **Trương Gia Bảo** | **Order Management & Customer Profile** | • Giao diện & chức năng Xem lịch sử đơn hàng cho Khách hàng (`Customer Order History`).<br>• Giao diện & chức năng Quản lý tất cả đơn hàng cho Admin: Xem danh sách, cập nhật trạng thái đơn hàng (`Pending` ➔ `Confirmed` ➔ `Completed`).<br>• Màn hình thông tin cá nhân khách hàng (`Customer Profile` liên kết `Account`).<br>• Thống kê cơ bản (tổng số đơn, trạng thái đơn) hỗ trợ Admin theo dõi đơn hàng nhanh chóng. |

---

## 🛠️ Công nghệ sử dụng

- **Ngôn ngữ**: Kotlin / Java
- **Nền tảng**: Android SDK (Android Studio)
- **Kiến trúc**: MVVM / MVP
- **Lưu trữ dữ liệu**: Room Database hoặc SQLite Helper
- **Quản lý phiên**: SharedPreferences
- **Quản lý phiên bản**: Git & GitHub

---

## 🚀 Quy trình làm việc nhóm trên GitHub

Để tối ưu hóa thời gian hoàn thành dự án:
1. Mỗi thành viên checkout nhánh riêng theo tính năng:
   - `feature/database-core` (Nguyễn Trung Nam)
   - `feature/auth-sharedpref` (Lê Duy Anh)
   - `feature/admin-fruit-crud` (Phan Nguyễn Việt Dũng)
   - `feature/customer-shopping` (Vũ Hoàng Anh)
   - `feature/order-management` (Trương Gia Bảo)
2. Sau khi hoàn thành và tự kiểm thử (Unit test / UI test), tạo Pull Request (PR) về nhánh `main`.
3. Nhóm trưởng review và merge vào `main`.
