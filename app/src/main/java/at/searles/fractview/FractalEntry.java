package at.searles.fractview;

import android.graphics.Bitmap;

/**
 * Interface for all kinds of fractal entries.
 */

public interface FractalEntry {
    String title();
    Bitmap icon();
    String description();
}
