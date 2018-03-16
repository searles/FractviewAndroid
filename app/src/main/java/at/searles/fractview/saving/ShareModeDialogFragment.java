package at.searles.fractview.saving;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Provides a dialog to call back to the activity what should
 * actually be done when the share-button is clicked.
 */
public class ShareModeDialogFragment extends DialogFragment {

    public static ShareModeDialogFragment newInstance() {
        return new ShareModeDialogFragment();
    }

    public enum Result {Share, Save, Wallpaper}

    public interface Callback {
        void onShareModeResult(Result result);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        String[] items = {"Share Image", "Save Image", "Set Image as Wallpaper"};

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0: // Share
                        ((Callback) getActivity()).onShareModeResult(Result.Share);
                        dialog.dismiss();
                        break;
                    case 1: // Save
                        ((Callback) getActivity()).onShareModeResult(Result.Share);
                        dialog.dismiss();
                        break;
                    case 2: // Set as Wallpaper
                        ((Callback) getActivity()).onShareModeResult(Result.Share);
                        dialog.dismiss();
                        break;
                    default:
                        throw new UnsupportedOperationException("no such selection: " + which);
                }
            }
        });

        builder.setCancelable(true);

        return builder.create();
    }
}