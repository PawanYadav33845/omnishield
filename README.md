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
## OmniShield Suite Architectural Diagram
```text
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                  OMNISHIELD SUITE ARCHITECTURE                                  │
│             100% Offline • Zero-Cloud • Clean MVVM Architecture • Air-Gapped Security           │
└─────────────────────────────────────────────────────────────────────────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               PRESENTATION LAYER (JETPACK COMPOSE UI)                           │
├───────────────────────────────┬───────────────────────────────┬─────────────────────────────────┤
│  SECURITY & CRYPTO STUDIO     │   DOCUMENTS & MEDIA STUDIO    │   HARDWARE & RADIOS STUDIO      │
│  • AES-256-GCM File Vault     │   • PDF Studio (Merge/Split)  │   • Zero-Cloud P2P AirBeam      │
│  • Duress Decoy PIN Notes     │   • Target-Size Compressor    │   • LAN Subnet & ARP Scanner    │
│  • JWT & X.509 Cert Inspector │   • Level-9 Lossless ZIP      │   • Open Socket / Port Checker  │
│  • DoD 5220.22-M Shredder     │   • Binary Hex Byte Viewer    │   • NFC Tag Toolkit & Vault     │
│  • Shannon Entropy Analyzer   │   • JSON / XML / YAML Conv    │   • Universal IR Remote Blaster │
├───────────────────────────────┴───────────────────────────────┴─────────────────────────────────┤
│                Unidirectional Data Flow: UI State ◄══ StateFlow ══► User Events                 │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                VIEWMODEL & STATE ORCHESTRATION LAYER                            │
│   • CryptoViewModel          • NotesVaultViewModel          • NetworkStudioViewModel            │
│   • PdfStudioViewModel       • FileConverterViewModel       • NfcViewModel / IrViewModel        │
│   • TokenInspectorViewModel  • SecurityStudioViewModel      • HardwareStudioViewModel           │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                DOMAIN & REPOSITORY ABSTRACTION LAYER                            │
│   • ICryptoRepository        • IFileConverterRepository     • IMetadataRepository               │
│   • Scoped Storage Access    • Model Data Contracts         • Duress Vault State Machine        │
└───────────────────────────────────────────────┬─────────────────────────────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                                   CORE ON-DEVICE NATIVE ENGINES                                 │
├───────────────────────┬────────────────────────┬───────────────────────┬────────────────────────┤
│  CRYPTOGRAPHIC CORE   │    DOCUMENT & MEDIA    │    NETWORK & P2P      │    HARDWARE DRIVERS    │
│ • AES-256-GCM Cipher  │ • Android PdfDocument  │ • TCP ServerSocket    │ • NfcAdapter Driver    │
│ • PBKDF2 (10k rounds) │ • PdfRenderer Engine   │ • Streamed AES Cipher │ • NDEF Read & Write    │
│ • Android KeyStore    │ • Target-Budget Search │ • ICMP Subnet Ping    │ • ConsumerIrManager    │
│ • Offline JWT Parser  │ • Deflater Level-9 ZIP │ • /proc/net/arp Table │ • 38 kHz NEC Carrier   │
│ • Shannon Entropy Math│ • ExifInterface Scrub  │ • Socket Port Prober  │ • BatteryManager       │
└───────────────────────┴────────────────────────┴───────────────────────┴────────────────────────┘
                                                │
                                                ▼
┌─────────────────────────────────────────────────────────────────────────────────────────────────┐
│                               ANDROID OS RUNTIME & PHYSICAL PERIPHERALS                         │
├───────────────────────┬────────────────────────┬───────────────────────┬────────────────────────┤
│  FILESYSTEM & SAF     │     MEDIA PIPELINES    │ LOCAL WI-FI / HOTSPOT │    PHYSICAL RADIOS     │
│ • Scoped App Sandbox  │ • Bitmap Quantization  │ • Raw TCP Sockets     │ • NFC Controller Loop  │
│ • Hardware Keystore   │ • Native PDF Surface   │ • Subnet ARP Discovery│ • Infrared (IR) LED    │
│ • Zero Memory Leak    │ • Binary File Streams  │ • Zero-Cloud AirBeam  │ • Battery Thermals     │
└───────────────────────┴────────────────────────┴───────────────────────┴────────────────────────┘
```
### Detailed Subsystem: The Duress Decoy PIN & Cryptographic Pipeline
```text
                   ┌──────────────────────────────────────────────┐
                   │           User Enters 6-Digit PIN            │
                   └──────────────────────┬───────────────────────┘
                                          │
                                          ▼
                   ┌──────────────────────────────────────────────┐
                   │    SHA-256 PIN Comparison & Key Derivation   │
                   │    PBKDF2WithHmacSHA256 (10,000 Iterations)  │
                   └──────┬────────────────────────────────┬──────┘
                          │                                │
            Matches Master PIN?              Matches Duress PIN?
                          │                                │
                          ▼                                ▼
       ┌────────────────────────────────────┐   ┌────────────────────────────────────┐
       │      MASTER ENCRYPTED VAULT        │   │        DECOY PLAUSIBLE VAULT       │
       │  • AES-256-GCM Decryption Stream   │   │  • Decoy Seed Phrase Records       │
       │  • Real SSH Keys & Crypto Seeds    │   │  • Plausible Non-Critical Notes    │
       │  • Authenticated GCM Tag Verified  │   │  • Zero Reference to True Secrets  │
       └──────────────────┬─────────────────┘   └──────────────────┬─────────────────┘
                          │                                        │
                          └───────────────────┬────────────────────┘
                                              │
                                              ▼
                               ┌─────────────────────────────┐
                               │     Decrypted UI State      │
                               │  (Held in Volatile Memory;  │
                               │   Cleared on Screen Exit)   │
                               └─────────────────────────────┘
```
### Detailed Subsystem: File Converter & Target-Size Budget Reduction
```text
┌─────────────────────────────────┐
│     High-Res Input Image        │
│    (Camera Capture / Gallery)   │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│    Binary Search Loop           │
│    Target File Budget (e.g. 500KB)
│    • Iterative Quality Factor   │
│    • Preserves Aspect Ratio     │
│    • Compresses via WebP / JPEG │
└────────────────┬────────────────┘
                 │
                 ▼
┌─────────────────────────────────┐
│  Target Size Achieved? (≤ 500KB)│
├────────────────┬────────────────┤
│ YES            │ NO             │
│ (Quality OK)   │ (Step Down Dim)│
└────────┬───────┴────────┬───────┘
         │                │
         ▼                ▼
┌─────────────────┐ ┌─────────────────────────────────────────┐
│ Output Document │ │ Downscale Dimensions by 15% & Re-quantize│
└─────────────────┘ └─────────────────────────────────────────┘
```
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

