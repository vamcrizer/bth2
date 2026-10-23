# FRUIT SHOP MANAGEMENT

Ứng dụng Android quản lý cửa hàng trái cây cho bài thực hành BTH2. Hệ thống phục vụ hai vai trò **Admin** và **Customer**, dùng SQLite cục bộ để lưu tài khoản, trái cây, hồ sơ khách hàng và đơn hàng.

---

## 👥 Danh sách thành viên

1. **Nguyễn Trung Nam** (Nhóm trưởng)
2. **Lê Duy Anh**
3. **Phan Nguyễn Việt Dũng**
4. **Vũ Hoàng Anh**
5. **Trương Gia Bảo**

---

## 📌 Tổng quan dự án

### Đề bài / phạm vi chức năng

- **Admin** quản lý danh mục trái cây, giá, tồn kho và hạn sử dụng; quản lý hồ sơ/đơn hàng và cập nhật trạng thái đơn.
- **Customer** đăng ký/đăng nhập, duyệt và tìm trái cây còn hạn/còn hàng, đặt hàng, xem hồ sơ và lịch sử mua hàng.
- Dữ liệu được lưu cục bộ bằng SQLite. Phiên đăng nhập được lưu bằng SharedPreferences.
- Không cho đặt mua trái cây hết hạn hoặc đã hết hàng. Trạng thái đơn đi theo luồng `Pending` → `Confirmed` → `Completed`.

### Tiêu chí hoàn thành

- Các màn hình chính hoạt động và điều hướng theo vai trò.
- CRUD trái cây và hồ sơ khách hàng hoạt động với dữ liệu SQLite.
- Có thể tạo đơn, xem lịch sử và cập nhật trạng thái đơn.
- Dữ liệu mẫu hỗ trợ chạy thử; các thao tác mua hàng kiểm tra tồn kho và hạn sử dụng.

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
| **1** | **Nguyễn Trung Nam** *(Lead)* | **Database, Domain Rules, Tester & Integration Core** | • Thiết kế và duy trì schema SQLite 4 bảng, khóa ngoại, ràng buộc dữ liệu và migration theo phiên bản.<br>• Cung cấp `DatabaseHelper`, seed data, repository/API dùng chung cho Account, Fruit, Customer, Order.<br>• Xây dựng quy tắc domain lõi: validate dữ liệu, trạng thái đơn hợp lệ, transaction cho thao tác nhiều bước và kiểm tra tồn kho/hạn dùng khi đặt hàng.<br>• **Tester BE:** viết/chạy kiểm thử schema, seed, CRUD, khóa ngoại, transaction, validation và các luồng lỗi; kiểm tra tương thích giữa repository với module nghiệp vụ.<br>• **Tester FE & tích hợp:** smoke test màn hình/điều hướng, kiểm tra dữ liệu hiển thị khớp database, test các luồng Admin/Customer trên emulator hoặc máy thật; ghi nhận và tái kiểm tra lỗi sau sửa.<br>• Cung cấp hợp đồng tích hợp (model, tên hàm, tham số/trả về) cho các module; hỗ trợ xử lý xung đột schema/API giữa các nhánh.<br>• Tích hợp các module, chạy build/kiểm tra hồi quy, quản lý nhánh/PR và cập nhật hướng dẫn chạy dự án. |
| **2** | **Lê Duy Anh** | **Auth & Session Management** | • Xây dựng UI/UX màn hình Đăng nhập (Login), Đăng ký (Register), Đăng xuất (Logout).<br>• Xử lý xác thực tài khoản cho 2 vai trò: `Admin` và `Customer`.<br>• Lưu trữ thông tin tài khoản đăng nhập và phiên làm việc bằng `SharedPreferences`.<br>• Điều hướng phân quyền (Role-based Navigation / Routing) chuyển đúng màn hình tương ứng với từng vai trò. |
| **3** | **Phan Nguyễn Việt Dũng** | **Admin: Fruit & Expiry Management** | • Xây dựng UI/UX giao diện Admin: Danh sách trái cây, form Thêm/Sửa/Xóa (CRUD).<br>• Triển khai các tính năng CRUD trái cây: Tên, danh mục, giá, số lượng, hạn sử dụng, mô tả.<br>• Chức năng tìm kiếm và lọc danh sách trái cây cho Admin.<br>• **Quản lý hạn sử dụng (Expiry Date Management)**: Hiển thị trực quan hạn sử dụng, tự động đánh dấu và cảnh báo trái cây đã hết hạn (badge cảnh báo/màu sắc nổi bật). |
| **4** | **Vũ Hoàng Anh** | **Customer: Browse & Place Order** | • Xây dựng UI/UX cho Khách hàng: Màn hình danh sách trái cây còn hạn, xem chi tiết trái cây.<br>• Chức năng tìm kiếm, lọc trái cây theo danh mục, kiểm tra thông tin và hạn sử dụng.<br>• **Nghiệp vụ chặn mua hàng hết hạn**: Khóa nút đặt hàng / không cho phép khách hàng đặt các mặt hàng đã quá hạn sử dụng hoặc hết số lượng (`quantity = 0`).<br>• Chức năng chọn mua, chọn số lượng và tiến hành Đặt hàng (`Place Order`). |
| **5** | **Trương Gia Bảo** | **Order Management & Customer Profile** | • Giao diện & chức năng Xem lịch sử đơn hàng cho Khách hàng (`Customer Order History`).<br>• Giao diện & chức năng Quản lý tất cả đơn hàng cho Admin: Xem danh sách, cập nhật trạng thái đơn hàng (`Pending` ➔ `Confirmed` ➔ `Completed`).<br>• Màn hình thông tin cá nhân khách hàng (`Customer Profile` liên kết `Account`).<br>• Thống kê cơ bản (tổng số đơn, trạng thái đơn) hỗ trợ Admin theo dõi đơn hàng nhanh chóng. |

