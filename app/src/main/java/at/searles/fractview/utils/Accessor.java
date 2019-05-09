package at.searles.fractview.utils;

import android.graphics.Bitmap;

import java.util.Set;

/**
 * Provides access to key-value-based data. Storage is subject of the concrete
 * implementation; might be in Json in SharedPreference.
 */
public interface Accessor<V> {
    boolean exists(String key);

    void add(String key, V value);
    void remove(String key);

    /**
     * For convenience.
     */
    void rename(String oldKey, String newKey);

// XXX maybe sometimes tags will come in handy...

    /**
     * Returns all keys.
     */
    Set<String> keys();

    /**
     * @return null iff exists(key) is false.
     */
    V get(String key);

    /**
     * @return may be null.
     */
    Bitmap icon(String key);

    /**
     * @return may be null.
     */
    String subtitle(String key);
}