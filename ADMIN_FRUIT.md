# Module Admin — Phan Nguyễn Việt Dũng

## Trạng thái sau tích hợp vào main

Admin đã dùng adapter SqliteFruitRepository qua DatabaseHelper/FruitRepository chung, file fruit_shop.db. Login là launcher; mở quản lý trái cây từ trang Admin. Hai lỗi biên dịch giao diện đã được sửa trong merge. Mô tả database admin_fruit.db bên dưới là handoff trước tích hợp.


Đã triển khai trong package `com.cie.bth2.admin.fruit`, nhánh `feature/admin-fruit-crud`.

## Chức năng

### Giao diện

- Tông xanh lá, nền dịu, thẻ mặt hàng bo góc và hỗ trợ giao diện sáng/tối.
- Tổng quan ba chỉ số: mặt hàng, đã hết hạn, hạn hôm nay; chạm chỉ số để lọc nhanh.
- Thanh cảnh báo hết hạn, tìm kiếm có nút xóa, bộ lọc cạnh nhau và nút đặt lại.
- Màn hình trống có hướng dẫn riêng cho kho chưa có hàng và tìm kiếm không có kết quả.
- Nút thêm cố định cuối màn hình; phần nội dung cuộn được trên màn hình nhỏ.
- Form viền nổi, chọn ngày bằng lịch, chỉ lỗi ở trường nhập, giữ bản nháp khi xoay màn hình; giữ form khi lưu thất bại.
- Thẻ mặt hàng hiển thị giá VNĐ, tồn kho, nhãn hết hàng, hạn dùng dạng `dd/MM/yyyy` và trạng thái bằng cả chữ lẫn màu.
- Thanh tiến trình khi truy cập dữ liệu và thông báo sau khi thêm/sửa/xóa thành công.

### Nghiệp vụ

- Danh sách trái cây; chạm một dòng để mở form sửa hoặc xóa.
- Thêm/sửa đủ tên, danh mục, giá VNĐ, số lượng, hạn sử dụng và mô tả.
- Kiểm tra trường bắt buộc, giá hữu hạn không âm, số lượng nguyên không âm và ngày hợp lệ. Cho phép lưu hàng đã hết hạn để quản lý tồn kho.
- Chọn hạn bằng lịch, xác nhận trước khi xóa.
- Tìm tên/mô tả không phân biệt hoa thường; kết hợp lọc danh mục và hạn sử dụng.
- Nhãn màu kèm chữ: đã hết hạn, hết hạn hôm nay, còn hạn; thống kê số mặt hàng hết hạn trên toàn kho.
- Hạn lưu theo `yyyy-MM-dd`. Hàng hết hạn khi ngày hạn **nhỏ hơn** ngày hiện tại theo múi giờ thiết bị; vẫn còn hạn trong ngày ghi trên sản phẩm.
- Cập nhật nhãn mỗi 30 giây khi màn hình đang mở và khi quay lại ứng dụng.
- SQLite lưu dữ liệu qua các lần đóng/mở ứng dụng; truy cập database trên executor riêng.

## Ghép với các module khác

Repository hiện chỉ có khung Android, chưa có database chung hoặc đăng nhập. Vì vậy module có launcher độc lập và database `admin_fruit.db`, bảng `Fruit` với đúng bảy trường README. Không tạo tài khoản, bảng đơn hàng hoặc dữ liệu mẫu của module khác.

1. Nhóm trưởng triển khai `FruitRepository` bằng DAO/database chung, thay `SqliteFruitRepository(this)` trong `AdminFruitActivity`. Nếu muốn giữ dữ liệu demo, cần chủ động chuyển dữ liệu từ database riêng.
2. Module Auth chuyển launcher về Login; đặt `AdminFruitActivity` thành `android:exported="false"`, chỉ điều hướng vào Activity sau khi xác thực quyền Admin và kiểm tra phiên/quyền khi quay lại màn hình. Launcher hiện tại dùng để chạy module độc lập, **chưa cung cấp bảo vệ quyền Admin**.
3. Giao diện `FruitRepository`: `all`, `save` (id = 0 để thêm; id khác 0 để sửa), `delete`, `close`. Các lỗi lưu/xóa được hiển thị để người dùng thử lại.
4. Presenter chịu trách nhiệm chạy repository ngoài UI thread và ngắt callback khi Activity bị hủy. Không cần thêm thư viện mới.

## Kiểm thử

Theo yêu cầu mới nhất, tập trung hoàn thiện UX/UI và **không chạy kiểm thử/build tiếp**. Lần build trước dừng ở bước tải Gradle do timeout; chưa xác nhận biên dịch hoặc chạy trên thiết bị. Các test đã viết sẵn được giữ lại cho nhóm sử dụng sau.

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat connectedDebugAndroidTest
```

- `FruitRulesTest`: ranh giới ngày/tháng/năm, năm nhuận, dữ liệu không hợp lệ, phối hợp bộ lọc.
- `FruitRepositoryTest`: thêm/sửa, mở lại database kiểm tra lưu bền vững, xóa; dùng database trong context APK kiểm thử riêng.
- Kiểm tra thủ công trên máy ảo/thiết bị: thêm hàng quá hạn/hôm nay/ngày mai; thử giá và số lượng sai; tìm kiếm kết hợp hai bộ lọc; hủy/xác nhận xóa; đóng mở lại ứng dụng; kiểm tra chế độ tối và bàn phím trên màn hình nhỏ.

Chỉ tạo PR vào `main` sau khi hoàn thành kiểm thử và commit/push nhánh; nhóm trưởng review/merge theo README.
