package at.searles.fractview.parameters.dialogs;

import android.app.DialogFragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import at.searles.fractview.R;

// TODO Rename or remove.
public class LoadFullScreenDialogFragment extends DialogFragment {

    private static final String PREFS_NAME_LABEL = "prefsNameLabel";

    public static LoadFullScreenDialogFragment newInstance(String prefsName) {
        LoadFullScreenDialogFragment ft = new LoadFullScreenDialogFragment();

        Bundle args = new Bundle();
        args.putString(PREFS_NAME_LABEL, prefsName);

        ft.setArguments(args);

        return ft;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.FullScreenDialogStyle);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        // TODO

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        getActivity().getMenuInflater().inflate(R.menu.items_manager, menu);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.manage_entries_dialog, container, false);

        Toolbar toolbar = rootView.findViewById(R.id.toolbar);
        toolbar.setTitle("Dialog title");

        setHasOptionsMenu(true);

        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel);
        toolbar.setNavigationOnClickListener(view1 -> cancel());

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                getContext(),
                android.R.layout.simple_list_item_1,
                new String[]{"AAA", "BBB", "CCC", "DDD"}
        );

        ListView listView = rootView.findViewById(R.id.entries_list);
        listView.setAdapter(adapter);

        return rootView;
    }

    private void cancel() {
        // TODO
    }

//    @Override
//    public Dialog onCreateDialog(Bundle savedInstanceState) {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//
//        // TODO Layout
//        builder.setView(R.layout.save_item_layout);
//
//        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                // will be removed later
//                throw new IllegalArgumentException();
//            }
//        });
//
//        // TODO Text
//        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dismiss();
//            }
//        });
//
//        AlertDialog dialog = builder.create();
//
//        dialog.show();
//
//        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(
//                new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//                        // TODO
//                        dismiss();
//                    }
//                }
//        );
//
//        return dialog;
//    }
}
