package at.searles.fractview.editors;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.IdRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;

import at.searles.fractview.MainActivity;
import at.searles.fractview.R;
import at.searles.fractview.ui.DialogHelper;

public class ImageSizeDialogFragment extends DialogFragment {

    private static final String WIDTH_KEY = "width";
    private static final String HEIGHT_KEY = "height";

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
        View dialogView = inflater.inflate(R.layout.image_size_editor, null);
        builder.setView(dialogView);

        // show initial size in editor
        int width = getArguments().getInt(WIDTH_KEY);
        int height = getArguments().getInt(HEIGHT_KEY);

        initSize(dialogView, width, height);

        // set up buttons
        RadioGroup radioGroup = (RadioGroup) dialogView.findViewById(R.id.size_mode);

        radioGroup.setOnCheckedChangeListener(
                new RadioGroup.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(RadioGroup group, @IdRes int checkedId) {
                        switch (checkedId) {
                            case R.id.default_size:
                                initDefaultSize(dialogView);
                                break;
                            case R.id.screen_size:
                                initScreenSize(dialogView);
                                break;
                            case R.id.custom_size:
                                initCustomSize(dialogView);
                                break;
                            default:
                                throw new UnsupportedOperationException("unknown id: " + checkedId);
                        }
                    }
                }
        );

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

    private void initSize(View dialogView, int width, int height) {
        EditText widthEditText = (EditText) dialogView.findViewById(R.id.widthEditText);
        EditText heightEditText = (EditText) dialogView.findViewById(R.id.heightEditText);

        widthEditText.setText(Integer.toString(width));
        heightEditText.setText(Integer.toString(height));
    }

    private void initDefaultSize(View dialogView) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        int width = prefs.getInt(MainActivity.WIDTH_LABEL, -1); // FIXME put into res
        int height = prefs.getInt(MainActivity.HEIGHT_LABEL, -1); // FIXME put into res

        if(width == -1 || height == -1) {
            initScreenSize(dialogView);
        } else {
            initSize(dialogView, width, height);
            enableInput(dialogView, false);
        }
    }

    private void initScreenSize(View dialogView) {
        Point dim = new Point();
        WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(dim);

        initSize(dialogView, dim.x, dim.y);
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

        boolean storeAsDefault = ((CheckBox) ((AlertDialog) d).findViewById(R.id.defaultCheckBox)).isChecked();

        int w, h;

        try {
            w = Integer.parseInt(widthView.getText().toString());
            h = Integer.parseInt(heightView.getText().toString());
        } catch(NumberFormatException e) {
            DialogHelper.error(((AlertDialog) d).getContext(), "Invalid Size");
            return;
        }

        if(w < 1 || h < 1) {
            DialogHelper.error(((AlertDialog) d).getContext(), "Width and height must be at least 1");
            return;
        }

        if(w == getArguments().getInt(WIDTH_KEY) || h == getArguments().getInt(HEIGHT_KEY)) {
            DialogHelper.info(((AlertDialog) d).getContext(), "Size not changed.");
        } else if(!((MainActivity) getActivity()).setImageSize(w, h)) {
            DialogHelper.error(((AlertDialog) d).getContext(), "Image is too large.");
            return;
        }
        if(storeAsDefault) {
            storeDefaultSize(w, h);
        }
    }
}
