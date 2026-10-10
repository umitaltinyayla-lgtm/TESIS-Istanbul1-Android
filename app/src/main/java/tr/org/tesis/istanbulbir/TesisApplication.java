package tr.org.tesis.istanbulbir;

import android.app.Application;
import android.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

public final class TesisApplication extends Application {
    private static final String TAG = "TESIS-FCM";

    @Override public void onCreate() {
        super.onCreate();
        String appId = getString(R.string.firebase_app_id).trim();
        String apiKey = getString(R.string.firebase_api_key).trim();
        String senderId = getString(R.string.firebase_sender_id).trim();
        String projectId = getString(R.string.firebase_project_id).trim();
        if (appId.isEmpty() || apiKey.isEmpty() || senderId.isEmpty() || projectId.isEmpty()) {
            Log.i(TAG, "Firebase yapilandirmasi bekleniyor. Push bildirimleri henuz etkin degil.");
            return;
        }
        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseOptions options = new FirebaseOptions.Builder()
                        .setApplicationId(appId)
                        .setApiKey(apiKey)
                        .setGcmSenderId(senderId)
                        .setProjectId(projectId)
                        .build();
                FirebaseApp.initializeApp(this, options);
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Firebase baslatilamadi", e);
        }
    }
}
