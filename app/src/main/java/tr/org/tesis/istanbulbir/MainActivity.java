package tr.org.tesis.istanbulbir;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Insets;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
    private FrameLayout splashOverlay;
    private final Handler splashHandler = new Handler(Looper.getMainLooper());
    private boolean splashDismissed = false;

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


    /** Logo internetten gelene kadar telefonda paketli TES-IS logosunu goster. */
    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showNativeSplash(FrameLayout root) {
        splashOverlay = new FrameLayout(this);
        splashOverlay.setBackgroundColor(TESIS_BLUE);
        root.addView(splashOverlay, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout center = new LinearLayout(this);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        // Kayip "ŞUBE" sorununun nedeni: ic kap 156dp logoya gore dar olculuyordu.
        // Tum ekran genisligini kullan, uzun alt basligi kirpma.
        FrameLayout.LayoutParams centerParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        centerParams.leftMargin = dp(20);
        centerParams.rightMargin = dp(20);
        splashOverlay.addView(center, centerParams);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tes_is_gercek_logo);
        logo.setContentDescription("TES-İŞ Sendikası Logosu");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setAdjustViewBounds(true);
        logo.setFilterBitmap(true);
        GradientDrawable whiteCircle = new GradientDrawable();
        whiteCircle.setShape(GradientDrawable.OVAL);
        whiteCircle.setColor(Color.WHITE);
        logo.setBackground(whiteCircle);
        logo.setPadding(dp(13), dp(13), dp(13), dp(13));
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(156), dp(156));
        logoParams.gravity = Gravity.CENTER_HORIZONTAL;
        center.addView(logo, logoParams);

        TextView title = new TextView(this);
        title.setText("TES-İŞ");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(-1, -2);
        titleParams.topMargin = dp(23);
        center.addView(title, titleParams);

        TextView subtitle = new TextView(this);
        subtitle.setText("İSTANBUL 1 NO'LU ŞUBE");
        subtitle.setTextColor(Color.rgb(236, 246, 252));
        subtitle.setTextSize(14);
        subtitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        subtitle.setLetterSpacing(0.035f);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setSingleLine(false);
        subtitle.setMaxLines(2);
        subtitle.setIncludeFontPadding(true);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(6);
        center.addView(subtitle, subtitleParams);

        // Cevapsiz ag istekleri acilis ekranini sonsuza kadar kilitlemesin.
        splashHandler.postDelayed(this::dismissNativeSplash, 9000);
    }

    private void waitForSiteLogos(WebView view, int tries) {
        if (splashDismissed || splashOverlay == null || webView == null) return;
        view.evaluateJavascript(
                "(function(){var els=document.querySelectorAll('.brand img,.hero-logo img');"
              + "return document.readyState==='complete' && els.length>0 && "
              + "Array.prototype.every.call(els,function(i){return i.complete && i.naturalWidth>0;});})()",
                result -> {
                    if (splashDismissed || splashOverlay == null) return;
                    if ("true".equals(result) || tries >= 35) {
                        // DOM ve logonun ilk karesi ekrana cizilsin.
                        splashHandler.postDelayed(this::dismissNativeSplash, 130);
                    } else {
                        splashHandler.postDelayed(() -> waitForSiteLogos(view, tries + 1), 120);
                    }
                });
    }

    private void dismissNativeSplash() {
        if (splashDismissed || splashOverlay == null) return;
        splashDismissed = true;
        FrameLayout overlay = splashOverlay;
        splashOverlay = null;
        overlay.animate().alpha(0f).setDuration(180).withEndAction(() -> {
            if (overlay.getParent() instanceof FrameLayout) {
                ((FrameLayout) overlay.getParent()).removeView(overlay);
            }
        }).start();
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
        showNativeSplash(root);
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
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (!splashDismissed) waitForSiteLogos(view, 0);
            }
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
        splashHandler.removeCallbacksAndMessages(null);
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
