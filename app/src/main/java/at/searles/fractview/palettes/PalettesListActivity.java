package at.searles.fractview.palettes;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ListView;

import at.searles.fractview.R;
import at.searles.fractview.utils.Accessor;
import at.searles.fractview.utils.SharedPreferencesAccessor;
import at.searles.math.color.Palette;

/**
 *
 */
public class PalettesListActivity extends Activity {

    public static final String FAVORITES_SHARED_PREF = "favorites";

    private static final int IMPORT_COLLECTION_CODE = 111;

    private PalettesListAdapter adapter;

    /**
     * Selected elements
     */
    private ListView listView;

    private Accessor<Palette> createAccessor() {
        SharedPreferences prefs = getSharedPreferences(, Context.MODE_PRIVATE);
        return new SharedPreferencesAccessor<>(prefs, Palette.class);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accessor);

        Accessor<Palette>  accessor = new SharedPreferencesAccessor<>(createAccessor(), Palette.class);
        this.adapter = new AccessorAdapter(this);

        initListView();
        initCloseButton();
    }

    //    private void initCloseButton() {
//        Button closeButton = (Button) findViewById(R.id.closeButton);
//        closeButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                // end this activity.
//                FavoritesActivity.this.finish();
//            }
//        });
//    }
//
//
//    private void initListView() {
//        this.listView = (ListView) findViewById(R.id.fractalListView);
//
//        listView.setAdapter(adapter);
//
//        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//            // On click return the selected fractal.
//            @Override
//            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
//                // get bookmark
//                FavoriteEntry entry = adapter.getItem(index);
//
//                if (entry.fractal != null) {
//                    // This is due to a bug in an old  version.
//                    Intent data = new Intent();
//                    data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.toBundle(entry.fractal));
//                    setResult(1, data);
//                    finish();
//                } else {
//                    DialogHelper.error(FavoritesActivity.this, "not available. If you think it should be, please provide feedback.");
//                }
//            }
//        });
//
//        // Enable selection mode
//        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
//
//        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
//            @Override
//            public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
//                final int RENAME_INDEX_IN_MENU = 2;
//                final int SELECT_PREFIX_INDEX_IN_MENU = 4;
//                final int EDIT_PREFIX_INDEX_IN_MENU = 5;
//
//                MenuItem renameItem = mode.getMenu().getItem(RENAME_INDEX_IN_MENU);
//                MenuItem selectPrefixItem = mode.getMenu().getItem(SELECT_PREFIX_INDEX_IN_MENU);
//                MenuItem editPrefixItem = mode.getMenu().getItem(EDIT_PREFIX_INDEX_IN_MENU);
//
//                int selectCount = getSelectedCount();
//                mode.setTitle(selectCount + " selected");
//
//                renameItem.setEnabled(selectCount == 1);
//
//                boolean prefixOptionsEnabled = selectCount > 1;
//
//                editPrefixItem.setEnabled(prefixOptionsEnabled);
//                selectPrefixItem.setEnabled(prefixOptionsEnabled);
//            }
//
//            @Override
//            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
//                MenuInflater inflater = mode.getMenuInflater();
//                inflater.inflate(R.menu.activity_favorites_selected, menu);
//                return true;
//            }
//
//            @Override
//            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
//                return false;
//            }
//
//            @Override
//            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
//                switch (item.getItemId()) {
//                    case R.id.action_select_all: {
//                        selectAll();
//                    }
//                    return true;
//                    case R.id.action_rename: {
//                        Iterable<String> keys = extractKeys(selected());
//
//                        String key = keys.iterator().next();
//
//                        DialogHelper.inputText(FavoritesActivity.this, "Rename " + key, key, new Commons.KeyAction() {
//                            @Override
//                            public void apply(String newKey) {
//                                newKey = SharedPrefsHelper.renameKey(FavoritesActivity.this, key, newKey, adapter.prefs);
//
//                                if (newKey != null) {
//                                    adapter.initializeAdapter();
//                                    mode.finish(); // unselect all
//                                    listView.setItemChecked(adapter.getKeyIndex(newKey), true);
//                                }
//                            }
//                        });
//                    }
//                    return true;
//                    case R.id.action_delete: {
//                        Iterable<String> keys = extractKeys(selected());
//
//                        DialogHelper.confirm(FavoritesActivity.this, "Delete",
//                                "Delete all selected favorites?",
//                                new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        for (String key : keys) {
//                                            SharedPrefsHelper.removeEntry(FavoritesActivity.this, key, adapter.prefs);
//                                        }
//                                        adapter.initializeAdapter();
//                                        mode.finish();
//                                    }
//                                });
//                    }
//                    return true;
//                    case R.id.action_export: {
//                        Iterable<FavoriteEntry> selected = selected();
//                        export(selected);
//                    }
//                    return true;
//                    case R.id.action_select_same_prefix: {
//                        String prefix = CharUtil.commonPrefix(extractKeys(selected()));
//
//                        selectPrefixRange(prefix);
//
//                        if (prefix.length() == 0) {
//                            DialogHelper.info(FavoritesActivity.this, "No common prefix - selecting all");
//                        } else {
//                            final int MAX_TITLE_LENGTH = 24;
//                            DialogHelper.info(FavoritesActivity.this, "Selecting all entries starting with \"" + CharUtil.shorten(prefix, MAX_TITLE_LENGTH) + "\"");
//                        }
//                    }
//                    return true;
//                    case R.id.action_rename_prefix: {
//                        String oldPrefix = CharUtil.commonPrefix(extractKeys(selected()));
//
//                        String title = oldPrefix.isEmpty() ? "Create common prefix" : "Change common prefix";
//
//                        DialogHelper.inputText(FavoritesActivity.this, title, oldPrefix,
//                                new Commons.KeyAction() {
//                                    @Override
//                                    public void apply(String newPrefix) {
//                                        List<String> newKeys = new LinkedList<>();
//
//                                        for (String oldKey : extractKeys(selected())) {
//                                            String newKey = newPrefix + oldKey.substring(oldPrefix.length());
//
//                                            newKeys.add(SharedPrefsHelper.renameKey(FavoritesActivity.this, oldKey, newKey, adapter.prefs));
//                                        }
//
//                                        // unselect all
//                                        mode.finish();
//
//                                        adapter.initializeAdapter();
//
//                                        // and select new entries
//                                        for (String key : newKeys) {
//                                            listView.setItemChecked(adapter.getKeyIndex(key), true);
//                                        }
//                                    }
//                                });
//                    }
//                    return true;
//                }
//
//                return false;
//            }
//
//            @Override
//            public void onDestroyActionMode(ActionMode mode) {
//
//            }
//        });
//    }
//
//    private int getSelectedCount() {
//        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
//
//        int count = 0;
//
//        for (int i = 0; i < checkedItemPositions.size(); ++i) {
//            if (checkedItemPositions.get(checkedItemPositions.keyAt(i))) {
//                count++;
//            }
//        }
//
//        return count;
//    }
//
//    private Iterable<String> extractKeys(Iterable<FavoriteEntry> entries) {
//        return new Iterable<String>() {
//            @Override
//            public Iterator<String> iterator() {
//
//                Iterator<FavoriteEntry> iterator = entries.iterator();
//
//                return new Iterator<String>() {
//                    @Override
//                    public boolean hasNext() {
//                        return iterator.hasNext();
//                    }
//
//                    @Override
//                    public String next() {
//                        return iterator.next().key;
//                    }
//                };
//            }
//        };
//    }
//
//    private Iterable<FavoriteEntry> selected() {
//        // FIXME do not create new list!
//        List<FavoriteEntry> elements = new LinkedList<>();
//
//        SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();
//
//        for (int i = 0; i < checkedItemPositions.size(); ++i) {
//            int key = checkedItemPositions.keyAt(i);
//            if (checkedItemPositions.get(key)) {
//                elements.add(adapter.getItem(key));
//            }
//        }
//
//        return elements;
//    }
//
//    private void selectAll() {
//        for (int i = 0; i < adapter.getCount(); ++i) {
//            listView.setItemChecked(i, true);
//        }
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.activity_favorites, menu);
//        return super.onCreateOptionsMenu(menu);
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle presses on the action bar items
//        switch (item.getItemId()) {
//            case R.id.action_import_collection: {
//                importCollection();
//            }
//            return true;
//            case R.id.action_select_all: {
//                selectAll();
//            }
//            return true;
//            default:
//                return super.onOptionsItemSelected(item);
//        }
//    }
//
//    private void importCollection() {
//        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
//
//        intent.addCategory(Intent.CATEGORY_OPENABLE);
//
//        intent.setType("text/*");
//
//        startActivityForResult(intent, IMPORT_COLLECTION_CODE);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//
//        if (data != null) {
//            if (requestCode == IMPORT_COLLECTION_CODE) {
//                // Fetch elements
//                Uri uri = data.getData();
//
//                try {
//                    FavoriteEntry.Collection collection = importFromUri(uri);
//                    importEntries(collection);
//                } catch(Throwable th) {
//                    th.printStackTrace();
//                    DialogHelper.error(this, th.getLocalizedMessage());
//                }
//            }
//        }
//    }
//
//    private FavoriteEntry.Collection importFromUri(Uri uri) throws FileNotFoundException {
//        // FIXME: Put into own fragment
//        BufferedReader reader = null;
//
//        try {
//            Log.d(getClass().getName(), "Importing from " + uri);
//
//            InputStream inputStream = getContentResolver().openInputStream(uri);
//            reader = new BufferedReader(new InputStreamReader(inputStream));
//
//            return Serializers.serializer().fromJson(reader, FavoriteEntry.Collection.class);
//        } finally {
//            try {
//                if(reader != null) reader.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    public void importEntries(FavoriteEntry.Collection newEntries) {
//        if (newEntries == null) {
//            DialogHelper.error(FavoritesActivity.this, "Could not read file!");
//            return;
//        }
//
//        // Find duplicates
//        Set<String> addedKeys = new TreeSet<>();
//
//        Map<String, FavoriteEntry> nonDuplicates = new HashMap<>();
//        Map<String, FavoriteEntry> duplicates = new HashMap<>();
//
//        for (Map.Entry<?, ?> entry : newEntries.getAll()) {
//            FavoriteEntry favEntry = (FavoriteEntry) entry.getValue();
//
//            String key = (String) entry.getKey();
//
//            if (adapter.prefs.contains(key)) {
//                duplicates.put(key, favEntry);
//            } else {
//                nonDuplicates.put(key, favEntry);
//            }
//        }
//
//        if (duplicates.isEmpty()) {
//            // Add non-duplicates
//            for (Map.Entry<String, FavoriteEntry> entry : nonDuplicates.entrySet()) {
//                String key = entry.getKey();
//                FavoriteEntry favEntry = entry.getValue();
//
//                // add it
//                addedKeys.add(key);
//                adapter.prefs.edit().putString(key, Serializers.serializer().toJson(favEntry)).apply();
//            }
//
//            adapter.initializeAdapter();
//            selectKeys(addedKeys);
//        } else {
//            // Ask what to do with duplicates
//            DialogHelper.showOptionsDialog(this, "Pick an option for new entries with already existing keys", new CharSequence[]{
//                    "Do not add items with existing keys",
//                    "Append index to new entries",
//                    "Overwrite existing entries"
//            }, false, new DialogInterface.OnClickListener() {
//                @Override
//                public void onClick(DialogInterface dialog, int which) {
//                    // Add non-duplicates
//                    for (Map.Entry<String, FavoriteEntry> entry : nonDuplicates.entrySet()) {
//                        String key = entry.getKey();
//                        FavoriteEntry favEntry = entry.getValue();
//
//                        // add it
//                        addedKeys.add(key);
//                        adapter.prefs.edit().putString(key, Serializers.serializer().toJson(favEntry)).apply();
//                    }
//
//                    switch (which) {
//                        case 0:
//                            break; // this is easy.
//                        case 1: {
//                            for (Map.Entry<String, FavoriteEntry> entry : duplicates.entrySet()) {
//                                String newKey = entry.getKey();
//
//                                do {
//                                    newKey = CharUtil.nextIndex(newKey);
//                                } while (adapter.prefs.contains(newKey));
//
//                                adapter.prefs.edit().putString(newKey, Serializers.serializer().toJson(entry.getValue())).apply();
//                                addedKeys.add(newKey);
//                            }
//                        }
//                        break;
//                        case 2: {
//                            for (Map.Entry<String, FavoriteEntry> entry : duplicates.entrySet()) {
//                                addedKeys.add(entry.getKey());
//                                adapter.prefs.edit().putString(entry.getKey(), Serializers.serializer().toJson(entry.getValue())).apply();
//                            }
//                        }
//                        break;
//                    }
//
//                    adapter.initializeAdapter();
//                    selectKeys(addedKeys);
//                }
//            });
//        }
//    }
//
//    /**
//     * Select entries in adapter
//     *
//     * @param keys keys of the entries that should be selected
//     */
//    private void selectKeys(Set<String> keys) {
//        // find indices and set checked-status.
//
//        for (int i = 0; i < adapter.getCount(); ++i) {
//            String key = adapter.getTitle(i);
//
//            listView.setItemChecked(i, keys.contains(key));
//        }
//    }
//
//    private void export(Iterable<FavoriteEntry> entries) {
//        // Fetch map from adapter
//        JsonWriter writer = null;
//
//        // Create a map
//        FavoriteEntry.Collection collection = new FavoriteEntry.Collection();
//
//        for (FavoriteEntry entry : entries) {
//            collection.add(entry.key(), entry);
//        }
//
//        try {
//            // Write json to temp file.
//            File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
//                    ".txt", this.getExternalCacheDir()); // extension fv for fractview
//            BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));
//            writer = new JsonWriter(bw);
//            writer.setIndent("  ");
//            Serializers.serializer().toJson(collection, FavoriteEntry.Collection.class, writer);
//
//            // Share text file
//            Uri contentUri = FileProvider.getUriForFile(this, "at.searles.fractview.fileprovider", textFile);
//
//            // after it was successfully saved, share it.
//            Intent share = new Intent(Intent.ACTION_SEND);
//            share.setType("text/plain");
//            share.putExtra(Intent.EXTRA_STREAM, contentUri);
//            startActivity(Intent.createChooser(share, "Share Collection"));
//        } catch (IOException e) {
//            e.printStackTrace();
//            DialogHelper.error(this, e.getMessage());
//        } finally {
//            try {
//                if (writer != null) writer.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//
//    public void selectPrefixRange(String prefix) {
//        int size = adapter.jsonEntries.size();
//        int l = adapter.jsonEntries.findPrefix(prefix, 0, size - 1, true);
//        if (l == -1) return; // nothing to select
//        int r = adapter.jsonEntries.findPrefix(prefix, l, size - 1, false);
//
//        for (int i = l; i <= r; ++i) {
//            listView.setItemChecked(i, true);
//        }
//    }
//

}