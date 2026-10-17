# Keyline Authenticator
<p align="center">
  <img src="https://github.com/user-attachments/assets/01265cf3-d458-49a7-ab6d-2d5417027708" width="160" height="160" alt="Keyline Authenticator Icon" />
</p>



[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2-purple.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.09-brightgreen.svg)](https://developer.android.com/jetpack/compose)

**Keyline Authenticator** is a privacy-first, offline TOTP/HOTP 2-factor authentication (2FA) app built for Android with modern Jetpack Compose. It safeguards your accounts using Hardware-backed Android KeyStore (TEE / SE) encryption, biometric vault locking, direct high-density dynamic QR migration (local air-gapped transfer), and zero-telemetry offline isolation.

---

## Features

- **100% Offline & Private:** Zero network communication. Secrets never leave your device.
- **Hardware-Backed Encryption:** Vault keys stored in Hardware-backed Android KeyStore (TEE / SE) using AES-256-GCM authenticated encryption.
- **Biometric Vault Lock:** Integrated with `androidx.biometric` (BiometricPrompt) with device credential fallback (PIN, Pattern, Password).
- **Direct High-Density Dynamic QR Migration (Local Air-Gapped Transfer):** Seamless bulk migration between devices using high-density dynamic QR payload streams without network access.
- **Encrypted File Backups:** Password-authenticated encrypted JSON exports using PBKDF2 (100,000 iterations) and AES-256-GCM.
- **Anti-Snooping Protection:** Strict `FLAG_SECURE` window management prevents screen recording, screenshots, and recents thumbnail leakage.
- **Modern Jetpack Compose UI:** Material Design 3 UI with system, dark, and light themes.

---

## Getting Started

### Prerequisites

- **Java Development Kit (JDK):** JDK 17 or JDK 21 (e.g. Android Studio JBR or OpenJDK)
- **Android SDK:** Compile SDK 36, Min SDK 26 (Android 8.0 Oreo)
- **Gradle:** Wrapper included (`./gradlew`)

---

## Building from Source

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/keyline-authenticator.git
cd keyline-authenticator
```

### 2. Build the Debug APK

To compile and assemble the debug APK:

```bash
# On Linux / macOS
./gradlew assembleDebug

# On Windows (PowerShell)
.\gradlew.bat assembleDebug
```

The compiled debug APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```

### 3. Run Unit Tests

```bash
./gradlew test
```

### 4. Build the Release Bundle (AAB) or APK

For release builds with R8 minification and resource shrinking, provide your signing configuration via environment variables or a local `keystore.properties` file:

#### Option A: Using `keystore.properties` (Recommended for Local Builds)
Create a `keystore.properties` file in the project root (this file is excluded by `.gitignore`):

```properties
KEYLINE_RELEASE_KEYSTORE=keyline-release.jks
KEYLINE_RELEASE_KEY_ALIAS=your_key_alias
KEYLINE_RELEASE_STORE_PASSWORD=your_store_password
KEYLINE_RELEASE_KEY_PASSWORD=your_key_password
```

#### Option B: Using Environment Variables (Recommended for CI/CD)

```bash
export KEYLINE_RELEASE_KEYSTORE=/path/to/keystore.jks
export KEYLINE_RELEASE_KEY_ALIAS=your_key_alias
export KEYLINE_RELEASE_STORE_PASSWORD=your_store_password
export KEYLINE_RELEASE_KEY_PASSWORD=your_key_password
```

#### Build Release Commands:

```bash
# Generate signed Android App Bundle (.aab) for Google Play
./gradlew bundleRelease

# Generate signed APK (.apk) for testing
./gradlew assembleRelease
```

*Note: If no release signing credentials are provided, Gradle will build without failing, allowing open source contributors to compile release code without proprietary keys.*

---

## Architecture & Security

- **Language:** 100% Kotlin with Coroutines & StateFlow architecture.
- **UI Toolkit:** Jetpack Compose + Material 3.
- **Cryptography:**
  - `androidx.security.crypto.EncryptedSharedPreferences` backed by Hardware-backed Android KeyStore (TEE / SE) `MasterKey` (AES256-GCM / AES256-SIV).
  - RFC 6238 (TOTP) and RFC 4226 (HOTP) implementation with HMAC-SHA1, SHA256, and SHA512.
  - Ephemeral in-memory byte zeroing after HMAC digest generation.
- **ProGuard / R8:** Hardened rules preserving cryptographic providers, shaded Google Tink protobuf metadata, and serialization schemas.

---

## License

Keyline Authenticator is free software licensed under the **GNU General Public License version 3.0 (GPL v3.0)**.  
See the [LICENSE](LICENSE) file for the full license text.
