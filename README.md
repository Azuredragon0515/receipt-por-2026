# receipt-por-2026 · On‑Site Check‑in and Receipt Assistant (Android · Kotlin · Jetpack Compose)

[![Android CI](https://img.shields.io/badge/Android%20CI-passing-brightgreen)](https://github.com/Azuredragon0515/receipt-por-2026/actions)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A lightweight, offline‑first Android app that helps event/market organizers do on‑site check‑in and manage digital receipts. It uses on‑device text recognition (ML Kit), Room for local storage, biometric gating for sensitive actions, and minimal web API integration for contacts.

- Repo: https://github.com/Azuredragon0515/receipt-por-2026  
- Latest green CI run: https://github.com/Azuredragon0515/receipt-por-2026/actions/runs/21060892028  
- Suggested tag for demo: `v0.1-cw2-demo` (see Releases)

## Features
- Receipts
  - Pick from gallery or capture image; ML Kit Text Recognition extracts header/date/total
  - Editable fields before saving; optional save original image
- Check‑in
  - Fused Location Provider (5s interval, 2s fastest) for on‑site verification
  - Biometric‑guarded JSON export; share/export to files
- Contacts (web API demo)
  - Fetch list, simple create/delete against a demo endpoint
- Sensors & Security
  - Accelerometer “shake‑to‑add” record (toggle in Settings)
  - BiometricPrompt for sensitive actions with numeric fallback
- Offline‑first data model
  - Room (SQLite) for records and contacts, SharedPreferences for settings
  - JSON export for audit/sharing; minimal networking (Volley/HTTP)

## Tech stack
- Language: Kotlin
- UI: Jetpack Compose
- Architecture: ViewModel + StateFlow + Repository + Room (Entities/DAO, compile‑time SQL checks)
- ML/AI: ML Kit Text Recognition; optional Image Labeling
- Sensors & OS: Biometric, Fused Location, SensorManager (accelerometer)
- Networking: Volley / simple HTTP endpoint
- Build: Gradle (AGP), GitHub Actions Android CI

## Screens (emulator)
<!-- 可替换为你仓库中的图 paths，如 docs/screens/*.png -->
- Receipts list, Contacts list, Check‑in (biometric export preview), Settings (API base URL, radius, toggles), Scan page

## Build & Run
1. Requirements: Android Studio Giraffe+ (or newer), JDK 17, minSdk 24
2. Clone: `git clone https://github.com/Azuredragon0515/receipt-por-2026`
3. Open the project root in Android Studio，等待 Gradle 同步完成
4. Select “app” configuration → Run on emulator or a physical device

## Settings
- API Base URL: default `https://jsonplaceholder.typicode.com/`
- Check‑in radius (meters)
- Save original image: on/off
- Enable shake to add: on/off

## Permissions & Privacy
- Camera/Photos: receipt image selection/capture and on‑device OCR
- Location: on‑site check‑in (interval 5s; fastest 2s; only while relevant screens are visible)
- Biometric: gate export/delete
- Storage/Share: JSON export and share
- Data minimization: by default only structured OCR fields are stored; raw images optional. Exported files exclude API tokens and sensitive secrets.

## Demo video (to be added)
A 10‑minute screencast will cover: OCR→save, check‑in→biometric JSON export/share, contacts API fetch/delete, settings toggles, shake‑to‑add, and code walkthrough (Room/Repository, OCR pipeline, biometric, JSON export).

## Testing & CI
- GitHub Actions: Android CI on push (build ~30s)
- Unit tests: lightweight JVM tests for JSON export formatting and shake detection logic (no device needed)

## Roadmap
- Improve real‑device UI layout, finalize contacts create flow
- Optional: Firestore/Storage/FCM integration (production path), HTTPS/reverse proxy
- Instrumented tests and sensors stress tests

## License
MIT
