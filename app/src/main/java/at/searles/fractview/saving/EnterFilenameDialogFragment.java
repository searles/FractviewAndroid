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
//import at.searles.utils.CharUtil;

public class EnterFilenameDialogFragment extends DialogFragment {

    private static final String BITMAP_FRAGMENT_TAG_KEY = "asdon";
    public static final String FILE_EXTENSION = ".png";
    
    private static final String RESERVED_CHARS = "|\\?*<\":>+[]/'";

    public static EnterFilenameDialogFragment newInstance(String bitmapFragmentTag) {
        EnterFilenameDialogFragment fragment = new EnterFilenameDialogFragment();

        Bundle bundle = new Bundle();

        bundle.putString(BITMAP_FRAGMENT_TAG_KEY, bitmapFragmentTag);

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

    public static File getMediaDirectory() {
        return new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "Fractview");
    }

    private void saveToMedia(DialogInterface d) {
        // check "bookmark"-checkbox.
        EditText editText = ((AlertDialog) d).findViewById(R.id.filenameEditText);
        CheckBox addToFavoritesCheckBox = (CheckBox) ((AlertDialog) d).findViewById(R.id.addToFavoritesCheckBox);

        String filenamePrefix = filenameWithoutExtension(editText.getText().toString());

        boolean addToFavorites = addToFavoritesCheckBox.isChecked();

        if (addToFavorites) {
            ((FractalProviderFragment) getParentFragment()).addToFavorites(filenamePrefix);
            // TODO
            //((MainActivity) getActivity()).saveFavorite(filenamePrefix);
        }

        File directory = getMediaDirectory();

        if (!directory.exists()) {
            if (!directory.mkdir()) {
                DialogHelper.error(getActivity(), "Could not create directory");
                return;
            }
        }

        File imageFile = new File(directory, filenamePrefix + FILE_EXTENSION);

        while (imageFile.exists()) {
            filenamePrefix = CharUtil.nextIndex(filenamePrefix);
            imageFile = new File(directory, filenamePrefix + FILE_EXTENSION);
        }

        String bitmapFragmentTag = getArguments().getString(BITMAP_FRAGMENT_TAG_KEY);

        // fixme
//        FractalCalculator fractalCalculator = (FractalCalculator) getFragmentManager().findFragmentByTag(bitmapFragmentTag);
//
//        fractalCalculator.registerFragmentAsChild(SaveBitmapToMediaFragment.newInstance(filenamePrefix), SaveInBackgroundFragment.SAVE_FRAGMENT_TAG);
    }
}
