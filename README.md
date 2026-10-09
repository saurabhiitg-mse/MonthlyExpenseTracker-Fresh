# Monthly Expense Tracker — GitHub Actions project

A dark, compact, offline-first Android expense tracker. It stores records locally in the app's WebView storage and includes monthly budget/expenses/remaining cards, add/edit/delete, search/category filter, CSV import/export, JSON backup, and monthly PDF printing/export through Android's print service. The app uses a separate 4–12 digit PIN prompt, stores a SHA-256 PIN hash in app-private preferences, and relocks when the app returns from the background. The interface uses text-only outlined controls with rounded corners; the launcher icon is a dark minimalist rupee/receipt design.

## Build APK without Android Studio
1. Create/open your GitHub repository.
2. Upload the **contents** of this project folder, including the hidden `.github` folder and `.github/workflows/build-apk.yml`, to the repository root. If GitHub's upload page hides `.github`, create/edit `.github/workflows/build-apk.yml` directly in GitHub's file editor.
3. Commit the changes to the `main` branch.
4. Open the repository's **Actions** tab, choose **Build Android APK**, and click **Run workflow** (or wait for the push build).
5. Open the completed green workflow run. Under **Artifacts**, download `MonthlyExpenseTracker-debug-apk` and extract `app-debug.apk`.
6. On your phone, open the APK and allow installation from your browser/file manager if Android asks.

## Updating an installed copy
- Back up your data first using **Backup JSON** and keep the backup somewhere safe.
- Install the new APK over the existing app only if its application ID and signing key match. Do not uninstall the old app first; uninstalling clears local data.
- GitHub Actions builds a debug APK. For updates, the same repository build key/signing identity must be used; GitHub's default debug signing is generally stable for this workflow.

## Data and security notes
- Expense and budget records are stored locally using WebView local storage. They are not synced to Google Sheets or any server.
- Use **Backup JSON** regularly. Uninstalling/clearing app data can delete local records.
- On first launch, create and confirm a 4–12 digit PIN. The app asks for the PIN again when returning from the background. The PIN gate is an app-level deterrent; it does not encrypt the WebView expense database. Back up data before replacing or uninstalling the app.
- PDF export opens Android's print dialog; choose **Save as PDF** to save a PDF locally.
- This project has not been independently security-audited. Avoid storing highly sensitive information until you have tested it on your device.
