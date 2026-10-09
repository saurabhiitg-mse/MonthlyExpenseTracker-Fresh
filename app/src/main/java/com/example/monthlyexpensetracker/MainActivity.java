package com.example.monthlyexpensetracker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.print.PrintManager;
import android.print.PrintAttributes;
import android.text.InputType;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class MainActivity extends Activity {
    private WebView webView;
    private boolean unlocked = false;
    private boolean promptShowing = false;
    private boolean shouldRelock = false;
    private boolean firstResume = true;
    private SharedPreferences lockPrefs;
    private AlertDialog pinDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(0xFF101411);
        getWindow().setNavigationBarColor(0xFF101411);
        lockPrefs = getSharedPreferences("app_lock", MODE_PRIVATE);
        webView = new WebView(this);
        webView.setBackgroundColor(0xFF101411);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAllowContentAccess(false);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new AppBridge(), "ExpenseApp");
        webView.setVisibility(View.INVISIBLE);
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
        showPinDialog();
    }

    private void showPinDialog() {
        if (promptShowing || isFinishing()) return;
        promptShowing = true;
        String savedHash = lockPrefs.getString("pin_hash", null);
        boolean setup = savedHash == null;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int)(20 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad / 2, pad, 0);
        EditText pin = new EditText(this);
        pin.setSingleLine(true);
        pin.setHint(setup ? "Create a 4–12 digit PIN" : "Enter your PIN");
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(pin);
        EditText confirm = null;
        if (setup) {
            confirm = new EditText(this);
            confirm.setSingleLine(true);
            confirm.setHint("Confirm PIN");
            confirm.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            layout.addView(confirm);
        }
        final EditText confirmField = confirm;
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(setup ? "Create app PIN" : "Unlock Monthly Expense Tracker")
                .setMessage(setup ? "Choose a PIN to protect your expenses." : "Enter your PIN to continue.")
                .setView(layout)
                .setCancelable(false)
                .setPositiveButton(setup ? "Save PIN" : "Unlock", null)
                .create();
        pinDialog = dialog;
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String entered = pin.getText().toString();
            if (entered.length() < 4 || entered.length() > 12) {
                pin.setError("PIN must contain 4–12 digits"); return;
            }
            if (setup) {
                String repeated = confirmField.getText().toString();
                if (!entered.equals(repeated)) { confirmField.setError("PINs do not match"); return; }
                lockPrefs.edit().putString("pin_hash", sha256(entered)).apply();
                unlockApp();
            } else if (constantTimeEquals(savedHash, sha256(entered))) {
                unlockApp();
            } else {
                pin.setText(""); pin.setError("Incorrect PIN");
            }
        }));
        dialog.setOnDismissListener(ignored -> promptShowing = false);
        dialog.show();
    }

    private void unlockApp() {
        unlocked = true;
        shouldRelock = false;
        promptShowing = false;
        if (webView != null) webView.setVisibility(View.VISIBLE);
        if (pinDialog != null && pinDialog.isShowing()) pinDialog.dismiss();
    }

    private String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : bytes) out.append(String.format(java.util.Locale.US, "%02x", b & 0xff));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }

    private boolean constantTimeEquals(String a, String b) {
        return a != null && MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }

    @Override protected void onPause() {
        super.onPause();
        if (unlocked && !promptShowing && !isFinishing()) shouldRelock = true;
    }

    @Override protected void onResume() {
        super.onResume();
        if (!firstResume && shouldRelock && !promptShowing) { unlocked = false; if (webView != null) webView.setVisibility(View.INVISIBLE); showPinDialog(); }
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
