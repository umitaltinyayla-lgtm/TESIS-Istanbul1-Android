package tr.org.tesis.istanbulbir;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends Activity {
    public static final String HOMEPAGE = "https://ist1tes-is.com.tr/";
    public static final String EXTRA_URL = "open_url";
    private static final String TOPIC = "tesis_istanbul1_duyuru";
    private static final int TESIS_BLUE = Color.rgb(16, 57, 95);
    private WebView webView;
    private ProgressBar progress;

    public static boolean isTrustedSite(String value) {
        if (value == null || value.trim().isEmpty()) return false;
        try {
            Uri uri = Uri.parse(value);
            String host = uri.getHost();
            return "https".equalsIgnoreCase(uri.getScheme())
                    && ("ist1tes-is.com.tr".equalsIgnoreCase(host)
                    || "www.ist1tes-is.com.tr".equalsIgnoreCase(host));
        } catch (Exception e) {
            return false;
        }
    }

    public static void subscribeToAnnouncements(Context context) {
        if (FirebaseApp.getApps(context).isEmpty()) return;
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)
                .addOnFailureListener(e -> Log.w("TESIS-FCM", "Duyuru aboneligi basarisiz", e));
    }

    private static String getNotificationUrl(Intent intent) {
        String url = intent == null ? null : intent.getStringExtra(EXTRA_URL);
        return isTrustedSite(url) ? url : HOMEPAGE;
    }

    /** Telefonlarda delik/kamera/centik alanini da kapsayan sabit lacivert durum cubugu. */
    private void configureSystemBars() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        int visibility = window.getDecorView().getSystemUiVisibility();
        visibility &= ~(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        window.getDecorView().setSystemUiVisibility(visibility);
        if (Build.VERSION.SDK_INT >= 30) {
            // Android 15+'ta sistem cubuklari transparent olabilir; arka plan ayni mavi renktir.
            window.setDecorFitsSystemWindows(false);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS);
            }
        } else {
            window.setStatusBarColor(TESIS_BLUE);
            window.setNavigationBarColor(TESIS_BLUE);
        }
        if (Build.VERSION.SDK_INT >= 29) {
            window.setStatusBarContrastEnforced(false);
            window.setNavigationBarContrastEnforced(false);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(TESIS_BLUE);
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.WHITE);
        root.addView(layout, new FrameLayout.LayoutParams(-1, -1));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        layout.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        layout.addView(progress, new FrameLayout.LayoutParams(-1, 6, Gravity.TOP));
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) layout.getLayoutParams();
                if (lp.topMargin != bars.top || lp.bottomMargin != bars.bottom
                        || lp.leftMargin != bars.left || lp.rightMargin != bars.right) {
                    lp.topMargin = bars.top;
                    lp.bottomMargin = bars.bottom;
                    lp.leftMargin = bars.left;
                    lp.rightMargin = bars.right;
                    layout.setLayoutParams(lp);
                }
                return insets;
            });
            root.requestApplyInsets();
        }

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request.isForMainFrame() && openExternalWhenNeeded(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternalWhenNeeded(Uri.parse(url));
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            String opened = getNotificationUrl(getIntent());
            if (!HOMEPAGE.equals(opened)) webView.loadUrl(opened);
        } else {
            webView.loadUrl(getNotificationUrl(getIntent()));
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 314);
        }
        subscribeToAnnouncements(this);
    }

    private boolean openExternalWhenNeeded(Uri uri) {
        if (uri == null) return true;
        String path = uri.getPath();
        if (isTrustedSite(uri.toString()) && (path == null || !path.toLowerCase().endsWith(".pdf"))) return false;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Bu bağlantıyı açacak bir uygulama bulunamadı.", Toast.LENGTH_LONG).show();
        }
        return true;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (webView != null && isTrustedSite(intent.getStringExtra(EXTRA_URL))) {
            webView.loadUrl(getNotificationUrl(intent));
        }
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (webView != null) webView.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onDestroy() {
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