---

## 🛠️ Công nghệ sử dụng

Phần **Admin: Fruit & Expiry Management** của Phan Nguyễn Việt Dũng được triển khai tại `app/src/main/java/com/cie/bth2/admin/fruit`. Xem [hướng dẫn chạy và tích hợp module](ADMIN_FRUIT.md) để ghép với database và đăng nhập của nhóm.

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

## 🤝 Handoff / hợp đồng tích hợp

### Phần lõi do Nguyễn Trung Nam cung cấp

Package dùng chung: `com.cie.bth2.data`.

- `DatabaseHelper(context)`: khởi tạo SQLite và seed dữ liệu lần đầu. Hằng số tên bảng/cột, role và trạng thái nằm trong `DatabaseHelper.Companion`.
- `AccountRepository(databaseHelper)`: `authenticate(username, password): Account?`, `createAccount(username, password, role): Long`.
- `FruitRepository(databaseHelper)`: `getAll()`, `getById(id)`, `search(query, category?)`, `insert(fruit)`, `update(fruit)`, `delete(id)`.
- `CustomerRepository(databaseHelper)`: `getAll()`, `getById(id)`, `getByAccountId(accountId)`, `insert(fullName, phone, email, accountId)`, `update(customer)`, `delete(id)`.
- `OrderRepository(databaseHelper)`: `getAll()`, `getById(id)`, `getForCustomer(customerId)`, `create(customerId, totalAmount, orderDate?)`, `placeOrder(customerId, items, orderDate?)`, `updateStatus(orderId, status)`. Luồng `placeOrder` kiểm tra customer, hạn dùng và tồn kho, trừ kho và tạo đơn trong một transaction; tổng tiền được tính từ giá hiện có trong database.
- Ngày hết hạn của Fruit dùng `yyyy-MM-dd`; ngày đặt hàng dùng `yyyy-MM-dd HH:mm:ss`. Trạng thái và role dùng đúng hằng số trong `DatabaseHelper`.

Khởi tạo dùng chung trong Activity/Application:

```kotlin
val database = DatabaseHelper(applicationContext)
val fruits = FruitRepository(database)
val orders = OrderRepository(database)
```

Ví dụ đặt một đơn gồm nhiều mặt hàng:

```kotlin
val orderId = orders.placeOrder(
    customerId = customerId,
    items = listOf(
        OrderRepository.OrderItem(fruitId = 1, quantity = 2),
        OrderRepository.OrderItem(fruitId = 3, quantity = 1)
    )
)
```

### Handoff theo thành viên

| Thành viên | Nhánh | Handoff cần bám theo |
|---|---|---|
| **Lê Duy Anh** | `feature/auth-sharedpref` | Dùng `AccountRepository.authenticate`; lưu `id`, `username`, `role` vào SharedPreferences; điều hướng theo `Admin`/`Customer`. Đăng ký tạo account với role `Customer`, sau đó tạo profile qua `CustomerRepository.insert`. Không lưu password vào session. |
| **Phan Nguyễn Việt Dũng** | `feature/admin-fruit-crud` | Dùng `FruitRepository` cho danh sách, tìm kiếm/lọc và CRUD. Giữ ngày hạn theo `yyyy-MM-dd`; hiển thị badge hết hạn dựa trên ngày hiện tại; validate giá/tồn kho không âm. |
| **Vũ Hoàng Anh** | `feature/customer-shopping` | Dùng `FruitRepository.getAll/search` để duyệt; chỉ cho chọn mặt hàng còn hạn và còn hàng. Gọi `OrderRepository.placeOrder` khi xác nhận mua; API sẽ kiểm tra lại hạn và tồn kho, tính tổng tiền, trừ kho và tạo đơn nguyên tử. Hiển thị lỗi nghiệp vụ nếu API từ chối đơn. |
| **Trương Gia Bảo** | `feature/order-management` | Dùng `OrderRepository.getForCustomer` cho lịch sử, `getAll` cho Admin, `updateStatus` cho luồng trạng thái. Dùng `CustomerRepository` để hiển thị hồ sơ; thống kê có thể tổng hợp từ danh sách đơn theo trạng thái. |
| **Nguyễn Trung Nam** | `feature/database-core` | Duy trì schema/repository, nghiệp vụ đặt hàng transaction-safe, migration/API chung; tích hợp các nhánh và xử lý xung đột. |

