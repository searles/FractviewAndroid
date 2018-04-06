package at.searles.fractview.saving;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class SkipCancelDialogFragment extends DialogFragment {

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Rendering not finished...");
        builder.setMessage("The image will be saved when the rendering is finished.");

        builder.setNeutralButton("Skip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((SaveInBackgroundFragment) getParentFragment()).onSkip();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ((SaveInBackgroundFragment) getParentFragment()).onCancel();
            }
        });

        return builder.create();
    }
}