package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import at.searles.fractview.fractal.Fractal;

/**
 *
 */
public class PresetProgramsActivity extends Activity {

	public static final int PRESETS_PARAMETERS_RETURN = 102;

	private Fractal inFractal;

	private static final FractalEntry KEEP = new FractalEntry() {
		@Override
		public String title() {
			return "Keep current program";
		}

		@Override
		public Bitmap icon() {
			return null;
		}

		@Override
		public String description() {
			return "Reuses the current program";
		}
	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout); // image + text

		Intent intent = getIntent();
		this.inFractal = intent.getParcelableExtra("fractal"); // FIXME test whether this is preserved on termination

		// and since it is sorted, use it to write label-map.
		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		// fetch assets
		List<AssetsHelper.ProgramAsset> assets = AssetsHelper.entries(getAssets());

		// entries contain a first empty dummy
		List<FractalEntry> entries = new ArrayList<>(assets.size() + 1);

		// first, add the 'keep'-entry
		entries.add(KEEP);

		entries.addAll(assets);

		// wrap the favorites-adapter so that first
		final FractalEntryAdapter adapter = new FractalEntryAdapter(this);
		adapter.setData(entries);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				Fractal f;

				if(index == 0) {
					// means 'keep'
					f = inFractal;
				} else {
					String sourceCode =
							((AssetsHelper.ProgramAsset) entries.get(index)).source;
					f = new Fractal(inFractal.scale(), sourceCode, inFractal.parameters());
				}

				// Start new Parameter activity and put this source code inside.
				Intent i = new Intent(PresetProgramsActivity.this,
						PresetParametersActivity.class);
				i.putExtra("fractal", f);
				startActivityForResult(i, PRESETS_PARAMETERS_RETURN);
			}
		});

		/*lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		  @Override
		  public boolean onItemLongClick(AdapterView<?> adapterView, View view, int index, long id) {
			  AlertDialog.Builder builder = new AlertDialog.Builder(PresetProgramsActivity.this);

			  builder.setTitle("Select an Option");
			  builder.setItems(options, new DialogInterface.OnClickListener() {
				  @Override
				  public void onClick(DialogInterface dialog, int which) {
					  String l = labels.get(index);

					  Fractal selected = entries.get(labels.get(index)).fractal;

					  switch (which) {
						  case 0: {
							  // Select
							  returnFractal(selected);
						  }
						  break;
						  case 1: {
							  // Merge
							  returnFractal(new Fractal(
									  inFractal.scale(),
									  selected.sourceCode(),
									  inFractal.parameters().merge(selected.parameters())));
						  }
						  break;
						  case 2: {
							  // Merge Program
							  returnFractal(new Fractal(
									  inFractal.scale(),
									  selected.sourceCode(),
									  inFractal.parameters()));
						  }
						  break;
						  case 3: {
							  // Merge Parameters
							  // ==> Merge parameters!
							  returnFractal(new Fractal(
									  inFractal.scale(),
									  inFractal.sourceCode(),
									  inFractal.parameters().merge(selected.parameters())));
						  }
						  break;
						  case 4: {
							  // Merge Parameters and Scale
							  // ==> Merge parameters!
							  returnFractal(new Fractal(
									  selected.scale(),
									  inFractal.sourceCode(),
									  inFractal.parameters().merge(selected.parameters())));
						  }
						  break;
					  }
				  }
			  });

			  builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				  @Override
				  public void onClick(DialogInterface dialogInterface, int which) {
					  dialogInterface.dismiss();
				  }
			  });

			  builder.show();

			  return true;
		  }
	  	});*/

		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				PresetProgramsActivity.this.finish();
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
}
