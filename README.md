# receipt-por-2026 · On‑Site Check‑in and Receipt Assistant (Android · Kotlin · Jetpack Compose)

A lightweight, offline‑first Android app for on‑site check‑in and digital receipt management.

- Repository: https://github.com/Azuredragon0515/receipt-por-2026
- Actions: https://github.com/Azuredragon0515/receipt-por-2026/actions

## Features

- Receipts
  - Pick or capture an image
  - Edit extracted fields before saving
  - Optional save of the original image
- Check‑in
  - Location‑based on‑site verification
  - Biometric‑guarded export
- Contacts
  - Simple web API demo for list/create/delete
- Data model
  - Local storage via Room
  - JSON export for sharing and audit

## Tech Stack

- Kotlin, Jetpack Compose
- Architecture: ViewModel, StateFlow, Repository, Room
- Networking: simple HTTP endpoint
- Build: Gradle, GitHub Actions Android CI

## Screens

- Receipts list
- Receipt detail and edit
- Check‑in
- Contacts
- Settings

## Build and Run

1. Requirements: Android Studio (Giraffe or newer), JDK 17, minSdk 24  
2. Clone: `git clone https://github.com/Azuredragon0515/receipt-por-2026`  
3. Open the project in Android Studio and wait for Gradle sync  
4. Select the `app` configuration and run on an emulator or device

## Download APK

- Go to Actions → latest green run → Artifacts → `app-debug.apk`

## Settings

- API base URL
- Check‑in radius
- Save original image toggle
- Shake‑to‑add toggle

## Permissions

- Camera/Photos for image selection or capture
- Location for on‑site check‑in
- Biometric for protecting sensitive actions
- Storage/Share for JSON export

## CI

- Android CI runs on every push and pull request to `main` and builds a Debug APK

## Roadmap

- Finalize contacts create flow
- Improve OCR mapping for receipts
- Add unit tests and basic UI tests
- Optional release build and tagging

## License

MIT
