package at.searles.fractview.favorites;

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

import at.searles.fractal.data.FractalData;
import at.searles.fractview.R;
import at.searles.fractview.main.FractalProviderFragment;

// This is practically the same as the IntDialogFragment, except for the parser...
public class AddFavoritesDialogFragment extends DialogFragment {

    private static final String INDEX_KEY = "index";

    public static AddFavoritesDialogFragment newInstance(int index) {
        Bundle b = new Bundle();

        b.putInt(INDEX_KEY, index);

        AddFavoritesDialogFragment fragment = new AddFavoritesDialogFragment();
        fragment.setArguments(b);

        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Add to Favorites");

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
        EditText editor = ((EditText) view.findViewById(R.id.editText));
        TextView msgTextView = (TextView) view.findViewById(R.id.msgTextView);

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

        return dialog;
    }

    private boolean onOkClick(Button okButton, EditText editor, TextView msgTextView) {
        String value = editor.getText().toString();

        FractalProviderFragment fractalProviderFragment = (FractalProviderFragment) getParentFragment();

        int index = getArguments().getInt(INDEX_KEY);

        String label = fractalProviderFragment.provider().label(index);
        FractalData fractal = fractalProviderFragment.provider().get(label).toData();

        // TODO: now, store it.

        return true;
    }
}
