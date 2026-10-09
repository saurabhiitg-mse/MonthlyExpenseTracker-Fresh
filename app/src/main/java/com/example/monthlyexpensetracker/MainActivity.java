package com.example.monthlyexpensetracker;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;

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
    }

    private class AppBridge {
        @JavascriptInterface
        public void exportCsv(String csv) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/csv");
                send.putExtra(Intent.EXTRA_SUBJECT, "Monthly Expense Tracker export.csv");
                send.putExtra(Intent.EXTRA_TEXT, csv);
                startActivity(Intent.createChooser(send, "Export expense sheet"));
            });
        }

        @JavascriptInterface
        public void notifyMessage(String message) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
