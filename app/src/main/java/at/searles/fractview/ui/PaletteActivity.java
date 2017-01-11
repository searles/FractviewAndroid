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
import android.widget.EditText;
import android.widget.Toast;
import at.searles.fractview.R;
import at.searles.fractview.SaveLoadDeleteSharedPref;
import at.searles.fractview.fractal.Adapters;
import at.searles.math.color.Colors;
import at.searles.math.color.Palette;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

public class PaletteActivity extends Activity implements MyAlertDialogFragment.DialogHandler {

	// Editors should contain all the important information because they are retained.

	// fixme save via parcels!

	public static final String PREFS_NAME = "SavedPalettes";

	public static final int PALETTE_ACTIVITY_RETURN = 99;

	private PaletteViewModel model = null;
	private PaletteView view = null;

	private String id;

	// if something is currently edited, these two
	// contain its coordinates
	//private int editX = -1;
	//private int editY = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.palette_layout);

		this.id = getIntent().getStringExtra("id");

		PaletteWrapper wrapper;

		if(savedInstanceState == null) {
			wrapper = getIntent().getParcelableExtra("palette");
		} else {
			wrapper = savedInstanceState.getParcelable("palette");
		}

		if(wrapper == null) {
			// can this happen?
			throw new IllegalArgumentException("No palette available");
		}

		model = new PaletteViewModel(wrapper.p);

		view = (PaletteView) findViewById(R.id.paletteView);
		view.setModel(model);

		Button okButton = (Button) findViewById(R.id.okButton);
		Button cancelButton = (Button) findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(view -> {
            // don't do anything.
            setResult(0);
            finish();
        });

		okButton.setOnClickListener(view -> {
            Intent data = new Intent();
            data.putExtra("palette", new PaletteWrapper(model.createPalette()));
			data.putExtra("id", this.id);
            setResult(1, data);
            finish();
        });
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putParcelable("palette", new PaletteWrapper(/*label, */model.createPalette()));
		savedInstanceState.putString("id", id);

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

	@Override
	public void initDialogView(String id, View view) {
		String[] s = id.split(",");

		int x = Integer.parseInt(s[0]);
		int y = Integer.parseInt(s[1]);

		int color = model.get(x, y);

		// I initialize the view here.
		ColorView colorView = (ColorView) view.findViewById(R.id.colorView);
		EditText webcolorEditor = (EditText) view.findViewById(R.id.webcolorEditText);

		// I need listeners for both of them.
		colorView.bindToEditText(webcolorEditor);

		webcolorEditor.setText(Colors.toColorString(color));
		colorView.setColor(color);
	}

	@Override
	public boolean applyDialogView(String id, View view) {
		String[] s = id.split(",");

		int x = Integer.parseInt(s[0]);
		int y = Integer.parseInt(s[1]);

		ColorView editor = (ColorView) view.findViewById(R.id.colorView);
		int color = editor.getColor();

		// fixme alpha!
		model.set(x, y, color  | 0xff000000);
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
