package at.searles.fractview;

import android.graphics.Bitmap;

/**
 * Interface for all kinds of fractal entries. This one is just a label type.
 */

public interface FractalLabel {
    String title();
    Bitmap icon();
    String description();
}
