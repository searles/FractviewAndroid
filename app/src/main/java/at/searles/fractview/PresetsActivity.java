package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import at.searles.fractview.fractal.FavoriteEntry;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.PresetFractals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

/**
 *
 */
public class PresetsActivity extends Activity {

	static final String[] options = {"Select", "Merge Program and Parameters", "Merge Program" , "Merge Parameters", "Merge Parameters and Scale"};

	// Reuse favorites.
	TreeMap<String, FavoriteEntry> entries = new TreeMap<>();

	Fractal inFractal;
	List<String> labels;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.favorite_layout);

		Intent intent = getIntent();
		this.inFractal = intent.getParcelableExtra("fractal"); // FIXME test whether this is preserved on termination

		// List to store all errors that occured
		List<String> errors = new LinkedList<>();

		for(PresetFractals.Preset entry : PresetFractals.PRESETS) {
			// load file from assets
			try {
				String sourceCode = PresetFractals.readSourcecode(this, entry.filename);
				Bitmap icon = PresetFractals.readIcon(this, entry);

				if (sourceCode != null) {
					FavoriteEntry fav = new FavoriteEntry(new Fractal(entry.scale, sourceCode, entry.parameters), icon);
					entries.put(entry.title, fav);
				} // otherwise, something would have been shown
			} catch(IOException e) {
				errors.add(e.getMessage());
			}
		}

		if(!errors.isEmpty()) {
			// Show a toast for the error messages
			StringBuilder sb = new StringBuilder(getString(R.string.error_read_presets));
			for(String s : errors) {
				sb.append(s).append("\n");
			}

			Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
		}

		// and since it is sorted, use it to write label-map.
		labels = new ArrayList<>(entries.size());

		for(String label : entries.keySet()) {
			labels.add(label);
		}

		ListView lv = (ListView) findViewById(R.id.bookmarkListView);

		final FavoritesAdapter adapter = new FavoritesAdapter(this, labels, entries);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				FavoriteEntry fav = entries.get(labels.get(index));
				returnFractal(fav.fractal);
			}
		});

		lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
		  @Override
		  public boolean onItemLongClick(AdapterView<?> adapterView, View view, int index, long id) {
			  AlertDialog.Builder builder = new AlertDialog.Builder(PresetsActivity.this);

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
	  	});

		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				PresetsActivity.this.finish();
			}
		});
	}

	void returnFractal(Fractal f) {
		Intent data = new Intent();
		data.putExtra("fractal", f);
		setResult(1, data);
		finish();
	}


	// And now for the presets.


}
