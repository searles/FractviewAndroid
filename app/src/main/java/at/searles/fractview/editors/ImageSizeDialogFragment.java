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

import java.util.Locale;

import at.searles.fractview.MainActivity;
import at.searles.fractview.R;
import at.searles.fractview.bitmap.BitmapFragment;
import at.searles.fractview.ui.DialogHelper;

public class ImageSizeDialogFragment extends DialogFragment {

    private static final String BITMAP_FRAGMENT_KEY = "bitmapfragment";
    private static final String RATIO_KEY = "ratio";
    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";

    public static ImageSizeDialogFragment newInstance(String bitmapFragmentTag, int width, int height) {
        ImageSizeDialogFragment fragment = new ImageSizeDialogFragment();

        Bundle arguments = new Bundle();

        arguments.putString(BITMAP_FRAGMENT_KEY, bitmapFragmentTag);
        arguments.putInt(WIDTH_KEY, width);
        arguments.putInt(HEIGHT_KEY, height);
        arguments.putDouble(RATIO_KEY, ((double) width) / (double) height);

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

        // Some layout fixes (same size for labels eg)

        // show initial size in editor
        int width = getArguments().getInt(WIDTH_KEY);
        int height = getArguments().getInt(HEIGHT_KEY);

        initSizeView(dialogView, width, height);
        initSizeModeSpinnerView(dialogView);
        initRatioToggleButton(dialogView);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setPositiveButton("Resize", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setImageSize(dialog);
                dismiss();
            }
        });

        return builder.create();
    }

    private void initSizeModeSpinnerView(View dialogView) {
        // set up buttons
        Spinner sizeModeSpinner = (Spinner) dialogView.findViewById(R.id.sizeModeSpinner);

        sizeModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0:
                        initScreenSize(dialogView);
                        break;
                    case 1:
                        initDefaultSize(dialogView);
                        break;
                    case 2:
                        initCustomSize(dialogView);
                        break;
                    default:
                        throw new UnsupportedOperationException("unknown position: " + position);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // do nothing.
            }
        });

        sizeModeSpinner.setSelection(2);
    }

    private void initSizeView(View dialogView, int width, int height) {
        EditText widthEditText = (EditText) dialogView.findViewById(R.id.widthEditText);
        EditText heightEditText = (EditText) dialogView.findViewById(R.id.heightEditText);

        widthEditText.setText(String.format(Locale.getDefault(), "%d", width));
        heightEditText.setText(String.format(Locale.getDefault(), "%d", height));
    }

    private void initRatioToggleButton(View dialogView) {
        // FIXME
        //ToggleButton keepRatioToggleButton = (ToggleButton) dialogView.findViewById(R.id.keepRatioToggle);

        // FIXME add logic for button
    }

    private void initDefaultSize(View dialogView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int width = prefs.getInt(MainActivity.WIDTH_LABEL, -1);
        int height = prefs.getInt(MainActivity.HEIGHT_LABEL, -1);

        if(width == -1 || height == -1) {
            initScreenSize(dialogView);
        } else {
            initSizeView(dialogView, width, height);
            enableInput(dialogView, false);
        }
    }

    private void initScreenSize(View dialogView) {
        Point dim = new Point();
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);

        if(wm == null) {
            DialogHelper.error(getActivity(), "Cannot determine screen size");
            return;
        }

        wm.getDefaultDisplay().getSize(dim);

        initSizeView(dialogView, dim.x, dim.y);
        enableInput(dialogView, false);
    }

    private void initCustomSize(View dialogView) {
        enableInput(dialogView, true);
    }

    private void enableInput(View dialogView, boolean enabled) {
        EditText widthView = (EditText) dialogView.findViewById(R.id.widthEditText);
        EditText heightView = (EditText) dialogView.findViewById(R.id.heightEditText);

        widthView.setEnabled(enabled);
        heightView.setEnabled(enabled);
    }

    private void storeDefaultSize(int width, int height) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
        editor.putInt(MainActivity.WIDTH_LABEL, width);
        editor.putInt(MainActivity.HEIGHT_LABEL, height);
        editor.apply();
    }

    private void setImageSize(DialogInterface d) {
        EditText widthView = (EditText) ((AlertDialog) d).findViewById(R.id.widthEditText);
        EditText heightView = (EditText) ((AlertDialog) d).findViewById(R.id.heightEditText);

        boolean storeAsDefault = ((CheckBox) ((AlertDialog) d).findViewById(R.id.saveAsDefaultCheckBox)).isChecked();

        int w, h;

        try {
            w = Integer.parseInt(widthView.getText().toString());
            h = Integer.parseInt(heightView.getText().toString());
        } catch(NumberFormatException e) {
            DialogHelper.error(((AlertDialog) d).getContext(), "Size contains invalid values");
            return;
        }

        if(w < 1 || h < 1) {
            DialogHelper.error(((AlertDialog) d).getContext(), "Width and height must be at least 1");
            return;
        }

        if(w == getArguments().getInt(WIDTH_KEY) || h == getArguments().getInt(HEIGHT_KEY)) {
            DialogHelper.info(((AlertDialog) d).getContext(), "Size not changed.");
        } else {
            // fetch fragment and change size
            BitmapFragment fragment = (BitmapFragment) getFragmentManager().findFragmentByTag(getArguments().getString(BITMAP_FRAGMENT_KEY));

            if(!fragment.setSize(w, h)) {
                DialogHelper.error(((AlertDialog) d).getContext(), "Image is too large.");
                return;
            }
        }

        if(storeAsDefault) {
            storeDefaultSize(w, h);
        }
    }
}
