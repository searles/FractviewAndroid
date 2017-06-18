package at.searles.fractview;

import android.app.Fragment;

/**
 * Created by searles on 18.06.17.
 */

public class SaveShareFragment extends Fragment {
    /*
    How this works:
    MainActivity gets the information that someone wants to save/share/set as wallpaper
    the image. It then creates and registers a new Fragment, this one.

    This fragment manages everything else.

    So, thing is that this one is clearly attached to a bitmap fragment,
    but it should be put into an extern element.

    MainActivity reads what should be done:
        Save + filename + bookmark.
        Share.
        Set As Wallpaper.

    Once, this has been decided, editing should not be possible.
    There should be visual feedback + decision for skip/cancel
    and once the image is finished or it was saved instantly,
    the decided action should be carried out.


    Instead, maybe a post-processor could be registered?
     */
}
