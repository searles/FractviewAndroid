package at.searles.fractview.editors;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

/**
 * Shows a string while some action is in progress.
 * There can be an optional skip button. If there
 * is one, then there is also a cancel button that
 * will dismiss the dialog.
 */
public class ProgressDialogFragment extends GenericDialogFragment {


    public static ProgressDialogFragment newInstance(int requestCode, boolean callFragment, String title,
                                                     String message, boolean skippable) {
        Bundle b = GenericDialogFragment.createBundle(requestCode, callFragment, title);

        b.putString("message", message);
        b.putBoolean("skippable", skippable);

        ProgressDialogFragment fragment = new ProgressDialogFragment();
        fragment.setArguments(b);

        return fragment;
    }

    private boolean isSkippable() {
        return getArguments().getBoolean("skippable");
    }

    private String message() {
        return getArguments().getString("message");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(isSkippable());
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d("PDF", "onStart");
    }

    @Override
    public void onResume() {
        Log.d("PDF", "onResume");
        super.onResume();

        if(isClosed()) {
            Log.d("PDF", "isClosed is true. Just created new dialog, but I have to dismiss it");
            getDialog().dismiss(); // dismiss it right away.
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Log.d("PDF", "onCreateDialog was called");
        ProgressDialog dialog = new ProgressDialog(getActivity());

        dialog.setIndeterminate(true);

        String title = title();
        if(title != null) dialog.setTitle(title);

        String message = message();
        if(message != null) dialog.setMessage(message);

        if(isSkippable()) {
            dialog.setButton(ProgressDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int which) {
                    dialogInterface.dismiss(); // fixme: Is this one needed?
                }
            });

            dialog.setButton(ProgressDialog.BUTTON_NEUTRAL, "Skip", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    getCallback().onSkip(requestCode());
                }
            });
        }

        return dialog;
    }

    private ProgressDialogFragment.Callback getCallback() {
        if(callbackFragment()) {
            return (ProgressDialogFragment.Callback) getTargetFragment();
        } else {
            return (ProgressDialogFragment.Callback) getActivity();
        }
    }

    public interface Callback {
        /**
         * The next method is called when the skip button is pressed.
         * @param requestCode Code to identify the dialog if needed
         */
        void onSkip(int requestCode);
    }
}
