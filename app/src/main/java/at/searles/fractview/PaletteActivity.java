package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.jetbrains.annotations.NotNull;

import at.searles.fractview.ui.DialogHelper;
import at.searles.fractview.ui.MultiScrollView;
import at.searles.fractview.ui.PaletteView;
import at.searles.fractview.ui.PaletteViewModel;
import at.searles.math.color.Palette;

/**
 * This activity is tightly coupled with the PaletteView. The palette view
 * starts dialogs that call back to either here or to the view. It would
 * be better to, well, what? Store the model in a fragment
 * and call back to there?
 * It is not so bad, but it should be clearly stated that the
 * owner of the model is the activity and not the view.
 *
 * Therefore, it would be better if the view did not hold the model.
 */
public class PaletteActivity extends Activity implements EditableDialogFragment.Callback {

	// Editors should contain all the important information because they are retained.

	// fixme save via parcels!

	public static final String PREFS_NAME = "SavedPalettes";

	public static final int PALETTE_ACTIVITY_RETURN = 99;

	private static final int SAVE_PALETTE = -1; // because positive numbers are for indices
	private static final int LOAD_PALETTE = -2; // because positive numbers are for indices

	private PaletteViewModel model = null;

	private String id;

	private SharedPrefsHelper prefsHelper;

	// if something is currently edited, these two
	// contain its coordinates
	//private int editX = -1;
	//private int editY = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.palette_layout);

		this.prefsHelper = new SharedPrefsHelper(this, PREFS_NAME);

		this.id = getIntent().getStringExtra("id");

		Commons.PaletteWrapper wrapper;

		if(savedInstanceState == null) {
			wrapper = getIntent().getParcelableExtra("palette");
		} else {
			wrapper = savedInstanceState.getParcelable("palette");
		}

		if(wrapper == null) {
			// can this happen?
			throw new IllegalArgumentException("No palette available");
		}

		// create model and palette view
		model = new PaletteViewModel(wrapper.p);

		// now that the model is set, we can update the size of the scrollviews
		((MultiScrollView) findViewById(R.id.multiScrollView)).updateSize();

		// the paletteview is embedded into a multiscrollview

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
				data.putExtra("palette", new Commons.PaletteWrapper(model.createPalette()));
				data.putExtra("id", id);
				setResult(1, data);
				finish();
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putParcelable("palette", new Commons.PaletteWrapper(/*label, */model.createPalette()));
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
		switch (item.getItemId()) {
			case R.id.action_load_palette: {
				// the fragment needs the name of the prefs as an argument.

				EditableDialogFragment ft = EditableDialogFragment.newInstance(
						LOAD_PALETTE, "Load Palette", false,
						EditableDialogFragment.Type.LoadSharedPref);

				ft.show(getFragmentManager(), "dialog");
				ft.getArguments().putString("prefs_name", PaletteActivity.PREFS_NAME);
			} return true;
			case R.id.action_save_palette: {
				EditableDialogFragment ft = EditableDialogFragment.newInstance(SAVE_PALETTE,
						"Save Palette", false, EditableDialogFragment.Type.SaveSharedPref);

				ft.show(getFragmentManager(), "dialog");

				// the fragment needs the name of the prefs as an argument.
				ft.getArguments().putString("prefs_name", PaletteActivity.PREFS_NAME);
			} return true;
			case R.id.action_export_palette: {
				// copy
				JsonElement o = model.createPalette().serialize();
				ClipboardHelper.copy(this, o.toString());
			} return true;
			case R.id.action_import_palette: {
				// paste
				CharSequence pastedText = ClipboardHelper.paste(this);

				if(pastedText != null) {
					Palette p = Palette.deserialize(new JsonParser().parse(pastedText.toString()));

					if (p != null) {
						model = new PaletteViewModel(p);

						PaletteView view = (PaletteView) findViewById(R.id.paletteView);

						view.invalidate();
					} else {
						DialogHelper.error(this, "No palette in clipboard");
					}
				} else {
					DialogHelper.error(this, "The clipboard is empty");
				}
			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public PaletteViewModel model() {
		return model;
	}

	/*@Override
	public void initDialogView(String id, View view) {
		switch (id) {
			case "save": {
			} break;
			default: {
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
			} break;
		}
	}*/

	@Override
	public void apply(int requestCode, Object o) {
		switch(requestCode) {
			case LOAD_PALETTE: {
				String name = (String) o;

				String paletteString = prefsHelper.get(name);

				Log.d(getClass().getName(), "Palette string is " + paletteString + ", name is " + name);

				if(paletteString != null) {
					// JSON-Parser
					Palette p = Palette.deserialize(new JsonParser().parse(paletteString).getAsJsonObject());

					if(p != null) {
						// set the palette.
						model = new PaletteViewModel(p);
						PaletteView view = (PaletteView) findViewById(R.id.paletteView);
						view.invalidate();
					} else {
						DialogHelper.error(this, "Could not create palette");
					}
				}
			} break;
			case SAVE_PALETTE: {
				String name = (String) o;
				String paletteString = model.createPalette().serialize().toString();

				prefsHelper.add(name, paletteString, SharedPrefsHelper.SaveMethod.FindNext);
			} break;
			default: {
				// Tag when it is a color are the coordinates
				int x = requestCode % model.width();
				int y = requestCode / model.width();

				// todo alpha!
				model.set(x, y, ((Integer) o) | 0xff000000);

				// redraw palette
				findViewById(R.id.paletteView).invalidate();
			}
		}
	}

	public void dimensionChanged() {
		MultiScrollView msView = (MultiScrollView) findViewById(R.id.multiScrollView);
		msView.updateSize();
	}

}
