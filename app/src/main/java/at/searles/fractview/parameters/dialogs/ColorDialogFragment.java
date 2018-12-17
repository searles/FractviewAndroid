package at.searles.fractview.parameters.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import at.searles.fractview.R;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ColorView;
import at.searles.fractview.parameters.palettes.PaletteActivity;
import at.searles.fractview.parameters.palettes.PaletteView;

// This is practically the same as the IntDialogFragment, except for the parser...
public class ColorDialogFragment extends DialogFragment {

    // TODO make full screen

    private static final String VALUE_KEY = "value";
    private static final String TITLE_KEY = "title";

    private static final String ID_KEY = "id";
    private static final String OWNER_KEY = "owner";

    private static final String X_KEY = "x";
    private static final String Y_KEY = "y";

    /**
     * This is called from PaletteView
     */
    public static ColorDialogFragment newInstance(String title, int x, int y, int value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putInt(X_KEY, x);
        b.putInt(Y_KEY, y);
        b.putInt(VALUE_KEY, value);

        ColorDialogFragment fragment = new ColorDialogFragment();
        fragment.setArguments(b);

        return fragment;
    }

    /**
     * This is called from ParameterEditor
     */
    public static ColorDialogFragment newInstance(String title, String id, int owner, int value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(ID_KEY, id);

        b.putInt(OWNER_KEY, owner);

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
        View view = getActivity().getLayoutInflater().inflate(R.layout.editor_color, null);

        // I initialize the view here.
        ColorView colorView = view.findViewById(R.id.colorView);
        EditText webcolorEditor = view.findViewById(R.id.webcolorEditText);

        // FIXME Text does not fit

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

        FractalProviderFragment fractalProviderFragment = (FractalProviderFragment) getParentFragment();

        if(fractalProviderFragment != null) {
            // color dialog fragments are also called from the palette activity.
            String id = getArguments().getString(ID_KEY);
            int owner = getArguments().getInt(OWNER_KEY); // null if it does not exist.
            fractalProviderFragment.setParameterValue(id, owner, value);

            return;
        }

        // otherwise it is the palette activity.
        PaletteActivity activity = (PaletteActivity) getActivity();

        int x = getArguments().getInt(X_KEY);
        int y = getArguments().getInt(Y_KEY);

        activity.model().set(x, y, value);
        PaletteView paletteView = activity.findViewById(R.id.paletteView);
        paletteView.invalidate();
    }
}
