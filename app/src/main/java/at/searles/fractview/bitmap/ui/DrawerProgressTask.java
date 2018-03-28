package at.searles.fractview.bitmap.ui;

import android.os.Handler;

/**
 * Class used for updating the view on a regular basis
 */
class DrawerProgressTask implements Runnable {

    private static final long PROGRESS_UPDATE_MILLIS = 500; // TODO move to res. update the progress bar every ... ms.

    private final BitmapFragmentView view;
    private final BitmapFragmentAccessor accessor;
    private final Handler handler;

    private boolean disposed;

    DrawerProgressTask(BitmapFragmentView view, BitmapFragmentAccessor accessor) {
        this.view = view;
        this.accessor = accessor;
        this.handler = new Handler();
    }

    void dispose() {
        disposed = true;
    }

    @Override
    public void run() {
        if(accessor != null) {
            if (accessor.isRunning()) {
                if(!disposed) {
                    view.setProgress(accessor.progress());
                    handler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
                }
            } else {
                view.hideProgress();
            }
        }
    }
}
