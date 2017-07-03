package at.searles.fractal;

import android.graphics.Bitmap;

/**
 * Interface for all kinds of fractal entries:
 *     Presets
 *     Favorites
 *
 */

public interface FractalLabel {
    String title();
    Bitmap icon();
    String description();
}
