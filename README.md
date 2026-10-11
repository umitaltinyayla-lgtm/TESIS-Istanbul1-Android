# TES-İŞ İstanbul 1 Android uygulaması

Siteden haber ve duyuruları görüntüleyen Android uygulaması.

## Bildirimler
Yeni WordPress `tesis_duyuru` yayımlandığında Firebase Cloud Messaging (FCM) topic
`tesis_istanbul1_duyuru` aracılığıyla cihazlara veri bildirimi gönderilir.

- Uygulama `POST_NOTIFICATIONS` izni ister.
- FCM token alınır ve topic aboneliği yapılır.
- Bildirime basınca ilgili duyuru uygulamada açılır.
- WordPress tarafına `TESIS-WORDPRESS-MOBIL-BILDIRIM.zip` eklentisi kurulmalıdır.
- Bildirim gönderebilmek için Firebase projesi ve güvenli sunucu servis hesabı bağlantısı gerekir.
- Android Firebase uygulamasının PUBLIC yapılandırması `app/src/main/res/values/firebase_config.xml` içine yazılır.
- **Servis hesabının JSON özel anahtarı Android projesine veya GitHub'a kesinlikle eklenmez.**

Firebase yapılandırması eklenmediyse uygulama sitesi çalışır, bildirim almaz.
Firebase Cloud Messaging ücret gerektirmeden temel mesajlaşmayı destekler.

## APK derlemesi
GitHub Actions: TES-IS Android APK iş akışı, gerçek Android SDK/Gradle ile APK üretir,
APK imzasını doğrular, artifact yükler ve Android emülatöründe kurup açmayı dener.

`app/logo.base64` içindeki gerçek TES-İŞ logosu derlemede PNG simgesine dönüştürülür.
