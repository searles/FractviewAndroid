package at.searles.fractview.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import at.searles.fractview.R;
import at.searles.fractview.fractal.Adapters;
import at.searles.fractview.ui.editors.*;
import at.searles.math.color.Palette;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class PaletteActivity extends Activity implements ColorEditor.Callback {

	// Editors should contain all the important information because they are retained.

	// fixme save via parcels!

	public static final String PREFS_NAME = "SavedPalettes";

	public static final int PALETTE_ACTIVITY_RETURN = 99;

	PaletteViewModel model = null;
	PaletteView view = null;

	// String label;
	Palette palette;

	// if something is currently edited, these two
	// contain its coordinates
	private int editX = -1;
	private int editY = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.palette_layout);

		PaletteWrapper wrapper;

		if(savedInstanceState == null) {
			wrapper = getIntent().getParcelableExtra("palette");
		} else {
			wrapper = savedInstanceState.getParcelable("palette");
			editX = savedInstanceState.getInt("edit_x");
			editY = savedInstanceState.getInt("edit_y");
		}

		palette = wrapper.p;
		//label = wrapper.label;

		model = new PaletteViewModel(palette);

		view = (PaletteView) findViewById(R.id.paletteView);
		view.setModel(model);

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// don't do anything.
				setResult(0);
				finish();
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent data = new Intent();
				data.putExtra("palette", new PaletteWrapper(/*label, */model.createPalette()));
				setResult(1, data);
				finish();
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putParcelable("palette", new PaletteWrapper(/*label, */model.createPalette()));
		savedInstanceState.putInt("edit_x", editX);
		savedInstanceState.putInt("edit_y", editY);

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.palette_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		SharedPreferences sharedPrefs = getSharedPreferences(
				PREFS_NAME, Context.MODE_PRIVATE);
		switch (item.getItemId()) {
			case R.id.action_load_palette: {
				SaveLoadDeleteSharedPref.openLoadDialog(this, sharedPrefs, "Load Palette", new SaveLoadDeleteSharedPref.StringFn() {
					@Override
					public void apply(String key, SharedPreferences sharedPrefs) {
						String paletteString = sharedPrefs.getString(key, "");

						if(!paletteString.isEmpty()) {
							// JSON-Parser
							try {
								Palette p = Adapters.paletteAdapter.fromJSON(new JSONObject(paletteString));
								model = new PaletteViewModel(p);
								view.setModel(model);
							} catch (JSONException e) {
								e.printStackTrace();
								Toast.makeText(PaletteActivity.this, "JSON-Error", Toast.LENGTH_LONG).show();
							}
						}
					}
				});
			} return true;
			case R.id.action_save_palette: {
				try {
					String paletteString = Adapters.paletteAdapter.toJSON(model.createPalette()).toString();
					SaveLoadDeleteSharedPref.openSaveDialog(this, sharedPrefs, "Save Palette", paletteString);
				} catch (JSONException e) {
					e.printStackTrace();
					Toast.makeText(PaletteActivity.this, "JSON-Error", Toast.LENGTH_LONG).show();
				}
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public void editColorAt(int x, int y) {
		editX = x;
		editY = y;
		SettingsEditor<Integer> editor = new ColorEditor("Edit Color", model.get(x, y));
		DialogEditFragment.createDialog(this, editor);
	}

	@Override
	public boolean applyColor(int color) {
		model.set(editX, editY, color);
		return true;
	}


	public static class PaletteWrapper implements Parcelable {

		//public final String label;
		public final Palette p;

		public PaletteWrapper(/*String label,*/ Palette p) {
			//this.label = label;
			this.p = p;
		}


		@Override
		public int describeContents() {
			return 0;
		}

		@Override
		public void writeToParcel(Parcel parcel, int flags) {
			//parcel.writeString(label);
			Adapters.paletteAdapter.toParcel(p, parcel, flags);
		}

		public static final Parcelable.Creator<PaletteWrapper> CREATOR
				= new Parcelable.Creator<PaletteWrapper>() {
			public PaletteWrapper createFromParcel(Parcel in) {
				return new PaletteWrapper(in);
			}

			public PaletteWrapper[] newArray(int size) {
				return new PaletteWrapper[size];
			}
		};

		/**
		 * Now, writeParcel in reverse
		 * @param parcel
		 */
		private PaletteWrapper(Parcel parcel) {
			//label = parcel.readString();
			p = Adapters.paletteAdapter.fromParcel(parcel);
		}
	}

}
