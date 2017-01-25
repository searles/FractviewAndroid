package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import at.searles.fractview.fractal.Fractal;

/**
 *
 */
public class PresetProgramsActivity extends Activity {

	private Fractal inFractal;
	private List<String> labels;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout); // image + text

		Intent intent = getIntent();
		this.inFractal = intent.getParcelableExtra("fractal"); // FIXME test whether this is preserved on termination

		// and since it is sorted, use it to write label-map.
		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		List<AssetsHelper.ProgramAsset> entries = AssetsHelper.entries(getAssets());

		final FavoritesAdapter adapter = new FavoritesAdapter(this, entries);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				String sourceCode = entries.get(index).source;
				returnFractal(new Fractal(inFractal.scale(), sourceCode, inFractal.parameters()));
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

	void returnFractal(Fractal f) {
		Intent data = new Intent();
		data.putExtra("fractal", f);
		setResult(1, data);
		finish();
	}

}
