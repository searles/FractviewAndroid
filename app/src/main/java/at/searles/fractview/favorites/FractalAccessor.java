package at.searles.fractview.favorites;

import at.searles.fractal.entries.FavoriteEntry;

public interface FractalAccessor {
    /**
     * Number of elements
     */
    int entriesCount();

    default FavoriteEntry valueAt(int position) {
        return value(keyAt(position));
    }

    String keyAt(int position);

    FavoriteEntry value(String key);
}
