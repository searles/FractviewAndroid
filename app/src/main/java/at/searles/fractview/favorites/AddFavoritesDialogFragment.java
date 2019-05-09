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

import at.searles.fractview.R;
import at.searles.fractview.provider.FractalProviderFragment;

// This is practically the same as the IntDialogFragment, except for the parser...
public class AddFavoritesDialogFragment extends DialogFragment {

    public static AddFavoritesDialogFragment newInstance() {
        Bundle b = new Bundle();

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

        return dialog;
    }

    private boolean onOkClick(Button okButton, EditText editor, TextView msgTextView) {
        String name = editor.getText().toString();

        if(name.isEmpty()) {
            msgTextView.setText("Name must not be empty");
            return false;
        }

        FractalProviderFragment parent = (FractalProviderFragment) getParentFragment();
        parent.addToFavorites(name);

        return true;
    }
}
