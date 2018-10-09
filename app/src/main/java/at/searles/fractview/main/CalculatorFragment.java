package at.searles.fractview.main;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.graphics.PointF;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.ui.CalculatorView;
import at.searles.fractview.fractal.Drawer;
import at.searles.math.Cplx;
import at.searles.math.Scale;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers. Furthermore
 * maintains views and parameters of views.
 */
public class CalculatorFragment extends Fragment {

    public static final String TAG = "fractal_calculator_fragment";
    private static final String FRAGMENT_INDEX_KEY = "index";

    private CreateDrawerTask createDrawerTask;
    private ProgressDialog progressDialog;

    private FractalCalculator calculator;
    private CalculatorView view;
    private FractalProviderFragment parent;
    private int fragmentIndex;

    private List<String> interactivePointKeys;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment CalculatorFragment.
     */
    public static CalculatorFragment newInstance(int fragmentIndex) {
        Bundle args = new Bundle();
        args.putInt(FRAGMENT_INDEX_KEY, fragmentIndex);

        CalculatorFragment fragment = new CalculatorFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");

        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        // create
        this.fragmentIndex = getArguments().getInt(FRAGMENT_INDEX_KEY);
        this.parent = (FractalProviderFragment) getParentFragment();

        this.interactivePointKeys = new ArrayList<>(2);

        this.calculator = new FractalCalculator(1024, 640);

        // connect
        parent.addListener(fragmentIndex, calculator);

        parent.addParameterMapListener(new FractalProvider.ParameterMapListener() {
            @Override
            public void parameterMapModified(FractalProvider src) {
                if(view != null) {
                    updateInteractivePoints();
                }
            }
        });

        // start it.
        createDrawerTask = new CreateDrawerTask(RenderScript.create(getContext()), this);
        createDrawerTask.execute();
    }

    /**
     * @return null if the parameter does not exist.
     */
    private Cplx interactivePointValue(String key) {
        FractalProvider.ParameterEntry parameterEntry = parent.getParameterEntryByFragmentIndex(key, fragmentIndex);
        return parameterEntry != null ? (Cplx) parameterEntry.value : null;
    }

    public void updateInteractivePoints() {
        // Scale might have changed or the point itself
        for(String pointKey : interactivePointKeys) {
            Cplx value = interactivePointValue(pointKey);
            PointF normalizedPoint = valueToNormalizedPoint(value);
            view.updatePoint(pointKey, normalizedPoint);
        }
    }

    private PointF valueToNormalizedPoint(Cplx value) {
        Scale scale = parent.getFractalByFragmentIndex(fragmentIndex).scale();
        float[] pt = scale.invScale(value.re(), value.im());
        return new PointF(pt[0], pt[1]);
    }

    public void addInteractivePoint(String key) {
        addInteractivePointInView(key);
        interactivePointKeys.add(key);
    }

    private void addInteractivePointInView(String key) {
        FractalProvider.ParameterEntry parameterEntry = parent.getParameterEntryByFragmentIndex(key, fragmentIndex);

        // XXX allow editing of Expr;
        PointF npt = valueToNormalizedPoint((Cplx) parameterEntry.value);

        view.addPoint(key, parameterEntry.description, npt);
    }

    public void moveParameterToNormalized(String key, PointF normalizedPoint) {
        Scale scale = parent.getFractalByFragmentIndex(fragmentIndex).scale();
        double[] value = scale.scale(normalizedPoint.x, normalizedPoint.y);
        parent.setParameterByFragmentIndex(key, fragmentIndex, new Cplx(value[0], value[1]));
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

        this.view = new CalculatorView(getContext(), null);
        this.view.setFragment(this);

        // the view can modify the scale parameter in the provider
        ((FractalProviderFragment) getParentFragment()).addFractalCalculatorView(fragmentIndex, view);

        // the next line sets the bitmap
        connectViewAndDrawer();

        // which is necessary to calculate the coordinates of editable points
        for(String pointKey : interactivePointKeys) {
            addInteractivePointInView(pointKey);
        }

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
        Fractal fractal = parent.getFractalByFragmentIndex(fragmentIndex);

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
