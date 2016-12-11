package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import at.searles.fractview.MainActivity;
import at.searles.fractview.R;
import at.searles.fractview.fractal.FavoriteEntry;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 *
 */
public class FavoritesActivity extends Activity {

	// fixme menus import from clipboard
	static final String[] options = {"Delete", "Copy To Clipboard"};

	TreeMap<String, FavoriteEntry> favorites = new TreeMap<>();
	List<String> labels;
	SharedPreferences persistent;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

		// read bookmark-map from shared preferences
		// fixme what do the other modes do?
		persistent = getSharedPreferences("favorites", Context.MODE_PRIVATE);

		manageOldEntries();

		for(Map.Entry<String, ?> entry : persistent.getAll().entrySet()) {
			try {
				favorites.put(entry.getKey(), FavoriteEntry.fromJSON(new JSONObject((String) entry.getValue())));
			} catch (JSONException e) {
				e.printStackTrace();
				// FIXME Error message in this case
			}
		}

		// and since it is sorted, use it to write label-map.
		labels = new ArrayList<>(favorites.size());

		for(String label : favorites.keySet()) {
			labels.add(label);
		}

		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		final FavoritesAdapter adapter = new FavoritesAdapter(this, labels, favorites);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				// get bookmark
				FavoriteEntry fav = favorites.get(labels.get(index));

				Intent data = new Intent();
				data.putExtra("fractal", fav.fractal);
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
						String l = labels.get(index);

						switch (which) {
							case 0: {
								// delete fav
								// first from internal DSs
								labels.remove(l);
								favorites.remove(l);

								// then from shared prefs
								SharedPreferences.Editor editor = persistent.edit();
								editor.remove(l);
								editor.apply();

								// and update view.
								adapter.notifyDataSetChanged(); // tell that sth changed.
							}
							break;
							case 1: {
								// copy to clipboard
								MainActivity.copyToClipboard(view.getContext(), favorites.get(l).fractal);
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

	// FIXME Remove after some time
	@Deprecated
	private void manageOldEntries() {
		// show menu
		// old bookmark entries found
		// copy to clipboard
		// delete them

		CharSequence options[] = new CharSequence[] {
				"Delete old bookmarks",
				"Copy old bookmarks to clipboard"};

		SharedPreferences oldPreferences = getSharedPreferences("bookmarks", Context.MODE_PRIVATE);

		final Map<String, ?> oldEntries = oldPreferences.getAll();

		if(oldEntries.isEmpty()) {
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Unfortunately old bookmarks cannot be supported");
		builder.setItems(options, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// the user clicked on colors[which]
				if(which == 0) {
					SharedPreferences.Editor editor = getSharedPreferences("bookmarks", Context.MODE_PRIVATE).edit();
					editor.clear();
					editor.apply();
				} else if(which == 1) {
					StringBuilder sb = new StringBuilder();

					for(Map.Entry<String, ?> entry : oldEntries.entrySet()) {
						sb.append(entry.getKey()).append("\n");
						sb.append(String.valueOf(entry.getValue()));

						sb.append("\n\n");
					}

					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("fractview_old_bookmarks", sb.toString());
					clipboard.setPrimaryClip(clip);
				}
			}
		});

		builder.setNegativeButton("Close", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				// Do nothing.
				dialogInterface.dismiss();
			}
		});

		builder.show();
	}

}