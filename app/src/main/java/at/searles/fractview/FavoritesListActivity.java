package at.searles.fractview;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

	private static final String[] options = {"Rename", "Delete", "Copy To Clipboard"};

	private FavoritesListAdapter adapter;

	/**
	 * Selected elements
	 */
	private ListView listView;

	// FIXME rename, delete, copy

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
						// TODO
					} return true;
					case R.id.action_delete: {
						// TODO
					} return true;
					case R.id.action_export: {
						// TODO
					} return true;
				}

				return false;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {

			}
		});
	}

	private List<FavoriteEntry> selected() {
		List<FavoriteEntry> elements = new LinkedList<>();

		SparseBooleanArray checkedItemPositions = listView.getCheckedItemPositions();

		for(int i = 0; i < checkedItemPositions.size(); ++i) {
			if(checkedItemPositions.get(i)) {
				elements.add(adapter.getItem(i));
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
				// FIXME
			} return true;
			case R.id.action_select_all: {
				selectAll();
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void export(int selectedElements[]) {
		// Fetch map from adapter
		List<FavoriteEntry> entries = new ArrayList<>(adapter.getCount());

		for(int selected : selectedElements) {
            FavoriteEntry entry = adapter.getItem(selected);
            entries.add(entry);
        }

		try {
            File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
                    ".fv", this.getExternalCacheDir()); // extension fv for fractview

            BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));

            JsonWriter writer = new JsonWriter(bw);

            writer.setIndent("  ");

            Serializers.serializer().toJson(entries, ArrayList.class, writer);

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
					entry.setTitle(key);
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
			return getItem(position).title();
		}

		@Override
		public String getDescription(int position) {
			return getItem(position).description();
		}
	}
}