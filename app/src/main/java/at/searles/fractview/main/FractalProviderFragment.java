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

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterKey;
import at.searles.fractal.data.ParameterType;
import at.searles.fractal.data.Parameters;
import at.searles.fractview.R;
import at.searles.fractview.SourceEditorActivity;
import at.searles.fractview.bitmap.ui.CalculatorView;
import at.searles.fractview.bitmap.ui.ScalableImageView;
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
 * MainActivity creates RenderscriptFragment/CalculatorFragment.
 *
 * After it has finished, CalculatorFragment creates a FractalProviderFragment. The
 * CalculatorFragment is kept for accessing renderscript.
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

//    public static final String TAG = "FractalProviderFragment";
    private static final String FRACTAL_CALCULATOR_LABEL_PREFIX = "fclp";

    private FractalProvider provider;

    private List<Integer> fragmentIndices; // this is a mapping from provider index to fragment index.
    private int fragmentCounter; // to generate unique and persistent fragment indices.

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

    private String fractalCalculatorLabel(int index) {
        return FRACTAL_CALCULATOR_LABEL_PREFIX + index;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        this.fragmentIndices = new ArrayList<>();

        Log.d(getClass().getName(), "onCreate called");

        initProvider();

        for(int i = 0; i < provider.fractalCount(); ++i) {
            int fragmentIndex = nextFragmentIndex();
            fragmentIndices.add(fragmentIndex);

            CalculatorFragment calculatorFragment = CalculatorFragment.newInstance(fragmentIndex);
            getChildFragmentManager().beginTransaction().add(calculatorFragment, fractalCalculatorLabel(i)).commit();
        }
    }

    private void initProvider() {
        Log.d(getClass().getName(), "initializing provider");

        this.provider = new FractalProvider();

        FractalData fractal = defaultFractal();
        this.provider.addFractal(fractal);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        Log.d(getClass().getName(), "on create view");

        this.fractalContainer = (LinearLayout) inflater.inflate(R.layout.fractalview_layout, container);
        return fractalContainer;
    }

    @Override
    public void onDestroyView() {
        this.fractalContainer = null;
        super.onDestroyView();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PaletteActivity.PALETTE_ACTIVITY_RETURN:
                if(resultCode == PaletteActivity.OK_RESULT) {
                    String id = data.getStringExtra(PaletteActivity.ID_LABEL);
                    int owner = data.getIntExtra(PaletteActivity.OWNER_LABEL, -1);

                    Palette palette = BundleAdapter.paletteFromBundle(data.getBundleExtra(PaletteActivity.PALETTE_LABEL));
                    provider.setParameter(id, owner, palette);
                }
                return;
            case SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN:
                if(resultCode == PaletteActivity.OK_RESULT) {
                    int owner = data.getIntExtra(SourceEditorActivity.OWNER_LABEL, -1);

                    String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);
                    provider.setParameter(FractalExternData.SOURCE_LABEL, owner, source);
                }
                return;
            default:
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void addFractalCalculatorView(int fragmentIndex, CalculatorView view) {
        view.scaleableImageView().setListener(new ImageViewListener(fragmentIndex));

        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        view.setLayoutParams(layoutParams);

        fractalContainer.addView(view);
    }

    public void addFractal(String booleanKey, String...exclusiveParameters) {
        int originalIndex = 0;

        int fragmentIndex = nextFragmentIndex();

        boolean value = (Boolean) provider.getParameter(booleanKey, originalIndex);

        FractalData splitData = provider.getFractal(originalIndex).toData();
        splitData.data.add(new ParameterKey(booleanKey, ParameterType.Bool), !value);

        provider.addFractal(splitData, exclusiveParameters);

        CalculatorFragment calculatorFragment = CalculatorFragment.newInstance(fragmentIndex);
        getChildFragmentManager().beginTransaction().add(calculatorFragment, fractalCalculatorLabel(fragmentIndex)).commit();

        fragmentIndices.add(fragmentIndex);
    }

    private int nextFragmentIndex() {
        return fragmentCounter++;
    }

    public void removeFractal(int providerIndex) {
        // remove fragment and owner.
        int fragmentIndex = fragmentIndices.get(providerIndex);
        String label = fractalCalculatorLabel(fragmentIndex);

        CalculatorFragment fragment = (CalculatorFragment) getChildFragmentManager().findFragmentByTag(label);
        getChildFragmentManager().beginTransaction().remove(fragment).commit();

        fragmentIndices.remove(providerIndex);

        // remove from provider
        provider.removeFractal(providerIndex);

        if(fractalContainer != null) {
            fractalContainer.removeViewAt(providerIndex);
        }
    }

    public void addListener(int fragmentIndex, FractalProvider.FractalListener listener) {
        int providerIndex = fragmentIndices.indexOf(fragmentIndex);
        provider.addFractalListener(providerIndex, listener);
    }

    public Fractal getFractalByFragmentIndex(int fragmentIndex) {
        int providerIndex = fragmentIndices.indexOf(fragmentIndex);
        return provider.getFractal(providerIndex);
    }

    /**
     * @return null if the parameter does not exist.
     */
    public FractalProvider.ParameterEntry getParameterEntryByFragmentIndex(String key, int fragmentIndex) {
        int owner = fragmentIndices.indexOf(fragmentIndex);

        if(owner == -1) {
            return null;
        }

        // provider will handle the case that key/owner is not exclusive.
        return provider.getParameterEntry(key, owner);
    }

    public FractalProvider.ParameterEntry getParameterEntry(String key, int owner) {
        return provider.getParameterEntry(key, owner);
    }

    public Object getParameter(String key, int owner) {
        return provider.getParameter(key, owner);
    }

    public void setParameterByFragmentIndex(String key, int fragmentIndex, Object newValue) {
        int owner = fragmentIndices.indexOf(fragmentIndex);

        if(owner == -1) {
            throw new IllegalArgumentException("no such fragment index");
        }

        // provider will handle the case that key/owner is not exclusive.
        provider.setParameter(key, owner, newValue);
    }

    public void setParameter(String key, int owner, Object newValue) {
        provider.setParameter(key, owner, newValue);
    }

    public int parameterCount() {
        return provider.parameterCount();
    }

    public FractalProvider.ParameterEntry getParameterByIndex(int position) {
        return provider.getParameterByIndex(position);
    }

    public void addParameterMapListener(FractalProvider.ParameterMapListener l) {
        provider.addParameterMapListener(l);
    }

    public boolean removeParameterMapListener(FractalProvider.ParameterMapListener l) {
        return provider.removeParameterMapListener(l);
    }

    public void addInteractivePoint(String key) {
        for(int fragmentIndex : fragmentIndices) {
            CalculatorFragment fragment = (CalculatorFragment) getChildFragmentManager().findFragmentByTag(fractalCalculatorLabel(fragmentIndex));
            fragment.addInteractivePoint(key);
        }
    }

    public String getSourceByOwner(int owner) {
        if(owner == -1) {
            owner = 0;
        }

        return provider.getFractal(owner).sourceCode();
    }

    private class ImageViewListener implements ScalableImageView.Listener {

        final int fragmentIndex;

        ImageViewListener(int fragmentIndex) {
            this.fragmentIndex = fragmentIndex;
        }

        @Override
        public void scaleRelative(Scale relativeScale) {
            int providerIndex = fragmentIndices.indexOf(fragmentIndex);
            Scale originalScale = ((Scale) provider.getParameter(FractalExternData.SCALE_LABEL, providerIndex));

            Scale absoluteScale = originalScale.relative(relativeScale);
            provider.setParameter(FractalExternData.SCALE_LABEL, providerIndex, absoluteScale);
        }
    }
}
