package at.searles.fractview.saving;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import at.searles.fractview.MainActivity;
import at.searles.fractview.R;
import at.searles.fractview.ui.DialogHelper;
import at.searles.utils.CharUtil;

public class SaveAsDialogFragment extends DialogFragment {

    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";

    public static SaveAsDialogFragment newInstance() {
        SaveAsDialogFragment fragment = new SaveAsDialogFragment();

        Bundle bundle = new Bundle();

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // null is ok here because there is no parent yet (ie the dialog)
        View dialogView = inflater.inflate(R.layout.save_image_layout, null);
        builder.setView(dialogView);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkValidity(dialog)) {
                    saveToMedia(dialog);
                    dismiss();
                }
            }
        });

        // disable button unless there is a valid filename

        AlertDialog dialog = builder.create();

        // XXX there must be a better way to get the positive button.
        dialog.show();

        initializeValidator(dialog);

        return dialog;
    }

    private void initializeValidator(AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        EditText editText = (EditText) dialog.findViewById(R.id.filenameEditText);

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(checkValidity(dialog));
            }
        });
    }

    private String filenameWithoutExtension(String filename) {
        if (filename.endsWith(".png") || filename.endsWith(".PNG")) {
            return filename.substring(0, filename.length() - 4);
        }

        return filename;
    }

    private boolean checkValidity(DialogInterface d) {
        EditText editText = (EditText) ((AlertDialog) d).findViewById(R.id.filenameEditText);

        String filename = filenameWithoutExtension(editText.getText().toString());

        if(filename.isEmpty()) {
            setMessage(" filename must not be empty.", d);
            return false;
        }

        for(int i = 0; i < filename.length(); ++i) {
            char ch = filename.charAt(i);
            if(RESERVED_CHARS.indexOf(ch) != -1 || ch < 32) {
                setMessage(" invalid character at position " + i + ".", d);
                return false;
            }
        }

        setMessage("", d);

        return true;
    }

    private void setMessage(String message, DialogInterface d) {
        TextView messageTextView = (TextView) ((AlertDialog) d).findViewById(R.id.messageTextView);
        messageTextView.setText(message);
    }

    private void saveToMedia(DialogInterface d) {
        // check "bookmark"-checkbox.
        EditText editText = (EditText) ((AlertDialog) d).findViewById(R.id.filenameEditText);
        CheckBox checkBox = (CheckBox) ((AlertDialog) d).findViewById(R.id.addToFavoritesCheckBox);

        String filename = filenameWithoutExtension(editText.getText().toString()) + ".png";

        boolean addToFavorites = checkBox.isChecked();

        if (addToFavorites) {
            ((MainActivity) getActivity()).saveFavorite(filename);
        }

        File directory = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Fractview");

        Log.d("MA", "Saving file: Path is " + directory);

        if (!directory.exists()) {
            Log.d("MA", "Creating directory");
            if (!directory.mkdir()) {
                DialogHelper.error(getActivity(), "Could not create directory");
            }
        }

        File imageFile = new File(directory, filename + ".png");

        while (imageFile.exists()) {
            filename = CharUtil.nextIndex(filename);
            imageFile = new File(directory, filename + ".png");
        }

        // Saving is done in the following plugin
        // FIXME SaveFragment.createSave(imageFile).init(bitmapFragment);
    }
}
