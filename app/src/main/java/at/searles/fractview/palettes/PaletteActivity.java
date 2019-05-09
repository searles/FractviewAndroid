package at.searles.fractview.palettes;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import at.searles.fractview.R;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;
import at.searles.fractview.ui.MultiScrollView;
import at.searles.fractview.utils.Accessor;
import at.searles.fractview.utils.SharedPreferencesAccessor;
import at.searles.math.color.Palette;

/**
 * This activity is tightly coupled with the PaletteView. The palette view
 * starts dialogs that call back to either here or to the view. It would
 * be better to, well, what? Store the model in a fragment
 * and call back to there?
 * It is not so bad, but it should be clearly stated that the
 * id of the model is the activity and not the view.
 *
 * Therefore, it would be better if the view did not hold the model.
 */
public class PaletteActivity extends Activity {

	public static final String PALETTE_LABEL = "palette";
	public static final String ID_LABEL = "id";
	public static final String OWNER_LABEL = "id";
	public static final String DESCRIPTION_LABEL = "description";

	public static final String PREFS_NAME = "SavedPalettes";

	public static final int PALETTE_ACTIVITY_RETURN = 99;

	public static final int CANCEL_RESULT = 0;
	public static final int OK_RESULT = 1;

	private static final String DIALOG_TAG = "dialogTag";

	private PaletteViewModel model = null;

	private String id;

	// if something is currently edited, these two
	// contain its coordinates
	//private int editX = -1;
	//private int editY = -1;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.palette_layout);

		this.id = getIntent().getStringExtra(ID_LABEL);

		Bundle bundle;

		if(savedInstanceState == null) {
			bundle = getIntent().getBundleExtra(PALETTE_LABEL);
		} else {
			bundle = savedInstanceState.getBundle(PALETTE_LABEL);
		}

		if(bundle == null) {
			// can this happen?
			throw new IllegalArgumentException("No palette available");
		}

		// create model and palette view
		model = new PaletteViewModel(BundleAdapter.paletteFromBundle(bundle));

		// now that the model is set, we can update the size of the scrollviews
		((MultiScrollView) findViewById(R.id.multiScrollView)).updateSize();

		// the paletteview is embedded into a multiscrollview

		// TODO Buttons, do it differently.

		Button okButton = findViewById(R.id.okButton);
		Button cancelButton = findViewById(R.id.cancelButton);

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// don't do anything.
				setResult(CANCEL_RESULT);
				finish();
			}
		});

		okButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent data = new Intent();
				data.putExtra(PALETTE_LABEL, BundleAdapter.toBundle(model.createPalette()));
				data.putExtra(ID_LABEL, id);
				setResult(OK_RESULT, data);
				finish();
			}
		});
	}

	@Override
	public void onSaveInstanceState(@NotNull Bundle savedInstanceState) {
		// Save the user's current game state
		savedInstanceState.putParcelable(PALETTE_LABEL, BundleAdapter.toBundle(model.createPalette()));
		savedInstanceState.putString(ID_LABEL, id);

		// Always call the superclass so it can save the view hierarchy state
		super.onSaveInstanceState(savedInstanceState);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.activity_palette, menu);
		return super.onCreateOptionsMenu(menu);
	}

	private Accessor<Palette> createAccessor() {
		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
		return new SharedPreferencesAccessor<>(prefs, Palette.class);
	}

	private void checkSavePalette() {
		// TODO from button.
		EditText nameEditor = findViewById(R.id.nameEditor);
		String name = nameEditor.getText().toString();

		if(name.isEmpty()) {
			DialogHelper.error(this, "Name must not be empty");
			return;
		}

		// TODO Check whether it already exists.
		Accessor<Palette> accessor = createAccessor();

		if(accessor.exists(name)) {
			// TODO Dialog with options
			// Overwrite/Save anyways
			// Append index
			// Cancel
			return;
		}

		accessor.add(name, model.createPalette());
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case R.id.action_palettes: {
				// TODO
				startActivityForResult(new Intent(this, PalettesListActivity.class), PALETTE_ACTIVITY_RETURN);
			}
			return true;
// FIXME remove			case R.id.action_load_palette: {
//				LoadFullScreenDialogFragment ft = LoadFullScreenDialogFragment.newInstance(PaletteActivity.PREFS_NAME);
//				// TODO LoadSharedPreferenceDialogFragment ft = LoadSharedPreferenceDialogFragment.newInstance(PaletteActivity.PREFS_NAME);
//				ft.show(getFragmentManager(), DIALOG_TAG);
//			} return true;
//			case R.id.action_copy_to_clipboard: {
//				// TODO Move to copy/paste menu
//				// copy
//				String entry = Serializers.serializer().toJson(model.createPalette(), Palette.class);
//				ClipboardHelper.copy(this, entry);
//			} return true;
//			case R.id.action_paste_from_clipboard: {
//				// paste
//				CharSequence pastedText = ClipboardHelper.paste(this);
//
//				if(pastedText != null) {
//					Palette p = Serializers.serializer().fromJson(pastedText.toString(), Palette.class);
//
//					if (p != null) {
//						model = new PaletteViewModel(p);
//
//						PaletteView view = findViewById(R.id.paletteView);
//
//						view.invalidate();
//					} else {
//						DialogHelper.error(this, "No palette in clipboard");
//					}
//				} else {
//					DialogHelper.error(this, "The clipboard is empty");
//				}
//			} return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	public PaletteViewModel model() {
		return model;
	}

//	@Override
//	public void apply(int requestCode, Object o) {
//		switch(requestCode) {
//			case LOAD_PALETTE: {
//				String name = (String) o;
//
//				String paletteString = SharedPrefsHelper.loadFromSharedPreferences(this, name, PREFS_NAME);
//
//				Log.d(getClass().getName(), "Palette string is " + paletteString + ", name is " + name);
//
//				if(paletteString != null) {
//					Palette p = Serializers.serializer().fromJson(paletteString, Palette.class);
//
//					if(p != null) {
//						// set the palette.
//						model = new PaletteViewModel(p);
//						PaletteView view = (PaletteView) findViewById(R.id.paletteView);
//						view.invalidate();
//					} else {
//						DialogHelper.error(this, "Could not create palette");
//					}
//				}
//			} break;
//			case SAVE_PALETTE: {
//				String name = (String) o;
//				SharedPrefsHelper.storeInSharedPreferences(this, name, model.createPalette(), PREFS_NAME);
//			} break;
//			default: {
//				// Tag when it is a color are the coordinates
//				int x = requestCode % model.width();
//				int y = requestCode / model.width();
//
//				// todo alpha!
//				model.set(x, y, ((Integer) o) | 0xff000000);
//
//				// redraw palette
//				findViewById(R.id.paletteView).invalidate();
//			}
//		}
//	}

	public void dimensionChanged() {
		MultiScrollView msView = findViewById(R.id.multiScrollView);
		msView.updateSize();
	}
}
