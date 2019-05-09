package at.searles.fractview.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import at.searles.fractview.Commons;
import at.searles.fractview.R;
import at.searles.fractview.ui.DialogHelper;

//
public abstract class ItemsManagerActivity extends Activity {

    public static final String ID_LABEL = "id"; // will be returned with the element.

    private ArrayList<String> keyOrder;

    private boolean invalid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.items_manager);

        this.keyOrder = new ArrayList<>(); // FIXME avoid recreation on rotation
        this.invalid = true;

        initListView();
        initFilterEditor();
    }

    private void initFilterEditor() {
        EditText filterEditor = findViewById(R.id.filter_edit_text);

        filterEditor.addTextChangedListener();
    }

    private void initListView() {
        ListView listView = findViewById(R.id.entries_list);

        listView.setAdapter(new ItemsAdapter());

        listView.setOnItemClickListener(new ItemClickedListener());

        // Enable selection mode
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new ItemsSelectedListener(listView));
    }

    protected abstract Iterable<String> keys();
    protected abstract Object item(String key);
    protected abstract Drawable icon(String key);
    protected abstract String subtitle(String key);
    protected abstract void addToIndent(String key, Intent intent);

    protected void validate() {
        if(invalid) {
            keyOrder.clear();

            for(String key : keys()) {
                keyOrder.add(key);
            }

            // TODO: Sort.

            invalid = false;
        }
    }

    private class ItemClickedListener implements AdapterView.OnItemClickListener {
        // On click return the selected fractal.
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
            // get checked item.
            // This is due to a bug in an old version.
            Intent data = getIntent();
            addToIndent(keyOrder.get(index), data);
            setResult(1, data);
            finish();
        }
    }

    private class ItemsAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            validate();
            return keyOrder.size();
        }

        @Override
        public Object getItem(int position) {
            validate();
            return item(keyOrder.get(position));
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(ItemsManagerActivity.this).inflate(R.layout.list_entry_with_icon, parent);
            }

            ImageView iconView = convertView.findViewById(R.id.iconView);
            TextView titleView = convertView.findViewById(R.id.titleView);
            TextView descriptionView = convertView.findViewById(R.id.descriptionView);

            String key = keyOrder.get(position);

            Drawable icon = icon(key);
            String subtitle = subtitle(key);

            iconView.setImageDrawable(icon);

            titleView.setText(key);
            descriptionView.setText(subtitle);

            convertView.invalidate();

            return convertView;
        }
    }

    class ItemsSelectedListener implements AbsListView.MultiChoiceModeListener {
        private final ListView listView;

        ItemsSelectedListener(ListView listView) {
            this.listView = listView;
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int RENAME_INDEX_IN_MENU = 2;

            MenuItem renameItem = mode.getMenu().getItem(RENAME_INDEX_IN_MENU);

            int selectCount = listView.getCheckedItemCount();

            mode.setTitle(selectCount + " selected");

            renameItem.setEnabled(selectCount == 1);
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.items_manager_selected, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        private void rename() {
            // no mode because mode is not finished.

            // Only possible it there is one item selected.

            List<String> keys = selectedKeys();

            if(keys.size() != 1) {
                // FIXME ERROR LOG
                return;
            }

            String key = keys.get(0);

            DialogHelper.inputText(listView.getContext(), "Rename " + key, key, new Commons.KeyAction() {
                @Override
                public void apply(String newKey) {
                    // unselect old
                    listView.setItemChecked(accessor.indexOf(key), false);

                    // act
                    String confirmedNewKey = accessor.rename(key, newKey);

                    if (confirmedNewKey != null) {
                        // and reselect the new item
                        listView.setItemChecked(accessor.indexOf(newKey), true);
                    }
                }
            });
        }

        private List<String> selectedKeys() {
            SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();

            LinkedList<String> selectedKeys = new LinkedList<>();

            for (int i = 0; i < checkedItemPositions.size(); ++i) {
                int keyIndex = checkedItemPositions.keyAt(i);
                if (checkedItemPositions.get(keyIndex)) {
                    selectedKeys.add(accessor.keyAt(i));
                }
            }

            return selectedKeys;
        }

// fixme remove    private List<Integer> selectedIndices() {
//        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
//
//        LinkedList<Integer> selectedIndices = new LinkedList<>();
//
//        for (int i = 0; i < checkedItemPositions.size(); ++i) {
//            int keyIndex = checkedItemPositions.keyAt(i);
//            if (checkedItemPositions.get(keyIndex)) {
//                selectedIndices.add(i);
//            }
//        }
//
//        return selectedIndices;
//    }

        private void delete(ActionMode mode) {
            DialogHelper.confirm(listView.getContext(), "Delete",
                    "Delete selected favorites?",
                    new Runnable() {
                        @Override
                        public void run() {
                            accessor.deleteEntries(selectedKeys());
                            mode.finish();
                        }
                    });
        }

        private void selectPrefixRange(String prefix) {
            // XXX do performance test. In old IndexedMap, there was
            // a binary search optimization.
            for(int i = 0; i < accessor.entriesCount(); ++i) {
                String key = accessor.keyAt(i);
                if(CharUtil.cmpPrefix(key, prefix) == 0) {
                    listView.setItemChecked(i, true);
                }
            }
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            Context context = listView.getContext();
            switch (item.getItemId()) {
                case R.id.action_select_all:
                    // XXX is there a faster way?
                    for (int i = 0; i < listView.getCount(); ++i) {
                        listView.setItemChecked(i, true);
                    }
                    return true;
                case R.id.action_rename:
                    rename();
                    return true;
                case R.id.action_delete:
                    delete(mode);
                    return true;
                case R.id.action_export:
                    accessor.exportEntries(context, selectedKeys());
                    return true;
                case R.id.action_select_same_prefix: {
                    String prefix = CharUtil.commonPrefix(selectedKeys());

                    selectPrefixRange(prefix);

                    if (prefix.length() == 0) {
                        DialogHelper.info(context, "No common prefix - selecting all");
                    } else {
                        final int MAX_TITLE_LENGTH = 24;
                        DialogHelper.info(context, "Selecting all entries starting with \"" + CharUtil.shorten(prefix, MAX_TITLE_LENGTH) + "\"");
                    }
                }
                return true;
                case R.id.action_rename_prefix: {
                    String oldPrefix = CharUtil.commonPrefix(selectedKeys());

                    String title = oldPrefix.isEmpty() ? "Create common prefix" : "Change common prefix";

                    DialogHelper.inputText(context, title, oldPrefix,
                            new Commons.KeyAction() {
                                @Override
                                public void apply(String newPrefix) {
                                    List<String> oldKeys = selectedKeys();
                                    List<String> newKeys = new LinkedList<>();

                                    for (String oldKey : oldKeys) {
                                        String newKey = newPrefix + oldKey.substring(oldPrefix.length());
                                        newKeys.add(accessor.rename(oldKey, newKey));
                                    }

                                    // unselect all
                                    mode.finish();

                                    // and select new entries
                                    for (String key : newKeys) {
                                        listView.setItemChecked(accessor.indexOf(key), true);
                                    }
                                }
                            });
                }

                return true;
            }

            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    }


    // FIXME consider using a Fragment that maintains connectivity to this Accessor at all time.
    // It is a bit an overkill to always recreate the adapter or Accessor.
}
