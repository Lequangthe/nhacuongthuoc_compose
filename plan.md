### Sửa lỗi giao diện Cài đặt – Nội dung bị che bởi thanh điều hướng
- File thay đổi: `app/src/main/res/layout/app_settings.xml`
- Chi tiết: Bao bọc `ConstraintLayout` bằng `ScrollView` và thêm `paddingBottom` để tránh bị thanh điều hướng che khuất nội dung bên dưới "Định dạng 24 giờ".

### Sửa lỗi build – Thiếu tài nguyên chuỗi `past_date_dialog_message`
- File thay đổi: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml`, `app/src/main/res/values-fr/strings.xml`
- Chi tiết: Thêm chuỗi `past_date_dialog_message` còn thiếu vào các file tài nguyên ngôn ngữ để khắc phục lỗi compile.

### Fix core alarm scheduling logic – Vòng lặp vô hạn + infinite re-fire
- File thay đổi: `app/src/main/java/com/quangthe/nhacnho_uongthuoc/Pill.java`, `MainActivity.java`, `MainRecyclerViewAdapter.java`
- Chi tiết: `setAlarm()` giờ tính động `convertTimeToCurrentDateTimeInMillis()` thay vì dùng `alarmReminderTimes[]` cũ. Sửa `deleteActiveNotifications()` cancel đúng index. Reschedule alarm sau khi take/dismiss.

### Fix test dose – Chip xanh vĩnh viễn + thiếu feedback giả lập
- File thay đổi: `app/src/main/java/com/quangthe/nhacnho_uongthuoc/MainActivity.java`, `MainRecyclerViewAdapter.java`
- Chi tiết: Context menu "Test Dose" giờ dùng `sendPillNotificationONLY_FOR_TEST()` thay vì `sendPillNotification()` (tránh mở `PillAlarmDisplay` và gọi `takePill()`). Long-press chip giờ tô xanh giả lập 2 giây rồi tự hồi phục.

### Xin quyền POST_NOTIFICATIONS runtime khi test lần đầu
- File thay đổi: `app/src/main/java/com/quangthe/nhacnho_uongthuoc/MainActivity.java`, `MainRecyclerViewAdapter.java`
- Chi tiết: Thêm `checkAndRequestNotificationPermission()` và `sendTestNotificationWithPermissionCheck()`. Khi test lần đầu trên Android 13+ sẽ hiện dialog xin quyền. Nếu chưa cấp, báo user bấm lại test sau khi cấp quyền.

### Xoá notification đúng liều khi bấm chip "Đã uống"
- File thay đổi: `app/src/main/java/com/quangthe/nhacnho_uongthuoc/Pill.java`, `MainRecyclerViewAdapter.java`, `PillAlarmDisplay.java`
- Chi tiết: `deleteActiveNotifications()` giờ nhận `doseIndex` và chỉ huỷ đúng notification của liều đó (dùng `getAlarmRequestCodes()[doseIndex]`). MainRecyclerViewAdapter gọi `deleteActiveNotifications(context, doseIndex)` sau `takePill()`. PillAlarmDisplay truyền `doseIndex` tương ứng.

### GitHub Actions workflow auto build & release APK
- File thay đổi: `.github/workflows/release-apk.yml`, `app/build.gradle`
- Chi tiết: Thêm workflow build APK khi push tag `v*`. Signing config dùng biến môi trường (không hardcode password). Keystore giải mã từ secret base64.
