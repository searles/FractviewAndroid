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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.fractview.R;
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
 * MainActivity creates RenderscriptFragment/FractalCalculatorFragment.
 *
 * After it has finished, FractalCalculatorFragment creates a FractalProviderFragment. The
 * FractalCalculatorFragment is kept for accessing renderscript.
 *
 * FractalProviderFragment must notify the activity that it has been created
 * (onAttach).
 * If there is no activity, onCreate in Activity will look for existing ones.
 *
 * When view is created in FractalProviderFragment, listeners to scale parameter are added.
 * When RenderscriptFragment notifies FractalProviderFragment, BitmapFragments are created.
 * BitmapFragments add listeners to FractalProvider.
 * If view exists, it listens to bitmap fragments.
 * Editors directly access FractalProviderFragment which accesses fractalprovider.
 */
public class FractalProviderFragment extends Fragment {

    private static final String FRACTAL_KEY = "fractal";

    private static final int WIDTH = 1920; // todo
    private static final int HEIGHT = 1080;

    public static final String TAG = "FractalProviderFragment";

    private FractalProvider provider;

    private List<FractalCalculatorFragment> children;
    private LinearLayout fractalContainer;

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

        this.children = new ArrayList<>();

        Log.d(TAG, "onCreate called");

        initProvider();

        for(int i = 0; i < provider.fractalsCount(); ++i) {
            String label = provider.label(i);
            FractalCalculatorFragment fractalCalculatorFragment = FractalCalculatorFragment.newInstance(label);
            getChildFragmentManager().beginTransaction().add(fractalCalculatorFragment, label).commit();

            children.add(fractalCalculatorFragment);
        }
    }

    private static final boolean DUAL_SCREEN_TOGGLE = true; // FIXME

    private void initProvider() {
        Log.d(TAG, "initializing provider");
        FractalData fractal = defaultFractal();

        if(DUAL_SCREEN_TOGGLE) {
            FractalData fractal2 = defaultFractal();
            fractal2.data.add(new ParameterKey("juliaset", ParameterType.Bool), true);

            this.provider = FractalProvider.dualFractal(fractal, fractal2, "Scale", "juliaset");
        } else {
            this.provider = FractalProvider.singleFractal(fractal);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "on create view");

        this.fractalContainer = (LinearLayout) inflater.inflate(R.layout.fractalview_layout, container);
        return fractalContainer;
    }

    @Override
    public void onDestroyView() {
        this.fractalContainer = null;
        super.onDestroyView();
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

    public ScaleableImageView.Listener createImageListener(String label) {
        return new ImageViewListener(label);
    }

    public void addFractalCalculatorView(String label, FractalCalculatorView view) {
        view.scaleableImageView().setListener(createImageListener(label));

        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        view.setLayoutParams(layoutParams);

        fractalContainer.addView(view);
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
