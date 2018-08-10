package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;


// Activity is the glue between FractalCalculator and Views.
public class FractviewActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState); // this one (re-)creates all fragments on rotation.

		Log.d(getClass().getName(), "creating activity, savedInstanceState=" + savedInstanceState);

		// First, take care of the view.
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); // make sure that screen stays awake

		initialize();
	}
	
	private void initialize() {
		FragmentManager fm = getFragmentManager();
		InitializationFragment initializationFragment = (InitializationFragment) fm.findFragmentByTag(InitializationFragment.TAG);

		if(initializationFragment == null) {
			initializationFragment = InitializationFragment.newInstance();

			FragmentTransaction transaction = fm.beginTransaction();
			transaction.add(initializationFragment, InitializationFragment.TAG);
			transaction.commit();
		}
	}

	public void registerFractalFragment(FractalFragment fractalFragment) {
		// TODO Set up menu.
	}
}
