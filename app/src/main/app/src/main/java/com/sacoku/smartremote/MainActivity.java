package com.sacoku.smartremote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private WebView webView;
    private ConsumerIrManager irManager;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);

        webView = new WebView(this);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);

        webView.addJavascriptInterface(new IrBridge(), "Android");
        webView.loadUrl("file:///android_asset/index.html");
    }

    public class IrBridge {
        @JavascriptInterface
        public void transmitIR(int frequency, String patternStr) {
            if (irManager == null || !irManager.hasIrEmitter()) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Sensor IR fisik tidak aktif!", Toast.LENGTH_SHORT).show());
                return;
            }
            try {
                String[] parts = patternStr.split(",");
                int[] pattern = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    pattern[i] = Integer.parseInt(parts[i].trim());
                }
                irManager.transmit(frequency, pattern);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
