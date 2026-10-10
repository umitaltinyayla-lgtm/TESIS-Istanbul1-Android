# TES-İŞ İstanbul 1 Android uygulaması

Bu depo, https://ist1tes-is.com.tr/ sitesini Android WebView içinde açan uygulamanın kaynak kodunu içerir.

APK derlemesi GitHub Actions üzerinden Android SDK + Gradle ile yapılır. İlk sürümde anlık bildirim yoktur; daha sonra eklenebilir.

## APK üretimi
main dalına değişiklik gönderildiğinde "TES-IS Android APK" iş akışı otomatik çalışır. Actions bölümündeki başarılı derlemenin Artifacts alanından "TESIS-Istanbul1-Android-APK" dosyasını indirin. ZIP içinde app-debug.apk bulunur.

Bu APK Android Debug anahtarıyla imzalanmıştır. Geliştirme/test amaçlıdır; Play Store dağıtımı için sürüm imza anahtarı gerekir.
