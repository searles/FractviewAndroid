package at.searles.fractview;

import android.app.Activity;

/**
 * Created by searles on 18.06.17.
 */

public interface BitmapFragmentPlugin {
    void init(BitmapFragment fragment);
    void attachContext(Activity context);
    void detach();
}
