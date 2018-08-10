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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.R;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.main.FractalFragment;
import at.searles.math.Scale;

// This is practically the same as the IntDialogFragment, except for the parser...
public class ScaleDialogFragment extends DialogFragment {

    private static final String VALUE_KEY = "value";
    private static final String TITLE_KEY = "title";
    private static final String ID_KEY = "id";

    public static ScaleDialogFragment newInstance(String title, String id, Scale value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(ID_KEY, id);
        b.putDoubleArray(VALUE_KEY, BundleAdapter.toArray(value));

        ScaleDialogFragment fragment = new ScaleDialogFragment();
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        String title = getArguments().getString(TITLE_KEY);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        if (title != null) {
            builder.setTitle(title);
        }

        // null is ok in this context.
        @SuppressLint("InflateParams")
        View view = getActivity().getLayoutInflater().inflate(R.layout.scale_editor, null);

        builder.setView(view);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });

        builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                throw new IllegalArgumentException();
            }
        });

        AlertDialog dialog = builder.create();

        dialog.show();

        Button okButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        TextView msgTextView = (TextView) view.findViewById(R.id.msgTextView);

        okButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (onOkClick(okButton, view, msgTextView)) {
                            dismiss();
                        }
                    }
                }
        );

        initEditorWatchers(view, okButton, msgTextView);

        // init values in view of dialog
        if (savedInstanceState == null) {
            Scale sc = BundleAdapter.scaleFromArray(getArguments().getDoubleArray(VALUE_KEY));
            setValue(sc, view);
        }

        return dialog;
    }

    private boolean onOkClick(Button okButton, View view, TextView msgTextView) {
        try {
            Scale value = getValue(view);

            // success

            FractalFragment fractalFragment = (FractalFragment) getParentFragment();
            String id = getArguments().getString(ID_KEY);
            fractalFragment.provider().set(new ParameterKey(id, ParameterType.Real), value);

            return true;
        } catch (NumberFormatException e) {
            msgTextView.setText(e.getMessage());
            msgTextView.setVisibility(View.VISIBLE);
            okButton.setEnabled(false);
            return false;
        }
    }

    private void initEditorWatchers(View view, Button okButton, TextView msgTextView) {
        EditText editorXX = (EditText) view.findViewById(R.id.xxEditText);
        EditText editorXY = (EditText) view.findViewById(R.id.xyEditText);
        EditText editorYX = (EditText) view.findViewById(R.id.yxEditText);
        EditText editorYY = (EditText) view.findViewById(R.id.yyEditText);
        EditText editorCX = (EditText) view.findViewById(R.id.cxEditText);
        EditText editorCY = (EditText) view.findViewById(R.id.cyEditText);

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(true);
                msgTextView.setVisibility(View.INVISIBLE);
            }
        };

        editorXX.addTextChangedListener(watcher);
        editorXY.addTextChangedListener(watcher);
        editorYX.addTextChangedListener(watcher);
        editorYY.addTextChangedListener(watcher);
        editorCX.addTextChangedListener(watcher);
        editorCY.addTextChangedListener(watcher);
    }

    private void setValue(Scale sc, View view) {
        EditText editorXX = (EditText) view.findViewById(R.id.xxEditText);
        EditText editorXY = (EditText) view.findViewById(R.id.xyEditText);
        EditText editorYX = (EditText) view.findViewById(R.id.yxEditText);
        EditText editorYY = (EditText) view.findViewById(R.id.yyEditText);
        EditText editorCX = (EditText) view.findViewById(R.id.cxEditText);
        EditText editorCY = (EditText) view.findViewById(R.id.cyEditText);

        editorXX.setText(String.valueOf(sc.xx));
        editorXY.setText(String.valueOf(sc.xy));
        editorYX.setText(String.valueOf(sc.yx));
        editorYY.setText(String.valueOf(sc.yy));
        editorCX.setText(String.valueOf(sc.cx));
        editorCY.setText(String.valueOf(sc.cy));
    }

    private Scale getValue(View view) {
        EditText editorXX = (EditText) view.findViewById(R.id.xxEditText);
        EditText editorXY = (EditText) view.findViewById(R.id.xyEditText);
        EditText editorYX = (EditText) view.findViewById(R.id.yxEditText);
        EditText editorYY = (EditText) view.findViewById(R.id.yyEditText);
        EditText editorCX = (EditText) view.findViewById(R.id.cxEditText);
        EditText editorCY = (EditText) view.findViewById(R.id.cyEditText);

        double xx = Double.parseDouble(editorXX.getText().toString());
        double xy = Double.parseDouble(editorXY.getText().toString());
        double yx = Double.parseDouble(editorYX.getText().toString());
        double yy = Double.parseDouble(editorYY.getText().toString());
        double cx = Double.parseDouble(editorCX.getText().toString());
        double cy = Double.parseDouble(editorCY.getText().toString());

        return new Scale(xx, xy, yx, yy, cx, cy);
    }
}
