package tr.org.tesis.istanbulbir;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public final class AnnouncementMessagingService extends FirebaseMessagingService {
    private static final String CHANNEL_ID = "tesis_duyuru_haberleri";
    private static final String TAG = "TESIS-FCM";

    @Override public void onNewToken(String token) {
        super.onNewToken(token);
        Log.i(TAG, "FCM token alindi");
        // Legacy shared topic must never be rejoined when Firebase rotates its token.
        com.google.firebase.messaging.FirebaseMessaging.getInstance()
                .unsubscribeFromTopic("tesis_istanbul1_duyuru")
                .addOnFailureListener(e -> Log.w(TAG, "Eski bildirim kanalindan ayrilma basarisiz", e));
    }

    @Override public void onMessageReceived(RemoteMessage message) {
        super.onMessageReceived(message);
        Map<String, String> data = message.getData();
        String title = data.get("title");
        String body = data.get("body");
        String url = data.get("url");
        if (title == null || title.trim().isEmpty()) title = "TES-İŞ • Yeni Duyuru";
        if (body == null || body.trim().isEmpty()) body = "Yeni bir duyuru yayımlandı.";
        if (!MainActivity.isTrustedSite(url)) url = MainActivity.HOMEPAGE;

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "TES-İŞ Duyuruları", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Yeni TES-İŞ duyuruları için bildirimler");
            channel.enableLights(true);
            channel.setLightColor(Color.RED);
            nm.createNotificationChannel(channel);
        }

        Intent open = new Intent(this, MainActivity.class);
        open.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        open.putExtra(MainActivity.EXTRA_URL, url);
        int id = (int)(System.currentTimeMillis() & 0x7fffffff);
        PendingIntent pending = PendingIntent.getActivity(this, id, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder builder = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        nm.notify(id, builder
                // Durum cubugunda unlem yerine TES-IS ambleminin beyaz simgesi.
                .setSmallIcon(R.drawable.tesis_notification_small)
                // Bildirim acilinca resmi renkli TES-IS logosu gorunur.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.tes_is_gercek_logo))
                .setColor(Color.rgb(16, 57, 95))
                .setSubText("TES-İŞ İstanbul 1")
                .setContentTitle(title.trim())
                .setContentText(body.trim())
                .setStyle(new Notification.BigTextStyle().setBigContentTitle(title.trim()).bigText(body.trim()))
                .setAutoCancel(true)
                .setContentIntent(pending)
                .build());
    }
}
