package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ListView;

import at.searles.fractview.R;
import at.searles.fractview.parameters.ParameterAdapter;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.fractview.parameters.ParameterSelectListener;


// Activity is the glue between FractalCalculator and Views.
public class FractviewActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // this one (re-)creates all fragments on rotation.

		Log.d(getClass().getName(), "creating activity, savedInstanceState=" + savedInstanceState);

		// First, take care of the view.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // make sure that screen stays awake

		setContentView(R.layout.fractview_layout); // will create fragment

		initializeFractalFragment();
	}
	
	private void initializeFractalFragment() {
		FragmentManager fm = getFragmentManager();
		FractalFragment fractalFragment = (FractalFragment) fm.findFragmentById(R.id.fractal_fragment);

		// init menu according to existing fragment
		initProviderMenu(fractalFragment);
	}

	private void initProviderMenu(FractalFragment fragment) {
		ParameterAdapter adapter = new ParameterAdapter(this, fragment.provider());

		ListView listView = (ListView) findViewById(R.id.nav_list_view);

		listView.setAdapter(adapter);

		listView.setOnItemClickListener(new ParameterSelectListener(fragment));

		listView.setOnItemLongClickListener(new ParameterLongSelectListener(fragment.provider()));
	}

	private void initMenu() {
    	// TODO
		/*
		 * [X] Hide top bar
		 * Edit (opens swipe-in from left)
		 *   -> FractalProviderEditor
		 * Settings (opens swipe-in from right)
		 *   -> Scroll-Lock (no edits)
		 *   -> Rotation-Lock
		 *   -> Keep centered
		 *   -> Edit points - opens dialog with all editable (cplx and expr).
		 *                    Warn if an expr is reset to 0:0 because it is not a number.
		 *   -> Show grid
		 *   -> Resolution
		 * Demos -> opens AssetsActivity.
		 * Direct Save/Share
		 * Favorites (opens favorites activity)
		 * Add #name to Favorites (opens test dialog) (multiple times)
		 * (conditional) Split view -> opens dialog for all extern bools. Bool is fixed, scale is not shared.
		 * (conditional) Show only #name
		 * Render at different resolution - opens dialog with resolution (option 'keep ratio') and Supersampling.
		 */
	}
}
