# راهنمای جامع تست API و پنل ادمین

این راهنما به شما کمک می‌کند تا تمام بخش‌های API و پنل ادمین پروژه خود را تست کنید.

## پیش‌نیازها
- مرورگر وب (Chrome, Firefox, etc.)
- ابزار تست API مانند [Postman](https://www.postman.com/downloads/) (پیشنهادی) یا cURL.
- سرور در حال اجرا باشد (پورت پیش‌فرض: 8080).

---

## بخش ۱: تست پنل ادمین (Admin Panel)

پنل ادمین شما شامل فایل‌های استاتیک HTML است که با جاوااسکریپت به API متصل می‌شوند.

### ۱. دسترسی به پنل
1. مرورگر خود را باز کنید.
2. به آدرس `http://localhost:8080/admin/index.html` بروید (یا صفحه لاگین اگر جداگانه است).
   * *نکته: اگر صفحه لاگین ندارید، باید ابتدا از طریق Postman لاگین کنید و توکن را در LocalStorage مرورگر ست کنید یا یک صفحه لاگین ساده بسازید.*

### ۲. تست لاگین (دستی)
اگر صفحه لاگین ندارید، می‌توانید به صورت دستی توکن را در مرورگر ست کنید:
1. در مرورگر، کلید `F12` را بزنید تا Developer Tools باز شود.
2. به تب **Console** بروید.
3. دستور زیر را وارد کنید (جای `YOUR_TOKEN` توکن واقعی را که از مرحله بعد می‌گیرید قرار دهید):
   ```javascript
   localStorage.setItem('adminToken', 'YOUR_TOKEN');
   localStorage.setItem('adminUser', JSON.stringify({ username: 'admin' }));
   window.location.href = 'products.html'; // هدایت به صفحه محصولات
   ```

### ۳. تست صفحات
- **محصولات**: `http://localhost:8080/admin/products.html`
  - لیست محصولات باید نمایش داده شود.
  - دکمه "افزودن محصول" را تست کنید.
  - دکمه‌های ویرایش و حذف را تست کنید.
- **رنگ‌ها**: `http://localhost:8080/admin/colors.html`
  - افزودن، ویرایش و حذف رنگ‌ها را تست کنید.
- **سایزها**: `http://localhost:8080/admin/sizes.html` (اگر وجود دارد)
- **دسته‌بندی‌ها**: `http://localhost:8080/admin/categories.html` (اگر وجود دارد)

---

## بخش ۲: تست API با Postman

برای تست دقیق‌تر و دیباگ، از Postman استفاده کنید.

### ۱. احراز هویت (Authentication)
قبل از هر کاری، باید لاگین کنید تا توکن دریافت کنید.

**Login / Register**
- **URL**: `http://localhost:8080/api/user/login`
- **Method**: `POST`
- **Body (JSON)**:
  ```json
  {
    "username": "your_username",
    "password": "your_password"
  }
  ```
- **Response**: توکن (`token`) را از پاسخ کپی کنید.

**نکته**: برای درخواست‌های بعدی که نیاز به لاگین دارند (مثل پنل ادمین)، در Postman به تب **Auth** بروید، نوع را **Bearer Token** انتخاب کنید و توکن را آنجا پیست کنید.

### ۲. مدیریت محصولات (Admin Product API)
*نیاز به توکن ادمین دارد.*

**ایجاد محصول (Create Product)**
- **URL**: `http://localhost:8080/api/admin/product`
- **Method**: `POST`
- **Body (JSON)**:
  ```json
  {
    "title": "گوشی موبایل سامسونگ",
    "price": 25000000,
    "description": "توضیحات محصول...",
    "category": { "id": 1 },
    "colors": [ { "id": 1 } ],
    "sizes": [ { "id": 1 } ]
  }
  ```

**آپدیت محصول (Update Product)**
- **URL**: `http://localhost:8080/api/admin/product/{id}`
- **Method**: `PUT`
- **Body**: مشابه Create.

**حذف محصول (Delete Product)**
- **URL**: `http://localhost:8080/api/admin/product/{id}`
- **Method**: `DELETE`

**آپلود عکس محصول (Upload Image)**
- **URL**: `http://localhost:8080/api/admin/product/{id}/images`
- **Method**: `POST`
- **Body**: form-data
  - key: `file`, type: `File`, value: (انتخاب فایل عکس)
  - key: `isPrimary`, value: `true`

### ۳. محصولات عمومی (Public Product API)
*نیاز به توکن ندارد.*

**لیست محصولات (Get All)**
- **URL**: `http://localhost:8080/api/product?pageIndex=0&pageSize=10`
- **Method**: `GET`

**محصولات جدید (New Products)**
- **URL**: `http://localhost:8080/api/product/new`
- **Method**: `GET`

**محصولات محبوب (Popular Products)**
- **URL**: `http://localhost:8080/api/product/popular`
- **Method**: `GET`

**جزئیات محصول (Get By ID)**
- **URL**: `http://localhost:8080/api/product/{id}`
- **Method**: `GET`

### ۴. مدیریت رنگ‌ها (Admin Color API)
*نیاز به توکن ادمین دارد.*

**لیست / ایجاد / ویرایش / حذف**
- **Base URL**: `http://localhost:8080/api/admin/color`
- **Methods**: `POST` (Create), `PUT /{id}` (Update), `DELETE /{id}` (Delete)
- **Body (Create/Update)**:
  ```json
  {
    "title": "قرمز",
    "hex": "#FF0000"
  }
  ```

### ۵. مدیریت سایزها (Admin Size API)
*نیاز به توکن ادمین دارد.*

**لیست / ایجاد / ویرایش / حذف**
- **Base URL**: `http://localhost:8080/api/admin/size`
- **Methods**: `POST`, `PUT /{id}`, `DELETE /{id}`
- **Body**:
  ```json
  {
    "title": "XL"
  }
  ```

### ۶. مدیریت دسته‌بندی‌ها (Admin Category API)
*نیاز به توکن ادمین دارد.*

**لیست / ایجاد / ویرایش / حذف**
- **Base URL**: `http://localhost:8080/api/admin/category`
- **Methods**: `POST`, `PUT /{id}`, `DELETE /{id}`
- **Body**:
  ```json
  {
    "title": "الکترونیک"
  }
  ```

---

## عیب‌یابی (Troubleshooting)

1. **خطای 401 یا 403**:
   - توکن شما منقضی شده یا ارسال نشده است. دوباره لاگین کنید و توکن جدید بگیرید.
   - مطمئن شوید در هدر درخواست `Authorization: Bearer YOUR_TOKEN` وجود دارد.

2. **خطای CORS**:
   - اگر از مرورگر تست می‌کنید و فرانت و بک‌اند روی پورت‌های مختلف هستند، باید `@CrossOrigin` را در کنترلرها اضافه کنید یا تنظیمات Global CORS را در اسپرینگ بوت انجام دهید. (چون هر دو روی 8080 هستند معمولا مشکلی نیست).

3. **خطای 500**:
   - لاگ‌های کنسول سرور (IntelliJ IDEA) را چک کنید تا علت دقیق خطا را ببینید.

