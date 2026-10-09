package com.example.monthlyexpensetracker;

import androidx.fragment.app.FragmentActivity;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.provider.Settings;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.concurrent.Executor;

public class MainActivity extends FragmentActivity {
    private WebView webView;
    private boolean unlocked = false;
    private boolean promptShowing = false;
    private boolean shouldRelock = false;
    private boolean firstResume = true;
    private final Executor executor = command -> runOnUiThread(command);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF101411);
        getWindow().setNavigationBarColor(0xFF101411);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF101411);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AppBridge(), "ExpenseApp");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        authenticate();
    }

    private void authenticate() {
        if (promptShowing || isFinishing()) return;
        BiometricManager manager = BiometricManager.from(this);
        int authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
        int result = manager.canAuthenticate(authenticators);
        if (result != BiometricManager.BIOMETRIC_SUCCESS) {
            Toast.makeText(this, "Set up a fingerprint or screen lock in Android Settings to protect this app.", Toast.LENGTH_LONG).show();
            try { startActivity(new Intent(Settings.ACTION_SECURITY_SETTINGS)); } catch (Exception ignored) {}
            // Do not silently claim biometric protection is active. Permit use if the device has no supported lock.
            unlocked = true;
            shouldRelock = false;
            return;
        }
        promptShowing = true;
        BiometricPrompt prompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                promptShowing = false; unlocked = true; shouldRelock = false;
            }
            @Override public void onAuthenticationFailed() {
                Toast.makeText(MainActivity.this, "Fingerprint not recognized. Try again.", Toast.LENGTH_SHORT).show();
            }
            @Override public void onAuthenticationError(int errorCode, CharSequence errString) {
                promptShowing = false;
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON || errorCode == BiometricPrompt.ERROR_CANCELED) {
                    unlocked = false; finish();
                } else {
                    Toast.makeText(MainActivity.this, "Authentication required to open your expenses.", Toast.LENGTH_LONG).show();
                    unlocked = false; finish();
                }
            }
        });
        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Monthly Expense Tracker")
                .setSubtitle("Use your fingerprint or your phone's screen lock")
                .setAllowedAuthenticators(authenticators)
                .build();
        prompt.authenticate(info);
    }

    @Override protected void onPause() {
        super.onPause();
        if (unlocked && !promptShowing && !isFinishing()) shouldRelock = true;
    }

    @Override protected void onResume() {
        super.onResume();
        if (!firstResume && shouldRelock && !promptShowing) { unlocked = false; authenticate(); }
        firstResume = false;
    }

    private class AppBridge {
        @JavascriptInterface public void exportCsv(String csv) {
            runOnUiThread(() -> { Intent send = new Intent(Intent.ACTION_SEND); send.setType("text/csv"); send.putExtra(Intent.EXTRA_SUBJECT, "Monthly Expense Tracker export.csv"); send.putExtra(Intent.EXTRA_TEXT, csv); startActivity(Intent.createChooser(send, "Export expense sheet")); });
        }
        @JavascriptInterface public void exportPdf(String html, String filename) {
            runOnUiThread(() -> { WebView printView = new WebView(MainActivity.this); printView.getSettings().setJavaScriptEnabled(false); printView.getSettings().setAllowFileAccess(false); printView.setWebViewClient(new WebViewClient() {
                @Override public void onPageFinished(WebView view, String url) { PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE); String jobName = filename == null ? "Monthly expenses" : filename.replace(".pdf", ""); android.print.PrintDocumentAdapter adapter = view.createPrintDocumentAdapter(jobName); PrintAttributes attributes = new PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).setResolution(new PrintAttributes.Resolution("pdf", "pdf", 300, 300)).setMinMargins(PrintAttributes.Margins.NO_MARGINS).build(); printManager.print(jobName, adapter, attributes); }
            }); printView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null); });
        }
        @JavascriptInterface public void notifyMessage(String message) { runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show()); }
    }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
