package pl.bydgoszcz.guideme.autofittextview;


import android.util.Log;

/**
 * Created by dszcz_000 on 14.01.2017.
 */

public class KLogger {
    public interface LogConsumer {
        String get();
    }
    private String key;

    public static KLogger from(final String key) {
        final KLogger kLogger = new KLogger();
        kLogger.key = key;
        return kLogger;
    }

    public void fine(LogConsumer logConsumer) {
        Log.d(key, logConsumer.get());
    }
}
