package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import com.google.gson.JsonParser;

import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import at.searles.fractview.fractal.FavoriteEntry;

/**
 *
 */
public class FavoritesActivity extends Activity {

	private static final String[] options = {"Rename", "Delete", "Copy To Clipboard"};

	// private Map<String, FavoriteEntry> entries;
	private SharedPrefsHelper prefsHelper;

	private void initData() {
		Map<String, ?> sharedPrefs = prefsHelper.getAll();

		HashMap<String, FavoriteEntry> entries = new HashMap<>(sharedPrefs.size());

		for(String key : sharedPrefs.keySet()) {
			entries.put(key, FavoriteEntry.deserialize(key, new JsonParser().parse((String) sharedPrefs.get(key))));
		}

		adapter.setData(entries);
		adapter.notifyDataSetChanged();
	}

	private FractalEntryAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

        prefsHelper = new SharedPrefsHelper(this, FavoritesManager.FAVORITES);

		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		this.adapter = new FractalEntryAdapter(this);

		initData();

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				// get bookmark
				FractalLabel entry = adapter.getItem(index);

				Intent data = new Intent();
				data.putExtra("fractal", ((FavoriteEntry) entry).fractal());
				setResult(1, data);
				finish();
			}
		});

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, final View view, final int index, long id) {
				AlertDialog.Builder builder = new AlertDialog.Builder(FavoritesActivity.this);

				builder.setTitle("Choose an option");
				builder.setItems(options, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						FavoriteEntry entry = (FavoriteEntry) adapter.getItem(index);

						switch (which) {
							case 0: {
                                // Show dialog for new name
								DialogHelper.inputText(FavoritesActivity.this, "Rename entry", entry.title(), new Commons.KeyAction() {
									@Override
									public void apply(String newKey) {
										prefsHelper.rename(entry.title(), newKey, SharedPrefsHelper.SaveMethod.FindNext, FavoritesActivity.this);
										initData(); // reinitialize because order might have changed
									}
								});
							}
							break;
							case 1: {
								// delete it
								prefsHelper.remove(entry.title(), FavoritesActivity.this);
								initData();
							}
							break;
							case 2: {
								// copy to clipboard
								ClipboardHelper.copyFractal(view.getContext(), entry.fractal());
							}
							break;
						}
					}
				});

				builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						// just dismiss it.
						dialogInterface.dismiss();
					}
				});

				builder.show();

				return true;
			}
		});

		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				FavoritesActivity.this.finish();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.favorites_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_export_collection: {

				// Fetch map from adapter
				Map<String, FavoriteEntry> map = new LinkedHashMap<>();

				for(int i = 0; i < adapter.getCount(); ++i) {
					FavoriteEntry entry = (FavoriteEntry) adapter.getItem(i);
					map.put(entry.title(), entry);
				}

				try {
					File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
							".fv", this.getExternalCacheDir()); // extension fv for fractview

					BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));
					bw.write(new JSONObject(map).toString());
					bw.close();

					// Share image
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
			} return true;
			case R.id.action_import_collection: {
				// FIXME
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}