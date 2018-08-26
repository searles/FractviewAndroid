package at.searles.fractview.main;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
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

    public static FractalFragment newInstance(FractalData fractal) {
        // assertion: fractal must be valid!
        Bundle args = new Bundle();

        args.putBundle(FRACTAL_KEY, BundleAdapter.toBundle(fractal));

        FractalFragment fractalFragment = new FractalFragment();
        fractalFragment.setArguments(args);

        return fractalFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initProvider(savedInstanceState);
        initRenderscript();

        FractviewActivity activity = (FractviewActivity) getActivity();

        if(activity != null) {
            // otherwise call will happen later.
            activity.fractalFragmentInitializeCallback(this);
        }
    }

    private void initRenderscript() {
        InitializationFragment initializationFragment = InitializationFragment.newInstance();
        getChildFragmentManager().beginTransaction().add(initializationFragment, InitializationFragment.TAG).commit();
    }

    private void initProvider(Bundle savedInstanceState) {
        // TODO what is the proper way?
        FractalData fractal;

        if(savedInstanceState != null) {
            fractal = BundleAdapter.fractalFromBundle(savedInstanceState.getBundle(FRACTAL_KEY));
        } else {
            fractal = BundleAdapter.fractalFromBundle(getArguments().getBundle(FRACTAL_KEY));
        }

        this.provider = FractalProvider.singleFractal(fractal);
    }

    public void initializationFinished(InitializationFragment initializer) {
        fractalCalculators = new HashMap<>();

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);

            FractalCalculator fc = new FractalCalculator(WIDTH, HEIGHT, provider.get(label), initializer);

            fractalCalculators.put(label, fc);

            FractalCalculatorView bv = fractalCalculatorViews.get(label);
            FractalCalculator bf = fractalCalculators.get(label);
            bf.addBitmapFragmentListener(bv);
        }
    }

    public Fractal registerListener(String label, FractalProvider.Listener l) {
        provider.addListener(label, l);
        return provider.get(label);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        fractalCalculatorViews = new HashMap<>();

        View view = inflater.inflate(R.layout.fractalview_layout, container);

        LinearLayout layout = (LinearLayout) view.findViewById(R.id.fractalview_layout);

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);

            FractalCalculatorView bv = new FractalCalculatorView(getContext(), null);

            bv.scaleableImageView().setListener(new ImageViewListener(label));

            layout.addView(bv, i);

            fractalCalculatorViews.put(label, bv);
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);
            FractalCalculator bf = fractalCalculators.get(label);
            FractalCalculatorView bv = fractalCalculatorViews.get(label);
            bf.removeBitmapFragmentListener(bv);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // stop all fractalcalculators
        for(FractalCalculator calc : fractalCalculators.values()) {
            calc.dispose();
        }

        fractalCalculators.clear();
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
            Scale absoluteScale = provider.get(label).scale().relative(relativeScale);
            provider.set(FractalExternData.SCALE_KEY, label, absoluteScale);
        }
    }
}
