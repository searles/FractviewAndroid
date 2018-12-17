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
import at.searles.fractview.main.FractalProviderFragment;

public class IntegerDialogFragment extends DialogFragment {

    private static final String VALUE_KEY = "value";
    private static final String TITLE_KEY = "title";
    private static final String ID_KEY = "id";
    private static final String OWNER_KEY = "owner";

    public static IntegerDialogFragment newInstance(String title, String id, int owner, int value) {
        Bundle b = new Bundle();

        b.putString(TITLE_KEY, title);
        b.putString(ID_KEY, id);
        b.putInt(OWNER_KEY, owner);
        b.putInt(VALUE_KEY, value);

        IntegerDialogFragment fragment = new IntegerDialogFragment();
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
        View view = getActivity().getLayoutInflater().inflate(R.layout.editor_int, null);

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
        EditText editor = view.findViewById(R.id.intEditText);
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
            int value = getArguments().getInt(VALUE_KEY);
            editor.setText(String.valueOf(value));
        }

        return dialog;
    }

    private boolean onOkClick(Button okButton, EditText editor, TextView msgTextView) {
        try {
            int value = Integer.parseInt(editor.getText().toString());

            // success

            FractalProviderFragment fractalProviderFragment = (FractalProviderFragment) getParentFragment();
            String id = getArguments().getString(ID_KEY);
            int owner = getArguments().getInt(OWNER_KEY);
            fractalProviderFragment.setParameterValue(id, owner, value);

            return true;
        } catch (NumberFormatException e) {
            msgTextView.setText(e.getMessage());
            msgTextView.setVisibility(View.VISIBLE);
            okButton.setEnabled(false);
            return false;
        }
    }
}
