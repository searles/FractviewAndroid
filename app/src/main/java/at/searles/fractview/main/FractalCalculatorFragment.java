package at.searles.fractview.main;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import at.searles.fractal.Fractal;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.ui.FractalCalculatorView;
import at.searles.fractview.fractal.Drawer;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers
 */
public class FractalCalculatorFragment extends Fragment {

    public static final String TAG = "fractal_calculator_fragment";
    private static final String INDEX_KEY = "index";

    private CreateDrawerTask createDrawerTask;
    private ProgressDialog progressDialog;

    private FractalCalculator calculator;
    private FractalCalculatorView view;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment FractalCalculatorFragment.
     */
    public static FractalCalculatorFragment newInstance(String label) {
        Bundle args = new Bundle();
        args.putString(INDEX_KEY, label);

        FractalCalculatorFragment fragment = new FractalCalculatorFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        String label = getArguments().getString(INDEX_KEY);
        FractalProviderFragment parent = (FractalProviderFragment) getParentFragment();

        calculator = new FractalCalculator(1024, 640);

        parent.provider().addListener(label, calculator);

        // start it.
        createDrawerTask = new CreateDrawerTask(RenderScript.create(getContext()), this);
        createDrawerTask.execute();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "on create view");

        if(createDrawerTask != null) {
            this.progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("Please wait while scripts are initialized.");
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        this.view = new FractalCalculatorView(getContext(), null);

        String label = getArguments().getString(INDEX_KEY);

        // the view can modify the scale parameter in the provider
        ((FractalProviderFragment) getParentFragment()).addFractalCalculatorView(label, view);

        connectViewAndDrawer();

        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (this.progressDialog != null) {
            this.progressDialog.dismiss();
            this.progressDialog = null;
        }

        this.view.dispose();
        calculator.removeListener(this.view);

        this.view = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(createDrawerTask != null) {
            createDrawerTask.cancel(true);
        }

        this.calculator.dispose();
    }

    void createDrawerTaskCallback(Drawer drawer) {
        FractalProviderFragment parent = (FractalProviderFragment) getParentFragment();
        String label = getArguments().getString(INDEX_KEY);

        Fractal fractal = parent.provider().get(label);

        drawer.setFractal(fractal);

        calculator.setDrawer(drawer);

        this.createDrawerTask = null;

        if(progressDialog != null) {
            progressDialog.dismiss();
            progressDialog = null;
        }

        connectViewAndDrawer();
    }

    private void connectViewAndDrawer() {
        if(createDrawerTask == null && view != null) {
            calculator.addListener(view);
            view.newBitmapCreated(calculator);
        }
    }
}
