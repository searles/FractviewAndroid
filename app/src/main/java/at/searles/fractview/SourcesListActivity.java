package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.List;

import at.searles.fractal.Fractal;
import at.searles.fractal.predefined.SourceEntry;
import at.searles.fractal.android.BundleAdapter;

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

		this.adapter = new SourceListAdapter(this, inFractal);

		initListView();

		Button closeButton = (Button) findViewById(R.id.closeButton);
		closeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				// end this activity.
				SourcesListActivity.this.finish();
			}
		});
	}

	private void initListView() {
		// and since it is sorted, use it to write label-map.
		ListView lv = (ListView) findViewById(R.id.sourceListView);

		lv.setAdapter(adapter);

		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
				SourceEntry entry = adapter.getItem(index);

				Fractal outFractal = new Fractal(entry.source, inFractal.parameterMap());

				// Start new Parameter activity and put this source code inside.
				Intent i = new Intent(SourcesListActivity.this,
						ParametersListActivity.class);
				i.putExtra(FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(outFractal));
				startActivityForResult(i, PRESETS_PARAMETERS_RETURN);
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
		private List<SourceEntry> entries;

		SourceListAdapter(Activity context, Fractal inFractal) {
			super(context);

			this.inEntry = new SourceEntry("Current", null, "Current source", inFractal.sourceCode());

			this.entries = SourceEntry.entries(context.getAssets());
		}

		@Override
		public int getCount() {
			return 1 + entries.size();// + customEntries.size();
		}

		@Override
		public SourceEntry getItem(int position) {
			if(position == 0) {
				return inEntry;
			} else {
				return entries.get(position - 1);
			}
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
	}
}
