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
import at.searles.fractview.main.FractalFragment;
import at.searles.math.Cplx;

// This is practically the same as the IntDialogFragment, except for the parser...
public class CplxDialogFragment extends DialogFragment {

    private static final String RE_VALUE_KEY = "re_value";
    private static final String IM_VALUE_KEY = "im_value";
    private static final String TITLE_KEY = "title";
    private static final String ID_KEY = "id";

    public static CplxDialogFragment newInstance(String title, String id, Cplx value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(ID_KEY, id);
        b.putDouble(RE_VALUE_KEY, value.re());
        b.putDouble(IM_VALUE_KEY, value.im());

        CplxDialogFragment fragment = new CplxDialogFragment();
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
        View view = getActivity().getLayoutInflater().inflate(R.layout.editor_cplx, null);

        builder.setView(view);

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
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
        EditText reEditor = ((EditText) view.findViewById(R.id.reEditText));
        EditText imEditor = ((EditText) view.findViewById(R.id.imEditText));
        TextView msgTextView = (TextView) view.findViewById(R.id.msgTextView);

        okButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(onOkClick(okButton, reEditor, imEditor, msgTextView)) {
                            dismiss();
                        }
                    }
                }
        );

        reEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(true);
                msgTextView.setVisibility(View.INVISIBLE);
            }
        });

        imEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(true);
                msgTextView.setVisibility(View.INVISIBLE);
            }
        });

        // init values in view of dialog
        if(savedInstanceState == null) {
            double reValue = getArguments().getDouble(RE_VALUE_KEY);
            double imValue = getArguments().getDouble(IM_VALUE_KEY);
            reEditor.setText(String.valueOf(reValue));
            imEditor.setText(String.valueOf(imValue));
        }

        return dialog;
    }

    private boolean onOkClick(Button okButton, EditText reEditor, EditText imEditor, TextView msgTextView) {
            double reValue;

            try {
                reValue = Double.parseDouble(reEditor.getText().toString());
            } catch (NumberFormatException e) {
                msgTextView.setText(R.string.error_invalid_real_number);
                msgTextView.setVisibility(View.VISIBLE);
                okButton.setEnabled(false);
                reEditor.requestFocus(); // todo is this the correct one?
                return false;
            }

            double imValue;

            try {
                imValue = Double.parseDouble(imEditor.getText().toString());
            } catch (NumberFormatException e) {
                msgTextView.setText(R.string.error_invalid_real_number);
                msgTextView.setVisibility(View.VISIBLE);
                okButton.setEnabled(false);
                imEditor.requestFocus(); // todo
                return false;
            }

            Cplx value = new Cplx(reValue, imValue);
            // success

            FractalFragment fractalFragment = (FractalFragment) getParentFragment();
            String id = getArguments().getString(ID_KEY);
            fractalFragment.provider().set(new ParameterKey(id, ParameterType.Cplx), value);

            return true;
    }
}
