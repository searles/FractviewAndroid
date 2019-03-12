package at.searles.fractview.parameters.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import at.searles.fractview.R;

public class LoadSharedPreferenceDialogFragment extends DialogFragment {

    private static final String PREFS_NAME_LABEL = "prefsName";

    public static LoadSharedPreferenceDialogFragment newInstance(String prefsName) {
        LoadSharedPreferenceDialogFragment ft = new LoadSharedPreferenceDialogFragment();

        Bundle args = new Bundle();
        args.putString(PREFS_NAME_LABEL, prefsName);

        ft.setArguments(args);

        return ft;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        // TODO Layout
        builder.setView(R.layout.save_item_layout);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // will be removed later
                throw new IllegalArgumentException();
            }
        });

        // TODO Text
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dismiss();
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // TODO
                        dismiss();
                    }
                }
        );

        return dialog;
    }
}
