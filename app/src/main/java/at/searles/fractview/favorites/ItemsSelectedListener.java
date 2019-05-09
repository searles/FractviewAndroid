package at.searles.fractview.favorites;

import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.AbsListView;
import android.widget.ListView;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractview.Commons;
import at.searles.fractview.R;
import at.searles.fractview.ui.DialogHelper;
import at.searles.fractview.utils.CharUtil;

/**
 * If sth is selected. Maintains all selected elements.
 */
class ItemsSelectedListener implements AbsListView.MultiChoiceModeListener {
    private final ListView listView;
    private final FavoritesAccessor accessor;

    ItemsSelectedListener(ListView listView, FavoritesAccessor accessor) {
        this.listView = listView;
        this.accessor = accessor;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        final int RENAME_INDEX_IN_MENU = 2;
        final int SELECT_PREFIX_INDEX_IN_MENU = 4;
        final int EDIT_PREFIX_INDEX_IN_MENU = 5;

        MenuItem renameItem = mode.getMenu().getItem(RENAME_INDEX_IN_MENU);
        MenuItem selectPrefixItem = mode.getMenu().getItem(SELECT_PREFIX_INDEX_IN_MENU);
        MenuItem editPrefixItem = mode.getMenu().getItem(EDIT_PREFIX_INDEX_IN_MENU);

        int selectCount = listView.getCheckedItemCount();

        mode.setTitle(selectCount + " selected");

        renameItem.setEnabled(selectCount == 1);

        boolean prefixOptionsEnabled = selectCount > 1;

        editPrefixItem.setEnabled(prefixOptionsEnabled);
        selectPrefixItem.setEnabled(prefixOptionsEnabled);
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
