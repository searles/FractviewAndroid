package at.searles.fractview.editors;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

import at.searles.fractview.R;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.ui.DialogHelper;

/**
 * This dialog allows to pick a size and at the same time save it as default.
 * It offers to use the available image screen size, the currently stored default size
 * (which is the screen size if nothing is saved) and to freely set an arbitrary size.
 * If the size is too large, an error message is displayed.
 */
public class ImageSizeDialogFragment extends DialogFragment {

    private static final int MIN_VALUE = 1;
    private static final int MAX_VALUE = 999999;

    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";
    public static final String TAG = "ImageSizeDialogFragment";

    public static ImageSizeDialogFragment newInstance(int width, int height) {
        ImageSizeDialogFragment fragment = new ImageSizeDialogFragment();

        Bundle arguments = new Bundle();

        arguments.putInt(WIDTH_KEY, width);
        arguments.putInt(HEIGHT_KEY, height);

        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        // null is ok here because there is no parent yet (ie the dialog)
        @SuppressLint("InflateParams")
        View dialogView = inflater.inflate(R.layout.image_size_layout, null);
        builder.setView(dialogView);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setPositiveButton("Resize", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                throw new IllegalArgumentException(""); // will not be called.
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(onOkClick(dialog)) {
                            dismiss();
                        }
                    }
                }
        );

        initializeViews(dialog);

        return dialog;
    }

    private void initializeViews(AlertDialog dialog) {
        // initialize logic of view
        // show initial size in editor
        int width = getArguments().getInt(WIDTH_KEY);
        int height = getArguments().getInt(HEIGHT_KEY);

        initSizeView(dialog, width, height);
        initSizeModeSpinnerView(dialog);
    }

    private int[] dimensions(AlertDialog dialog) {
        // fetch values
        EditText widthEditText = dialog.findViewById(R.id.widthEditText);
        EditText heightEditText = dialog.findViewById(R.id.heightEditText);

        int width = numericValue(widthEditText);
        int height = numericValue(heightEditText);

        if(width <= 0) {
            setErrorMessage(dialog, String.format("%s must be an integer from %d to %d", "Width", MIN_VALUE, MAX_VALUE));
            widthEditText.requestFocus();
            return null;
        }

        if(height <= 0) {
            setErrorMessage(dialog, String.format("%s must be an integer from %d to %d", "Height", MIN_VALUE, MAX_VALUE));
            heightEditText.requestFocus();
            return null;
        }

        return new int[]{ width, height };
    }

    private boolean onOkClick(AlertDialog dialog) {

        int dimensions[] = dimensions(dialog);

        if(dimensions == null) {
            return false;
        }

        int width = dimensions[0];
        int height = dimensions[1];

        boolean storeAsDefault = ((CheckBox) dialog.findViewById(R.id.saveAsDefaultCheckBox)).isChecked();

        if(width == getArguments().getInt(WIDTH_KEY) && height == getArguments().getInt(HEIGHT_KEY)) {
            if(storeAsDefault) {
                setErrorMessage(dialog, String.format("Size unchanged (stored size as default)"));
                storeDefaultSize(width, height);
                ((CheckBox) dialog.findViewById(R.id.saveAsDefaultCheckBox)).setChecked(false);
                return false;
            } else {
                setErrorMessage(dialog, String.format("Size unchanged"));
                return false;
            }
        }

        // fetch fragment and change size
        FractalProviderFragment parent = (FractalProviderFragment) getParentFragment();

        if(!parent.setBitmapSizeInKeyFractal(width, height)) {
            setErrorMessage(dialog, String.format("Could not change size (size too large)."));
            return false;
        }

        if(storeAsDefault) {
            // so that it would not be stored
            storeDefaultSize(width, height);
        }

        return true;
    }

    private void setErrorMessage(AlertDialog dialog, String msg) {
        TextView errorMessageTextView = dialog.findViewById(R.id.msgTextView);
        errorMessageTextView.setVisibility(View.VISIBLE);
        errorMessageTextView.setText(msg);
        errorMessageTextView.setCompoundDrawablesWithIntrinsicBounds(0, 0, android.R.drawable.stat_notify_error, 0);
    }

    private static int numericValue(EditText editText) {
        String s = editText.getText().toString().trim();

        int value = 0;

        for(int i = 0; i < s.length() && '0' <= s.charAt(i) && s.charAt(i) <= '9'; ++i) {
            if(value > MAX_VALUE) {
                return 0;
            }

            value = value * 10 + s.charAt(i) - '0';
        }

        return value;
    }

    private void initSizeModeSpinnerView(AlertDialog dialog) {
        // set up buttons
        Spinner sizeModeSpinner = dialog.findViewById(R.id.sizeModeSpinner);

        sizeModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        switchToScreenSize(dialog);
                        break;
                    case 1:
                        switchToDefaultSize(dialog);
                        break;
                    case 2:
                        switchToCustomSize(dialog);
                        break;
                    default:
                        throw new IllegalArgumentException("unknown position: " + position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing.
            }
        });

        sizeModeSpinner.setSelection(2);
    }

    private void initSizeView(AlertDialog dialog, int width, int height) {
        EditText widthEditText = dialog.findViewById(R.id.widthEditText);
        EditText heightEditText = dialog.findViewById(R.id.heightEditText);

        widthEditText.setText(String.format(Locale.getDefault(), "%d", width));
        heightEditText.setText(String.format(Locale.getDefault(), "%d", height));
    }

    private void switchToDefaultSize(AlertDialog dialog) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int width = prefs.getInt(WIDTH_KEY, -1);
        int height = prefs.getInt(HEIGHT_KEY, -1);

        if(width == -1 || height == -1) {
            switchToScreenSize(dialog);
        } else {
            initSizeView(dialog, width, height);
            enableInput(dialog, false);
        }
    }

    private void switchToScreenSize(AlertDialog dialog) {
        Point dim = new Point();
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);

        if(wm == null) {
            DialogHelper.error(getActivity(), "Cannot determine screen size");
            return;
        }

        wm.getDefaultDisplay().getSize(dim);

        initSizeView(dialog, dim.x, dim.y);
        enableInput(dialog, false);
    }

    private void switchToCustomSize(AlertDialog dialogView) {
        enableInput(dialogView, true);
    }

    private void enableInput(AlertDialog dialog, boolean enabled) {
        EditText widthView = dialog.findViewById(R.id.widthEditText);
        EditText heightView = dialog.findViewById(R.id.heightEditText);

        widthView.setEnabled(enabled);
        heightView.setEnabled(enabled);
    }

    private void storeDefaultSize(int width, int height) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putInt(WIDTH_KEY, width);
        editor.putInt(HEIGHT_KEY, height);
        editor.apply();
    }
}
