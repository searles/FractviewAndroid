package at.searles.fractview.parameters.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.R;
import at.searles.fractview.main.FractalFragment;
import at.searles.fractview.parameters.ColorView;

// This is practically the same as the IntDialogFragment, except for the parser...
public class ColorDialogFragment extends DialogFragment {

    // TODO make full screen

    private static final String VALUE_KEY = "value";
    private static final String TITLE_KEY = "title";
    private static final String ID_KEY = "id";

    public static ColorDialogFragment newInstance(String title, String id, int value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(ID_KEY, id);
        b.putInt(VALUE_KEY, value);

        ColorDialogFragment fragment = new ColorDialogFragment();
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getArguments().getString(TITLE_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if(title != null) {
            builder.setTitle(title);
        }

        // null is ok in this context.
        @SuppressLint("InflateParams")
        View view = getActivity().getLayoutInflater().inflate(R.layout.color_editor, null);

        // I initialize the view here.
        ColorView colorView = (ColorView) view.findViewById(R.id.colorView);
        EditText webcolorEditor = (EditText) view.findViewById(R.id.webcolorEditText);

        // I need listeners for both of them.
        colorView.bindToEditText(webcolorEditor); // fixme put this into colorView.

        builder.setView(view);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onOkClick(colorView);
            }
        });

        // init values in view of dialog
        if(savedInstanceState == null) {
            int value = getArguments().getInt(VALUE_KEY);
            colorView.setColor(value);
            // todo does this also set the webcolorEditorText?
        }

        AlertDialog dialog = builder.create();

        dialog.show();

        return dialog;
    }

    private void onOkClick(ColorView view) {
        int value = view.getColor();

        FractalFragment fractalFragment = (FractalFragment) getParentFragment();
        String id = getArguments().getString(ID_KEY);
        fractalFragment.provider().set(new ParameterKey(id, ParameterType.Color), value);
    }
}
