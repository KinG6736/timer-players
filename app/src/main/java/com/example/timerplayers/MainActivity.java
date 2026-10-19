package com.example.timerplayers;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 1001;
    private ValueCallback<Uri[]> fileCallback;
    private WebView web;
    private String pendingAuthUrl;
    private boolean pageLoaded = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        pendingAuthUrl = getIntent() != null && getIntent().getData() != null
            ? getIntent().getData().toString() : null;

        web = new WebView(this);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.setBackgroundColor(0xFF071321);
        web.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                pageLoaded = true;
                deliverPendingAuth();
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(
                        Intent.createChooser(intent, "Выбрать фото"),
                        PICK_IMAGE
                    );
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                    return false;
                }
            }
        });

        setContentView(web);
        web.loadUrl("file:///android_asset/index.html");
    }

    private class AndroidBridge {
        @JavascriptInterface
        public void openExternalUrl(String address) {
            runOnUiThread(() -> {
                try {
                    Uri uri = Uri.parse(address);
                    if (!"https".equalsIgnoreCase(uri.getScheme())) return;
                    Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                    startActivity(intent);
                } catch (Exception e) {
                    android.util.Log.e("TimerPlayers", "Cannot open login URL", e);
                }
            });
        }
    }

    private void deliverPendingAuth() {
        if (!pageLoaded || web == null || pendingAuthUrl == null) return;
        String url = pendingAuthUrl;
        pendingAuthUrl = null;
        String quoted = org.json.JSONObject.quote(url);
        web.evaluateJavascript(
            "if(window.handleAuthCallback) window.handleAuthCallback(" + quoted + ");",
            null
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri data = intent != null ? intent.getData() : null;
        if (data != null &&
            "com.example.timerplayers".equals(data.getScheme()) &&
            "auth".equals(data.getHost()) &&
            "/callback".equals(data.getPath())) {
            pendingAuthUrl = data.toString();
            deliverPendingAuth();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && fileCallback != null) {
            Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}
