package at.searles.fractview.main;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.R;
import at.searles.fractview.SourceEditorActivity;
import at.searles.fractview.assets.AssetsHelper;
import at.searles.fractview.favorites.AddFavoritesDialogFragment;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.parameters.palettes.PaletteActivity;
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

    private static final String ADD_FRAGMENT_TAG = "add_fragment";

    private static final int WIDTH = 1920; // FIXME Change to smaller items
    private static final int HEIGHT = 1080;

    private FractalProvider provider;
    private Queue<FractalData> fractalDataQueue;

    private List<CalculatorWrapper> calculatorWrappers;
    private List<RadioButton> selectorButtons;

    private List<InteractivePoint> interactivePoints;
    private LinearLayout containerView;

    public FractalProviderFragment() {
        this.selectorButtons = new ArrayList<>(2);
        this.calculatorWrappers = new ArrayList<>(2);
        this.fractalDataQueue = new LinkedList<>();
        this.interactivePoints = new ArrayList<>(5); // arraylist because they can remove themselves
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        this.provider = new FractalProvider();

        this.provider.addExclusiveParameter(Fractal.SCALE_LABEL);

        this.provider.addListener(src -> updateInteractivePoints());

        FractalData defaultFractal = AssetsHelper.defaultFractal(getActivity());

        lazyAppendFractal(defaultFractal);
    }

    public void addFractalFromKey(String exclusiveParameterId, Object newValue) {
        Fractal src = provider.getFractal(provider.keyIndex());

        FractalData originalData = src.data();
        ParameterType type = src.getParameter(exclusiveParameterId).type;
        FractalData newData = originalData.newSetParameter(exclusiveParameterId, type, newValue);

        addExclusiveParameter(exclusiveParameterId);

        lazyAppendFractal(newData);
    }

    public void addFractalFromKey() {
        Fractal src = provider.getFractal(provider.keyIndex());
        lazyAppendFractal(src.data());
    }

    private void lazyAppendFractal(FractalData fractal) {
        // Use this one to add a fractal
        CalculatorWrapper calculatorWrapper = new CalculatorWrapper(this, getWidth(), getHeight());
        fractalDataQueue.offer(fractal);
        calculatorWrapper.startInitialization();
    }

    @SuppressLint("DefaultLocale")
    void appendInitializedWrapper(CalculatorWrapper wrapper) {
        // this method informs this parent that a new wrapper has been created and is ready to use.
        int index = provider.addFractal(fractalDataQueue.poll());

        calculatorWrappers.add(wrapper);
        wrapper.setIndex(index);

        wrapper.startRunLoop(provider.getFractal(index));

        if(containerView != null) {
            addView(index,  wrapper);
        }
    }

    public void removeFractalFromKey() {
        if(fractalCount() < 2) {
            throw new IllegalArgumentException("cannot remove it there are no fractals left");
        }

        int removedIndex = this.provider.keyIndex();

        CalculatorWrapper removedWrapper = this.calculatorWrappers.remove(removedIndex);
        removedWrapper.dispose();

        if(containerView != null) {
            removeView(removedIndex);
        }

        for(int i = removedIndex; i < calculatorWrappers.size(); ++i) {
            // update indices
            calculatorWrappers.get(i).setIndex(i);
        }

        // update points
        interactivePoints.removeIf(pt -> pt.owner == removedIndex);
        interactivePoints.forEach(pt -> { if(pt.owner > removedIndex) pt.owner--; });

        if(this.provider.removeFractal() != removedIndex) {
            throw new IllegalArgumentException();
        }
    }

    // ========= Handle exclusive parameter ids. ==============

    public void addExclusiveParameter(String id) {
        provider.addExclusiveParameter(id);
    }

    public boolean isExclusiveParameter(String id) {
        return provider.isExclusiveParameter(id);
    }

    public void removeExclusiveParameter(String id) {
        provider.removeExclusiveParameter(id);
    }

    // ============= Interactive Points ================

    public void addInteractivePoint(String id, int owner) {
        interactivePoints.add(new InteractivePoint(this, id, owner));
        containerView.invalidate();
        calculatorWrappers.forEach(CalculatorWrapper::updateInteractivePointsInView);
    }

    public boolean isInteractivePoint(String id, int owner) {
        for(InteractivePoint pt : interactivePoints) {
            if(pt.is(id, owner)) {
                return true;
            }
        }

        return false;
    }

    public void removeInteractivePoint(String id, int owner) {
        for(Iterator<InteractivePoint> it = interactivePoints.iterator(); it.hasNext(); ) {
            InteractivePoint pt = it.next();

            if(pt.is(id, owner)) {
                it.remove();

                calculatorWrappers.forEach(CalculatorWrapper::updateInteractivePointsInView);

                return;
            }
        }
    }

    public Iterable<InteractivePoint> interactivePoints() {
        return interactivePoints;
    }

    // ===============================

    private int getWidth() {
        return WIDTH; // FIXME
    }

    private int getHeight() {
        return HEIGHT; // FIXME
    }

    // ========== Dealing with View =============

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        containerView = (LinearLayout) inflater.inflate(R.layout.fractalview_layout, container);

        for(int i = 0; i < calculatorWrappers.size(); ++i) {
            CalculatorWrapper wrapper = calculatorWrappers.get(i);
            addView(i, wrapper);
        }

        return this.containerView;
    }

    @Override
    public void onDestroyView() {
        for(int i = provider.fractalCount() - 1; i >= 0; --i) {
            calculatorWrappers.get(i).destroyView();
        }

        this.containerView = null;
        this.selectorButtons.clear();

        super.onDestroyView();
    }

    private void addView(int index, CalculatorWrapper wrapper) {
        RadioButton selectorButton = new RadioButton(getContext());
        selectorButtons.add(selectorButton);

        selectorButton.setOnClickListener(src -> {
            selectorButtons.get(provider.keyIndex()).setChecked(false);
            provider.setKeyIndex(wrapper.index());
            selectorButton.setChecked(true);
        });

        containerView.addView(selectorButton, index * 2);
        containerView.addView(wrapper.createView(), index * 2 + 1);

        updateKeySelection();
    }

    private void removeView(int removedIndex) {
        containerView.removeViewAt(removedIndex * 2 + 1);
        containerView.removeViewAt(removedIndex * 2);

        selectorButtons.remove(removedIndex);

        // update selection
        updateKeySelection();
    }

    public void updateKeySelection() {
        // fixme can be seriously improved!
        for(int i = selectorButtons.size(); i --> 0;) { // well, that is fun...
            selectorButtons.get(i).setText("View " + i); // FIXME
            selectorButtons.get(i).setChecked(i == provider.keyIndex());
        }
    }

    // =================================================

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case PaletteActivity.PALETTE_ACTIVITY_RETURN:
                if(resultCode == PaletteActivity.OK_RESULT) {
                    String id = data.getStringExtra(PaletteActivity.ID_LABEL);
                    int owner = data.getIntExtra(PaletteActivity.OWNER_LABEL, -1);

                    Palette palette = BundleAdapter.paletteFromBundle(data.getBundleExtra(PaletteActivity.PALETTE_LABEL));
                    provider.setParameterValue(id, owner, palette);
                }
                return;
            case SourceEditorActivity.SOURCE_EDITOR_ACTIVITY_RETURN:
                if(resultCode == PaletteActivity.OK_RESULT) {
                    int owner = data.getIntExtra(SourceEditorActivity.OWNER_LABEL, -1);

                    String source = data.getStringExtra(SourceEditorActivity.SOURCE_LABEL);
                    provider.setParameterValue(Fractal.SOURCE_LABEL, owner, source);
                }
                return;
            default:
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateInteractivePoints() {
        interactivePoints.removeIf(pt -> !pt.update());

        calculatorWrappers.forEach(CalculatorWrapper::updateInteractivePointsInView);
    }

    public void setParameterValue(String key, int owner, Object newValue) {
        provider.setParameterValue(key, owner, newValue);
        updateInteractivePoints();
    }

    public int parameterCount() {
        return provider.parameterCount();
    }

    public FractalProvider.ParameterEntry getParameterEntryByIndex(int position) {
        return provider.getParameterEntryByIndex(position);
    }

    public void addListener(FractalProvider.Listener l) {
        provider.addListener(l);
    }

    public boolean removeListener(FractalProvider.Listener l) {
        return provider.removeListener(l);
    }

    public String getSourceByOwner(int owner) {
        if(owner == -1) {
            owner = 0;
        }

        return provider.getFractal(owner).source();
    }

    public void addToFavorites() {
        AddFavoritesDialogFragment fragment = AddFavoritesDialogFragment.newInstance(provider.keyIndex());
        fragment.show(getChildFragmentManager(), ADD_FRAGMENT_TAG);
    }


    void removeInteractivePoint(InteractivePoint pt) {
        interactivePoints.remove(pt);
    }

    public FractalData getKeyFractal() {
        return provider.getFractal(0).data();
    }

    public Object getParameterValue(String key, int index) {
        return provider.getParameterValue(key, index);
    }

    public Fractal getFractal(int index) {
        return provider.getFractal(index);
    }

    public Bitmap getBitmap(int index) {
        return calculatorWrappers.get(index).bitmap();
    }

    public int fractalCount() {
        return provider.fractalCount();
    }

    public void setKeyFractal(FractalData newFractal) {
        this.provider.setKeyFractal(newFractal);
    }

    public boolean parameterExists(String id, int owner) {
        if(owner != -1) {
            return provider.getFractal(owner).getParameter(id) != null;
        } else {
            return provider.getParameterValue(id, owner) != null;
        }
    }
}
