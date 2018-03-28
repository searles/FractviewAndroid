package at.searles.fractview.bitmap.ui;

import android.graphics.Bitmap;

public interface BitmapFragmentAccessor {
    float progress();
    boolean isRunning();
    Bitmap bitmap();
}
