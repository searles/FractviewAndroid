package at.searles.fractview.bitmap.ui;

import android.util.Log;

import at.searles.fractview.bitmap.BitmapFragmentListener;

class BitmapFragmentListenerImpl implements BitmapFragmentListener {

    private BitmapFragmentView view;

    BitmapFragmentListenerImpl(BitmapFragmentView view) {
        this.view = view;
    }

    @Override
    public void bitmapUpdated(BitmapFragmentAccessor src) {
        view.invalidate();
    }

    @Override
    public void previewGenerated(BitmapFragmentAccessor src) {
        // can be called from outside the UI-thread!
        Log.d(getClass().getName(), "preview generated");
        view.scaleableImageView().removeLastScale();
        view.invalidate();
    }

    @Override
    public void drawerStarted(BitmapFragmentAccessor src) {
        view.startShowProgress(src);
    }

    @Override
    public void drawerFinished(long ms, BitmapFragmentAccessor src) {
        // progress bar is hidden in update task.
    }

    @Override
    public void newBitmapCreated(BitmapFragmentAccessor src) {
        view.scaleableImageView().setBitmap(src.bitmap());
        view.requestLayout();
    }
}
