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
        public String getIrStatus() {
            if (irManager == null) {
                return "ERROR: Layanan ConsumerIrManager tidak tersedia di Android.";
            }

            try {
                if (!irManager.hasIrEmitter()) {
                    return "ERROR: Android tidak mendeteksi pemancar IR pada perangkat ini.";
                }
                return "READY: Android mendeteksi dukungan pemancar IR.";
            } catch (Exception e) {
                return "ERROR: Gagal memeriksa IR: " + safeMessage(e);
            }
        }

        @JavascriptInterface
        public String transmitIR(int frequency, String patternStr) {
            if (irManager == null) {
                return "ERROR: Layanan ConsumerIrManager tidak tersedia.";
            }

            try {
                if (!irManager.hasIrEmitter()) {
                    return "ERROR: Pemancar IR tidak terdeteksi oleh Android.";
                }

                if (frequency < 1 || frequency > 1000000) {
                    return "ERROR: Frekuensi IR tidak valid: " + frequency;
                }

                if (patternStr == null || patternStr.trim().isEmpty()) {
                    return "ERROR: Data pulsa IR kosong.";
                }

                String[] parts = patternStr.split(",");
                if (parts.length < 2 || parts.length % 2 != 0) {
                    return "ERROR: Pola pulsa harus berisi pasangan mark/space.";
                }

                int[] pattern = new int[parts.length];
                for (int i = 0; i < parts.length; i++) {
                    pattern[i] = Integer.parseInt(parts[i].trim());
                    if (pattern[i] <= 0) {
                        return "ERROR: Durasi pulsa ke-" + (i + 1) + " tidak valid.";
                    }
                }

                irManager.transmit(frequency, pattern);

                // Android API tidak memberikan konfirmasi optik bahwa LED IR benar-benar
                // memancarkan sinyal atau bahwa AC menerima perintah.
                return "OK: Perintah transmit() diterima Android (" +
                        frequency + " Hz, " + pattern.length + " durasi). Respons AC belum terkonfirmasi.";
            } catch (NumberFormatException e) {
                return "ERROR: Data pulsa berisi angka yang tidak valid.";
            } catch (Exception e) {
                return "ERROR: Pengiriman IR gagal: " + safeMessage(e);
            }
        }

        private String safeMessage(Exception e) {
            String message = e.getMessage();
            return (message == null || message.trim().isEmpty())
                    ? e.getClass().getSimpleName()
                    : message;
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
