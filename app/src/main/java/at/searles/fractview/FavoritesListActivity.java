package at.searles.fractview;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.stream.JsonWriter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import at.searles.fractal.FavoriteEntry;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractal.gson.Serializers;
import at.searles.fractview.ui.DialogHelper;
import at.searles.utils.CharUtil;
import at.searles.utils.IndexedKeyMap;

/**
 *
 */
public class FavoritesListActivity extends Activity {

	public static final String FAVORITES_SHARED_PREF = "favorites";

	private static final int IMPORT_COLLECTION_CODE = 111;

	private FavoritesListAdapter adapter;

	/**
	 * Selected elements
	 */
	private ListView listView;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fractal_list_activity_layout);

		this.adapter = new FavoritesListAdapter(this);

		initListView();
		initCloseButton();
	}

	private void initCloseButton() {
		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				FavoritesListActivity.this.finish();
			}
		});
	}

	private void initListView() {
		this.listView = (ListView) findViewById(R.id.fractalListView);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			// On click return the selected fractal.
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				// get bookmark
				FavoriteEntry entry = adapter.getItem(index);

				if(entry.fractal() != null) {
					// This is due to a bug in an old  version.
					Intent data = new Intent();
					data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(entry.fractal()));
					setResult(1, data);
					finish();
				} else {
					DialogHelper.error(FavoritesListActivity.this, "not available. If you think it should be, please provide feedback.");
				}
			}
		});

		// Enable selection mode
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

		listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
			@Override
			public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
				final int RENAME_INDEX_IN_MENU = 2;
				final int SELECT_PREFIX_INDEX_IN_MENU = 4;
				final int EDIT_PREFIX_INDEX_IN_MENU = 5;

                MenuItem renameItem = mode.getMenu().getItem(RENAME_INDEX_IN_MENU);
                MenuItem selectPrefixItem = mode.getMenu().getItem(SELECT_PREFIX_INDEX_IN_MENU);
                MenuItem editPrefixItem = mode.getMenu().getItem(EDIT_PREFIX_INDEX_IN_MENU);

                int selectCount = getSelectedCount();
				mode.setTitle(selectCount + " selected");

                renameItem.setEnabled(selectCount == 1);

                boolean prefixOptionsEnabled = selectCount > 1;

				editPrefixItem.setEnabled(prefixOptionsEnabled);
				selectPrefixItem.setEnabled(prefixOptionsEnabled);
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				MenuInflater inflater = mode.getMenuInflater();
				inflater.inflate(R.menu.activity_favorites_selected, menu);
				return true;
			}

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				switch(item.getItemId()) {
					case R.id.action_select_all: {
						selectAll();
					} return true;
					case R.id.action_rename: {
						Iterable<String> keys = extractKeys(selected());

						String key = keys.iterator().next();

						DialogHelper.inputText(FavoritesListActivity.this, "Rename " + key, key, new Commons.KeyAction() {
							@Override
							public void apply(String newKey) {
								newKey = SharedPrefsHelper.renameKey(FavoritesListActivity.this, key, newKey, adapter.prefs);

								if(newKey != null) {
									adapter.initializeAdapter();
									mode.finish(); // unselect all
									listView.setItemChecked(adapter.getKeyIndex(newKey), true);
								}
							}
						});
					} return true;
					case R.id.action_delete: {
						Iterable<String> keys = extractKeys(selected());

						DialogHelper.confirm(FavoritesListActivity.this, "Delete",
								"Delete all selected favorites?",
								new Runnable() {
									@Override
									public void run() {
										for(String key : keys) {
											SharedPrefsHelper.removeEntry(FavoritesListActivity.this, key, adapter.prefs);
										}
										adapter.initializeAdapter();
										mode.finish();
									}
								});
					} return true;
					case R.id.action_export: {
						Iterable<FavoriteEntry> selected = selected();
						export(selected);
					} return true;
                    case R.id.action_select_same_prefix: {
                        String prefix = CharUtil.commonPrefix(extractKeys(selected()));

						selectPrefixRange(prefix);

                        if(prefix.length() == 0) {
                            DialogHelper.info(FavoritesListActivity.this, "No common prefix - selecting all");
                        } else {
                            final int MAX_TITLE_LENGTH = 24;
                            DialogHelper.info(FavoritesListActivity.this, "Selecting all entries starting with \"" + CharUtil.shorten(prefix, MAX_TITLE_LENGTH)+ "\"");
                        }
                    } return true;
                    case R.id.action_rename_prefix: {
						String oldPrefix = CharUtil.commonPrefix(extractKeys(selected()));

                        String title = oldPrefix.isEmpty() ? "Create common prefix" : "Change common prefix";

						DialogHelper.inputText(FavoritesListActivity.this, title, oldPrefix,
								new Commons.KeyAction() {
									@Override
									public void apply(String newPrefix) {
										List<String> newKeys = new LinkedList<>();

										for(String oldKey : extractKeys(selected())) {
											String newKey = newPrefix + oldKey.substring(oldPrefix.length());

											newKeys.add(SharedPrefsHelper.renameKey(FavoritesListActivity.this, oldKey, newKey, adapter.prefs));
										}

										// unselect all
										mode.finish();

										adapter.initializeAdapter();

                                        // and select new entries
										for(String key : newKeys) {
											listView.setItemChecked(adapter.getKeyIndex(key), true);
										}
									}
								});
                    } return true;
				}

				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {

			}
		});
	}

	private int getSelectedCount() {
		SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();

		int count = 0;

		for(int i = 0; i < checkedItemPositions.size(); ++i) {
			if(checkedItemPositions.get(checkedItemPositions.keyAt(i))) {
				count++;
			}
		}

		return count;
	}

	private Iterable<String> extractKeys(Iterable<FavoriteEntry> entries) {
		return new Iterable<String>() {
			@Override
			public Iterator<String> iterator() {

				Iterator<FavoriteEntry> iterator = entries.iterator();

				return new Iterator<String>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public String next() {
						return iterator.next().key();
					}
				};
			}
		};
	}

	private Iterable<FavoriteEntry> selected() {
		// FIXME do not create new list!
		List<FavoriteEntry> elements = new LinkedList<>();

		SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();

		for(int i = 0; i < checkedItemPositions.size(); ++i) {
			int key = checkedItemPositions.keyAt(i);
			if(checkedItemPositions.get(key)) {
				elements.add(adapter.getItem(key));
			}
		}

		return elements;
	}

	private void selectAll() {
		for(int i = 0; i < adapter.getCount(); ++i) {
			listView.setItemChecked(i, true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_favorites, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_import_collection: {
				importCollection();
			} return true;
			case R.id.action_select_all: {
				selectAll();
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void importCollection() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

		intent.addCategory(Intent.CATEGORY_OPENABLE);

		intent.setType("text/*");

		startActivityForResult(intent, IMPORT_COLLECTION_CODE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (data != null) {
			if (requestCode == IMPORT_COLLECTION_CODE) {
				// Fetch elements
				Uri uri = data.getData();

				try {
					Log.d(getClass().getName(), "Importing from " + uri);

					ParcelFileDescriptor pfd = getContentResolver().
                            openFileDescriptor(uri, "r");
					BufferedReader bufferedReader =
							new BufferedReader(new FileReader(pfd.getFileDescriptor()));

					StringBuilder sb = new StringBuilder();

					String line;

					while((line = bufferedReader.readLine()) != null) {
						sb.append(line);
						sb.append("\n");
					}

					FavoriteEntry.Collection newEntries;

                    try {
					    newEntries = Serializers.serializer().fromJson(sb.toString(), FavoriteEntry.Collection.class);
                        
                        if(newEntries == null) {
                            DialogHelper.error(FavoritesListActivity.this, "Could not read file!");
                            return;
                        }
                    } catch(Throwable e) {
                        e.printStackTrace();
                        DialogHelper.error(FavoritesListActivity.this, e.getMessage());
                        return;
                    }

					// Find duplicates
                    Set<String> addedKeys = new TreeSet<String>();
                    
					Map<String, FavoriteEntry> duplicates = new HashMap<>();

					for(Map.Entry<?, ?> entry : newEntries.getAll()) {
						FavoriteEntry favEntry = (FavoriteEntry) entry.getValue();

						String key = (String) entry.getKey();

						if(adapter.prefs.contains(key)) {
							duplicates.put(key, favEntry);
						} else {
							// add it
                            addedKeys.add(key);
							adapter.prefs.edit().putString(key, Serializers.serializer().toJson(favEntry)).apply();
						}
					}

					if(duplicates.isEmpty()) {
    					adapter.initializeAdapter();
                        selectKeys(addedKeys);
                    } else {
						// Ask what to do with duplicates
						DialogHelper.showOptionsDialog(this, "Pick an option for new entries with already existing keys", new CharSequence[]{
								"Do not add items with existing keys",
								"Append index to new entries",
								"Overwrite existing entries"
						}, false, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									case 0: break; // this is easy.
									case 1: {
										for(Map.Entry<String, FavoriteEntry> entry : duplicates.entrySet()) {
											String newKey = entry.getKey();

											do {
												newKey = CharUtil.nextIndex(newKey);
											} while(adapter.prefs.contains(newKey));

											adapter.prefs.edit().putString(newKey, Serializers.serializer().toJson(entry.getValue())).apply();
                                            addedKeys.add(newKey);
										}

										adapter.initializeAdapter();
                                        selectKeys(addedKeys);
									} break;
									case 2: {
										for(Map.Entry<String, FavoriteEntry> entry : duplicates.entrySet()) {
                                            addedKeys.add(entry.getKey());
											adapter.prefs.edit().putString(entry.getKey(), Serializers.serializer().toJson(entry.getValue())).apply();
										}

										adapter.initializeAdapter();
                                        selectKeys(addedKeys);
									} break;
								}
							}
						});
					}
				} catch (IOException e) {
					DialogHelper.error(this, e.getLocalizedMessage());
				}

				// If there are duplicated
			}
		}
	}

	/**
	 * Select entries in adapter with given keys
	 * @param keys
	 */
	private void selectKeys(Set<String> keys) {
        // find indices and set checked-status.

		for(int i = 0; i < adapter.getCount(); ++i) {
			String key = adapter.getTitle(i);

			listView.setItemChecked(i, keys.contains(key));
		}
    }

	private void export(Iterable<FavoriteEntry> entries) {
		// Fetch map from adapter
		try {
			// Create a map
			FavoriteEntry.Collection collection = new FavoriteEntry.Collection();

			for(FavoriteEntry entry : entries) {
				collection.add(entry.key(), entry);
			}

			File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
                    ".txt", this.getExternalCacheDir()); // extension fv for fractview

            BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));

            JsonWriter writer = new JsonWriter(bw);

            writer.setIndent("  ");

            Serializers.serializer().toJson(collection, FavoriteEntry.Collection.class, writer);

            writer.close();

            // Share text file
            Uri contentUri = Uri.fromFile(textFile);
            // after it was successfully saved, share it.
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            startActivity(Intent.createChooser(share, "Share Collection"));
        } catch (IOException e) {
            e.printStackTrace();
            DialogHelper.error(this, e.getMessage());
        }
	}

	private static class FavoritesListAdapter extends FractalListAdapter<FavoriteEntry> {

		final SharedPreferences prefs;

		private IndexedKeyMap<String> jsonEntries;
		private Map<String, FavoriteEntry> entries;

		public FavoritesListAdapter(Activity context) {
			super(context);

			// Fetch shared preferences
			this.prefs = context.getSharedPreferences(
					FAVORITES_SHARED_PREF,
					Context.MODE_PRIVATE);

			this.jsonEntries = new IndexedKeyMap<>();
			this.entries = new HashMap<>();

			initializeAdapter();
		}

		protected void initializeAdapter() {
			this.entries.clear();
			this.jsonEntries.clear();

			for(String key : this.prefs.getAll().keySet()) {
				String value = this.prefs.getString(key, null);

				if(value != null) {
					this.jsonEntries.add(key, value);
				} else {
					Log.e(getClass().getName(), "Value for key " + key + " was null!");
				}
			}

			this.jsonEntries.sort();
			notifyDataSetChanged();
		}
        
		@Override
		public int getCount() {
			return jsonEntries.size();
		}

		public int getKeyIndex(String key) {
			return jsonEntries.indexAt(key);
		}

		@Override
		public FavoriteEntry getItem(int position) {
			String key = jsonEntries.keyAt(position);

			FavoriteEntry entry = this.entries.get(key);

			if(entry == null) {
				String json = jsonEntries.valueAt(position);

				try {
					entry = Serializers.serializer().fromJson(json, FavoriteEntry.class);
					entry.setKey(key);
					this.entries.put(key, entry);
				} catch (Throwable th) {
					entry = null;
				}
			}

			return entry;
		}

		@Override
		public Bitmap getIcon(int position) {
			return getItem(position).icon();
		}

		@Override
		public String getTitle(int position) {
			return getItem(position).key();
		}

		@Override
		public String getDescription(int position) {
			return getItem(position).description();
		}
	}

	public void selectPrefixRange(String prefix) {
        int size = adapter.jsonEntries.size();
		int l = adapter.jsonEntries.findPrefix(prefix, 0, size - 1, true);
		if(l == -1) return; // nothing to select
		int r = adapter.jsonEntries.findPrefix(prefix, l, size - 1, false);

        for(int i = l; i <= r; ++i) {
            listView.setItemChecked(i, true);
        }
	}
	
}
