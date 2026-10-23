# Auth & Session Management — Lê Duy Anh

## Trạng thái sau tích hợp vào main

RoleHomeActivity hiện có nút mở module Admin (kho, đơn hàng, showcase) và Customer (mua sắm, hồ sơ, lịch sử). Các module cùng dùng fruit_shop.db; phiên lưu tại auth_session. Showcase dùng file fruit_shop_showcase.db riêng.


Branch: `feature/auth-sharedpref`. Module dùng Kotlin, XML, MVP và SQLite core của nhánh `feature/database-core` (commit `be83d79`).

## Chức năng

- `LoginActivity`: kiểm tra dữ liệu nhập, xác thực Admin/Customer qua `AccountRepository`, báo sai tài khoản/mật khẩu.
- `RegisterActivity`: họ tên, điện thoại, email, username, mật khẩu và xác nhận mật khẩu. Chỉ tạo Customer; Account và hồ sơ Customer được ghi trong một transaction.
- `SessionManager`: SharedPreferences `auth_session`, gồm `accountId`, `username`, `role`; không lưu mật khẩu. Phiên được loại khỏi backup và chuyển thiết bị.
- `AuthRepository.restore`: đối chiếu ID/username và đọc lại role từ SQLite khi khôi phục phiên. Tài khoản đã xóa không được truy cập.
- `AuthNavigator`: điều hướng theo role, xóa back stack khi đăng nhập/đăng xuất.
- `RoleHomeActivity`: kiểm tra phiên khi quay lại màn hình, có xác nhận đăng xuất. Hai màn hình Admin/Customer hiện là điểm nối cho module của Dũng và Hoàng Anh.
- `AuthPresenter`: xử lý database trên worker thread, chặn gửi lặp khi đang xử lý, bỏ callback nếu màn hình đã bị hủy.

## Ghép code với các thành viên

1. Dùng cùng `DatabaseHelper` và `AccountRepository`; không tạo database riêng cho auth.
2. Thay nội dung `AdminHomeActivity` / `CustomerHomeActivity` bằng giao diện tương ứng, giữ kiểm tra phiên/phân quyền từ `RoleHomeActivity`. Nếu thay class đích, sửa `AuthNavigator` và manifest.
3. Lấy account đang đăng nhập bằng `SessionManager(context).current()`; lấy hồ sơ bằng `CustomerRepository(helper).getByAccountId(account.id)`. Với màn hình bảo vệ, đối chiếu database trước khi hiển thị dữ liệu.
4. Đăng xuất bằng `AuthNavigator.logout(activity)`.
5. Các màn hình nội bộ để `android:exported="false"`.

## Tài khoản mẫu từ database core

| Vai trò | Username | Password |
|---|---|---|
| Admin | admin | admin123 |
| Customer | customer | customer123 |

Database core hiện lưu mật khẩu dạng văn bản. Module này giữ tương thích với repository của nhóm; cần phối hợp thay cơ chế lưu/xác thực và migrate dữ liệu nếu dùng ngoài bài thực hành.

## Kiểm thử

Dùng JDK 17 cho Gradle 8.13 của dự án, Android SDK Platform 36 và emulator/thiết bị Android. Trong Android Studio, chọn JDK 17 tại Settings → Build Tools → Gradle → Gradle JDK.

- Build/lint: `./gradlew assembleDebug testDebugUnitTest lintDebug`
- Kiểm thử SQLite/SharedPreferences trên emulator: `./gradlew connectedDebugAndroidTest`
- Thử đăng nhập hai tài khoản mẫu, sai mật khẩu, input trống.
- Đăng ký mới; kiểm tra trùng tên, email/điện thoại sai, mật khẩu ngắn và xác nhận không khớp.
- Tắt/mở ứng dụng: vào đúng vai trò; đăng xuất rồi Back: không vào lại trang đã đăng nhập.
- Xoay màn hình trong lúc xử lý: thao tác database vẫn có thể hoàn tất nhưng callback màn hình cũ bị bỏ. Nếu đăng ký vừa hoàn tất, có thể đăng nhập bằng tài khoản đã tạo.
