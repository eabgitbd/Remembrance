# Remembrance (স্মরণ • الذكر)

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android_7.0+_(API_24+)-brightgreen.svg?style=flat-square" alt="Android Version">
  <img src="https://img.shields.io/badge/Language-Kotlin_2.1-7F52FF.svg?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack_Compose_Material3-4285F4.svg?style=flat-square&logo=android" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Architecture-Clean_+_MVVM-blue.svg?style=flat-square" alt="Architecture">
  <img src="https://img.shields.io/badge/Developer-CodeToday-success.svg?style=flat-square" alt="Developer">
</p>

**Remembrance** is an ultra-lightweight, battery-efficient Android application crafted with a minimal glassmorphic aesthetic. It periodically displays meaningful spiritual and motivational remembrances directly on the screen using floating squircle overlays and Android system notification hooks.

---

## ✨ Features

- **🎨 Minimal Glassmorphic Aesthetic**: Clean frosted-glass surfaces with translucent gradients, subtle specular borders, and high contrast typography.
- **🌐 Native Multi-Language Typography**:
  - **বাংলা (Bengali)**: Native Unicode ligature rendering ensuring complex conjuncts (*যুক্তবর্ণ*) never break. Defaults to **Noto Serif Bengali**.
  - **English**: Modern, crisp sans-serif rendering. Defaults to **Roboto**.
  - **العربية (Arabic)**: Full Bidirectional (BiDi) RTL layout. Defaults to **Amiri**.
  - **Top 10 Google Fonts Selection**: Built-in dynamic font switchers for each supported language (Bengali: *Noto Serif Bengali, Noto Sans Bengali, Hind Siliguri, Tiro Bangla, Kalpurush, Galada, Mina, Atma, Anek Bangla, Baloo Da 2*; English: *Roboto, Poppins, Montserrat, Open Sans, Lato, Inter, Playfair Display, Raleway, Merriweather, Oswald*; Arabic: *Amiri, Cairo, Scheherazade New, Noto Naskh Arabic, Tajawal, Almarai, Readex Pro, El Messiri, Lateef, Changa*).
- **⏱️ Precision Timing Controls**:
  - **Display Duration**: Dual slider and numeric input in **seconds** (1s – 120s).
  - **Display Frequency**: Dual slider and numeric input in **minutes** (1m – 360m).
- **📐 5 Screen Positions**:
  - **Top** (5% top margin)
  - **Semi-Top** (20% top margin)
  - **Middle** (Centered)
  - **Semi-Bottom** (20% bottom margin)
  - **Bottom** (5% bottom margin)
- **🛡️ Collision-Free Scheduling Engine**:
  - Add multiple reminder boxes with independent timers, fonts, and positions.
  - An intelligent queue coordinator prevents overlapping texts—simultaneous triggers are smoothly staggered with automatic safety buffers.
- **🟢 Signature Squircle Overlay**:
  - **Default Design**: Squircle box with a thin black border (`#000000`), filled with lime green (`#32CD32`) at **65% transparency**, with pure black text (`#000000`).
  - **Full Customizability**: Users can customize background opacity, border stroke, corner radius, and colors for individual reminders.
- **⚡ Ultra-Low Memory & Battery Profile**:
  - Idle RAM target under 15–20 MB.
  - Zero continuous wake-locks; leverages coroutines and alarm manager sleeping.
- **🔄 System Integration & Auto-Run**:
  - First-run onboarding permission dialog for `SYSTEM_ALERT_WINDOW` (Draw over apps) and `POST_NOTIFICATIONS`.
  - Persistent Foreground Service notification hook showing active status.
  - Device boot receiver (`RECEIVE_BOOT_COMPLETED`) to automatically resume the service after a phone restart.
- **🚫 Zero Settings Clutter**:
  - Direct inline card controls without hidden sub-menus or complicated configuration trees.
- **👨‍💻 About Developer**:
  - Developed with care by **CodeToday**.
  - Quick-access button to [Visit us on Facebook](https://www.facebook.com/people/CodeToday/61592160592518/).

---

## 🏗️ Project Architecture

```
Remembrance/
├── .github/workflows/android.yml     # Automated CI for tests and APK builds
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml       # Permissions, Foreground Service & Receivers
│   │   ├── java/com/codetoday/remembrance/
│   │   │   ├── RemembranceApp.kt     # App instance, notification channel init
│   │   │   ├── data/
│   │   │   │   ├── db/               # Room Database, Converters & DAO
│   │   │   │   ├── model/            # ReminderItem entity, Language & Position enums
│   │   │   │   └── repository/       # ReminderRepository
│   │   │   ├── domain/
│   │   │   │   ├── typography/       # Google Fonts catalog & providers
│   │   │   │   └── scheduler/        # Collision-Free scheduler coordinator
│   │   │   ├── service/
│   │   │   │   ├── RemembranceForegroundService.kt  # Persistent service engine
│   │   │   │   ├── OverlayWindowManager.kt          # Floating window overlay manager
│   │   │   │   └── BootCompletedReceiver.kt         # Autorun after device boot
│   │   │   └── ui/
│   │   │       ├── MainActivity.kt                  # Single Activity entry point
│   │   │       ├── home/                            # HomeScreen, ReminderCard, ViewModel
│   │   │       ├── about/                           # AboutDialog (CodeToday & Facebook link)
│   │   │       ├── permission/                      # First-run permission gate
│   │   │       ├── components/                      # Squircle live preview
│   │   │       └── theme/                           # Color, Theme & Glassmorphic modifiers
│   │   └── res/                                     # String, color, theme & cert resources
│   └── build.gradle.kts                             # Module dependencies & SDK config
├── gradle/libs.versions.toml                        # Version catalog
├── settings.gradle.kts                              # Project settings
└── build.gradle.kts                                 # Root build configuration
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer.
- **JDK 17** installed and configured in your environment.
- Android Device or Emulator running **Android 7.0 (API 24)** or higher.

### Build and Run
```bash
# Clone the repository
git clone https://github.com/eabgitbd/Remembrance.git
cd Remembrance

# Run unit tests
./gradlew test

# Assemble debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🔒 Permissions Used

| Permission | Purpose |
| :--- | :--- |
| `SYSTEM_ALERT_WINDOW` | Required to float squircle remembrance overlays on top of other applications. |
| `FOREGROUND_SERVICE` | Keeps the background scheduling engine active and reliable. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Complies with Android 14+ (API 34+) foreground service policy. |
| `POST_NOTIFICATIONS` | Displays the persistent status notification hook indicating active remembrance. |
| `RECEIVE_BOOT_COMPLETED` | Automatically restarts active reminders when the device reboots. |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Prevents OEM task killers from prematurely ending the reminder service. |

---

## 🤝 Connect with Us

- **Developer:** **CodeToday**
- **Facebook:** [Visit CodeToday on Facebook](https://www.facebook.com/people/CodeToday/61592160592518/)

---

## 📄 License

This project is licensed under the [Apache License 2.0](LICENSE).
