# WingeD PDF Reader

A modern, fast, and feature-rich Android PDF reader app built with Kotlin, Jetpack libraries, AndroidX PDF Viewer, and PDFBox for Android.
# This project is inspired by a bookmark gifted by a friend <3
---

## Features

- **High-Performance PDF Viewer**: Smooth scrolling, page rendering, and text selection using the AndroidX `pdf-viewer-fragment`.
- **Smart Index & Table of Contents (TOC)**:
  - Parses native PDF outlines and headings into a slide-out navigation drawer.
  - Automatically builds an index for documents without a built-in table of contents.
  - Highlights active chapters dynamically as you scroll.
- **Text Highlighting**:
  - Contextual text selection menu to highlight passages in yellow, green, blue, or pink.
  - Persistent highlight storage so annotations remain saved across sessions.
- **Bookmarks & Reading Progress**:
  - One-tap page bookmarking with visual corner indicators.
  - Auto-saves and restores your last reading position when reopening files.
- **Dark Mode & PDF Inversion**:
  - Supports System Default, Light, and Dark themes.
  - Inverts document colors in Dark Mode for comfortable night reading.
- **Clean Responsive UI**:
  - Edge-to-edge layout support.
  - Floating controls, gesture-enabled navigation, and real-time page indicator.

---

## Tech Stack & Architecture

- **Language**: Kotlin
- **Min SDK**: 28 (Android 9.0)
- **Target SDK**: 37
- **UI Components**: AndroidX AppCompat, Material Components, Compose, DrawerLayout
- **PDF Libraries**:
  - `androidx.pdf:pdf-viewer-fragment`
  - `com.tom-roush:pdfbox-android`

---

## Project Structure

```
PDFReader/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/pdfreader/
│   │   │   │   ├── MainActivity.kt        # Primary activity, UI handling & gestures
│   │   │   │   ├── ReaderPdfFragment.kt   # PDF fragment host & viewport callbacks
│   │   │   │   ├── IndexBuilder.kt        # TOC parsing & automatic index builder
│   │   │   │   ├── HighlightStorage.kt    # Serialization for text highlights
│   │   │   │   └── PdfHelper.kt           # Local document URI management
│   │   │   ├── res/                       # Layouts, drawables, themes, strings
│   │   │   └── AndroidManifest.xml
│   │   └── test/                          # Unit tests
│   └── build.gradle.kts                   # Module build configuration
├── build.gradle.kts                       # Root build configuration
├── settings.gradle.kts                    # Gradle settings
└── README.md                              # Documentation
```

---

## Getting Started

### Prerequisites

- **Android Studio** (Ladybug or newer recommended)
- **JDK**: 11 or higher
- **Android SDK**: API 37 (Compile/Target SDK)

### Building and Running

1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd PDFReader2
   ```

2. **Open in Android Studio**:
   - Launch Android Studio and open the `PDFReader2` project directory.
   - Allow Gradle to sync dependencies.

3. **Run on Device or Emulator**:
   - Select an emulator or connected device running Android 9.0 (API 28) or higher.
   - Click **Run** (`Shift + F10`) or execute:
     ```bash
     ./gradlew assembleDebug
     ```

---

## License

Distributed under the MIT License. See `LICENSE` for more information if applicable.

