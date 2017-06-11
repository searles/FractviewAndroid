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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.searles.fractview.fractal.FavoriteEntry;

/**
 *
 */
public class FavoritesActivity extends Activity {

	private static final String[] options = {"Rename", "Delete", "Copy To Clipboard"};

	private List<String> keys;
	private Map<String, FavoriteEntry> entries;
	private SharedPrefsHelper prefsHelper;

	private void initData() {
		Map<String, ?> sharedPrefs = prefsHelper.getAll();

		entries = new HashMap<>(sharedPrefs.size());

		for(String key : sharedPrefs.keySet()) {
			try {
				entries.put(key, FavoriteEntry.fromJSON(key, new JSONObject((String) sharedPrefs.get(key))));
			} catch (JSONException e) {
				// should not happen.
				e.printStackTrace();
			}
		}

		adapter.setData(entries);
		adapter.notifyDataSetChanged();
	}

	private FavoritesAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

        prefsHelper = new SharedPrefsHelper(this, FavoritesManager.FAVORITES);

		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		this.adapter = new FavoritesAdapter(this);

		initData();

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				// get bookmark
				FavoriteEntry fav = entries.get(index);

				Intent data = new Intent();
				data.putExtra("fractal", fav.fractal());
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
						String key = keys.get(index);

						switch (which) {
							case 0: {
                                // Show dialog for new name
								DialogHelper.inputText(FavoritesActivity.this, "Rename entry", key, new Commons.KeyAction() {
									@Override
									public void apply(String newKey) {
										prefsHelper.rename(key, newKey, SharedPrefsHelper.SaveMethod.FindNext, FavoritesActivity.this);
										initData(); // reinitialize because order might have changed
									}
								});
							}
							break;
							case 1: {
								// delete it
								prefsHelper.remove(key);
								initData();
							}
							break;
							case 2: {
								// copy to clipboard
								ClipboardHelper.copyFractal(view.getContext(), entries.get(key).fractal());
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
			case R.id.action_export: {
				JSONObject obj = new JSONObject(entries);

				try {
					File textFile = File.createTempFile("fractview_collection", ".txt", this.getExternalCacheDir());

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
			case R.id.action_import: {
				// FIXME
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}