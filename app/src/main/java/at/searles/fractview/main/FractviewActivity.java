package at.searles.fractview.main;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.Parameters;


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
		FractalFragment fractalFragment = (FractalFragment) fm.findFragmentByTag(InitializationFragment.TAG);

		if(fractalFragment == null) {
			fractalFragment = FractalFragment.newInstance(defaultFractal());

			FragmentTransaction transaction = fm.beginTransaction();
			transaction.add(fractalFragment, FractalFragment.TAG);
			transaction.commit();

			// init menu will happen by callback.
		} else {
			// init menu according to existing fragment
			fractalFragmentInitializeCallback(fractalFragment);
		}
	}

	public void fractalFragmentInitializeCallback(FractalFragment src) {
    	// TODO init menu
	}

	private FractalData defaultFractal() {
		// TODO: Move to assets
		AssetManager am = getAssets();
		try(InputStream is = am.open("sources/Default.fv")) {
			BufferedReader br = new BufferedReader(new InputStreamReader(is));

			String source = br.lines().collect(Collectors.joining("\n"));
			return new FractalData(source, new Parameters());
		} catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}
}
