package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import at.searles.fractview.R;


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

		if(fractalFragment == null) {
			throw new IllegalArgumentException("fragment is part of view. it should have been created.");
		} else {
			// init menu according to existing fragment
			fractalFragmentInitializeCallback(fractalFragment);
		}
	}

	public void fractalFragmentInitializeCallback(FractalFragment src) {
    	// TODO init menu

		/*
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
