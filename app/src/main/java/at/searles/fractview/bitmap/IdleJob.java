package at.searles.fractview.bitmap;

import android.os.AsyncTask;

/**
 * These are jobs that can be run while the
 * BitmapFragment is idle.
 */
public interface IdleJob {
    /**
     * If true,
     * @return
     */
    boolean imageIsModified();

    AsyncTask<Void, Void, Void> task();
}
