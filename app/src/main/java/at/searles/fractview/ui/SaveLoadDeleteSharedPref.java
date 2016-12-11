package at.searles.fractview.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.*;
import at.searles.fractview.R;

import java.util.TreeSet;

/**
 * Load data from sharedPrefs
 */
public class SaveLoadDeleteSharedPref {
	public static interface StringFn {
		public void apply(String key, SharedPreferences sharedPrefs);
	}

	static void initListView(final Context context, ListView lv, final SharedPreferences sharedPrefs, final StringFn fn) {
		TreeSet<String> data = new TreeSet<>();
		data.addAll(sharedPrefs.getAll().keySet()); // fixme is there a better way?

		final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
				context, android.R.layout.select_dialog_item); // fixme allow other layouts sth it can be used for bookmarks

		arrayAdapter.addAll(data);

		lv.setAdapter(arrayAdapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				String selected = arrayAdapter.getItem(position);

				// successfully got sth.
				fn.apply(selected, sharedPrefs);
			}
		});

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				// offer to delete it
				// fixme I know this is a bit a violation because I open a Dialog from a Dialog
				// fixme but here it is ok.
				// fixme just make sure to not preserve it on rotate.
				final String selected = arrayAdapter.getItem(position);

				AlertDialog.Builder yesNoBuilder = new AlertDialog.Builder(context);
				yesNoBuilder.setIcon(android.R.drawable.ic_delete);
				yesNoBuilder.setTitle("Delete entry " + selected + "?");

				yesNoBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						SharedPreferences.Editor edit = sharedPrefs.edit();
						edit.remove(selected);
						edit.apply();
						dialogInterface.dismiss();
						arrayAdapter.remove(selected);
					}
				});

				yesNoBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int which) {
						dialogInterface.dismiss();
					}
				});

				yesNoBuilder.show();

				return true;
			}
		});
	}

	// commons for loading and storing a value to shared preferences
	// handles further problems
	public static void openLoadDialog(final Context context, final SharedPreferences sharedPrefs, String title, final StringFn fn) {
		// open dialog with all keys
		AlertDialog.Builder fileSelectorBuilder = new AlertDialog.Builder(context);
		fileSelectorBuilder.setIcon(R.drawable.ic_launcher);
		fileSelectorBuilder.setTitle(title);

		ListView lv = new ListView(context); // I don't use 'setAdapter' because I need longClick

		class AcceptFn implements StringFn {

			AlertDialog dialog;

			@Override
			public void apply(String key, SharedPreferences sharedPrefs) {
				fn.apply(key, sharedPrefs);
				dialog.dismiss();
			}
		}

		AcceptFn afn = new AcceptFn();

		initListView(context, lv, sharedPrefs, afn);

		fileSelectorBuilder.setView(lv);

		fileSelectorBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
				// don't select anything
				dialogInterface.dismiss();
			}
		});

		afn.dialog = fileSelectorBuilder.show();
	}

	public static void openSaveDialog(final Context context, final SharedPreferences sharedPrefs, String title, final String data) {
		AlertDialog.Builder filenameDialogBuilder = new AlertDialog.Builder(context);
		filenameDialogBuilder.setIcon(R.drawable.ic_launcher);
		filenameDialogBuilder.setTitle(title);

		View view = ((Activity) context).getLayoutInflater().inflate(R.layout.save_layout, null);

		final EditText fileEdit = (EditText) view.findViewById(R.id.saveNameEditText);
		final ListView lv = (ListView) view.findViewById(R.id.existingListView);

		initListView(context, lv, sharedPrefs, new StringFn() {
			@Override
			public void apply(String key, SharedPreferences sharedPrefs) {
				fileEdit.setText(key);
			}
		});

		filenameDialogBuilder.setView(view);

		filenameDialogBuilder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
				final String filename = fileEdit.getText().toString();
				if(filename.isEmpty()) {
					// fixme do not dismiss (though, dismissing with error message is not bad)
					Toast.makeText(context, "Name cannot be empty", Toast.LENGTH_LONG).show();
				} else {
					if(sharedPrefs.contains(filename)) {
						AlertDialog.Builder yesNoBuilder = new AlertDialog.Builder(context);
						yesNoBuilder.setIcon(android.R.drawable.ic_delete);
						yesNoBuilder.setTitle("Overwrite entry " + filename + "?");

						yesNoBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								// fixme violation of DRY
								SharedPreferences.Editor edit = sharedPrefs.edit();

								edit.putString(filename, data);
								edit.apply();

								Toast.makeText(context, "Successfully stored " + filename, Toast.LENGTH_SHORT).show();
								dialogInterface.dismiss();
							}
						});

						yesNoBuilder.setNegativeButton("No", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialogInterface, int which) {
								Toast.makeText(context, "Did not save", Toast.LENGTH_LONG).show();
								dialogInterface.dismiss();
							}
						});

						yesNoBuilder.show();
					} else {
						// does not exist yet.
						// fixme once Android allows lambda expressions, put this one in one
						// to avoid WET
						SharedPreferences.Editor edit = sharedPrefs.edit();

						edit.putString(filename, data);
						edit.apply();

						Toast.makeText(context, "Successfully stored " + filename, Toast.LENGTH_SHORT).show();
					}

					dialogInterface.dismiss();
				}
			}
		});

		filenameDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int which) {
				// fixme what is the difference to dialogInterface.cancel()?
				dialogInterface.dismiss();
			}
		});

		filenameDialogBuilder.show();
	}
}
