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

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalLabel;

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
		setContentView(R.layout.fractal_list_activity_layout); // image + text

		Intent intent = getIntent();
		this.inFractal = intent.getParcelableExtra(FRACTAL_INDENT_LABEL);

		// and since it is sorted, use it to write label-map.
		ListView lv = (ListView) findViewById(R.id.fractalListView);

		// fetch assets
		List<AssetsHelper.SourceEntry> assets = AssetsHelper.entries(getAssets());

		// entries contain a first empty dummy
		List<AssetsHelper.SourceEntry> entries = new ArrayList<>(assets.size() + 1);

		// first, add the 'keep'-entry

		entries.addAll(assets);

		// wrap the favorites-adapter so that first
		final FractalEntryListAdapter adapter = new FractalEntryListAdapter(this);
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
							((AssetsHelper.SourceEntry) entries.get(index)).source;
					f = inFractal.copyNewSource(sourceCode, true);//new Fractal(inFractal.scale(), sourceCode, inFractal.data());
				}

				// Start new Parameter activity and put this source code inside.
				Intent i = new Intent(SourcesListActivity.this,
						ParametersListActivity.class);
				i.putExtra("fractal", f);
				startActivityForResult(i, PRESETS_PARAMETERS_RETURN);
			}
		});

		/*lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		  @Override
		  public boolean onItemLongClick(AdapterView<?> adapterView, View view, int index, long id) {
			  AlertDialog.Builder builder = new AlertDialog.Builder(SourcesListActivity.this);

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

	private static class SourceListAdapter extends FractalEntryListAdapter {

		/*
		 * Index | Purpose
		 * ------+-------------------------------
		 *     0 | Current []
		 */

		@Override
		public int getCount() {

			return 0;
		}

		@Override
		public Fractal getItem(int position) {
			return null;
		}

		@Override
		public Bitmap getIcon(int position) {
			return null;
		}

		@Override
		public String getTitle(int position) {
			return null;
		}

		@Override
		public String getDescription(int position) {
			return null;
		}

		@Override
		public void showOptions(int position) {

		}
	}
}
