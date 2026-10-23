# HANDOFF — Module 4: Customer Browse & Place Order

## Trạng thái sau tích hợp vào main

Đã bỏ FruitShopDbHelper placeholder. CustomerShopRepository dùng DatabaseHelper, FruitRepository và OrderRepository.placeOrder chung. CustomerSession lấy Account từ auth_session rồi tra Customer.id; không còn fallback id=1. Login là launcher, mở mua sắm/lịch sử/hồ sơ từ trang Customer. Tài khoản mẫu hiện tại: customer/customer123. Các mục placeholder/session bên dưới lưu lại handoff gốc trước tích hợp.


- **Owner**: Vũ Hoàng Anh (VHA)
- **Branch**: `feature/customer-shopping`
- **Trạng thái**: DONE — `assembleDebug` BUILD SUCCESSFUL, `OrderRulesTest` 8/8 pass. Chờ Lead review + merge.

## Đã làm (đối chiếu README mục 4)

| Yêu cầu README | Nơi triển khai |
|---|---|
| Danh sách trái cây **còn hạn** | `customer/CustomerFruitListActivity` — mặc định chỉ hiện hàng còn hạn; checkbox "Hiển thị cả hàng đã hết hạn" để xem cả hàng hết hạn (có badge đỏ) |
| Tìm kiếm theo tên, lọc theo danh mục | Ô tìm kiếm + ChipGroup danh mục (lấy động từ DB) |
| Xem chi tiết, kiểm tra hạn sử dụng | `customer/CustomerFruitDetailActivity` |
| **Chặn mua hàng hết hạn / hết hàng (`quantity = 0`)** | Nút Đặt hàng + nút +/− bị khoá, hiện banner lý do. Rule nằm ở `customer/OrderRules` |
| Chọn số lượng + Đặt hàng | Stepper giới hạn trong `1..quantity`; `CustomerShopRepository.placeOrder()` |

Chặn mua có **2 lớp**: UI khoá nút, và `placeOrder()` kiểm tra lại hạn + tồn kho **trong cùng transaction** trước khi ghi (hàng có thể hết hạn/hết kho giữa lúc mở màn hình và lúc bấm).

`placeOrder()` làm trong 1 transaction: insert `Order` (`status = "Pending"`, `orderDate = yyyy-MM-dd HH:mm:ss`, `totalAmount = price × qty`) + trừ `Fruit.quantity`.

## File

```
app/src/main/java/com/cie/bth2/
  data/FruitShopDbHelper.kt        ← PLACEHOLDER DB (xem mục "Cho Nam")
  data/Fruit.kt                    ← model Fruit
  customer/OrderRules.kt           ← rule hết hạn / hết hàng / vượt kho (hàm thuần, có test)
  customer/CustomerShopRepository.kt
  customer/CustomerSession.kt      ← đọc customer_id từ SharedPreferences
  customer/CustomerFruitListActivity.kt
  customer/CustomerFruitAdapter.kt
  customer/CustomerFruitDetailActivity.kt
  customer/Format.kt               ← formatVnd()
app/src/main/res/layout/{activity_customer_fruit_list,activity_customer_fruit_detail,item_customer_fruit}.xml
app/src/main/res/values/customer_shopping.xml   ← mọi string/color/id prefix `cs_`, không sửa strings.xml chung
app/src/test/java/com/cie/bth2/customer/OrderRulesTest.kt
```

## Contract với các module khác (QUAN TRỌNG)

### Cho Nguyễn Trung Nam — Database core
- Chưa có DB core lúc làm, nên có `data/FruitShopDbHelper` (SQLiteOpenHelper, file `fruit_shop.db`) **đúng schema README**: `Account`, `Fruit`, `Customer`, `"Order"` (tên bảng `Order` là từ khoá SQL → phải quote).
- **Định dạng `Fruit.expiryDate` = chuỗi `yyyy-MM-dd`.** Rule hết hạn so sánh chuỗi với ngày hôm nay; ngày hết hạn = vẫn còn dùng được trong ngày đó. Nếu DB core dùng định dạng khác (epoch/Long), báo VHA hoặc sửa `OrderRules.isExpired` + query `expiryDate >= ?` trong `CustomerShopRepository.searchFruits`.
- Khi merge DB core: xoá `FruitShopDbHelper` của nhánh này, đổi `CustomerShopRepository` sang dùng helper/DAO chung. Chỉ có **1 file** phụ thuộc DB: `CustomerShopRepository.kt`. Nếu đổi sang Room: giữ nguyên 4 hàm public (`searchFruits`, `getCategories`, `getFruit`, `placeOrder`) là UI không phải sửa.
- Seed data trong placeholder có cố ý: 1 hàng hết hàng (Táo Fuji), 2 hàng hết hạn (Cam Úc, Nho đen) để test chặn mua. Account seed: `admin/admin123` (Admin), `user/user123` (Customer, customer id = 1).

