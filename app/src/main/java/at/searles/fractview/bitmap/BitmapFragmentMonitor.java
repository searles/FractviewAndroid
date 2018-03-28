package at.searles.fractview.bitmap;

/**
 * XXX I guess the name is misleading. it just returns the status. No need for a full-blown fragment.
 */

public interface BitmapFragmentMonitor {
    BitmapFragment.Status status();
    float progress();
}
