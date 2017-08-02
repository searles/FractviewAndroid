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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import at.searles.fractal.FavoriteEntry;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractal.gson.Serializers;
import at.searles.fractview.ui.DialogHelper;
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
				int selectCount = getSelectedCount();
				mode.getMenu().getItem(RENAME_INDEX_IN_MENU).setEnabled(selectCount == 1);
				mode.setTitle(selectCount + " selected");
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
						List<String> keys = extractKeys(selected());

						if(keys.size() != 1) {
							throw new IllegalArgumentException("there should be only 1 element selectable!");
						}

						String key = keys.get(0);

						DialogHelper.inputText(FavoritesListActivity.this, "Rename " + key, key, new Commons.KeyAction() {
							@Override
							public void apply(String newKey) {
								if(SharedPrefsHelper.renameKey(FavoritesListActivity.this, key, newKey, adapter.prefs)) {
									adapter.initializeAdapter();
								}
							}
						});
					} return true;
					case R.id.action_delete: {
						List<String> keys = extractKeys(selected());

						DialogHelper.confirm(FavoritesListActivity.this, "Delete",
								"Delete all selected favorites?",
								new Runnable() {
									@Override
									public void run() {
										for(String key : keys) {
											SharedPrefsHelper.removeEntry(FavoritesListActivity.this, key, adapter.prefs);
										}
									}
								});
					} return true;
					case R.id.action_export: {
						List<FavoriteEntry> selected = selected();
						export(selected);
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

	private List<String> extractKeys(List<FavoriteEntry> entries) {
		List<String> keys = new ArrayList<String>(entries.size());

		for(FavoriteEntry entry : entries) {
            keys.add(entry.key());
        }

        return keys;
	}

	private List<FavoriteEntry> selected() {
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

					Map<?, ?> newEntries = Serializers.serializer().fromJson(sb.toString(), Map.class);

					// Find duplicates
					Map<String, FavoriteEntry> duplicates = new HashMap<>();

					for(Map.Entry<?, ?> entry : newEntries.entrySet()) {
						if(entry.getKey() instanceof String && entry.getValue() instanceof FavoriteEntry) {
							FavoriteEntry favEntry = (FavoriteEntry) entry.getValue();

							String key = (String) entry.getKey();

							if(adapter.prefs.contains(key)) {
								duplicates.put(key, favEntry);
							} else {
								// add it
								adapter.prefs.edit().putString(key, Serializers.serializer().toJson(favEntry)).apply();
							}
						} else {
							Log.e(getClass().getName(), "Bad entry: " + newEntries);
						}
					}

					adapter.initializeAdapter();

					if(!duplicates.isEmpty()) {
						// Ask what to do with duplicates
						DialogHelper.showOptionsDialog(this, new CharSequence[]{
								"Do not add items with existing keys",
								"Add index to already existing keys",
								"Overwrite existing entries"
						}, false, new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									case 0: break; // this is easy.
									case 1: {
										for(Map.Entry<String, FavoriteEntry> entry : duplicates.entrySet()) {
											String key = entry.getKey();

											String newKey;

											for(int i = 1;; ++i) {
												newKey = key + "(" + i + ")";
												if(!adapter.prefs.contains(newKey)) {
													break;
												}
											}

											adapter.prefs.edit().putString(newKey, Serializers.serializer().toJson(entry.getValue())).apply();
										}

										adapter.initializeAdapter();
									} break;
									case 2: {
										for(Map.Entry<String, FavoriteEntry> entry : duplicates.entrySet()) {
											adapter.prefs.edit().putString(entry.getKey(), Serializers.serializer().toJson(entry.getValue())).apply();
										}

										adapter.initializeAdapter();
									} break;
								}
							}
						});
					}
				} catch (IOException e) {
					DialogHelper.error(this, e.getLocalizedMessage());
				}


				Log.d("HELLO", "Something was imported");
				// FIXME Allow to select a prefix
				// If there are duplicated
			}
		}
	}

	private void export(List<FavoriteEntry> entries) {
		// Fetch map from adapter
		try {
			// Create a map
			Map<String, FavoriteEntry> map = new TreeMap<>();

			for(FavoriteEntry entry : entries) {
				map.put(entry.key(), entry);
			}

			File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
                    ".txt", this.getExternalCacheDir()); // extension fv for fractview

            BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));

            JsonWriter writer = new JsonWriter(bw);

            writer.setIndent("  ");

            Serializers.serializer().toJson(map, Map.class, writer);

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

		private final SharedPreferences prefs;

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
		}

		@Override
		public int getCount() {
			return jsonEntries.size();
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
}