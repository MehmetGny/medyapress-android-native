package press.medya.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.content.Intent;
import android.net.Uri;

public class MainActivity extends Activity {

    private static final String SITE_URL = "https://medya.press/";
    private static final long AUTO_REFRESH_MS = 120000; // 2 dakika

    private WebView webView;
    private Handler refreshHandler;
    private long lastRefreshTime = 0;

    private final Runnable autoRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            refreshHomeIfNeeded();
            refreshHandler.postDelayed(this, AUTO_REFRESH_MS);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setupWindow();
        setupWebView();

        refreshHandler = new Handler(Looper.getMainLooper());

        webView.loadUrl(SITE_URL);
        lastRefreshTime = System.currentTimeMillis();
    }

    private void setupWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.WHITE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.getDecorView().setSystemUiVisibility(
                    android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            );
        }
    }

    private void setupWebView() {
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);

        setContentView(webView);

        WebSettings settings = webView.getSettings();

        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadsImagesAutomatically(true);

        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(false);

        settings.setMediaPlaybackRequiresUserGesture(false);

        // Yeni haberleri daha hızlı görmek için cache minimumda tutulur.
        settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        }

        webView.clearCache(false);
        webView.clearHistory();

        webView.setWebChromeClient(new WebChromeClient());

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                return handleUrl(url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(url);
            }

            private boolean handleUrl(String url) {
                if (url == null) {
                    return false;
                }

                if (url.startsWith("https://medya.press") || url.startsWith("http://medya.press")) {
                    webView.loadUrl(url);
                    return true;
                }

                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(intent);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
        });
    }

    private void refreshHomeIfNeeded() {
        if (webView == null) {
            return;
        }

        String currentUrl = webView.getUrl();

        if (currentUrl == null) {
            return;
        }

        boolean isHome =
                currentUrl.equals("https://medya.press/")
                        || currentUrl.equals("https://medya.press")
                        || currentUrl.equals("http://medya.press/")
                        || currentUrl.equals("http://medya.press");

        if (isHome) {
            webView.reload();
            lastRefreshTime = System.currentTimeMillis();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(autoRefreshRunnable);
            refreshHandler.postDelayed(autoRefreshRunnable, AUTO_REFRESH_MS);
        }

        long now = System.currentTimeMillis();

        // Uygulama arka plandan dönünce 1 dakikadan fazla geçmişse ana sayfayı tazeler.
        if (now - lastRefreshTime > 60000) {
            refreshHomeIfNeeded();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(autoRefreshRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        if (refreshHandler != null) {
            refreshHandler.removeCallbacks(autoRefreshRunnable);
        }

        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
            return;
        }

        super.onBackPressed();
    }
}
