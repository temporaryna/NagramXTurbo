package tw.nekomimi.nekogram.helpers.remote;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.Utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// Downloads an APK from an arbitrary HTTPS URL (GitHub Release asset) into the
// app's private storage, reporting progress on the UI thread. Used by the in-app
// updater when the manifest points at a `url` instead of a Telegram document —
// APK no longer fits Bot API's 50MB upload limit, so it lives in GitHub Releases.
public class ApkDownloader {

    public interface Callback {
        default void onProgress(float progress) {}
        void onSuccess(File file);
        default void onError(String message) {}
    }

    private static final int BUFFER_SIZE = 16384;
    private static final long PROGRESS_THROTTLE_MS = 100;
    private static final int CONNECT_TIMEOUT_S = 30;
    private static final int READ_TIMEOUT_S = 60;
    private static final int WRITE_TIMEOUT_S = 60;
    // Defence-in-depth: only ever install an APK fetched from this project's own
    // GitHub Release, even if the metadata-channel manifest were compromised.
    private static final String GITHUB_RELEASE_PREFIX = "https://github.com/temporaryna/NagramXTurbo/releases/";

    // Dedicated single-thread executor: a 50MB download must not block the shared
    // Utilities.globalQueue (used by polling, message events, etc.).
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static OkHttpClient client;

    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                    .connectTimeout(CONNECT_TIMEOUT_S, TimeUnit.SECONDS)
                    .readTimeout(READ_TIMEOUT_S, TimeUnit.SECONDS)
                    .writeTimeout(WRITE_TIMEOUT_S, TimeUnit.SECONDS)
                    .build();
        }
        return client;
    }

    public static File getDestFile(String url) {
        File dir = ApplicationLoader.getFilesDirFixed("update");
        return new File(dir, Utilities.MD5(url) + ".apk");
    }

    public static void download(String url, Callback callback) {
        if (url == null || !url.startsWith(GITHUB_RELEASE_PREFIX)) {
            AndroidUtilities.runOnUIThread(() -> callback.onError("Refusing download URL outside project releases"));
            return;
        }
        File dest = getDestFile(url);
        File tmp = new File(dest.getPath() + ".tmp");
        executor.submit(() -> {
            Response response = null;
            try {
                Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", "NagramXTurbo")
                        .build();
                response = getClient().newCall(request).execute();
                ResponseBody body = response.body();
                if (!response.isSuccessful() || body == null) {
                    throw new RuntimeException("HTTP " + response.code());
                }
                long total = body.contentLength();
                try (InputStream in = body.byteStream();
                     FileOutputStream out = new FileOutputStream(tmp)) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    long read = 0;
                    int n;
                    long lastReport = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        read += n;
                        long now = System.currentTimeMillis();
                        if (total > 0 && now - lastReport > PROGRESS_THROTTLE_MS) {
                            final float p = Math.min(1f, read / (float) total);
                            lastReport = now;
                            AndroidUtilities.runOnUIThread(() -> callback.onProgress(p));
                        }
                    }
                }
                // Finalize atomically: dest only appears once the full file is written,
                // so an interrupted download never leaves a partial APK to install.
                if (!tmp.renameTo(dest)) {
                    throw new RuntimeException("Failed to finalize download");
                }
                File done = dest;
                AndroidUtilities.runOnUIThread(() -> callback.onSuccess(done));
            } catch (Exception e) {
                FileLog.e(e);
                try {
                    tmp.delete();
                } catch (Exception ignored) {
                }
                String message = e.getMessage();
                AndroidUtilities.runOnUIThread(() -> callback.onError(message));
            } finally {
                if (response != null) response.close();
            }
        });
    }
}
