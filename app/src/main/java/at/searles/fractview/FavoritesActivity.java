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

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import at.searles.fractal.FractalEntry;
import at.searles.fractal.FractalLabel;
import at.searles.fractview.ui.DialogHelper;

/**
 *
 */
public class FavoritesActivity extends Activity {

	public static final String FAVORITES = "favorites";


	private static final String[] options = {"Rename", "Delete", "Copy To Clipboard"};

	// private Map<String, FractalEntry> entries;
	private SharedPrefsHelper prefsHelper;

	private void initData() {
		Map<String, ?> sharedPrefs = prefsHelper.getAll();

		HashMap<String, FractalEntry> entries = new HashMap<>(sharedPrefs.size());

		for(String key : sharedPrefs.keySet()) {
			String specification = (String) sharedPrefs.get(key);

			try {
				entries.put(key, FractalEntry.deserialize(key, new JsonParser().parse(specification)));
			} catch (Exception e) {
				// FIXME
				e.printStackTrace();
			}
		}

		adapter.setData(entries);
		adapter.notifyDataSetChanged();
	}

	private FractalEntryAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

        prefsHelper = new SharedPrefsHelper(this, FAVORITES);

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
				data.putExtra("fractal", ((FractalEntry) entry).fractal());
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
						FractalEntry entry = (FractalEntry) adapter.getItem(index);

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
				JsonArray array = new JsonArray();

				for(int i = 0; i < adapter.getCount(); ++i) {
					FractalEntry entry = (FractalEntry) adapter.getItem(i);
					array.add(entry.serialize());
				}

				try {
					File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
							".fv", this.getExternalCacheDir()); // extension fv for fractview

					BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));

					bw.write(array.toString());

					bw.close();

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
			} return true;
			case R.id.action_import_collection: {
				// FIXME
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}
}