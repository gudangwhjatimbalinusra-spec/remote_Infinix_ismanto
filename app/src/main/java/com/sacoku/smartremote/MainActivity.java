package com.sacoku.smartremote;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;

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
                return "ERROR: Layanan ConsumerIrManager tidak tersedia.";
            }

            try {
                if (!irManager.hasIrEmitter()) {
                    return "ERROR: Android tidak mendeteksi pemancar IR.";
                }
                return "READY: Android mendeteksi dukungan pemancar IR. Pancaran fisik dan respons AC belum teruji.";
            } catch (Exception e) {
                return "ERROR: Pemeriksaan IR gagal: " + safeMessage(e);
            }
        }

        @JavascriptInterface
        public String transmitIR(int frequency, String patternStr) {
            if (irManager == null) {
                return "ERROR: Layanan ConsumerIrManager tidak tersedia.";
            }

            try {
                if (!irManager.hasIrEmitter()) {
                    return "ERROR: Pemancar IR tidak terdeteksi Android.";
                }

                if (frequency < 1 || frequency > 1000000) {
                    return "ERROR: Frekuensi tidak valid: " + frequency;
                }
                if (patternStr == null || patternStr.trim().isEmpty()) {
                    return "ERROR: Pola pulsa kosong.";
                }

                String[] parts = patternStr.split(",");
                if (parts.length < 2) {
                    return "ERROR: Pola pulsa terlalu pendek.";
                }

                // Data harus berupa durasi positif. Jika jumlahnya ganjil,
                // tambahkan jeda akhir (space/off) agar pasangan mark/space lengkap.
                int extra = parts.length % 2;
                int[] pattern = new int[parts.length + extra];

                for (int i = 0; i < parts.length; i++) {
                    pattern[i] = Integer.parseInt(parts[i].trim());
                    if (pattern[i] <= 0) {
                        return "ERROR: Durasi pulsa ke-" + (i + 1) + " tidak valid.";
                    }
                }

                if (extra == 1) {
                    pattern[pattern.length - 1] = 10000;
                }

                irManager.transmit(frequency, pattern);

                String note = extra == 1
                        ? " Jeda akhir 10000 us ditambahkan otomatis."
                        : "";

                return "OK: Android menerima pemanggilan transmit() pada " +
                        frequency + " Hz dengan " + pattern.length +
                        " durasi." + note +
                        " Ini belum membuktikan LED IR memancar atau AC menerima perintah.";
            } catch (NumberFormatException e) {
                return "ERROR: Pola pulsa berisi angka yang tidak valid.";
            } catch (Exception e) {
                return "ERROR: Android gagal memproses IR: " + safeMessage(e);
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
