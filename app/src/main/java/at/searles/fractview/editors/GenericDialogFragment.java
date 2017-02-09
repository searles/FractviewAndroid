package at.searles.fractview.editors;

import android.app.DialogFragment;
import android.os.Bundle;

/**
 * General purpose dialog fragment
 */
public abstract class GenericDialogFragment extends DialogFragment {

    public static Bundle createBundle(
            int requestCode,
            boolean callFragment,
            String title) {
        Bundle b = new Bundle();
        b.putInt("request_code", requestCode);
        b.putBoolean("call_fragment", callFragment);
        if(title != null) b.putString("title", title);

        b.putBoolean("closed", false);
        // this one is here to check whether the dialog should still
        // exist.

        return b;
    }

    protected boolean callbackFragment() {
        return getArguments().getBoolean("call_fragment");
    }

    /**
     * @return may return null if there is no title
     */
    protected String title() {
        if(getArguments().containsKey("title")) {
            return getArguments().getString("title");
        } else {
            return null;
        }
    }

    protected int requestCode() {
        return getArguments().getInt("request_code");
    }
}
