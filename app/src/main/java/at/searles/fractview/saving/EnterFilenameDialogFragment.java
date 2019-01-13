package at.searles.fractview.saving;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.io.File;

import at.searles.fractview.R;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.ui.DialogHelper;
import at.searles.fractview.utils.CharUtil;

public class EnterFilenameDialogFragment extends DialogFragment {

    public static final String TAG = "filename.tag";
    public static final String FILE_EXTENSION = ".png";
    
    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";

    public static EnterFilenameDialogFragment newInstance() {
        EnterFilenameDialogFragment fragment = new EnterFilenameDialogFragment();
        Bundle bundle = new Bundle();

        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        builder.setTitle("Enter Filename");

        // null is ok here because there is no parent yet (ie the dialog)
        @SuppressLint("InflateParams") View dialogView = inflater.inflate(R.layout.save_image_layout, null);
        builder.setView(dialogView);

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

        dialog.show();

        initializeValidator(dialog);

        return dialog;
    }

    private void initializeValidator(AlertDialog dialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);

        EditText editText = dialog.findViewById(R.id.filenameEditText);

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
        if (filename.endsWith(FILE_EXTENSION) || filename.endsWith(FILE_EXTENSION)) {
            return filename.substring(0, filename.length() - 4);
        }

        return filename;
    }

    private boolean checkValidity(DialogInterface d) {
        EditText editText = ((AlertDialog) d).findViewById(R.id.filenameEditText);

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
        TextView messageTextView = ((AlertDialog) d).findViewById(R.id.messageTextView);
        messageTextView.setText(message);
    }

    private static File getMediaDirectory() {
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Fractview");
    }

    private void saveToMedia(DialogInterface d) {
        // check "bookmark"-checkbox.
        EditText editText = ((AlertDialog) d).findViewById(R.id.filenameEditText);
        CheckBox addToFavoritesCheckBox = ((AlertDialog) d).findViewById(R.id.addToFavoritesCheckBox);

        String filenamePrefix = filenameWithoutExtension(editText.getText().toString());

        boolean addToFavorites = addToFavoritesCheckBox.isChecked();

        if (addToFavorites) {
            ((FractalProviderFragment) getParentFragment()).addToFavorites(filenamePrefix);
        }

        File directory = getMediaDirectory();

        if (!directory.exists()) {
            if (!directory.mkdir()) {
                DialogHelper.error(getActivity(), "Could not create directory");
                return;
            }
        }

        File imageFile = getFile(filenamePrefix);

        while (imageFile.exists()) {
            filenamePrefix = CharUtil.nextIndex(filenamePrefix);
            imageFile = getFile(filenamePrefix);
        }

        ((FractalProviderFragment) getParentFragment()).saveBitmap(filenamePrefix);
    }

    public static File getFile(String filenamePrefix) {
        File directory = getMediaDirectory();
        return new File(directory, filenamePrefix + EnterFilenameDialogFragment.FILE_EXTENSION);
    }
}
