# Monthly Expense Tracker — fresh Android project

A small Android app with a dark spreadsheet-style expense tracker. The app loads a bundled local HTML interface in Android WebView; it does not need network access and declares no Internet permission. Expense records are saved locally in WebView local storage on the device.

## Features
- Monthly total, transaction count, average, and top category
- Spreadsheet-style rows with date, category, description, payment method, amount
- Add, edit, delete, search, and category filtering
- CSV import and export/share; JSON backup
- No account, no server, and no internet permission

## Build the APK with GitHub (no Android Studio required)
1. Create a new GitHub repository (or a clean branch/repository).
2. Upload the **contents** of this folder so `settings.gradle`, `build.gradle`, `app/`, and `.github/` are at the repository root. Do not upload the outer folder as a nested directory.
3. Open **Actions**. The workflow **Build Android APK** should appear. If prompted, enable GitHub Actions.
4. Select the workflow and click **Run workflow** on `main`.
5. Open the successful run and download `MonthlyExpenseTracker-debug-apk` under Artifacts. Extract the ZIP and install `app-debug.apk` on your Android phone.

The GitHub runner needs internet to download build dependencies while compiling. The installed app itself does not request internet permission and is designed to work offline. App data remains on the device; export CSV/JSON backups before uninstalling or clearing app data.

## Project structure
- `app/` Android application module
- `app/src/main/java/.../MainActivity.java` WebView host and CSV share bridge
- `app/src/main/assets/index.html` offline interface and expense-sheet logic
- `.github/workflows/build-apk.yml` GitHub Actions build workflow