### Quy tắc phối hợp

1. Mỗi người làm trên nhánh tính năng riêng, không sửa schema chung nếu chưa trao đổi với Nam.
2. Khi cần thêm cột/bảng hoặc đổi hàm repository, mở PR kèm mô tả API/schema trước để các nhánh không lệch hợp đồng.
3. PR ghi rõ màn hình/nghiệp vụ hoàn thành, cách chạy và kết quả build/kiểm tra; không commit `local.properties`, `.gradle/` hoặc file cấu hình cá nhân.
4. Merge theo thứ tự nền tảng dữ liệu → auth và màn hình tính năng → tích hợp cuối; xử lý xung đột qua PR, không force-push lên `main`.

---

## 🔐 Module Auth & Session — Lê Duy Anh

Đăng nhập, đăng ký Customer, đăng xuất, lưu phiên SharedPreferences và điều hướng Admin/Customer nằm trong package `com.cie.bth2.auth`. Xem [hướng dẫn tích hợp và kiểm thử](docs/AUTH.md).

Tài khoản mẫu: `admin / admin123` và `customer / customer123`.
### Checklist kiểm thử của nhóm trưởng

- **BE / database:** schema và seed data; CRUD repository; khóa ngoại/ràng buộc; tạo đơn thành công; chặn mặt hàng hết hạn/hết kho; rollback nếu một mặt hàng trong đơn lỗi; chuyển trạng thái đơn đúng thứ tự.
- **FE:** mở màn hình, điều hướng và hiển thị trạng thái rỗng/lỗi/dữ liệu; kiểm tra luồng Admin và Customer trên emulator/máy thật.
- **Tích hợp:** đối chiếu dữ liệu UI với database sau thao tác; build lại sau khi merge; chạy lại các luồng bị ảnh hưởng và ghi rõ thiết bị/kết quả trong PR.
- Instrumented tests nằm tại `app/src/androidTest`; chạy bằng `./gradlew connectedDebugAndroidTest` khi đã kết nối emulator hoặc thiết bị Android.


## Chạy ứng dụng sau tích hợp

- Mở ứng dụng → Login; Admin (`admin/admin123`) mở quản lý trái cây và đơn hàng.
- Customer (`customer/customer123`) mở mua sắm, hồ sơ và lịch sử đơn; Account.id được tra sang Customer.id qua database chung.
- Tất cả module nghiệp vụ dùng `fruit_shop.db`.
- Handoff: [Auth](docs/AUTH.md), [Admin](ADMIN_FRUIT.md), [Customer shopping](HANDOFF_customer-shopping.md). Order/Profile dùng repository core và được nối từ trang theo vai trò.
- Build và unit test: `./gradlew :app:assembleDebug :app:testDebugUnitTest`.
- Kiểm thử Android trên emulator/máy thật: `./gradlew :app:connectedDebugAndroidTest`.

Kết quả kiểm tra trước khi gỡ màn demo CSDL (23/10/2026): build debug thành công; 16 unit test đạt; 29/29 instrumented test đạt trên Pixel 9 Pro emulator, Android 15. Sau khi gỡ màn demo và 2 test dành riêng cho demo, còn 27 instrumented test; chưa chạy lại bộ test cho thay đổi này. Các test còn lại bao phủ database/repository, quy tắc nghiệp vụ và auth/session/navigation; chưa thay thế kiểm tra thủ công toàn bộ giao diện.


### Rà soát phần core và tester của Nam

- Repository kiểm tra ngày có thật, giá/tổng tiền hữu hạn và số lượng không tràn khi gộp mặt hàng.
- Kiểm tra hạn dùng dựa trên ngày hiện tại của thiết bị; truyền ngày đơn quá khứ không cho phép mua hàng đã hết hạn.
- Test gây lỗi ghi Order sau khi đã trừ kho xác nhận toàn bộ giao dịch rollback.
- Test tích hợp repository: Admin thêm hàng → Customer mua → tồn kho giảm → Admin xác nhận/hoàn thành → lịch sử và doanh thu khớp.
- Test giao diện mở mua sắm, hồ sơ, lịch sử, kho Admin và quản lý đơn từ trang theo vai trò. Chưa kiểm thử toàn bộ thao tác nghiệp vụ bằng UI tự động.
- Schema hiện là phiên bản 1, chưa có chuyển đổi schema giữa các phiên bản. Khi thay đổi schema cần bổ sung migration bảo toàn dữ liệu trong `onUpgrade` và test nâng cấp tương ứng.