## ⚙️ GitHub Installation & Build Guide

### Prerequisites

* **Operating System:** Windows 10/11, macOS, or Linux.
* **Android Studio:** Android Studio Ladybug (2024.2+) or Meerkat with Android SDK Platform 34/35/37 installed.
* **Java Development Kit:** JDK 21 (uses the bundled JetBrains Runtime `jbr` from Android Studio).
* **Target Hardware:** Android phone or emulator running Android 8.0 (API 26) or higher.

---

### Step 1: Clone the Repository
Open a terminal (PowerShell, Command Prompt, or bash) and clone the source code:
```bash
git clone https://github.com/PawanYadav33845/omnishield.git 
```
### Step 2: Configure Environment Paths
Make sure JAVA_HOME points to your JDK 21 installation (or Android Studio's bundled JBR).
```Windows (PowerShell):
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
```
```macos/linux
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" # macOS
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$PATH"
```
Create or verify the local.properties file in the project root:
```
sdk.dir=C\:\\Users\\<your-username>\\AppData\\Local\\Android\\Sdk
```
### Step 3: Build the Application via CLI or IDE
Option A: Build via Command Line
Run the Gradle wrapper to build the debug APK:
```Windows
.\gradlew.bat :androidApp:assembleDebug
```
```macos/linus
./gradlew :androidApp:assembleDebug
```
### Option B: Build & Run via Android Studio GUI
* Launch Android Studio.
* Select File -> Open... and select the root directory of this repository.
* Allow Gradle Sync to finish downloading dependencies.
* Select the androidApp run configuration at the top toolbar.
* Click Run (Shift + F10) to compile and launch directly on your connected device or emulator.

### Step 4: Install to Device via ADB
Connect your Android phone with USB Debugging enabled (or launch an Android Virtual Device from Device Manager):
```bash
adb devices
adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk
adb shell am start -n com.omnishield.utility/.presentation.MainActivity
```
