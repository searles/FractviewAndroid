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

import at.searles.fractview.R;
import at.searles.fractview.provider.FractalProviderFragment;
import at.searles.meelan.MeelanException;
import at.searles.meelan.ParsingException;

// This is practically the same as the IntDialogFragment, except for the parser...
public class ExprDialogFragment extends DialogFragment {

    private static final String VALUE_KEY = "value";
    private static final String TITLE_KEY = "title";
    private static final String PARAMETER_NAME_LABEL = "name";
    private static final String FRACTAL_ID_LABEL = "id";

    public static ExprDialogFragment newInstance(String title, String name, int id, String value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(PARAMETER_NAME_LABEL, name);

        b.putInt(FRACTAL_ID_LABEL, id);

        b.putString(VALUE_KEY, value);

        ExprDialogFragment fragment = new ExprDialogFragment();
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
        View view = getActivity().getLayoutInflater().inflate(R.layout.editor_string, null);

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
        EditText editor = view.findViewById(R.id.editText);
        TextView msgTextView = view.findViewById(R.id.msgTextView);

        okButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(onOkClick(okButton, editor, msgTextView)) {
                            dismiss();
                        }
                    }
                }
        );

        editor.addTextChangedListener(new TextWatcher() {
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
            String value = getArguments().getString(VALUE_KEY);
            editor.setText(value);
        }

        return dialog;
    }

    private boolean onOkClick(Button okButton, EditText editor, TextView msgTextView) {
        try {
            String value = editor.getText().toString();

            FractalProviderFragment fractalProviderFragment = (FractalProviderFragment) getParentFragment();

            String id = getArguments().getString(PARAMETER_NAME_LABEL); // fixme
            int owner = getArguments().getInt(FRACTAL_ID_LABEL);

            // the next line will throw in case of an error.
             fractalProviderFragment.setParameterValue(id, owner, value);

            return true;
        } catch (MeelanException | ParsingException e) {
            msgTextView.setText(e.getMessage());
            msgTextView.setVisibility(View.VISIBLE);
            okButton.setEnabled(false);

            return false;
        }
    }
}
