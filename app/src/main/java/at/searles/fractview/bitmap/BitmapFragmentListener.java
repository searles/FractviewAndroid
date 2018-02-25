package at.searles.fractview.bitmap;

import android.graphics.Bitmap;

/**
 * BitmapFragment does some callbacks to the activity that
 * created this BitmapFragment. It uses the following interface
 * for this purpose.
 */
public interface BitmapFragmentListener {
    /**
     * The view and progress bars should be updated because
     * the bitmap changed
     */
    void bitmapUpdated(BitmapFragment src);

    /**
     * The view should be updated and the view matrices reset
     * because a first preview was generated in the bitmap.
     * TODO: Maybe merge this one with drawingUpdated?
     */
    void previewGenerated(BitmapFragment src);

    /**
     * We will now start a new calc. This one is called from the UI-thread.
     */
    void drawerStarted(BitmapFragment src);

    /**
     * Called when the calculation is finished (and it was not cancelled)
     * @param ms milliseconds
     */
    void drawerFinished(long ms, BitmapFragment src);

    /**
     * Called after a new bitmap was created.
     * @param bitmap The new bitmap
     */
    void newBitmapCreated(Bitmap bitmap, BitmapFragment src);
}