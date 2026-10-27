package tr.org.tesis.istanbulbir;

import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Uses the existing MainActivity without replacing its notification,
 * login, Firebase, or WebView setup. Only Android Back handling changes.
 */
public final class BackActivity extends MainActivity {
    private static final long SEQUENCE_WINDOW_MS = 2800L;
    private static final String HOME = "https://ist1tes-is.com.tr/";
    private int backCount = 0;
    private long lastBackMs = 0L;

    private static WebView findWebView(View root) {
        if (root instanceof WebView) return (WebView) root;
        if (root instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) root;
            for (int i = 0; i < group.getChildCount(); i++) {
                WebView found = findWebView(group.getChildAt(i));
                if (found != null) return found;
            }
        }
        return null;
    }

    private static boolean isHome(String url) {
        if (url == null) return false;
        Uri uri = Uri.parse(url);
        String host = uri.getHost();
        String path = uri.getPath();
        return "https".equalsIgnoreCase(uri.getScheme())
                && ("ist1tes-is.com.tr".equalsIgnoreCase(host)
                    || "www.ist1tes-is.com.tr".equalsIgnoreCase(host))
                && (path == null || path.isEmpty() || "/".equals(path))
                && (uri.getEncodedQuery() == null || uri.getEncodedQuery().isEmpty());
    }

    @Override
    public void onBackPressed() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastBackMs > SEQUENCE_WINDOW_MS || now < lastBackMs) {
            backCount = 0;
        }
        lastBackMs = now;
        backCount++;

        WebView web = findWebView(getWindow().getDecorView());
        if (backCount == 1) {
            // Home page is the root: exit immediately, even if WebView has history.
            if (web != null && isHome(web.getUrl())) {
                finish();
                return;
            }
            // Inner pages: first press goes to the previous page.
            if (web != null && web.canGoBack()) {
                web.goBack();
            } else if (web != null && !isHome(web.getUrl())) {
                web.loadUrl(HOME);
            }
        } else if (backCount == 2) {
            // Second rapid press: jump to homepage even while a page loads.
            if (web != null && !isHome(web.getUrl())) {
                web.stopLoading();
                web.loadUrl(HOME);
            }
        } else {
            // Third rapid press: close the app without changing login state.
            finish();
        }
    }
}
