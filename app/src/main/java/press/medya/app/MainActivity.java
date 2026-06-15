package press.medya.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.content.Intent;
import android.net.Uri;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

public class MainActivity extends Activity {

```
private static final String SITE_URL = "https://medya.press/";
private static final long AUTO_REFRESH_MS = 120000; // 2 dakika

private WebView webView;
private ProgressBar progressBar;
private Handler refreshHandler;
private long lastRefreshTime = 0;

private final Runnable autoRefreshRunnable = new Runnable() {
    @Override
    public void run() {
        refreshHomeIfNeeded();

        if (refreshHandler != null) {
            refreshHandler.postDelayed(this, AUTO_REFRESH_MS);
        }
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
    window.setNavigationBarColor(Color.WHITE);

    int flags = 0;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        flags = flags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        flags = flags | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
    }

    window.getDecorView().setSystemUiVisibility(flags);
}

private void setupWebView() {
    FrameLayout root = new FrameLayout(this);
    root.setBackgroundColor(Color.WHITE);

    // Telefon saat / batarya alanı sitenin üstüne binmesin diye güvenli boşluk.
    root.setPadding(0, getStatusBarHeight(), 0, 0);

    webView = new WebView(this);
    webView.setBackgroundColor(Color.WHITE);
    webView.setInitialScale(100);

    progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
    progressBar.setMax(100);
    progressBar.setVisibility(View.VISIBLE);

    FrameLayout.LayoutParams webParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
    );

    FrameLayout.LayoutParams progressParams = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            6
    );

    root.addView(webView, webParams);
    root.addView(progressBar, progressParams);

    setContentView(root);

    WebSettings settings = webView.getSettings();

    settings.setJavaScriptEnabled(true);
    settings.setDomStorageEnabled(true);
    settings.setDatabaseEnabled(true);
    settings.setLoadsImagesAutomatically(true);

    // Bu görünüm, hoşuna giden gazete/masaüstüye yakın ana sayfa düzenini korur.
    settings.setUseWideViewPort(false);
    settings.setLoadWithOverviewMode(false);

    // Haber içi yazılar küçük kalmasın diye genel WebView yazı ölçeği biraz büyütüldü.
    settings.setTextZoom(115);

    settings.setBuiltInZoomControls(false);
    settings.setDisplayZoomControls(false);
    settings.setSupportZoom(false);

    settings.setMediaPlaybackRequiresUserGesture(false);
    settings.setCacheMode(WebSettings.LOAD_NO_CACHE);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        settings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
    }

    webView.clearCache(false);

    webView.setWebChromeClient(new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int progress) {
            if (progressBar != null) {
                progressBar.setProgress(progress);

                if (progress >= 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        }
    });

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

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            keepLayoutAndImproveArticleReading(view);
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

private void keepLayoutAndImproveArticleReading(WebView view) {
    if (view == null) {
        return;
    }

    String js =
            "(function() {" +
                    "var currentUrl = window.location.href || '';" +

                    "document.documentElement.style.maxWidth = '100%';" +
                    "document.documentElement.style.overflowX = 'hidden';" +

                    "if (document.body) {" +
                    "document.body.style.maxWidth = '100%';" +
                    "document.body.style.overflowX = 'hidden';" +
                    "}" +

                    "var isHome = currentUrl === 'https://medya.press/' || currentUrl === 'https://medya.press';" +
                    "var isSingle = document.body && (" +
                    "document.body.className.indexOf('single') !== -1 || " +
                    "document.body.className.indexOf('post-template') !== -1" +
                    ");" +

                    "if (!isHome && isSingle) {" +
                    "var style = document.getElementById('mp-app-reading-style');" +
                    "if (!style) {" +
                    "style = document.createElement('style');" +
                    "style.id = 'mp-app-reading-style';" +
                    "style.innerHTML = " +
                    "'.entry-content p, .post-content p, .s-post-content p, .the-post p, article p {' +" +
                    "'font-size: 20px !important;' +" +
                    "'line-height: 1.75 !important;' +" +
                    "'letter-spacing: 0 !important;' +" +
                    "}' +" +
                    "'.entry-content li, .post-content li, .s-post-content li, .the-post li, article li {' +" +
                    "'font-size: 20px !important;' +" +
                    "'line-height: 1.75 !important;' +" +
                    "}' +" +
                    "'.entry-content, .post-content, .s-post-content, .the-post {' +" +
                    "'max-width: 100% !important;' +" +
                    "'overflow-x: hidden !important;' +" +
                    "}';" +
                    "document.head.appendChild(style);" +
                    "}" +
                    "}" +
                    "})();";

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        view.evaluateJavascript(js, null);
    }
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

private int getStatusBarHeight() {
    int result = 0;
    int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");

    if (resourceId > 0) {
        result = getResources().getDimensionPixelSize(resourceId);
    }

    return result;
}
```

}
