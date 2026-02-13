# MiniChat — راهنمای کامل استقرار روی سرور (Deployment Guide)

> **سرور هدف:** VPS Debian 13 (Bookworm)
> **IP سرور:** `46.249.100.239`
> **پورت‌ها:** 80 (HTTP), 443 (HTTPS), 8080 (API internal)

---

## فهرست مطالب

1. [پیش‌نیازها](#1-پیشنیازها)
2. [اتصال اولیه به سرور — SSH](#2-اتصال-اولیه-به-سرور)
3. [امنیت اولیه سرور](#3-امنیت-اولیه-سرور)
4. [نصب Docker و Docker Compose](#4-نصب-docker-و-docker-compose)
5. [ساخت ریپازیتوری GitHub و پوش کد](#5-github-و-پوش-کد)
6. [کلون روی سرور و تنظیم Environment](#6-کلون-و-تنظیم-environment)
7. [بالا آوردن سرویس‌ها با Docker Compose](#7-بالا-آوردن-سرویسها)
8. [نصب و تنظیم Nginx به عنوان Reverse Proxy](#8-nginx-reverse-proxy)
9. [تنظیم SSL/TLS با Let's Encrypt](#9-ssl-tls)
10. [مانیتورینگ و لاگ](#10-مانیتورینگ)
11. [بکاپ و بازیابی](#11-بکاپ)
12. [بروزرسانی و Rollback](#12-بروزرسانی)
13. [چک‌لیست نهایی](#13-چکلیست-نهایی)

---

## 1. پیش‌نیازها

### روی ماشین لوکال شما:
- Git نصب شده
- حساب GitHub
- SSH key ساخته شده (`ssh-keygen -t ed25519`)

### روی سرور:
- Debian 13 (Bookworm) نصب شده
- دسترسی root یا sudo
- IP ثابت: `46.249.100.239`
- دامنه (اختیاری ولی توصیه شده برای SSL)

---

## 2. اتصال اولیه به سرور

```bash
# از ماشین لوکال
ssh root@172.86.95.177
```

### ساخت کاربر غیر root



```bash
# روی سرور
adduser minichat


usermod -aG sudo minichat

# کپی SSH key
mkdir -p /home/minichat/.ssh
cp ~/.ssh/authorized_keys /home/minichat/.ssh/
chown -R minichat:minichat /home/minichat/.ssh
chmod 700 /home/minichat/.ssh
chmod 600 /home/minichat/.ssh/authorized_keys
```

---

## 3. امنیت اولیه سرور

### 3.1 بروزرسانی سیستم

```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y ufw fail2ban curl wget gnupg2 apt-transport-https ca-certificates
```

### 3.2 تنظیم فایروال (UFW)

```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
sudo ufw allow 22/tcp      # SSH
sudo ufw allow 80/tcp      # HTTP
sudo ufw allow 443/tcp     # HTTPS
sudo ufw enable
sudo ufw status verbose
```

> ⚠️ **هشدار:** پورت 8080 را باز نکنید! API فقط از طریق Nginx (پورت 443) در دسترس است.

### 3.3 امن‌سازی SSH

```bash
sudo nano /etc/ssh/sshd_config
```

تنظیمات زیر را اعمال کنید:

```
Port 1586                          # تغییر پورت پیش‌فرض
PermitRootLogin no                 # غیرفعال کردن لاگین root
PasswordAuthentication no          # فقط SSH key
MaxAuthTries 3
LoginGraceTime 30
AllowUsers minichat               # فقط کاربر minichat
ClientAliveInterval 300
ClientAliveCountMax 2
```

```bash
# اعمال تغییرات
sudo systemctl restart sshd

# بروزرسانی فایروال
sudo ufw allow 1586/tcp
sudo ufw delete allow 22/tcp
```

> ⚠️ **مهم:** قبل از بستن پورت 22، مطمئن شوید که با پورت 1586 می‌توانید وصل شوید!

### 3.4 تنظیم Fail2Ban

```bash
sudo nano /etc/fail2ban/jail.local
```

```ini
[DEFAULT]
bantime  = 3600
findtime = 600
maxretry = 3

[sshd]
enabled = true
port    = 1586
logpath = /var/log/auth.log
maxretry = 3
bantime  = 86400
```

```bash
sudo systemctl enable fail2ban
sudo systemctl start fail2ban
```

---

## 4. نصب Docker و Docker Compose

```bash
# حذف نسخه‌های قدیمی
sudo apt remove -y docker docker-engine docker.io containerd runc 2>/dev/null

# اضافه کردن ریپازیتوری Docker
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/debian/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/debian \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# نصب Docker
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# اضافه کردن کاربر به گروه docker
sudo usermod -aG docker minichat

# تست
docker --version
docker compose version
```

> بعد از `usermod` یک بار logout/login کنید.

---

## 5. GitHub و پوش کد

### 5.1 روی ماشین لوکال — ساخت SSH key برای GitHub

```bash
ssh-keygen -t ed25519 -C "your-email@example.com"
cat ~/.ssh/id_ed25519.pub
# خروجی را در GitHub > Settings > SSH Keys اضافه کنید
```

### 5.2 ساخت ریپازیتوری و Push

```bash
cd e:\Learn\programming\ponisha\MiniChat

# فقط اگر هنوز git init نشده:
git init

# ساخت .gitignore ریشه (اگر نیست)
# مطمئن شوید .env فایل‌ها ignore شده‌اند

git remote add origin git@github.com:YOUR_USERNAME/MiniChat.git
git add .
git commit -m "chore: production-ready with security hardening"
git branch -M main
git push -u origin main
```

### 5.3 فایل‌هایی که نباید Push شوند

اطمینان حاصل کنید که `.gitignore` شامل این موارد هست:

```
.env
*.pem
*.key
*.jks
local.properties
```

---

## 6. کلون و تنظیم Environment

### 6.1 کلون روی سرور

```bash
# روی سرور با کاربر minichat
ssh minichat@46.249.100.239 -p 1586

mkdir -p ~/apps
cd ~/apps
git clone git@github.com:YOUR_USERNAME/MiniChat.git
cd MiniChat/SpringBoot
```

### 6.2 ساخت فایل .env

```bash
cp .env.example .env
nano .env
```

**مقادیر واقعی** را جایگزین کنید:

```bash
# رمز عبور قوی دیتابیس بسازید
openssl rand -base64 24

# سکرت JWT بسازید
openssl rand -base64 64

# رمز عبور Redis بسازید
openssl rand -base64 16
```

> ⚠️ **هرگز** فایل `.env` را به Git push نکنید!

### 6.3 تنظیم مجوز فایل .env

```bash
chmod 600 .env
```

---

## 7. بالا آوردن سرویس‌ها

```bash
cd ~/apps/MiniChat/SpringBoot

# بیلد و اجرا (اولین بار زمان‌بر است)
docker compose --env-file .env up -d --build

# بررسی وضعیت
docker compose ps

# بررسی لاگ‌ها
docker compose logs -f api
docker compose logs -f postgres
docker compose logs -f redis
```

### بررسی سلامت سرویس‌ها

```bash
# تست اینکه API پاسخ می‌دهد
curl http://localhost:8080/auth/health

# بررسی health check ها
docker inspect --format='{{.State.Health.Status}}' minichat-api
docker inspect --format='{{.State.Health.Status}}' minichat-postgres
docker inspect --format='{{.State.Health.Status}}' minichat-redis
```

---

## 8. Nginx Reverse Proxy

### نصب Nginx

```bash
sudo apt install -y nginx
sudo systemctl enable nginx
```

### تنظیم Virtual Host

```bash
sudo nano /etc/nginx/sites-available/minichat
```

```nginx
# Rate limiting
limit_req_zone $binary_remote_addr zone=api_limit:10m rate=30r/s;
limit_req_zone $binary_remote_addr zone=auth_limit:10m rate=10r/s;

# Upstream
upstream minichat_api {
    server 127.0.0.1:8080;
    keepalive 32;
}

server {
    listen 80;
    server_name 46.249.100.239;  # یا دامنه شما

    # Redirect HTTP to HTTPS (بعد از فعال‌سازی SSL)
    # return 301 https://$server_name$request_uri;

    # تا زمانی که SSL ندارید، از این تنظیمات استفاده کنید:
    location / {
        proxy_pass http://minichat_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # Rate limiting
        limit_req zone=api_limit burst=20 nodelay;
    }

    # Auth endpoints — rate limit سخت‌تر
    location /auth/ {
        proxy_pass http://minichat_api;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        limit_req zone=auth_limit burst=5 nodelay;
    }

    # WebSocket support
    location /ws-native {
        proxy_pass http://minichat_api;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_read_timeout 86400;
    }

    # Security headers
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;
    # HSTS — فقط بعد از فعال‌سازی SSL فعال کنید:
    # add_header Strict-Transport-Security "max-age=31536000; includeSubDomains; preload" always;

    # فایل‌های آپلود شده
    location /api/files/ {
        proxy_pass http://minichat_api;
        proxy_set_header Host $host;
        client_max_body_size 10M;
    }

    # مسدود کردن دسترسی مستقیم به Swagger در production
    location /swagger-ui {
        deny all;
        return 404;
    }
    location /v3/api-docs {
        deny all;
        return 404;
    }
}
```

### فعال‌سازی و تست

```bash
sudo ln -s /etc/nginx/sites-available/minichat /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

---

## 9. SSL/TLS

### اگر دامنه دارید — Let's Encrypt (رایگان)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d YOUR_DOMAIN.com
sudo certbot renew --dry-run  # تست تمدید خودکار
```

### اگر فقط IP دارید — Self-Signed Certificate

```bash
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/minichat.key \
    -out /etc/nginx/ssl/minichat.crt \
    -subj "/CN=46.249.100.239"
```

سپس در فایل Nginx:

```nginx
server {
    listen 443 ssl http2;
    server_name 46.249.100.239;

    ssl_certificate /etc/nginx/ssl/minichat.crt;
    ssl_certificate_key /etc/nginx/ssl/minichat.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # ... باقی تنظیمات location مانند بالا ...
}
```

```bash
sudo nginx -t
sudo systemctl reload nginx
```

---

## 10. مانیتورینگ

### لاگ‌های Docker

```bash
# لاگ زنده API
docker compose -f ~/apps/MiniChat/SpringBoot/docker-compose.yml logs -f api

# لاگ آخرین 100 خط
docker compose -f ~/apps/MiniChat/SpringBoot/docker-compose.yml logs --tail=100 api

# لاگ‌های Nginx
sudo tail -f /var/log/nginx/access.log
sudo tail -f /var/log/nginx/error.log
```

### مانیتورینگ مصرف منابع

```bash
# مصرف RAM و CPU کانتینرها
docker stats

# دیسک
df -h
docker system df
```

### Log Rotation

```bash
sudo nano /etc/logrotate.d/minichat
```

```
/var/log/nginx/access.log /var/log/nginx/error.log {
    daily
    rotate 14
    compress
    delaycompress
    missingok
    notifempty
    postrotate
        systemctl reload nginx > /dev/null 2>&1 || true
    endscript
}
```

### هشدار دیسک (اسکریپت ساده)

```bash
sudo nano /usr/local/bin/disk-alert.sh
```

```bash
#!/bin/bash
THRESHOLD=85
USAGE=$(df / | tail -1 | awk '{print $5}' | sed 's/%//')
if [ "$USAGE" -gt "$THRESHOLD" ]; then
    echo "DISK WARNING: ${USAGE}% used on $(hostname)" | \
    mail -s "Disk Alert - MiniChat Server" your-email@example.com
fi
```

```bash
sudo chmod +x /usr/local/bin/disk-alert.sh
# اضافه به crontab (هر ساعت)
echo "0 * * * * /usr/local/bin/disk-alert.sh" | sudo crontab -
```

---

## 11. بکاپ

### بکاپ خودکار دیتابیس

```bash
sudo mkdir -p /opt/backups/postgres
sudo nano /usr/local/bin/backup-db.sh
```

```bash
#!/bin/bash
BACKUP_DIR="/opt/backups/postgres"
DATE=$(date +%Y%m%d_%H%M%S)
KEEP_DAYS=7

# بکاپ
docker exec minichat-postgres pg_dump -U postgres minichat | \
    gzip > "${BACKUP_DIR}/minichat_${DATE}.sql.gz"

# حذف بکاپ‌های قدیمی
find "${BACKUP_DIR}" -name "*.sql.gz" -mtime +${KEEP_DAYS} -delete

echo "[$(date)] Backup completed: minichat_${DATE}.sql.gz"
```

```bash
sudo chmod +x /usr/local/bin/backup-db.sh

# اجرای خودکار هر شب ساعت 3
echo "0 3 * * * /usr/local/bin/backup-db.sh >> /var/log/minichat-backup.log 2>&1" | sudo crontab -
```

### بازیابی از بکاپ

```bash
gunzip < /opt/backups/postgres/minichat_YYYYMMDD_HHMMSS.sql.gz | \
    docker exec -i minichat-postgres psql -U postgres minichat
```

---

## 12. بروزرسانی و Rollback

### بروزرسانی

```bash
cd ~/apps/MiniChat

# دریافت آخرین تغییرات
git pull origin main

# بکاپ قبل از بروزرسانی
/usr/local/bin/backup-db.sh

# بیلد مجدد و ری‌استارت
cd SpringBoot
docker compose --env-file .env up -d --build

# بررسی
docker compose ps
docker compose logs -f api
```

### Rollback

```bash
# برگشت به commit قبلی
git log --oneline -5
git checkout <COMMIT_HASH>

# بیلد مجدد
cd SpringBoot
docker compose --env-file .env up -d --build

# بازیابی دیتابیس (در صورت نیاز)
gunzip < /opt/backups/postgres/minichat_LATEST.sql.gz | \
    docker exec -i minichat-postgres psql -U postgres minichat
```

### استفاده از Tag برای Version‌ها

```bash
# ساخت تگ قبل از هر دیپلوی
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# برگشت به یک ورژن
git checkout v1.0.0
```

---

## 13. چک‌لیست نهایی

| # | مورد | وضعیت |
|---|------|--------|
| 1 | سیستم بروز شده | ☐ |
| 2 | فایروال فعال (UFW) | ☐ |
| 3 | SSH امن (key-only, پورت تغییر کرده) | ☐ |
| 4 | Fail2Ban فعال | ☐ |
| 5 | Docker نصب شده | ☐ |
| 6 | کد روی GitHub push شده | ☐ |
| 7 | `.env` فایل تنظیم شده (با مقادیر قوی) | ☐ |
| 8 | رمزها همه تغییر کرده‌اند | ☐ |
| 9 | Docker Compose بالاست | ☐ |
| 10 | Health check ها سبزند | ☐ |
| 11 | Nginx تنظیم شده | ☐ |
| 12 | SSL فعال شده | ☐ |
| 13 | Swagger در production مسدود است | ☐ |
| 14 | بکاپ خودکار فعال | ☐ |
| 15 | مانیتورینگ فعال | ☐ |

---

> 📝 **نکته:** بعد از استقرار اولیه، رمزهای پیش‌فرض (`application.properties` قدیمی) را **compromised** در نظر بگیرید و همه رمزها را عوض کنید.
