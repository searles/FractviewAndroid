package at.searles.fractview.assets;

import android.app.Activity;

/**
 * This activity shows all source assets and all stored source files. It
 * allows to edit sources (starting the source code editor activity)
 * and delete saved sources.
 * It returns the selected source code or null if nothing has been selected.
 */
public class SourcesListActivity extends Activity {

//	public static final String FRACTAL_INDENT_LABEL = "fractal";
//
//	public static final int PRESETS_PARAMETERS_RETURN = 102;
//
//	private SourceListAdapter adapter;
//
//	@Override
//	public void onCreate(Bundle savedInstanceState) {
//		super.onCreate(savedInstanceState);
//		setContentView(R.layout.sources_list_layout); // image + text
//
//		this.adapter = new SourceListAdapter(this);
//		initListView();
//
//		Button closeButton = (Button) findViewById(R.id.closeButton);
//		closeButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View view) {
//				// end this activity.
//				SourcesListActivity.this.finish();
//			}
//		});
//	}
//
//	private void initListView() {
//		// and since it is sorted, use it to write label-map.
//		ListView lv = (ListView) findViewById(R.id.sourceListView);
//
//		lv.setAdapter(adapter);
//
//		lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//			@Override
//			public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
//				SourceAsset entry = adapter.getItem(index);
//
//				Fractal outFractal = new Fractal(entry.source, inFractal.parameterMap());
//
//				// Start new Parameter activity and put this source code inside.
//				Intent i = new Intent(SourcesListActivity.this,
//						SelectAssetActivity.class);
//				i.putExtra(FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(outFractal));
//				startActivityForResult(i, PRESETS_PARAMETERS_RETURN);
//			}
//		});
//	}
//
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//
//		if (requestCode == PRESETS_PARAMETERS_RETURN) {
//			setResult(1, data);
//			finish();
//		}
//	}

}
