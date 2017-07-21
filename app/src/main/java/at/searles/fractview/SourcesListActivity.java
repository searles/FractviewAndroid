package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ListView;

import java.util.ArrayList;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;
import at.searles.meelan.CompileException;

/**
 * Source shows up here in the following order:
 * 0. "Current"
 * 1..m. "Presets"
 * m+1..n "Saved Programs"
 * + [X] Keep parameters
 */
public class SourcesListActivity extends Activity {

	public static final String FRACTAL_INDENT_LABEL = "fractal";

	public static final int PRESETS_PARAMETERS_RETURN = 102;

	private Fractal inFractal;
	private SourceListAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sources_list_layout); // image + text

		Intent intent = getIntent();
		this.inFractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(FRACTAL_INDENT_LABEL));

		// and since it is sorted, use it to write label-map.
		ListView lv = (ListView) findViewById(R.id.sourceListView);

		CheckBox useDefaultsCheckBox = (CheckBox) findViewById(R.id.useDefaultsCheckBox);

		// wrap the favorites-adapter so that first
		this.adapter = new SourceListAdapter(this, inFractal);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				// FIXME if selection mode then select and unselect.
				SourceEntry entry = adapter.getItem(index);

				boolean useDefaults = useDefaultsCheckBox.isChecked();

				Fractal outFractal;

				if(useDefaults) {
					outFractal = new Fractal(entry.source, null);
				} else {
					// compile it to assure that it works.
					outFractal = new Fractal(entry.source, inFractal.parameterMap());

					try {
						outFractal.compile();
					} catch (CompileException e) {
						DialogHelper.error(SourcesListActivity.this, "Compile error (select \"useDefaults\" to avoid)\n" + e.getMessage());
						outFractal = null;
					}
				}

				if(outFractal != null) {
					// Start new Parameter activity and put this source code inside.
					Intent i = new Intent(SourcesListActivity.this,
							ParametersListActivity.class);
					i.putExtra(FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(outFractal));
					startActivityForResult(i, PRESETS_PARAMETERS_RETURN);
				}
			}
		});

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		  @Override
		  public boolean onItemLongClick(AdapterView<?> adapterView, View view, int index, long id) {
			  // Select this entry

			  return true;
		  }
	  	});

		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				SourcesListActivity.this.finish();
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == PRESETS_PARAMETERS_RETURN) {
			setResult(1, data);
			finish();
		}
	}

	private static class SourceListAdapter extends FractalListAdapter<SourceEntry> {

		private SourceEntry inEntry;
		//private ArrayList<SourceEntry> customEntries;
		//private final SharedPreferences prefs;

		public SourceListAdapter(Activity context, Fractal inFractal) {
			super(context);

			this.inEntry = new SourceEntry("Current", null, "Current source", inFractal.sourceCode());

			// FIXME put PREFS_NAME into resource file
			/*this.prefs = context.getSharedPreferences(
					SourceEditorActivity.PREFS_NAME,
					Context.MODE_PRIVATE);
			initEntries(context.getAssets());
			initializeCustomEntries();*/
		}

		/*private void initializeCustomEntries() {
			if(this.customEntries == null) {
				this.customEntries = new ArrayList<>();
			} else {
				this.customEntries.clear();
			}

			for(String key : prefs.getAll().keySet()) {
				String source = prefs.getString(key, null);

				if(source == null) {
					Log.e(getClass().getName(), "shared prefs contains entry " + key + " but no string");
				} else {
					this.customEntries.add(new SourceEntry(key, null, null, source));
				}
			}
		}*/

		@Override
		public int getCount() {
			return 1 + _ENTRIES.size();// + customEntries.size();
		}

		@Override
		public SourceEntry getItem(int position) {
			if(position == 0) {
				return inEntry;
			} else /*if(position < 1 + _ENTRIES.size())*/ {
				return _ENTRIES.get(position - 1);
			} /*else {
				return customEntries.get(position - _ENTRIES.size() - 1);
			}*/
		}

		@Override
		public Bitmap getIcon(int position) {
			return getItem(position).icon;
		}

		@Override
		public String getTitle(int position) {
			return getItem(position).title;
		}

		@Override
		public String getDescription(int position) {
			return getItem(position).description;
		}

		@Override
		public void showOptions(int position) {
			// FIXME:
			// Options are 'edit', 'copy', 'rename'.
		}
	}


	// ======= Fetch assets =========

	// Create a list of assets and icons that come with it.
	// Read private entries
	public static SourceEntry createEntry(AssetManager am, String title, String iconFilename, String description, String sourceFilename) {
		String sourceCode = AssetsHelper.readSourcecode(am, sourceFilename);
		Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

		if(sourceCode == null/* || icon == null*/) {
			throw new IllegalArgumentException("bad asset: " + title);
		}

		return new SourceEntry(title, icon, description, sourceCode);
	}

	// And now for the presets.
	public static class SourceEntry {
		public final String title;
		public final Bitmap icon;
		public final String description;
		public final String source;

		private SourceEntry(String title, Bitmap icon, String description, String source) {
			this.title = title;
			this.icon = icon;
			this.description = description;
			this.source = source;
		}
	}

	private static ArrayList<SourceEntry> _ENTRIES = null;

	public static synchronized void initEntries(AssetManager am) {
		if (_ENTRIES == null) {
			// create entries.
			_ENTRIES = new ArrayList<>();

			// grouped : the ones with maxpower
			_ENTRIES.add(createEntry(am, "Default", "default.png", "Basic fractal with bailout and lake coloring", "Default.fv"));
			_ENTRIES.add(createEntry(am, "Julia Map", "juliamap.png", "Variation of \"Default\" that shows a map of julia sets.", "JuliaMap.fv"));
			_ENTRIES.add(createEntry(am, "Branching", "branching.png", "\"Default\" with an addend for average coloring methods for polynom formulas", "Branching.fv"));
			_ENTRIES.add(createEntry(am, "Cczcpaczcp", "ccz.png", "Default with a built-in special formula by Mark R Eggleston, called Cczcpaczcp", "Cczcpaczcp.fv"));

			// the ones with orbit traps
			_ENTRIES.add(createEntry(am, "Orbit Trap", "orbittrap.png", "\"Default\" with an orbit trap", "OrbitTrap.fv"));
			_ENTRIES.add(createEntry(am, "Frame Orbit Trap", "frameorbittrap.png", "\"Default\" with an orbit trap", "FrameOrbitTrap.fv"));
			_ENTRIES.add(createEntry(am, "Min/Max Trap", "minmaxtrap.png", "Picks the maximum distance to the orbit trap", "MinMaxOrbitTrap.fv"));

			// the ones with fold
			_ENTRIES.add(createEntry(am, "Fold", "fold.png", "\"Default\" with a more general addend (fold), also suitable for stripe coloring methods of non-polynomial fractals", "Fold.fv"));
			_ENTRIES.add(createEntry(am, "Two Folds", "twofolds.png", "\"Default\" with two fold functions", "TwoFold.fv"));
			_ENTRIES.add(createEntry(am, "Lake Fold", "lakefold.png", "Draws only the lake of a fractal, thus useful for bounded fractals like Duck or Newton", "Lake.fv"));

			// Special Lake Fold ones
			_ENTRIES.add(createEntry(am, "Newton", "newton.png", "Newton method for root finding fractals", "Newton.fv"));
			_ENTRIES.add(createEntry(am, "Nova", "nova.png", "Nova fractal defined by z - R * (z^power + argument) / (z^power + argument)' + p", "Nova.fv"));
			_ENTRIES.add(createEntry(am, "Secant", "secant.png", "Secant method for root finding fractals", "Secant.fv"));

			// Completely different onces
			_ENTRIES.add(createEntry(am, "Lyapunov", "lyapunov.png", "Lyapunov fractals", "Lyapunov.fv"));

			_ENTRIES.add(createEntry(am, "Pendulum (Multiple Magnets)", "pendulum.png", "Magnetic Pendulum Simulation with 3 Magnets", "Pendulum.fv"));
			_ENTRIES.add(createEntry(am, "Pendulum (3 Magnets)", "pendulum3.png", "Magnetic Pendulum Simulation with 3 Magnets", "Pendulum3.fv"));

			_ENTRIES.add(createEntry(am, "Complex Function", "complexfn.png", "Drawing of Complex function (Color Wheel method by default)", "ComplexFn.fv"));
		}
	}

}
