package at.searles.fractview.parameters.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import java.util.Optional;

import at.searles.fractview.R;
import at.searles.fractview.palettes.ColorView;
import at.searles.fractview.palettes.PaletteActivity;
import at.searles.fractview.palettes.PaletteView;
import at.searles.fractview.provider.FractalProviderFragment;

// This is practically the same as the IntDialogFragment, except for the parser...
public class ColorDialogFragment extends DialogFragment {

    // TODO make full screen

    private static final String VALUE_KEY = "value";
    private static final String TITLE_KEY = "title";

    private static final String PARAMETER_NAME_LABEL = "name";
    private static final String FRACTAL_ID_LABEL = "id";

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
    public static ColorDialogFragment newInstance(String title, String name, int id, int value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(PARAMETER_NAME_LABEL, name);

        b.putInt(FRACTAL_ID_LABEL, id);

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

        AutoCompleteTextView textView = view.findViewById(R.id.webcolorEditText);

        ColorParser colorParser = new ColorParser();

        textView.setAdapter(new ArrayAdapter<>(
                getContext(), android.R.layout.simple_list_item_1,
                colorParser.colorNames()
        ));

        CommonListener listener = new CommonListener(colorView, textView);

        colorView.addColorEditedListener(listener);
        textView.addTextChangedListener(listener);

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
            String name = getArguments().getString(PARAMETER_NAME_LABEL);
            int id = getArguments().getInt(FRACTAL_ID_LABEL);
            fractalProviderFragment.setParameterValue(name, id, value);

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

    class CommonListener implements ColorView.Listener, TextWatcher {

        private final ColorView colorView;
        private final AutoCompleteTextView textView;
        private boolean lockColorViewCallbacks = false;
        private final ColorParser parser;

        CommonListener(ColorView colorView, AutoCompleteTextView textView) {
            this.colorView = colorView;
            this.textView = textView;

            this.parser = new ColorParser();
        }

        @Override
        public void onColorChanged(int color) {
            // from color view.
            try {
                lockColorViewCallbacks = true;

                String colorString = String.format("#%06X", (0xFFFFFF & color));
                textView.setText(colorString);
            } finally {
                lockColorViewCallbacks = false;
            }
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // ignore
        }

        @Override
        public void afterTextChanged(Editable s) {
            if(s.length() == 0) {
                return;
            }

            Optional<Integer> color = parser.parseColor(s.toString());

            if(!color.isPresent()) {
                // fixme draw red
                return;
            }

            // fixme draw green

            if(!lockColorViewCallbacks) {
                colorView.setColor(color.get());

                return;
            }
        }
    }
}
