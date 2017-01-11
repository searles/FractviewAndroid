package at.searles.fractview.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * This is an implementation of a DialogFragment for Progress Dialogs that are preserved on rotation.
 *
 */
public class MyProgressDialogFragment extends DialogFragment {
    // see https://developer.android.com/reference/android/app/DialogFragment.html

    public interface DialogHandler {
        void onSkip(DialogInterface dialog, int id);
    }

    /**
     *
     * @param title
     * @param message
     * @param skipable
     * @param cancelable
     * @param id Id is used for callbacks, in this case there is only onSkip. It can be used
     *           to determine why the dialog was created in the first place.
     * @return
     */
    public static MyProgressDialogFragment newInstance(String title,
                                                       String message,
                                                       boolean skipable,
                                                       boolean cancelable,
                                                       int id) {
        MyProgressDialogFragment frag = new MyProgressDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("message", message);
        args.putBoolean("skipable", skipable);
        args.putBoolean("cancelable", cancelable);
        args.putInt("id", id);
        frag.setArguments(args);
        return frag;
    }

    /**
     * For progress dialogs an individual tag can be set so that
     * they can be accessed individually.
     * @param activity The activity
     * @param tag Tag to be used for the fragment manager.
     */
    public void showDialog(Activity activity, String tag) {
        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = activity.getFragmentManager().beginTransaction();
        Fragment prev = activity.getFragmentManager().findFragmentByTag(tag);
        if (prev != null) {
            ft.remove(prev);
        }

        ft.addToBackStack(null);

        // Create and show the dialog.
        show(ft, "dialog");
    }

    @Override
    public ProgressDialog onCreateDialog(Bundle savedInstanceState) {
        // Fetch data
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");
        boolean skipable = getArguments().getBoolean("skipable");
        boolean cancelable = getArguments().getBoolean("cancelable");
        int id = getArguments().getInt("id");

        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(true);
        dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        if(skipable) {
            dialog.setButton(DialogInterface.BUTTON_NEUTRAL, "Skip", (source, which) -> {
                ((DialogHandler) getActivity()).onSkip(dialog, id);
                source.dismiss();
            });
        }

        if(cancelable) {
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", (source, which) -> {
                source.dismiss();
            });
        }


        return dialog;
    }
}
