package tr.org.tesis.istanbulbir;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.animation.LinearInterpolator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.SharedPreferences;
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
import android.webkit.JavascriptInterface;
import org.json.JSONObject;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebStorage;
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
    private FrameLayout loadingOverlay;
    private ObjectAnimator loadingRingAnimator;
    private final Handler loadingHandler = new Handler(Looper.getMainLooper());
    private int loadingRequest = 0;
    private boolean navigationPending = false;
    private long visibleSince = 0L;
    private boolean pageLifecycleLoading = false;
    private LinearLayout bottomNavigation;
    private FrameLayout pageContent;
    private FrameLayout appRoot;
    private int systemInsetTop, systemInsetBottom, systemInsetLeft, systemInsetRight;
    private boolean isMemberPage = false;
    private int navigationEpoch = 0;
    private static final String RELEASE_PREFS = "tesis_release_state";
    private static final String RELEASE_KEY = "last_started_version";
    private final LinearLayout[] bottomTabs = new LinearLayout[5];
    private final LinearLayout[] bottomSelectionBackgrounds = new LinearLayout[5];
    private final ImageView[] bottomTabIcons = new ImageView[5];
    private final TextView[] bottomTabTitles = new TextView[5];
    private static final int NAV_BAR_HEIGHT_DP = 72;
    private static final int NAV_RED = Color.rgb(153, 45, 61);
    private static final int NAV_TURQUOISE = Color.rgb(190, 231, 234);
    private LinearLayout expandedMenu;
    private boolean expandedMenuOpen = false;
    private static final int NAV_HIGHLIGHT = Color.rgb(222, 242, 246);
    private static final int NAV_DEFAULT = Color.rgb(20, 59, 82);

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


    // Tum sayfa gecislerinde, giriste ve duyuru/galeri acilisinda gosterilen
    // yazisiz TES-IS logolu bekleme halkasi. Ilk splash ekrani korunur.
    private void createPageLoadingOverlay(FrameLayout root) {
        loadingOverlay = new FrameLayout(this);
        loadingOverlay.setBackgroundColor(Color.argb(242, 247, 252, 254));
        loadingOverlay.setClickable(true);
        loadingOverlay.setVisibility(View.GONE);
        root.addView(loadingOverlay, new FrameLayout.LayoutParams(-1, -1));

        FrameLayout circle = new FrameLayout(this);
        FrameLayout.LayoutParams circleParams = new FrameLayout.LayoutParams(
                dp(138), dp(138), Gravity.CENTER);
        loadingOverlay.addView(circle, circleParams);

        // Gercek tam boy cember: standart ProgressBar kucuk gorunebiliyor.
        View ring = new View(this) {
            private final android.graphics.Paint paint =
                    new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas canvas) {
                super.onDraw(canvas);
                paint.setStyle(android.graphics.Paint.Style.STROKE);
                paint.setStrokeCap(android.graphics.Paint.Cap.ROUND);
                paint.setStrokeWidth(dp(5));
                float gap = dp(5);
                android.graphics.RectF bounds = new android.graphics.RectF(
                        gap, gap, getWidth() - gap, getHeight() - gap);
                paint.setColor(Color.rgb(207, 227, 238));
                canvas.drawArc(bounds, 0, 360, false, paint);
                paint.setColor(TESIS_BLUE);
                canvas.drawArc(bounds, -90, 252, false, paint);
            }
        };
        FrameLayout.LayoutParams ringParams = new FrameLayout.LayoutParams(
                dp(130), dp(130), Gravity.CENTER);
        circle.addView(ring, ringParams);
        loadingRingAnimator = ObjectAnimator.ofFloat(ring, View.ROTATION, 0f, 360f);
        loadingRingAnimator.setDuration(1100);
        loadingRingAnimator.setRepeatCount(ValueAnimator.INFINITE);
        loadingRingAnimator.setInterpolator(new LinearInterpolator());

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.tes_is_gercek_logo);
        logo.setContentDescription("TES-İŞ");
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable white = new GradientDrawable();
        white.setShape(GradientDrawable.OVAL);
        white.setColor(Color.WHITE);
        logo.setBackground(white);
        logo.setPadding(dp(7), dp(7), dp(7), dp(7));
        FrameLayout.LayoutParams logoParams = new FrameLayout.LayoutParams(
                dp(88), dp(88), Gravity.CENTER);
        circle.addView(logo, logoParams);
    }

    private void showPageLoading() {
        showPageLoading(false);
    }

    private void showPageLoading(boolean fromBottomMenu) {
        if (loadingOverlay == null || !splashDismissed) return;
        // Ayni WebView gecisi icin her ilerleme olayinda sureyi bastan baslatma.
        if (navigationPending) return;
        navigationPending = true;
        final int request = ++loadingRequest;
        loadingHandler.removeCallbacksAndMessages(null);
        // Gercekten uzun suren yuklemede donsun, hizli sayfaya gecisi tutmasin.
        final long delay = fromBottomMenu ? 120L : 180L;
        loadingHandler.postDelayed(() -> {
            if (request != loadingRequest || !navigationPending
                    || loadingOverlay == null || !splashDismissed) return;
            loadingOverlay.setVisibility(View.VISIBLE);
            if (loadingRingAnimator != null && !loadingRingAnimator.isStarted())
                loadingRingAnimator.start();
        }, delay);
        loadingHandler.postDelayed(() -> {
            if (request == loadingRequest) hidePageLoading();
        }, 15000L);
    }

    private void hidePageLoading() {
        navigationPending = false;
        ++loadingRequest;
        loadingHandler.removeCallbacksAndMessages(null);
        // Yuklenen sayfayi bekletmeden ac: eskiden 300ms fazladan tutuyordu.
        if (loadingOverlay != null) loadingOverlay.setVisibility(View.GONE);
        if (loadingRingAnimator != null) loadingRingAnimator.cancel();
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        configureSystemBars();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(TESIS_BLUE);
        FrameLayout layout = new FrameLayout(this);
        layout.setBackgroundColor(Color.WHITE);
        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(-1, -1);
        // Giris sayfasinda alt navigasyon icin bosluk birakma.
        contentParams.bottomMargin = 0;
        root.addView(layout, contentParams);
        pageContent = layout;
        appRoot = root;
        webView = new WebView(this);
        webView.setBackgroundColor(Color.WHITE);
        layout.addView(webView, new FrameLayout.LayoutParams(-1, -1));
        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        layout.addView(progress, new FrameLayout.LayoutParams(-1, 6, Gravity.TOP));
        bottomNavigation = createBottomNavigation();
        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                -1, dp(NAV_BAR_HEIGHT_DP), Gravity.BOTTOM);
        root.addView(bottomNavigation, bottomParams);
        bottomNavigation.setVisibility(View.GONE);
        createExpandedMenu(root);
        createPageLoadingOverlay(root);
        showNativeSplash(root);
        setContentView(root);

        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                Insets bars = insets.getInsets(WindowInsets.Type.systemBars()
                        | WindowInsets.Type.displayCutout());
                systemInsetTop = bars.top;
                systemInsetBottom = bars.bottom;
                systemInsetLeft = bars.left;
                systemInsetRight = bars.right;
                updateNavigationLayout();
                return insets;
            });
            root.requestApplyInsets();
        }

        WebSettings settings = webView.getSettings();
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 23) settings.setOffscreenPreRaster(true);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        webView.addJavascriptInterface(new MemberBridge(), "TesisNative");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int newProgress) {
                progress.setProgress(newProgress);
                progress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
                if (isTrustedSite(view.getUrl()) && newProgress > 0 && newProgress < 100
                        && splashDismissed && loadingOverlay != null
                        && !navigationPending) {
                    // Ustteki yukleme cubugu ilerlerken logo da gercekten gorunsun.
                    showPageLoading(true);
                }
            }
        });
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                navigationEpoch++;
                // Oturum acma/cikis yonlendirmesinde menu aninda gizlensin.
                if (isLoginOrAuthUrl(url) || !isMemberPage)
                    setMemberNavigationVisible(false);
                hideExpandedMenu();
                pageLifecycleLoading = isTrustedSite(url);
                if (pageLifecycleLoading) showPageLoading();
                else hidePageLoading();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame()) {
                    pageLifecycleLoading = false;
                    hidePageLoading();
                }
                super.onReceivedError(view, request, error);
            }
            @Override public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageLifecycleLoading = false;
                hidePageLoading();
                if (!splashDismissed) waitForSiteLogos(view, 0);
                if (isTrustedSite(url) && !isLoginOrAuthUrl(url)) {
                    refreshMemberNavigation(url, navigationEpoch);
                    updateActiveBottomTab(url);
                    installNavigationLoadingTriggers();
                    installNativeHeader();
                    registerDeviceToken();
                } else {
                    setMemberNavigationVisible(false);
                }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return request.isForMainFrame() && openExternalWhenNeeded(request.getUrl());
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return openExternalWhenNeeded(Uri.parse(url));
            }
        });

        // Google Play guncellemesinden sonra eski oturumu kapat;
        // normal acilis ve cihazda yeni kurulumda oturumu koru.
        openAfterVersionCheck(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 314);
        }
        // V1.11: legacy global-topic subscription is deliberately disabled.
        FirebaseMessaging.getInstance().unsubscribeFromTopic(TOPIC);
    }



    private void updateNavigationLayout() {
        if (pageContent == null || bottomNavigation == null) return;
        FrameLayout.LayoutParams content =
                (FrameLayout.LayoutParams) pageContent.getLayoutParams();
        content.topMargin = systemInsetTop;
        content.leftMargin = systemInsetLeft;
        content.rightMargin = systemInsetRight;
        content.bottomMargin = systemInsetBottom +
                (isMemberPage ? dp(NAV_BAR_HEIGHT_DP) : 0);
        pageContent.setLayoutParams(content);

        FrameLayout.LayoutParams nav =
                (FrameLayout.LayoutParams) bottomNavigation.getLayoutParams();
        nav.bottomMargin = systemInsetBottom;
        nav.leftMargin = systemInsetLeft;
        nav.rightMargin = systemInsetRight;
        bottomNavigation.setLayoutParams(nav);
    }

    private void setMemberNavigationVisible(boolean visible) {
        if (bottomNavigation == null) return;
        if (isMemberPage == visible &&
                bottomNavigation.getVisibility() == (visible ? View.VISIBLE : View.GONE))
            return;
        isMemberPage = visible;
        bottomNavigation.setVisibility(visible ? View.VISIBLE : View.GONE);
        if (!visible) hideExpandedMenu();
        updateNavigationLayout();
    }

    private static boolean isLoginOrAuthUrl(String url) {
        if (!isTrustedSite(url)) return false;
        try {
            Uri u = Uri.parse(url);
            String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
            return u.getQueryParameter("tesis_uye_giris") != null
                    || path.endsWith("/admin-post.php")
                    || path.contains("uye-giris")
                    || path.contains("uye-cikis");
        } catch (Exception ignored) {
            return false;
        }
    }

    private void refreshMemberNavigation(String expectedUrl, int expectedEpoch) {
        if (webView == null || !isTrustedSite(expectedUrl) || isLoginOrAuthUrl(expectedUrl)) {
            setMemberNavigationVisible(false);
            return;
        }
        // Giris formu varken bar kesinlikle gosterilmez.
        // Yetkili sayfada ise sunucunun urettigi logout/FCM isaretine bakilir.
        webView.evaluateJavascript(
            "(function(){" +
            "var hasLoginForm=!!(document.querySelector('form input[name=\"telefon\"]')" +
            "&&document.querySelector('form input[name=\"sicil\"]'));" +
            "if(hasLoginForm)return false;" +
            "var n=document.querySelector('meta[name=\"tesis-device-nonce\"]');" +
            "return !!((n&&n.content)||document.querySelector('.tesis-logout-item'));" +
            "})()",
            active -> {
                if (webView == null || expectedEpoch != navigationEpoch) return;
                String current = webView.getUrl();
                if (!expectedUrl.equals(current) || isLoginOrAuthUrl(current)) return;
                setMemberNavigationVisible("true".equals(active));
            });
    }

    private void openAfterVersionCheck(Bundle savedInstanceState) {
        int version = 21;
        long firstInstall = 0, lastUpdate = 0;
        try {
            android.content.pm.PackageInfo pkg =
                    getPackageManager().getPackageInfo(getPackageName(), 0);
            version = Build.VERSION.SDK_INT >= 28
                    ? (int) pkg.getLongVersionCode() : pkg.versionCode;
            firstInstall = pkg.firstInstallTime;
            lastUpdate = pkg.lastUpdateTime;
        } catch (Exception e) {
            Log.w("TESIS", "Uygulama surumu okunamadi", e);
        }
        final int currentVersion = version;
        SharedPreferences prefs = getSharedPreferences(RELEASE_PREFS, MODE_PRIVATE);
        int previousVersion = prefs.getInt(RELEASE_KEY, -1);
        boolean appWasUpdated = previousVersion >= 0
                ? previousVersion != currentVersion
                : (firstInstall > 0 && lastUpdate > firstInstall + 1500);

        if (appWasUpdated) {
            // Uygulama guncellendi: eski uyenin oturumu ve FCM tokeni kalmasin.
            setMemberNavigationVisible(false);
            FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener(e -> Log.w("TESIS-FCM", "Onceki cihaz tokeni kaldirilamadi", e));
            CookieManager.getInstance().removeAllCookies(done -> {
                CookieManager.getInstance().flush();
                WebStorage.getInstance().deleteAllData();
                if (webView != null) {
                    webView.clearCache(true);
                    webView.clearHistory();
                    prefs.edit().putInt(RELEASE_KEY, currentVersion).apply();
                    webView.loadUrl(HOMEPAGE);
                }
            });
        } else {
            prefs.edit().putInt(RELEASE_KEY, currentVersion).apply();
            if (savedInstanceState != null) {
                webView.restoreState(savedInstanceState);
                String opened = getNotificationUrl(getIntent());
                if (!HOMEPAGE.equals(opened)) webView.loadUrl(opened);
            } else {
                webView.loadUrl(getNotificationUrl(getIntent()));
            }
        }
    }

    /** TES-IS uygulamasi icin sabit alt gezinme cubugu. */
    private LinearLayout createBottomNavigation() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        bar.setElevation(dp(8));
        GradientDrawable background = new GradientDrawable();
        background.setColor(NAV_RED);
        background.setStroke(dp(1), NAV_RED);
        bar.setBackground(background);

        String[] titles = {"Ana Sayfa", "Haberler", "Duyurular", "TİS", "Menü"};
        int[] iconResources = {
                R.drawable.tesis_nav_home, R.drawable.tesis_nav_news,
                R.drawable.tesis_nav_bell, R.drawable.tesis_nav_docs,
                R.drawable.tesis_nav_menu
        };
        for (int i = 0; i < titles.length; i++) {
            final int selected = i;
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(0, 0, 0, 0);
            LinearLayout selectedArea = new LinearLayout(this);
            selectedArea.setOrientation(LinearLayout.VERTICAL);
            selectedArea.setGravity(Gravity.CENTER);
            selectedArea.setPadding(dp(5), dp(6), dp(5), dp(6));
            selectedArea.setClickable(false);
            item.addView(selectedArea, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            item.setClickable(true);
            item.setFocusable(true);
            item.setContentDescription(titles[i]);

            ImageView icon = new ImageView(this);
            icon.setImageResource(iconResources[i]);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(24), dp(24));
            iconParams.gravity = Gravity.CENTER_HORIZONTAL;
            selectedArea.addView(icon, iconParams);

            TextView label = new TextView(this);
            label.setText(titles[i]);
            label.setTextSize(10);
            label.setSingleLine(true);
            label.setGravity(Gravity.CENTER);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            labelParams.gravity = Gravity.CENTER_HORIZONTAL;
            labelParams.topMargin = dp(4);
            selectedArea.addView(label, labelParams);
            item.setOnClickListener(view -> openBottomNavigation(selected));

            bottomTabs[i] = item;
            bottomSelectionBackgrounds[i] = selectedArea;
            bottomTabIcons[i] = icon;
            bottomTabTitles[i] = label;
            bar.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
        }
        highlightBottomTab(0);
        return bar;
    }

    private void highlightBottomTab(int activeIndex) {
        for (int i = 0; i < bottomTabs.length; i++) {
            if (bottomTabs[i] == null) continue;
            final boolean selected = (i == activeIndex);
            final int color = selected ? TESIS_BLUE : Color.WHITE;
            bottomTabIcons[i].setColorFilter(color);
            bottomTabTitles[i].setTextColor(color);
            GradientDrawable itemBg = new GradientDrawable();
            itemBg.setCornerRadius(dp(12));
            itemBg.setColor(selected ? NAV_TURQUOISE : Color.TRANSPARENT);
            bottomSelectionBackgrounds[i].setBackground(itemBg);
        }
    }

    private void updateActiveBottomTab(String url) {
        if (!isTrustedSite(url)) return;
        try {
            Uri uri = Uri.parse(url);
            String path = uri.getPath() == null ? "/" : uri.getPath();
            String fragment = uri.getFragment();
            if (path.startsWith("/sube-haberleri/")) highlightBottomTab(1);
            else if (path.startsWith("/tesis_duyuru/") || path.startsWith("/duyuru/")
                    || "tesis_duyuru".equals(uri.getQueryParameter("post_type"))) highlightBottomTab(2);
            else if ("sozlesmeler".equals(fragment) || path.startsWith("/tis/")) highlightBottomTab(3);
            else if ("/".equals(path) && (fragment == null || fragment.isEmpty())) highlightBottomTab(0);
            else highlightBottomTab(4);
        } catch (Exception ignored) { }
    }

    private void openBottomNavigation(int index) {
        if (index == 4) {
            toggleExpandedMenu();
            return;
        }
        hideExpandedMenu();
        String url = HOMEPAGE;
        if (index == 1) url = HOMEPAGE + "sube-haberleri/";
        else if (index == 2) url = HOMEPAGE + "?post_type=tesis_duyuru";
        else if (index == 3) url = HOMEPAGE + "#sozlesmeler";
        navigateInApp(url);
        highlightBottomTab(index);
    }

    private int menuIconResource(int index) {
        switch (index) {
            case 0: return android.R.drawable.ic_menu_myplaces;
            case 1: return android.R.drawable.ic_menu_gallery;
            case 2: return android.R.drawable.ic_menu_info_details;
            default: return android.R.drawable.ic_menu_call;
        }
    }

    private void createExpandedMenu(FrameLayout root) {
        expandedMenu = new LinearLayout(this);
        expandedMenu.setOrientation(LinearLayout.VERTICAL);
        expandedMenu.setBackgroundColor(NAV_RED);
        expandedMenu.setVisibility(View.GONE);
        expandedMenu.setElevation(dp(7));
        final String[] labels = {"Şube Yönetimi", "Galeri", "Mevzuat", "İletişim"};
        final String[] destinations = {
            HOMEPAGE + "#yonetim",
            HOMEPAGE + "?post_type=tesis_galeri",
            HOMEPAGE + "#mevzuat",
            HOMEPAGE + "#iletisim"
        };
        for (int i = 0; i < labels.length; i++) {
            final int choice = i;
            LinearLayout item = new LinearLayout(this);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(dp(20), dp(4), dp(14), dp(4));
            ImageView symbol = new ImageView(this);
            symbol.setImageResource(menuIconResource(i));
            symbol.setColorFilter(Color.WHITE);
            item.addView(symbol, new LinearLayout.LayoutParams(dp(23), dp(23)));
            TextView label = new TextView(this);
            label.setText(labels[i]);
            label.setTextColor(Color.WHITE);
            label.setTextSize(15);
            label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
            lp.leftMargin = dp(18);
            item.addView(label, lp);
            item.setOnClickListener(v -> {
                hideExpandedMenu();
                navigateInApp(destinations[choice]);
                highlightBottomTab(4);
            });
            expandedMenu.addView(item, new LinearLayout.LayoutParams(-1, dp(49)));
            if (i < labels.length - 1) {
                View divider = new View(this);
                divider.setBackgroundColor(Color.argb(65,255,255,255));
                expandedMenu.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));
            }
        }
        View strongDivider = new View(this);
        strongDivider.setBackgroundColor(NAV_TURQUOISE);
        expandedMenu.addView(strongDivider, new LinearLayout.LayoutParams(-1, dp(3)));
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(-1, dp(202), Gravity.BOTTOM);
        params.bottomMargin = dp(NAV_BAR_HEIGHT_DP);
        root.addView(expandedMenu, params);
    }

    private void toggleExpandedMenu() {
        if (!isMemberPage || expandedMenu == null) return;
        expandedMenuOpen = !expandedMenuOpen;
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) expandedMenu.getLayoutParams();
        params.bottomMargin = systemInsetBottom + dp(NAV_BAR_HEIGHT_DP);
        params.leftMargin = systemInsetLeft;
        params.rightMargin = systemInsetRight;
        expandedMenu.setLayoutParams(params);
        expandedMenu.setVisibility(expandedMenuOpen ? View.VISIBLE : View.GONE);
        highlightBottomTab(4);
    }

    private void hideExpandedMenu() {
        expandedMenuOpen = false;
        if (expandedMenu != null) expandedMenu.setVisibility(View.GONE);
    }

    private void navigateInApp(String url) {
        if (webView == null || !isTrustedSite(url)) return;
        if (url.equals(webView.getUrl())) {
            webView.scrollTo(0, 0);
            return;
        }
        // Butona tiklandiginda aninda gorsel yanit ver.
        showPageLoading(true);
        webView.loadUrl(url);
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


    private void installNavigationLoadingTriggers() {
        if (webView == null || !isTrustedSite(webView.getUrl())) return;
        String script = "(function(){if(window.__tesisNavLoadingBound)return;" +
            "window.__tesisNavLoadingBound=true;" +
            "document.addEventListener('submit',function(e){" +
            "var form=e.target;if(!form||!form.matches)return;" +
            "var isLogin=!!(form.querySelector('input[name=\"sicil\"]')" +
            "&&form.querySelector('input[name=\"telefon\"]'));" +
            "var isLogout=form.classList.contains('tesis-logout-form');" +
            "if(isLogin||isLogout)TesisNative.startNavigationLoading();" +
            "},true);" +
            "})();";
        webView.evaluateJavascript(script, null);
    }

    /** Sadece Android WebView: mobil hamburger yerine kucuk hesap/cikis. */
    private void installNativeHeader() {
        if (webView == null || !isTrustedSite(webView.getUrl())) return;
        String js = "(function(){" +
            "var css='@media(max-width:900px){'+" +
            "'.menu-toggle,.mobile-menu-toggle,.hamburger,.hamburger-btn,.nav-toggle,.site-menu-toggle,.mobile-nav-toggle,.header-toggle,.menu-button,.mobile-menu-button{display:none!important;}'+" +
            "'#tesis-app-account{display:flex!important;flex-direction:column;align-items:center;justify-content:center;gap:2px;background:#992d3d;color:#fff;border-radius:8px;padding:5px 8px;font-size:11px;font-weight:700;line-height:1.15;max-width:148px;min-width:100px;box-sizing:border-box;text-align:center;}'+" +
            "'#tesis-app-account span{max-width:100%;white-space:normal;overflow:visible;overflow-wrap:normal;font-size:11px;}'+" +
            "'#tesis-app-account b{display:block;font-size:11px;white-space:nowrap;}'+" +
            "'}';" +
            "if(!document.getElementById('tesis-app-header-style')){" +
            "var st=document.createElement('style');st.id='tesis-app-header-style';st.textContent=css;document.head.appendChild(st);}" +
            "var n=document.querySelector('meta[name=\"tesis-device-nonce\"]');" +
            "if(!n||!n.content)return;" +
            "var old=document.getElementById('tesis-app-account');if(old)return;" +
            "var hamburger=document.querySelector('.menu-toggle,.mobile-menu-toggle,.hamburger,.hamburger-btn,.nav-toggle,.site-menu-toggle,.mobile-nav-toggle,.header-toggle,.menu-button,.mobile-menu-button');" +
            "if(!hamburger){hamburger=Array.from(document.querySelectorAll('header button,header a')).find(function(el){var label=((el.getAttribute('aria-label')||'')+' '+(el.className||'')).toLowerCase();return /menu|menü|hamburger|toggle/.test(label);});}" +
            "var top=hamburger&&hamburger.parentElement;" +
            "if(!top){top=document.querySelector('header .header-actions,header .header-right,header .brand-row,header .header-inner,header');}" +
            "if(!top)return;" +
            "var logout=document.querySelector('form.tesis-logout-form');" +
            "var username=document.querySelector('.tesis-logout-item .tesis-user-name,.tesis-logout-item .user-name,.tesis-member-name,.member-name');" +
            "var item=document.querySelector('.tesis-logout-item');" +
            "var name=username?username.textContent.trim():(item?item.textContent.replace(/çıkış\s*yap/ig,'').trim():'Üye');" +
            "if(!name)name='Üye';" +
            "var b=document.createElement('button');b.id='tesis-app-account';b.type='button';" +
            "var t=document.createElement('span');t.textContent=name;b.appendChild(t);" +
            "var x=document.createElement('b');x.textContent='Çıkış Yap';b.appendChild(x);" +
            "b.onclick=function(){var f=document.querySelector('form.tesis-logout-form');" +
            "if(f){if(f.requestSubmit)f.requestSubmit();else f.submit();}" +
            "else{var btn=document.querySelector('.tesis-logout-item button,.tesis-logout-item a');if(btn)btn.click();}};" +
            "if(hamburger){hamburger.style.setProperty('display','none','important');hamburger.insertAdjacentElement('afterend',b);}else{top.appendChild(b);}" +
            "})()";
        webView.evaluateJavascript(js, null);
    }

    private void registerDeviceToken() {
        // Giris sayfasinda nonce bulunmaz. Bu durumda sessizce atla:
        // gercek FCM kaydi yalnizca oturum acmis uyenin sayfasinda yapilir.
        if (webView == null || !isTrustedSite(webView.getUrl())) return;
        webView.evaluateJavascript(
            "(function(){var m=document.querySelector('meta[name=\\\"tesis-device-nonce\\\"]');return !!(m&&m.content);})()",
            hasNonce -> {
                if ("true".equals(hasNonce))
                    FirebaseMessaging.getInstance().getToken()
                        .addOnSuccessListener(this::registerDevice)
                        .addOnFailureListener(e -> Log.w("TESIS-FCM", "FCM token alinamadi", e));
            });
    }
    private void registerDevice(String token) {
        if (webView == null || !isTrustedSite(webView.getUrl()) || token == null) return;
        String quoted = JSONObject.quote(token);
        // Oncelikle oturum nonce'unu dogrula. Giris sayfasinda yoksa
        // normaldir; uyari gosterme. HTTP hatalari ise raporlanir.
        webView.evaluateJavascript(
            "(async function(){" +
            "var n=document.querySelector('meta[name=\\\"tesis-device-nonce\\\"]');" +
            "if(!n||!n.content)return;" +
            "try{" +
            "var r=await fetch('/wp-json/tesis-mobile/v1/device',{" +
            "method:'POST',credentials:'same-origin'," +
            "headers:{'Content-Type':'application/json','X-Tesis-Device-Nonce':n.content}," +
            "body:JSON.stringify({token:" + quoted + "})});" +
            "if(!r.ok){var p=await r.json().catch(function(){return {};});" +
            "TesisNative.registrationError('HTTP '+r.status+' '+(p.code||''));}" +
            "}catch(e){TesisNative.registrationError('Baglanti hatasi');}" +
            "})()", null);
    }
    // Device deregistration is best-effort. It must never block website logout.
    private boolean registrationErrorShown = false;
    public class MemberBridge {
        @JavascriptInterface public void startNavigationLoading() {
            runOnUiThread(() -> {
                if (webView != null && isTrustedSite(webView.getUrl())) showPageLoading();
            });
        }
        @JavascriptInterface public void registrationError(String message) {
            android.util.Log.w("TESIS-FCM", "Cihaz kaydi: " + message);
            runOnUiThread(() -> {
                if (!registrationErrorShown) {
                    registrationErrorShown = true;
                    Toast.makeText(MainActivity.this, "Bildirim cihaz kaydi: " + message, Toast.LENGTH_LONG).show();
                }
            });
        }
        @JavascriptInterface public void logout() {
            runOnUiThread(() -> {
                if (webView == null || !isTrustedSite(webView.getUrl())) return;
                FirebaseMessaging.getInstance().getToken()
                    .addOnSuccessListener(this::leaveAndLogout)
                    .addOnFailureListener(e -> leaveAndLogout(""));
            });
        }
        private void leaveAndLogout(String token) {
            if (webView == null || !isTrustedSite(webView.getUrl())) return;
            String quotedToken = JSONObject.quote(token == null ? "" : token);
            String js = "(async function(){" +
                "var form=window.__tesisPendingLogout;" +
                "if(!form)return;" +
                "try{" +
                "var n=document.querySelector('meta[name=\\\"tesis-device-nonce\\\"]');" +
                "if(n&&n.content&&" + quotedToken + "){" +
                "var controller=new AbortController();" +
                "var timeout=setTimeout(function(){controller.abort();},1800);" +
                "try{await fetch('/wp-json/tesis-mobile/v1/device',{" +
                "method:'DELETE',credentials:'same-origin',signal:controller.signal," +
                "headers:{'Content-Type':'application/json','X-Tesis-Device-Nonce':n.content}," +
                "body:JSON.stringify({token:" + quotedToken + "})});}finally{clearTimeout(timeout);}" +
                "}" +
                "}catch(e){}" +
                "HTMLFormElement.prototype.submit.call(form);" +
                "})();";
            webView.evaluateJavascript(js, null);
            FirebaseMessaging.getInstance().deleteToken()
                .addOnFailureListener(e -> Log.w("TESIS-FCM", "Token silinemedi", e));
        }
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
        if (webView != null && webView.canGoBack()) {
            // Hizli geri gecisinde logo donmesin; sadece uzun suren
            // gercek WebView yuklemelerinde onPageStarted tetikler.
            webView.goBack();
        } else {
            hidePageLoading();
            super.onBackPressed();
        }
    }
    @Override protected void onDestroy() {
        splashHandler.removeCallbacksAndMessages(null);
        hidePageLoading();
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
