# TES-İŞ İstanbul 1 Android uygulaması

Android uygulaması: https://ist1tes-is.com.tr/

## Firebase push bildirimleri

- **Android Firebase istemci bağlantısı yapılandırıldı**: Firebase projesi `tesis-istanbul-1` (`939464714660`).
- Android paket adı: `tr.org.tesis.istanbulbir.uygulama`.
- Android 13+ için bildirim izni istenir.
- FCM topic: `tesis_istanbul1_duyuru`.
- Uygulama kapalıyken gelen yüksek öncelikli veri mesajı bildirim merkezinde gösterilir; bildirime basınca ilgili duyuru uygulamada açılır.
- Bildirimlerin gerçekten gönderilebilmesi için WordPress'de kurulu `TESIS-WORDPRESS-MOBIL-BILDIRIM.zip` eklentisindeki **Firebase servis hesabı** ayrıca bağlanmalıdır.
- WordPress eklentisinde Firebase Console → Project settings → Service accounts → Generate new private key ile edinilen JSON dosyası **doğrudan WordPress yönetim paneline** yüklenir.
- **Servis hesabı özel anahtarını asla Android projesine, APK'ye veya herkese açık GitHub deposuna koymayın.**
- `google-services.json` dosyasındaki API anahtarı Android uygulamasının herkese açık istemci yapılandırmasının parçasıdır; bu, FCM gönderim yetkisi taşıyan servis hesabı anahtarı değildir.
- WordPress'e servis hesabı eklenince bildirim testi için `Ayarlar → TES-İŞ Mobil Bildirim` sayfasındaki test butonu kullanılabilir.

## APK üretimi

GitHub Actions iş akışı Android SDK/Gradle ile APK üretir, imza ve ZIP bütünlüğünü doğrular, emülatörde kurup açmayı test eder.

`app/logo.base64` içerisindeki TES-İŞ logosu derlemede uygulama simgesine dönüştürülür.
