# OmniShield Suite : The All-in-One Offline Android Utility & Security Suite

OmniShield Suite is an advanced, privacy-first offline utility, hardware controller, and cryptographic security suite for Android. Designed as a digital "Swiss Army knife," it combines low-level hardware access, military-grade file security, network diagnostic tools, document processing, and peer-to-peer transfer into a single unified application.

Built entirely with **Jetpack Compose (Material 3)** and **Kotlin**, OmniShield operates under a strict **Zero-Cloud, 100% Offline philosophy**: no external cloud servers, no account logins, no analytics tracking, and zero remote data harvesting.

---

## 🎯 Core Philosophy & Design Principles

* **100% Offline & Air-Gapped:** Every tool—from cryptographic hashing and EXIF stripping to PDF merging and local subnet scanning—runs directly on-device using Android's native runtime and cryptographic APIs.
* **Zero-Cloud & Zero-Telemetry:** OmniShield never sends telemetry, diagnostic packets, or user data to remote servers. The app can be run with complete functionality in full Airplane mode.
* **Plausible Deniability & Anti-Coercion:** Built with specialized defenses, such as a Duress Decoy PIN System, protecting your sensitive passwords and recovery phrases even under forced unlock situations.

---

## 🚀 Comprehensive Feature Breakdown

The application is structured into three specialized studios accessible via the Main Dashboard:

### 1. 🔐 Security & Cryptography Studio

* **Hardware-Backed AES-256-GCM Vault:** Encrypt and decrypt files of any size with authenticated AES-256-GCM encryption, PBKDF2 (10,000 iterations), 128-bit salt, and random 96-bit initialization vectors (IV). Includes an integrated Fortified Password Generator and password entropy analyzer (`CryptoRepository.kt`).
* **Secret Notes & Recovery Seed Vault with Duress Decoy PIN:** An encrypted scratchpad for crypto seed phrases, server credentials, and sensitive notes. Features dual-PIN protection: entering the Master PIN unlocks your real notes, while entering an emergency Duress PIN loads a plausible decoy vault, protecting you against physical coercion.
* **Developer Token & Security Inspector (JWT & X.509 Decoder):** Inspect, parse, and validate JSON Web Tokens (headers, payload claims, expiration dates) and X.509 SSL/TLS certificates 100% offline without exposing private secrets to online web utilities.
* **DoD 5220.22-M File Shredder & Shannon Entropy Analyzer:** Securely shred deleted files with multi-pass zero/random overwriting before unlinking. Includes a real-time byte entropy visualizer to identify encrypted payloads, packed malware, or compressed binaries.
* **EXIF Metadata Stripper & Inspector:** Detect and scrub sensitive metadata (GPS coordinates, camera serials, timestamps) from photos and documents prior to sharing.

### 2. 📄 Documents & Media Processing Studio

* **PDF Studio (Merge, Split & Optimize):** Merge multiple PDF documents into a single document, split page ranges, and optimize page hierarchies entirely on-device without relying on third-party PDF cloud converters.
* **Universal Media Converter & Adaptive Compressor:**
  * **Lossless ZIP Archiving:** Level-9 ultra-compression engine for files and folders.
  * **Target-Size Budget Compression:** Compress images to match a precise file size limit (e.g., `< 500 KB` or `< 2 MB` for email attachments or portals).
  * **Dimension & Aspect Ratio Resizer:** Downscale high-resolution photos with preserved aspect ratios.
  * **Structured Data Converter:** Convert interchangeably between JSON, XML, and YAML structures.
* **Binary & Hex Viewer:** Inspect raw file bytes, analyze ASCII representations, inspect magic byte signatures, and debug binary file structures directly from your phone.

### 3. 📡 Hardware, Radios & Connectivity Studio

* **Zero-Cloud P2P Encrypted Local Transfer (AirBeam):** Transfer gigabyte files wirelessly between two Android devices over a local Wi-Fi network or a personal Wi-Fi Hotspot. Encrypted on-the-fly with AES-256-GCM and paired instantly via air-gapped QR codes and 6-digit dynamic PINs (`P2pTransferManager.kt`).
* **Local Network & Wi-Fi Intruder Scanner (LAN Audit):** Audit the local Wi-Fi subnet (e.g., `192.168.1.0/24`) using ICMP echo requests and `/proc/net/arp` cache analysis to detect connected devices (smart TVs, IP cameras, rogue laptops, and unknown devices).
* **Open Port & Exposed Service Checker:** Local socket scanner against `localhost` (`127.0.0.1`) or local gateway routers to flag exposed service ports (FTP 21, SSH 22, HTTP 80, HTTPS 443, Wireless ADB 5555, HTTP Proxy 8080).
* **NFC Tag Toolkit & Offline Tag Vault:** Read NFC tag UIDs, inspect and parse NDEF records (URIs, text, vCards), write custom NFC records, and save scanned physical cards into a local offline card vault.
* **Universal Consumer IR Remote Blaster:** Transmit infrared control signals using your device's built-in IR blaster. Includes pre-configured NEC protocol profiles (Power, Volume, Channel, Inputs) and support for raw Pronto Hex codes for home appliances and TVs.
* **Battery Diagnostics & Air-Gapped QR Generator:** Real-time battery temperature, charging voltage, health diagnostics, and an air-gapped QR generator/scanner for sharing text and payloads offline.

---

## 👥 Target Audience

| User Profile | Primary Use Case |
| :--- | :--- |
| **Privacy Enthusiasts & Security Researchers** | File shredding, EXIF sanitization, Shannon entropy analysis, offline JWT token decoding, and duress vault protection. |
| **Tech Professionals & Sysadmins** | Local subnet auditing, open port scanning, hex byte analysis, and offline P2P transfers. |
| **Everyday Android Power Users** | Lossless file/image compression, PDF manipulation, NFC card backup, and universal TV/AC remote controls. |

---

## 🛠️ Technical Specifications

* **Operating System:** Android 8.0+ (API Level 26 through API Level 37).
* **Language & Architecture:** 100% Kotlin with Clean Architecture, MVVM design pattern, and Kotlin Coroutines/Flows.
* **User Interface:** Jetpack Compose with Material 3, Dark Cyber-Security aesthetic, dynamic accent palettes, and haptic feedback.
* **Security & Cryptography:** Android Keystore Provider, `javax.crypto` (`AES/GCM/NoPadding`), `PBKDF2WithHmacSHA256`, and SHA-256/SHA-512 digest algorithms.
