package at.searles.fractview.editors;

import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.util.Log;

/**
 * General purpose dialog fragment
 */
public class GenericDialogFragment extends DialogFragment {

    public static Bundle createBundle(
            int requestCode,
            boolean callFragment,
            String title
            ) {
        Bundle b = new Bundle();
        b.putInt("request_code", requestCode);
        b.putBoolean("call_fragment", callFragment);
        if(title != null) b.putString("title", title);

        b.putBoolean("closed", false);
        // this one is here to check whether the dialog should still
        // exist.

        return b;
    }

    public void closeDialogRequest() {
        // in createDialog, the dialog must be dismissed at creation time if this one is set.
        Log.d("GDF", "Setting closed to true.");

        getArguments().putBoolean("closed", true);

        Dialog d = getDialog();

        if(d != null) {
            Log.d("GDF", "and since dialog is not null I dismiss it");
            d.dismiss();
        } else {
            Log.d("GDF", "dialog is null");
        }
    }

    protected boolean isClosed() {
        return getArguments().getBoolean("closed");
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