### Cho Lê Duy Anh — Auth & Session
- Sau khi login role `Customer`, ghi:
  `getSharedPreferences("fruit_shop_session", MODE_PRIVATE).edit().putLong("customer_id", <Customer.id>).apply()`
  (hằng số ở `CustomerSession.PREFS_NAME` / `KEY_CUSTOMER_ID`). **Là `Customer.id`, không phải `Account.id`.**
- Chưa có key thì module này fallback về customer id = 1 (khách seed) — **xoá fallback sau khi Auth merge**.
- Điều hướng Customer sau login: `startActivity(Intent(ctx, CustomerFruitListActivity::class.java))`.
- `CustomerFruitListActivity` đang là **LAUNCHER tạm** trong `AndroidManifest.xml` để chạy độc lập. Khi Auth merge: chuyển intent-filter LAUNCHER sang màn Login, xoá ở activity này.
- Nút Logout: chưa đặt trên màn Customer (thuộc phạm vi Auth). Nếu cần, thêm menu vào `CustomerFruitListActivity`.

### Cho Trương Gia Bảo — Order Management
- Đơn tạo từ module này: `"Order"(customerId, orderDate 'yyyy-MM-dd HH:mm:ss', totalAmount REAL, status 'Pending')`.
- Bảng Order **không có bảng chi tiết (order items)** theo README → 1 đơn = 1 loại trái cây; không lưu fruitId trong đơn. Nếu cần hiển thị đơn mua gì → cần thêm bảng `OrderItem` (phải thống nhất với Nam).
- Tồn kho trừ ngay lúc đặt (`Pending`). Nếu Admin huỷ đơn sau này thì phải cộng lại kho — hiện README không có trạng thái huỷ.

### Cho Phan Nguyễn Việt Dũng — Admin Fruit CRUD
- Dùng chung rule hết hạn: `OrderRules.isExpired(fruit)` để badge Admin và chặn mua phía Customer khớp nhau.
- Admin nhập `expiryDate` phải lưu đúng `yyyy-MM-dd`.

## Cách kiểm thử

```bash
./gradlew assembleDebug testDebugUnitTest
```
- **Dùng JDK 17/21.** JBR đi kèm Android Studio bản mới là JDK 25 → Gradle 8.13 fail với message rỗng. CLI: `JAVA_HOME=<jdk21> ./gradlew ...`; Android Studio: Settings → Gradle → Gradle JDK = 21.
- Lần build đầu tải Gradle 8.13 + AGP 8.13.2 (~9 phút); sau đó build offline nhanh.
- `OrderRulesTest`: hết hạn hôm qua / còn hạn đúng ngày hết hạn / chặn hết hạn / chặn hết hàng / chặn số lượng ≤ 0 / chặn vượt kho / cho phép mua đúng bằng tồn kho.
- Thủ công: mở app → "Cam Úc" không hiện (hết hạn) → tick "Hiển thị cả hàng đã hết hạn" → mở Cam Úc → nút Đặt hàng bị khoá. Mở "Táo Fuji" → khoá (hết hàng). Mở "Táo Envy" → tăng số lượng → Đặt hàng → dialog mã đơn, quay lại thấy tồn kho giảm.

## Giới hạn đã biết
- Đặt hàng 1 loại/đơn (không có giỏ hàng nhiều sản phẩm) vì schema Order không có order items.
- Truy vấn DB chạy trên main thread (dữ liệu nhỏ, bài thực hành). Nếu chuyển MVVM chuẩn thì đưa vào ViewModel + coroutine.
- `minSdk 24` → không dùng `java.time`, dùng `SimpleDateFormat`.
