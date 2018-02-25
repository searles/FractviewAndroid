package at.searles.fractview.bitmap;

import android.os.AsyncTask;

/**
 * These are jobs that can be run while the
 * BitmapFragment is idle.
 */
public interface IdleJob {
    boolean cancelRunning();

    boolean imageIsModified();

    AsyncTask<Void, Void, Void> createTask();
}
