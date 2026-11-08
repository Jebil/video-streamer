# 🎬 Video Streamer

A high-performance, reactive video streaming server built with **Java 25**, **Spring Boot 4**, and **Spring WebFlux (Netty)**.

Provides chunked video streaming with HTTP byte-range request support (`206 Partial Content`), subfolder library scanning, path-traversal protection, in-memory caching, instant search, and a modern HTML5 video player with auto-resume and keyboard controls.

---

## 🚀 Key Features

* **Reactive Byte-Range Streaming:** Streams videos using HTTP Partial Content (`Range: bytes=start-end`) in 1 MB chunks via Spring WebFlux's non-blocking I/O event loop on Netty.
* **Full & Chunked Endpoints:**
  * Chunked streaming endpoint (`/videos/{*name}`) for smooth, low-latency playback in HTML5 video players.
  * Direct download endpoint (`/videos/full/{*name}`) for downloading entire files.
* **Subdirectory & Library Navigation:** Recursively scans and streams media files stored in nested subfolders (e.g. `Action/Sci-Fi/movie.mp4`).
* **Path-Traversal Security:** Enforces strict boundary verification (`resolved.startsWith(baseLocation)`), rejecting any directory traversal attempts with `403 Forbidden`.
* **In-Memory Library Cache:** Scans directory contents with try-with-resources stream isolation and caches results in memory (60s TTL), with instant manual cache invalidation (`/refresh`).
* **Modern Responsive Dark UI:**
  * **Video Library:** Instant client-side search filtering by title and path, format badges (`MP4`, `MKV`, `AVI`), and human-readable file sizes.
  * **Video Player:** HTML5 player with auto-resume from `localStorage`, toast notifications, and full keyboard navigation shortcuts.

---

## 🛠️ Architecture & Request Flow

```text
Browser / Client
   │
   ├─► GET /                     ──► PageController ──► MovieService (In-memory cache) ──► index.ftl
   ├─► GET /watch/{*videoPath}   ──► PageController ──► video.ftl
   ├─► GET /refresh              ──► PageController ──► MovieService.refreshCache() ──► redirect:/
   │
   ├─► GET /videos/{*name}       ──► VideoController ──► MovieService.resolveVideoPath()
   │   (with Range: bytes=...)        │
   │                                  ▼
   │                             ResourceRegion (1MB chunk)
   │                                  │
   │                                  ▼
   │                             ResourceRegionMessageWriter (Content-Range: 206)
   │                                  │
   │                                  ▼
   └───────────────────────────  Netty EventLoop (Non-blocking response)
```

---

## 📋 Technology Stack

* **Language:** Java 25 (OpenJDK 25.0.3+)
* **Framework:** Spring Boot 4.1.2
* **Reactive Engine:** Spring WebFlux & Project Reactor on Netty
* **Template Engine:** FreeMarker 2.3+
* **Build Tool:** Gradle 9.7.1 (Wrapper included)
* **Code Quality & Verification:**
  * **Spotless 7.0.2** with **Google Java Format 2.0.0**
  * **SpotBugs 6.1.7** static bytecode analysis
  * **JUnit 5 Platform** & Spring WebFlux TestClient

---

## ⚙️ Configuration

Application settings are configured in [`src/main/resources/application.yml`](src/main/resources/application.yml):

```yaml
video:
  location: ${MOVIES_PATH:./videos}

spring:
  freemarker:
    charset: UTF-8
    check-template-location: true
    enabled: true
    prefer-file-system-access: true
    suffix: .ftl
    template-loader-path: classpath:/templates/
```

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `MOVIES_PATH` | Path to directory containing video files | `./videos` |
| `SERVER_PORT` | HTTP port for the web server | `8085` |
| `LOG_FILE` | Filepath for rolling application logs | `logs/video-streamer.log` |

### Supported Video Formats

* **Containers & Extensions:** `.mp4`, `.mkv`, `.avi`, `.wmv`, `.vob`, `.mov`, `.webm`, `.flv`, `.m4v`
* **Media Types:** Automatically resolved via Spring's `MediaTypeFactory` with fallbacks.

> **Note on Browser Playback:** HTML5 `<video>` natively decodes MP4 (H.264/AAC) and WebM (VP8/VP9/Opus) across all browsers. MKV, AVI, and WMV can be downloaded via `/videos/full/{*name}` or played in browsers that bundle native container support.

---

## 📡 API & Endpoint Reference

| Method | Endpoint | Description | Response Status |
|---|---|---|---|
| `GET` | `/` | Library overview page with instant search | `200 OK` (HTML) |
| `GET` | `/watch/{*videoPath}` | Embedded player page for selected video | `200 OK` (HTML) |
| `GET` | `/refresh` | Manually clears library cache and rescans disk | `302 Found` (Redirect) |
| `GET` | `/videos/{*name}` | Streams chunked video with byte-range support | `206 Partial Content` / `404` / `403` |
| `GET` | `/videos/full/{*name}` | Direct full file download | `200 OK` (Binary) / `404` / `403` |

---

## ⌨️ Player Keyboard Shortcuts

When watching a video on `/watch/{*videoPath}`, the following shortcuts are active:

| Key | Action |
|---|---|
| <kbd>Space</kbd> | Toggle Play / Pause |
| <kbd>←</kbd> / <kbd>→</kbd> | Seek backward / forward by 5 seconds |
| <kbd>↑</kbd> / <kbd>↓</kbd> | Increase / decrease volume by 10% |
| <kbd>F</kbd> | Toggle fullscreen mode |
| <kbd>M</kbd> | Toggle mute |
| <kbd>[</kbd> / <kbd>]</kbd> | Decrease / increase playback speed (0.25x – 2.5x) |

---

## 🏗️ Build & Development Commands

Use the Gradle wrapper to build and run the project:

### 1. Build and Run Tests
```bash
./gradlew build
```

### 2. Run the Application
```bash
./gradlew bootRun
```
Then navigate to [http://localhost:8085](http://localhost:8085) in your browser.

### 3. Auto-Format Code (Google Java Format)
```bash
./gradlew spotlessApply
```

### 4. Check Code Formatting
```bash
./gradlew spotlessCheck
```

### 5. Run Static Code Analysis (SpotBugs)
```bash
./gradlew spotbugsMain spotbugsTest
```

---

## 🔒 Security

* **Directory Traversal Mitigation:** All relative paths from client requests are normalized and validated to ensure they reside strictly within `video.location`. Any attempt to escape the directory root throws `SecurityException` and returns HTTP `403 Forbidden`.
* **Zero System Path Exposure:** Client URLs never disclose the server's root filesystem paths (e.g., `D:\Movies\secret`), using relative identifiers only.
