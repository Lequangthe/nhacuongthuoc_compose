### Sửa lỗi giao diện Cài đặt – Nội dung bị che bởi thanh điều hướng
- File thay đổi: `app/src/main/res/layout/app_settings.xml`
- Chi tiết: Bao bọc `ConstraintLayout` bằng `ScrollView` và thêm `paddingBottom` để tránh bị thanh điều hướng che khuất nội dung bên dưới "Định dạng 24 giờ".

### Sửa lỗi build – Thiếu tài nguyên chuỗi `past_date_dialog_message`
- File thay đổi: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml`, `app/src/main/res/values-fr/strings.xml`
- Chi tiết: Thêm chuỗi `past_date_dialog_message` còn thiếu vào các file tài nguyên ngôn ngữ để khắc phục lỗi compile.
