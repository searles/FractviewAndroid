package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import at.searles.fractview.fractal.FavoriteEntry;

/**
 *
 */
public class FavoritesActivity extends Activity {
	// TODO: Copy collection to clipboard
	// TODO: Import collection from clipboard

	// fixme menus import from clipboard
	static final String[] options = {"Delete", "Copy To Clipboard"};

	List<FavoriteEntry> entries;
	SharedPreferences persistent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

		// read bookmark-map from shared preferences
		// fixme what do the other modes do?
		persistent = getSharedPreferences("favorites", Context.MODE_PRIVATE);

		Map<String, ?> sharedPrefs = persistent.getAll();
		entries = new ArrayList<>(sharedPrefs.size());

		// sort alphabetically
		TreeSet<String> keys = new TreeSet<>();
		keys.addAll(sharedPrefs.keySet());

		for(String key : keys) {
			try {
				entries.add(FavoriteEntry.fromJSON(key, new JSONObject((String) sharedPrefs.get(key))));
			} catch (JSONException e) {
				e.printStackTrace();
				// should not happen.
				// FIXME but seriously think of using a different JSON.
				throw new IllegalArgumentException(e);
			}
		}

		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		final FavoritesAdapter adapter = new FavoritesAdapter(this, entries);

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
						FavoriteEntry entry = entries.get(index);

						switch (which) {
							case 0: {
								// delete fav
								// first from internal DSs
								entries.remove(index);

								// then from shared prefs
								SharedPreferences.Editor editor = persistent.edit();
								editor.remove(entry.title());
								editor.apply();

								// and update view.
								adapter.notifyDataSetChanged(); // tell that sth changed.
							}
							break;
							case 1: {
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
}