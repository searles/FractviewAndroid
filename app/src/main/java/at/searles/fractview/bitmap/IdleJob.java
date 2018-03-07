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

    public static IdleJob editor(Runnable editor) {
        return new IdleJob() {
            @Override
            public boolean imageIsModified() {
                return true;
            }

            @Override
            public AsyncTask<Void, Void, Void> task() {
                return new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected void onPreExecute() {
                        editor.run();
                    }

                    @Override
                    protected Void doInBackground(Void... params) {
                        // do nothing.
                        return null;
                    }
                };
            }
        };
    }
}
