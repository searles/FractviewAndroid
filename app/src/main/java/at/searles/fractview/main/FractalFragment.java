package at.searles.fractview.main;

import android.app.Fragment;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.fractview.R;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.ui.FractalCalculatorView;
import at.searles.fractview.bitmap.ui.ScaleableImageView;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.parameters.palettes.PaletteActivity;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * Central fragment managing renderscriptfragment, bitmapfragments, fractalprovider
 * and scalableimageviews.
 *
 * Lifecycle:
 *
 * MainActivity creates RenderscriptFragment/InitializationFragment.
 *
 * After it has finished, InitializationFragment creates a FractalFragment. The
 * InitializationFragment is kept for accessing renderscript.
 *
 * FractalFragment must notify the activity that it has been created
 * (onAttach).
 * If there is no activity, onCreate in Activity will look for existing ones.
 *
 * When view is created in FractalFragment, listeners to scale parameter are added.
 * When RenderscriptFragment notifies FractalFragment, BitmapFragments are created.
 * BitmapFragments add listeners to FractalProvider.
 * If view exists, it listens to bitmap fragments.
 * Editors directly access FractalFragment which accesses fractalprovider.
 */
public class FractalFragment extends Fragment {

    private static final String FRACTAL_KEY = "fractal";

    private static final int WIDTH = 640;
    private static final int HEIGHT = 640;

    public static final String TAG = "FractalFragment";

    private FractalProvider provider;

    private Map<String, FractalCalculator> fractalCalculators;
    private Map<String, FractalCalculatorView> fractalCalculatorViews;

    private FractalData defaultFractal() {
        // TODO: Move to assets
        AssetManager am = getActivity().getAssets();
        try(InputStream is = am.open("sources/Default.fv")) {
            BufferedReader br = new BufferedReader(new InputStreamReader(is));

            String source = br.lines().collect(Collectors.joining("\n"));
            return new FractalData(source, new Parameters());
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        Log.d(TAG, "onCreate called");

        initProvider();
        initRenderscript();
    }

    private void initRenderscript() {
        InitializationFragment initializationFragment = (InitializationFragment) getChildFragmentManager().findFragmentByTag(InitializationFragment.TAG);

        if(initializationFragment == null) {
            Log.d(TAG, "creating new initFragment");
            initializationFragment = InitializationFragment.newInstance();
            getChildFragmentManager().beginTransaction().add(initializationFragment, InitializationFragment.TAG).commit();
        } else {
            Log.d(TAG, "init fragment already exists");
        }
    }

    private void initProvider() {
        Log.d(TAG, "initializing provider");
        FractalData fractal = defaultFractal();
        this.provider = FractalProvider.singleFractal(fractal);
    }

    public void createCalculators(InitializationFragment initializer) {
        // this is called indirectly via the initializationFragment.
        // TODO: Do not call when this fragment was scheduled for destruction!

        Log.d(TAG, "initializing providers");

        // creates fractal calculators.
        fractalCalculators = new HashMap<>();

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);

            Log.d(TAG, "initializing provider " + label);

            FractalCalculator fc = new FractalCalculator(WIDTH, HEIGHT, provider.get(label), initializer);

            fractalCalculators.put(label, fc);

            this.provider.addListener(label, fc);
        }

        checkConnectViewsAndCalculators();
    }

    public Fractal registerListener(String label, FractalProvider.Listener l) {
        provider.addListener(label, l);
        return provider.get(label);
    }

    private void checkConnectViewsAndCalculators() {
        if(fractalCalculatorViews == null || fractalCalculators == null) {
            Log.d(TAG, "cannot connect calculators and views yet");
            return;
        }

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);

            Log.d(TAG, "connecting " + label);

            FractalCalculatorView bv = fractalCalculatorViews.get(label);
            FractalCalculator bf = fractalCalculators.get(label);

            bf.addListener(bv);

            bv.newBitmapCreated(bf);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if(fractalCalculatorViews != null) {
            Log.e(TAG, "fractalCalculatorViews is not null!");
        }

        Log.d(TAG, "create new view");
        fractalCalculatorViews = new HashMap<>();

        View view = inflater.inflate(R.layout.fractalview_layout, container);

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.fractalview_layout);

        layout.setBackgroundColor(0xff00ff00); // todo remove this

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);

            Log.d(TAG, "create view: " + label);

            FractalCalculatorView bv = new FractalCalculatorView(getContext(), null);

            bv.setBackgroundColor(0xffff00ff);

            bv.scaleableImageView().setListener(new ImageViewListener(label));
            layout.addView(bv); // fixme

            fractalCalculatorViews.put(label, bv);
        }

        checkConnectViewsAndCalculators();

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        Log.d(TAG, "destroying view");

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);
            FractalCalculator bf = fractalCalculators.get(label);
            FractalCalculatorView bv = fractalCalculatorViews.get(label);
            bf.removeListener(bv);
        }

        fractalCalculatorViews = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // stop all fractalcalculators
        Log.d(TAG, "onDestroy called");

        for(FractalCalculator calc : fractalCalculators.values()) {
            calc.dispose();
        }

        fractalCalculators = null;
    }

    public FractalProvider provider() {
        return provider;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PaletteActivity.PALETTE_ACTIVITY_RETURN:
                if(resultCode == PaletteActivity.OK_RESULT) {
                    String id = data.getStringExtra(PaletteActivity.ID_LABEL);
                    Palette palette = BundleAdapter.paletteFromBundle(data.getBundleExtra(PaletteActivity.PALETTE_LABEL));
                    provider.set(new ParameterKey(id, ParameterType.Palette), palette);
                }
                return;
            default:
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private class ImageViewListener implements ScaleableImageView.Listener {

        final String label;

        ImageViewListener(String label) {
            this.label = label;
        }

        @Override
        public void scaleRelative(Scale relativeScale) {
            Log.d(getClass().getName(), "setting relative scale");

            Scale absoluteScale = provider.get(label).scale().relative(relativeScale);
            provider.set(FractalExternData.SCALE_KEY, label, absoluteScale);
        }
    }
}
