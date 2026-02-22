# MiniChat — Deployment Guide for Latest Changes

> **This guide covers deploying the WebRTC reliability fixes and Admin Panel live chat monitoring feature.**
> **Server IP:** `172.86.95.177`
> **Prerequisite:** Your existing deployment setup (Docker, Nginx, PostgreSQL, Redis) must already be running.

---

## Table of Contents

1. [Prerequisites Checklist](#1-prerequisites-checklist)
2. [Step 1 — Push Code to GitHub](#2-step-1--push-code-to-github)
3. [Step 2 — Update Server (Spring Boot Backend)](#3-step-2--update-server-spring-boot-backend)
4. [Step 3 — Update Server (Admin Panel)](#4-step-3--update-server-admin-panel)
5. [Step 4 — SSL Certificate Verification](#5-step-4--ssl-certificate-verification)
6. [Step 5 — Build Release APK](#6-step-5--build-release-apk)
7. [Step 6 — Testing WebRTC Fixes](#7-step-6--testing-webrtc-fixes)
8. [Step 7 — Testing Admin Panel Live Chats](#8-step-7--testing-admin-panel-live-chats)
9. [Step 8 — TURN Server Setup (Optional but Recommended)](#9-step-8--turn-server-setup-optional-but-recommended)
10. [Step 9 — Deploy Latest WebRTC & Admin Fixes (v2.1)](#10-step-9--deploy-latest-webrtc--admin-fixes-v21)
11. [Step 10 — Build APK After Latest Fixes](#11-step-10--build-apk-after-latest-fixes)
12. [Step 11 — KMP Multiplatform Build Guide (Web & iOS)](#12-step-11--kmp-multiplatform-build-guide-web--ios)
13. [Rollback Plan](#13-rollback-plan)
14. [Troubleshooting](#14-troubleshooting)

---

## 1. Prerequisites Checklist

Before deploying, make sure the following are in place:

| # | Requirement | Status |
|---|-------------|--------|
| 1 | Docker + Docker Compose running on VPS | ☐ |
| 2 | Nginx configured as reverse proxy | ☐ |
| 3 | SSL certificate active (self-signed or Let's Encrypt) | ☐ |
| 4 | Git installed on local machine | ☐ |
| 5 | SSH access to VPS | ☐ |
| 6 | Android Studio installed locally (for APK build) | ☐ |
| 7 | 2 physical Android devices available for testing | ☐ |

---

## 2. Step 1 — Push Code to GitHub

On your **local machine** (Windows):

```powershell
# Navigate to project root
cd e:\Learn\programming\ponisha\MiniChat

# Stage all changed files
git add -A

# Commit with descriptive message
git commit -m "fix: WebRTC production reliability + admin live chat monitoring

- Fix HTTP->HTTPS and WS->WSS in release builds
- Rewrite WebRtcClient with ICE monitoring, recovery timers, audio verification
- Rewrite WebSocketManager with auto-reconnect, heartbeat, signal queueing
- Fix PeerConnection race condition (synchronous creation before joinQueue)
- Add admin panel active chats endpoint (ephemeral, in-memory only)
- Add ActiveChats.tsx page with 3s auto-polling"

# Push to remote
git push origin main
```

### Files Changed (Summary)

| Component | File | Change Type |
|-----------|------|-------------|
| Android | `app/build.gradle.kts` | Modified |
| Android | `app/src/.../webrtc/WebRtcClient.kt` | Rewritten |
| Android | `app/src/.../data/remote/WebSocketManager.kt` | Rewritten |
| Android | `app/src/.../ui/viewmodels/MatchViewModel.kt` | Rewritten |
| Backend | `domain/services/MatchService.kt` | Modified |
| Backend | `api/dtos/AdminDtos.kt` | Modified |
| Backend | `api/controllers/AdminController.kt` | Modified |
| Admin Panel | `src/pages/ActiveChats.tsx` | **New** |
| Admin Panel | `src/App.tsx` | Modified |
| Admin Panel | `src/components/layout/MainLayout.tsx` | Modified |

---

## 3. Step 2 — Update Server (Spring Boot Backend)

SSH into your VPS and pull the latest code:

```bash
# Connect to VPS
ssh root@172.86.95.177

# Navigate to project
cd ~/apps/MiniChat

# Pull latest changes
git pull origin main

# Backup database before rebuilding
docker exec minichat-postgres pg_dump -U postgres minichat | gzip > /opt/backups/postgres/minichat_pre_webrtc_fix.sql.gz

# Rebuild and restart the backend
cd SpringBoot
docker compose --env-file .env up -d --build

# Wait for container to start (usually 30-60 seconds)
sleep 30

# Verify the API is running
docker compose ps
curl http://localhost:8080/auth/health

# Check logs for errors
docker compose logs --tail=50 api
```

### Verify New Endpoint

```bash
# Test the new active-chats endpoint (should return empty list)
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" http://localhost:8080/admin/active-chats
```

Expected response:
```json
{"success": true, "message": "Active chats", "data": []}
```

---

## 4. Step 3 — Update Server (Admin Panel)

If your admin panel is deployed separately (e.g., via Nginx static files):

```bash
# Navigate to admin panel directory
cd ~/apps/MiniChat/admin-panel

# Install dependencies (if new packages were added)
npm install

# Build production bundle
npm run build

# Copy dist to Nginx serving directory (adjust path as needed)
# Example: cp -r dist/* /var/www/admin-panel/
```

If the admin panel is served via Docker, rebuild that container:

```bash
cd ~/apps/MiniChat/admin-panel
docker build -t minichat-admin .
docker stop minichat-admin && docker rm minichat-admin
docker run -d --name minichat-admin -p 3001:80 minichat-admin
```

---

## 5. Step 4 — SSL Certificate Verification

The release build now uses `https://` and `wss://`. Your Nginx **must** have SSL configured.

### Check if SSL is already working

```bash
curl -k https://172.86.95.177/auth/health
```

If this fails, you need to set up SSL. Two options:

### Option A: Self-Signed Certificate (for IP-based servers)

```bash
# Generate certificate
sudo mkdir -p /etc/nginx/ssl
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
    -keyout /etc/nginx/ssl/minichat.key \
    -out /etc/nginx/ssl/minichat.crt \
    -subj "/CN=172.86.95.177"

# Update Nginx config to use SSL
sudo nano /etc/nginx/sites-available/minichat
```

Add the SSL server block:

```nginx
server {
    listen 443 ssl http2;
    server_name 172.86.95.177;

    ssl_certificate /etc/nginx/ssl/minichat.crt;
    ssl_certificate_key /etc/nginx/ssl/minichat.key;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers HIGH:!aNULL:!MD5;

    # ... copy all location blocks from existing HTTP config ...
}
```

```bash
sudo nginx -t
sudo systemctl reload nginx
```

### Option B: Let's Encrypt (requires a domain name)

```bash
sudo certbot --nginx -d YOUR_DOMAIN.com
```

> **IMPORTANT:** If you use a self-signed certificate, the Android app may reject it. You need to either:
> - Add a `network_security_config.xml` to trust your self-signed cert, OR
> - Use a proper domain with Let's Encrypt (recommended)

---

## 6. Step 5 — Build Release APK

On your **local machine** (Windows), open Android Studio:

```powershell
# Navigate to Android project
cd e:\Learn\programming\ponisha\MiniChat\MiniChat

# Build release APK (unsigned debug for testing)
.\gradlew.bat assembleRelease

# APK will be at:
# app\build\outputs\apk\release\app-release.apk
```

Or build from Android Studio:
1. Open `MiniChat/MiniChat` project
2. Go to **Build** → **Generate Signed Bundle / APK**
3. Select **APK**
4. Choose your keystore (`chatora1.jks`)
5. Select **release** build variant
6. Click **Finish**

### Install on Test Devices

```powershell
# Connect device via USB and install
adb install -r app\build\outputs\apk\release\app-release.apk

# Or transfer APK to device and install manually
```

---

## 7. Step 6 — Testing WebRTC Fixes

You need **2 physical Android devices** with the release APK installed.

### Test 1: Basic Connection (Critical)
1. Open app on both devices
2. Press **Start** on both
3. They should match within 5 seconds
4. **Verify:** Both see each other's video AND hear audio
5. **Expected:** No black screens, no missing audio

### Test 2: Stability (Repeat 10 Times)
1. Press **Next** on one device
2. Wait for new match
3. **Verify:** Video and audio load correctly each time
4. **Expected:** 10/10 successful connections with video + audio

### Test 3: Network Disruption Recovery
1. Start a video call between 2 devices
2. On one device, enable **Airplane Mode** for 3-5 seconds
3. Disable Airplane Mode
4. **Expected:** Connection recovers within 10 seconds (ICE restart)
5. **Not expected:** App should NOT auto-press "Next"

### Test 4: No Auto-FindNext
1. Start a video call
2. Observe for 30 seconds
3. **Expected:** Call stays stable, no automatic match cycling
4. **If it cycles:** Check logcat for `PARTNER_LEFT` or `Connection FAILED` messages

### Test 5: Rapid Next Pressing
1. Start matching
2. Press **Next** 5 times rapidly
3. **Expected:** No crash, no infinite cycling loop, eventually settles on a stable match

### Reading Logs (using ADB)

```bash
# Filter WebRTC and WebSocket logs
adb logcat | grep -E "WebRtcClient|WebSocket|MINICHAT_DEBUG"
```

Key log messages to look for:

| Log | Meaning |
|-----|---------|
| `ICE Connection State: CONNECTED` | ✅ Connection successful |
| `ICE Connection State: DISCONNECTED` | ⚠️ Temporary disconnect (will retry) |
| `ICE Connection State: FAILED` | ❌ Connection failed (will restart) |
| `Heartbeat received from server` | ✅ STOMP keepalive working |
| `Scheduling reconnect attempt N in Xms` | ⚠️ WebSocket reconnecting |
| `PeerConnection created and ready` | ✅ PC created before signaling |
| `Audio verification: local=true, remote=true` | ✅ Both audio tracks active |

---

## 8. Step 7 — Testing Admin Panel Live Chats

1. Open admin panel in browser: `https://172.86.95.177:ADMIN_PORT` or your admin URL
2. Log in with admin credentials
3. Navigate to **"چت‌های فعال"** (Active Chats) in the sidebar
4. **Expected:** Empty state message "هیچ تماس فعالی وجود ندارد"

5. Start a video call between 2 devices
6. Refresh admin panel (or wait 3 seconds for auto-refresh)
7. **Expected:** Active chat appears with:
   - Match ID (first 8 characters)
   - Both usernames
   - Live duration counter

8. On one device, press **Next**
9. **Expected:** Chat disappears from admin panel within 3 seconds

10. Verify the data is ephemeral:
    - No new database tables created
    - No data persisted after server restart
    - Restarting the Docker container clears all active chats

---

## 9. Step 8 — TURN Server Setup (Optional but Recommended)

Currently the app uses free `openrelay.metered.ca` TURN servers. For production, consider setting up your own TURN server.

### Option A: Self-Hosted Coturn on Your VPS

```bash
# Install Coturn
sudo apt install -y coturn

# Configure
sudo nano /etc/turnserver.conf
```

```ini
listening-port=3478
tls-listening-port=5349
realm=172.86.95.177
server-name=172.86.95.177
fingerprint
lt-cred-mech
user=minichat:YOUR_STRONG_PASSWORD
total-quota=100
max-bps=300000
no-multicast-peers
```

```bash
# Start Coturn
sudo systemctl enable coturn
sudo systemctl start coturn

# Open firewall ports
sudo ufw allow 3478/tcp
sudo ufw allow 3478/udp
sudo ufw allow 5349/tcp
sudo ufw allow 5349/udp
sudo ufw allow 49152:65535/udp  # TURN relay ports
```

Then update `build.gradle.kts` in the **release** block:

```kotlin
buildConfigField("String", "TURN_URL", "\"turn:172.86.95.177:3478\"")
buildConfigField("String", "TURN_USERNAME", "\"minichat\"")
buildConfigField("String", "TURN_PASSWORD", "\"YOUR_STRONG_PASSWORD\"")
```

### Option B: Metered.ca (Paid Service, ~$10/month)

1. Go to [metered.ca](https://www.metered.ca/)
2. Sign up and get your API key
3. Use their TURN credentials in `build.gradle.kts`

### Option C: Twilio Network Traversal (Free Tier Available)

1. Sign up at [twilio.com](https://www.twilio.com/)
2. Navigate to **Network Traversal** → **TURN**
3. Use the provided credentials

---

## 10. Step 9 — Deploy Latest WebRTC & Admin Fixes (v2.1)

> **Changes:** WebRTC connection reliability rewrite (6 bug fixes) + Admin panel ActiveChats error handling improvement.

### 10.1 Push Latest Code

On your **local machine** (Windows):

```powershell
cd e:\Learn\programming\ponisha\MiniChat

git add -A

git commit -m "fix: WebRTC production reliability v2.1 + admin panel fix

- Add CompletableDeferred for local track readiness (fixes black screen)
- Preserve local tracks across PeerConnection recreations
- Guard stale callbacks with isPeerConnectionActive AtomicBoolean
- FAILED state: ICE restart + re-offer instead of auto-findNext
- debouncedFindMatch no longer calls stopMatching (state corruption fix)
- handleMatchFound always creates fresh PC with awaited local tracks
- Admin ActiveChats: robust response parsing + retry button"

git push origin main
```

### 10.2 Update Backend (Spring Boot)

```bash
# SSH into VPS
ssh root@172.86.95.177

# Pull latest changes
cd ~/apps/MiniChat
git pull origin main

# Backup database
docker exec minichat-postgres pg_dump -U postgres minichat | gzip > /opt/backups/postgres/minichat_pre_v2.1.sql.gz

# Rebuild backend
cd SpringBoot
docker compose --env-file .env up -d --build

# Wait and verify
sleep 30
docker compose ps
curl http://localhost:8080/auth/health

# Check for errors
docker compose logs --tail=30 api
```

### 10.3 Update Admin Panel

```bash
# Navigate to admin panel
cd ~/apps/MiniChat/admin-panel

# Install dependencies (if changed)
npm install

# Build production bundle
npm run build

# Deploy to Nginx
# Option A: Static files
cp -r dist/* /var/www/admin-panel/

# Option B: Docker
# docker build -t minichat-admin .
# docker stop minichat-admin && docker rm minichat-admin
# docker run -d --name minichat-admin -p 3001:80 minichat-admin
```

### 10.4 Verify Admin Panel Fix

```bash
# Test active-chats endpoint
curl -H "Authorization: Bearer YOUR_ADMIN_TOKEN" http://localhost:8080/admin/active-chats
```

Expected: `{"success": true, "data": []}` — Then open admin panel in browser → Active Chats should load without error.

---

## 11. Step 10 — Build APK After Latest Fixes

On your **local machine** (Windows):

```powershell
cd e:\Learn\programming\ponisha\MiniChat\MiniChat

# Verify compilation
.\gradlew.bat compileDebugKotlin

# Build release APK
.\gradlew.bat assembleRelease

# APK location:
# app\build\outputs\apk\release\app-release.apk
```

Install on **2 physical devices** and run all tests from [Section 7](#7-step-6--testing-webrtc-fixes).

### New Log Messages to Watch

```bash
adb logcat | grep -E "WebRtcClient|MatchViewModel"
```

| Log | Meaning |
|-----|---------|
| `Camera + Audio initialized. Local tracks ready` | ✅ Tracks ready before PC creation |
| `Added local VIDEO track to PeerConnection` | ✅ Video will be sent to partner |
| `Added local AUDIO track to PeerConnection` | ✅ Audio will be sent to partner |
| `localVideoTrack is NULL` | ❌ Camera init failed — investigate |
| `ICE Connection State: ... (IGNORED — PC inactive)` | ✅ Stale callback filtered |
| `closePeerConnection: Done. Local tracks preserved` | ✅ Tracks survive match transition |
| `Creating fresh PeerConnection for match` | ✅ New PC for each match |
| `Connection FAILED — attempting recovery` | ⚠️ Will try ICE restart, not auto-findNext |
| `ICE restart did not recover — re-creating offer` | ⚠️ Last resort recovery attempt |

---

## 12. Step 11 — KMP Multiplatform Build Guide (Web & iOS)

> ⚠️ **IMPORTANT:** The current project is a standard Android-only project. Converting to KMP is a **major migration** requiring significant restructuring. This section describes what's needed — it is NOT a simple toggle.

### 12.1 Current Architecture vs KMP Architecture

```
CURRENT (Android-only):            KMP TARGET:
┌──────────────────────┐           ┌──────────────────────┐
│     MiniChat/        │           │     MiniChat/        │
│  app/ (Android)      │           │  shared/             │
│    └─ src/main/      │           │    └─ commonMain/    │ ← Shared logic
│       ├─ webrtc/     │           │    └─ androidMain/   │ ← Android specifics
│       ├─ viewmodels/ │           │    └─ iosMain/       │ ← iOS specifics
│       ├─ data/       │           │    └─ jsMain/        │ ← Web specifics
│       └─ ui/         │           │  androidApp/         │ ← Android UI (Compose)
│                      │           │  iosApp/             │ ← iOS UI (SwiftUI)
│                      │           │  webApp/             │ ← Web UI (Compose/Web)
└──────────────────────┘           └──────────────────────┘
```

### 12.2 Dependencies That Need Migration

| Current (Android) | KMP Replacement | Notes |
|-------------------|-----------------|-------|
| Hilt (DI) | **Koin** | Koin supports KMP natively |
| Room (DB) | **SQLDelight** | Cross-platform SQL |
| Retrofit (HTTP) | **Ktor Client** | KMP-native HTTP |
| OkHttp (WebSocket) | **Ktor WebSocket** | Built-in support |
| Jetpack Compose (UI) | **Compose Multiplatform** | Supports Android, iOS, Web |
| CameraX | **Platform-specific** | No KMP equivalent |
| WebRTC | **Platform-specific** | Separate impl per platform |
| EncryptedSharedPrefs | **multiplatform-settings** | KMP settings library |
| AndroidX Navigation | **Voyager / Decompose** | KMP navigation |

### 12.3 How to Build for Web (Kotlin/JS)

**Step 1:** Add the Compose for Web plugin to root `build.gradle.kts`:

```kotlin
plugins {
    kotlin("multiplatform") version "2.1.0"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
}
```

**Step 2:** Configure the `webApp/` module:

```kotlin
kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "minichat.js"
            }
        }
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(compose.html.core)
                implementation(compose.runtime)
                implementation(project(":shared"))
            }
        }
    }
}
```

**Step 3:** Build and run:

```bash
# Development server
./gradlew :webApp:jsBrowserDevelopmentRun

# Production build
./gradlew :webApp:jsBrowserDistribution
# Output: webApp/build/dist/js/productionExecutable/
```

**WebRTC for Web:** Use JavaScript WebRTC API via Kotlin/JS `external` declarations:

```kotlin
// jsMain — wrap browser WebRTC API
external class RTCPeerConnection(config: dynamic) {
    fun createOffer(): Promise<dynamic>
    fun setLocalDescription(desc: dynamic): Promise<Unit>
    // ...
}
```

### 12.4 How to Build for iOS (Kotlin/Native)

**Step 1:** Add iOS target to `shared/build.gradle.kts`:

```kotlin
kotlin {
    androidTarget()
    iosX64()      // Intel Mac simulator
    iosArm64()    // Physical iPhone
    iosSimulatorArm64()  // Apple Silicon simulator
    
    cocoapods {
        summary = "MiniChat Shared Module"
        homepage = "https://github.com/your/repo"
        ios.deploymentTarget = "16.0"
        framework { baseName = "shared" }
        
        // WebRTC pod for iOS
        pod("GoogleWebRTC") { version = "~> 1.1" }
    }
}
```

**Step 2:** Create the iOS app in Xcode:

```bash
# Generate the shared framework
./gradlew :shared:linkDebugFrameworkIosArm64

# Output: shared/build/bin/iosArm64/debugFramework/shared.framework
```

**Step 3:** In Xcode, create a new SwiftUI project (`iosApp/`) and embed the framework:

```swift
import shared  // The KMP shared module

struct ContentView: View {
    let viewModel = MatchViewModelWrapper()
    var body: some View {
        // Use shared logic from Kotlin
        Text(viewModel.state)
    }
}
```

**Step 4:** Build for iOS:

```bash
# Build framework for all iOS architectures
./gradlew :shared:linkReleaseFrameworkIosArm64

# Then build in Xcode:
# Open iosApp/iosApp.xcworkspace → Build (⌘+B)
```

### 12.5 Migration Timeline Estimate

| Phase | Task | Duration |
|-------|------|----------|
| 1 | Create `shared` module + move data models | 2-3 days |
| 2 | Migrate Hilt → Koin, Room → SQLDelight | 3-5 days |
| 3 | Migrate Retrofit → Ktor, OkHttp WS → Ktor WS | 3-5 days |
| 4 | Platform-specific WebRTC implementations | 5-7 days |
| 5 | Web UI (Compose for Web) | 5-7 days |
| 6 | iOS UI (SwiftUI + shared framework) | 5-7 days |
| 7 | Testing + CI/CD for all platforms | 3-5 days |
| **Total** | | **~4-6 weeks** |

> **Recommendation:** Complete all current fixes, stabilize the Android app, then start KMP migration as a new phase.

---

## 13. Rollback Plan

If the new changes cause issues in production:

### Rollback Backend

```bash
cd ~/apps/MiniChat

# View recent commits
git log --oneline -5

# Revert to previous commit
git checkout HEAD~1

# Rebuild
cd SpringBoot
docker compose --env-file .env up -d --build
```

### Rollback Android

Reinstall the previous APK version on devices.

### Rollback Admin Panel

```bash
cd ~/apps/MiniChat
git checkout HEAD~1
cd admin-panel
npm install && npm run build
# Re-deploy static files
```

---

## 14. Troubleshooting

### Problem: Black screen after connecting

**Check:**
1. Is SSL configured? (`curl -k https://172.86.95.177/auth/health`)
2. Is `wss://` working? (`websocat wss://172.86.95.177/ws-native` or check Nginx WebSocket config)
3. Are TURN servers reachable? (`openrelay.metered.ca` might be down)

**Fix:** Check Nginx WebSocket proxy config includes:
```nginx
location /ws-native {
    proxy_pass http://minichat_api;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 86400;
}
```

### Problem: App auto-presses "Next"

**Check ADB logs:**
```bash
adb logcat | grep "PARTNER_LEFT\|Connection FAILED\|DISCONNECTED"
```

- If `PARTNER_LEFT` with matchId "none" → Server-side bug (should be fixed now)
- If `Connection FAILED` → TURN server issue or SSL issue
- If `DISCONNECTED` → Temporary, should recover (check for ICE restart logs)

### Problem: WebSocket keeps disconnecting

**Check:**
```bash
# On server, check Nginx timeout
grep proxy_read_timeout /etc/nginx/sites-available/minichat
```

`proxy_read_timeout` must be at least `86400` (24 hours) for WebSocket.

**Check Docker container memory:**
```bash
docker stats minichat-api --no-stream
```

### Problem: Admin panel active-chats endpoint returns 404

**Check:**
1. Is the backend running the latest code? `docker compose logs --tail=20 api`
2. Is the endpoint accessible? `curl -H "Authorization: Bearer TOKEN" http://localhost:8080/admin/active-chats`
3. Check if `MatchService` injection works (look for Spring startup errors in logs)

### Problem: Admin panel shows empty but users are chatting

**Check:**
- The data is ephemeral — if the backend container was restarted, all in-memory data is lost
- Ensure the admin panel is calling the correct API URL (check `.env` file)
- Open browser DevTools → Network tab → look for `/admin/active-chats` requests

---

> **After successful testing, tag the release:**
> ```bash
> git tag -a v2.0.0 -m "WebRTC reliability fixes + admin live chat monitoring"
> git push origin v2.0.0
> ```
