package at.searles.fractview.provider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.ParameterTable;
import at.searles.fractal.data.FractalData;
import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractview.Commons;
import at.searles.fractview.SharedPrefsHelper;
import at.searles.fractview.SourceEditorActivity;
import at.searles.fractview.assets.AssetsHelper;
import at.searles.fractview.editors.ImageSizeDialogFragment;
import at.searles.fractview.favorites.AddFavoritesDialogFragment;
import at.searles.fractview.favorites.FavoritesAccessor;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.main.FractviewActivity;
import at.searles.fractview.parameters.palettes.PaletteActivity;
import at.searles.fractview.provider.view.UISettings;
import at.searles.fractview.saving.EnterFilenameDialogFragment;
import at.searles.fractview.saving.SaveBitmapToMediaFragment;
import at.searles.fractview.saving.SaveInBackgroundFragment;
import at.searles.fractview.saving.SetWallpaperFragment;
import at.searles.fractview.saving.ShareBitmapFragment;
import at.searles.fractview.saving.ShareModeDialogFragment;
import at.searles.fractview.ui.DialogHelper;
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

    // FIXME single resposibility, move view stuff into own class.

    private static final String ADD_FRAGMENT_TAG = "add_fragment";

    private static final int FAVORITES_ICON_SIZE = 64;

    private static final int WIDTH = 1920; // FIXME Change to smaller items
    private static final int HEIGHT = 1080;

    private FractalProvider provider;

    private Queue<FractalData> fractalDataQueue;

    private Map<Integer, CalculatorWrapper> calculatorWrappers;
    private ContainerViewController containerViewController;

    private List<InteractivePoint> interactivePoints; // FIXME move.

    @SuppressLint("UseSparseArrays") // SparseArray does not allow forEach.
    public FractalProviderFragment() {
        this.calculatorWrappers = new HashMap<>();
        this.fractalDataQueue = new LinkedList<>();
        this.containerViewController = new ContainerViewController(this);
        this.interactivePoints = new ArrayList<>(5); // arraylist because they can remove themselves
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        this.provider = new FractalProvider();
        this.provider.addExclusiveParameter(Fractal.SCALE_LABEL);
        this.provider.addListener(src -> updateInteractivePoints());

        addDefault();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // todo save state of provider and interactive points.
    }

    /**
     * Creates a new CalculatorWrapper and triggers the initialization.
     */
    private void lazyAppendFractal(FractalData fractal) {
        // Use this one to add a fractal
        CalculatorWrapper calculatorWrapper = new CalculatorWrapper(this, getWidth(), getHeight());
        fractalDataQueue.offer(fractal);
        calculatorWrapper.startInitialization();
    }

    /**
     * Callback from wrapper, that it has been initialized.
     */
    @SuppressLint("DefaultLocale")
    void appendInitializedWrapper(CalculatorWrapper wrapper) {
        // this method informs this parent that a new wrapper has been created and is ready to use.
        int id = provider.addFractal(fractalDataQueue.poll());
        Fractal fractal = provider.getFractal(id);

        // set fractal, id and start it.
        wrapper.startRunLoop(id, fractal);

        calculatorWrappers.put(id, wrapper);
        containerViewController.addWrapper(wrapper);

        // always switch to the new one.
        containerViewController.updateSelection();
    }

    /**
     * Adds the default fractal.
     */
    public void addDefault() {
        FractalData defaultFractal = AssetsHelper.defaultFractal(getActivity());
        lazyAppendFractal(defaultFractal);
    }

    /**
     * Clones the key fractal
     */
    public void addFromSelected() {
        Fractal src = provider.getFractal(provider.keyId());
        lazyAppendFractal(src.data());
    }

    /**
     * Adds a new fractal based on the current key fractal
     * @param exclusiveParameterKey Modifies this parameter compared to key fractal
     * @param newValue to this value
     */
    public void addFromSelected(String exclusiveParameterKey, Object newValue) {
        Fractal src = provider.getFractal(provider.keyId());

        FractalData newData = src.data().copySetParameter(exclusiveParameterKey, newValue);

        addExclusiveParameter(exclusiveParameterKey);
        lazyAppendFractal(newData);
    }

    public void removeSelected() {
        if(fractalCount() <= 0) {
            // ignore
            DialogHelper.error(getContext(), "Nothing to remove...");
            return;
        }

        int id = provider.keyId();

        interactivePoints.removeIf(pt -> pt.id() == id);

        CalculatorWrapper calculatorWrapper = calculatorWrappers.get(id);
        calculatorWrapper.dispose();
        calculatorWrappers.remove(id);

        containerViewController.remove(id);

        this.provider.removeFractal(id);

        if(provider.fractalCount() > 0) {
            containerViewController.updateSelection();
        }
    }

    /**
     * Adds the default fractal.
     */
    public void setFractal(FractalData fractalData) {
        provider.setFractal(provider.keyId(), fractalData);
    }

    // ========= Handle exclusive parameter ids. ==============

    public void addExclusiveParameter(String id) {
        provider.addExclusiveParameter(id);
    }

    public boolean isSharedParameter(String id) {
        return provider.isSharedParameter(id);
    }

    public void removeExclusiveParameter(String id) {
        provider.removeExclusiveParameter(id);
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
        View view = containerViewController.onCreateView(inflater, container, savedInstanceState, calculatorWrappers);

        if(fractalCount() > 0) {
            containerViewController.updateSelection();
        }

        return view;
    }

    @Override
    public void onDestroyView() {
        containerViewController.onDestroyView();

        for(CalculatorWrapper wrapper : calculatorWrappers.values()) {
            wrapper.destroyView();
        }

        super.onDestroyView();
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

    public void setParameterValue(String key, int owner, Object newValue) {
        provider.setParameterValue(key, owner, newValue);
        updateInteractivePoints();
    }

    public int parameterCount() {
        return provider.parameterCount();
    }

    public ParameterTable.Entry getParameterEntryByIndex(int position) {
        return provider.getParameterEntryByIndex(position);
    }

    public void addListener(FractalProvider.Listener l) {
        provider.addListener(l);
    }

    public void removeListener(FractalProvider.Listener l) {
        provider.removeListener(l);
    }

    public Bitmap getBitmap() {
        return getBitmap(provider.keyId());
    }

    public String getSourceByOwner(int owner) {
        if(owner == -1) {
            owner = 0;
        }

        return provider.getFractal(owner).source();
    }

    public Object getParameterValue(String key, int index) {
        return provider.getParameterValue(key, index);
    }

    public Fractal getFractal(int id) {
        return provider.getFractal(id);
    }

    public Bitmap getBitmap(int index) {
        return calculatorWrappers.get(index).bitmap();
    }

    public int fractalCount() {
        return provider.fractalCount();
    }

    // FIXME From here, move to external classes

    public void showChangeSizeDialog() {
        Bitmap bitmap = calculatorWrappers.get(provider.keyId()).bitmap();

		ImageSizeDialogFragment fragment = ImageSizeDialogFragment.newInstance(bitmap.getWidth(), bitmap.getHeight());
		fragment.show(getChildFragmentManager(), ImageSizeDialogFragment.TAG);
    }

    public void showAddToFavoritesDialog() {
        AddFavoritesDialogFragment fragment = AddFavoritesDialogFragment.newInstance(provider.keyId());
        fragment.show(getChildFragmentManager(), ADD_FRAGMENT_TAG);
    }

    public void addToFavorites(String name) {
        FractalData fractal = getFractal(provider.keyId()).data();
        Bitmap icon = Commons.createIcon(getBitmap(provider.keyId()), FAVORITES_ICON_SIZE);

        // create icon out of bitmap
        byte[] iconData = Commons.toPNG(icon);

        FavoriteEntry fav = new FavoriteEntry(iconData, fractal, Commons.fancyTimestamp());

        SharedPrefsHelper.putWithUniqueKey(getContext(), name, fav, FavoritesAccessor.FAVORITES_SHARED_PREF);
    }

    // ===== Save bitmap =====

    public void showShareOrSaveDialog() {
        ShareModeDialogFragment shareModeDialogFragment = ShareModeDialogFragment.newInstance();
        shareModeDialogFragment.show(getChildFragmentManager(), ShareModeDialogFragment.TAG);
    }

    public void setBitmapAsWallpaper() {
        int wallpaperPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.SET_WALLPAPER);

        if(wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.SET_WALLPAPER)) {
                DialogHelper.info(getContext(), "Without permission to set the wallpaper, " +
                        "the wallpaper cannot be changed.");
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{
                                Manifest.permission.SET_WALLPAPER
                        }, FractviewActivity.WALLPAPER_PERMISSIONS);
            }
            return;
        }

        SetWallpaperFragment fragment = SetWallpaperFragment.newInstance();
        getChildFragmentManager().beginTransaction().add(fragment, SetWallpaperFragment.SAVE_FRAGMENT_TAG).commit();
    }

    public void showSaveBitmapDialog() {
        int wallpaperPermission = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if(wallpaperPermission != PackageManager.PERMISSION_GRANTED) {
            if(ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                DialogHelper.info(getContext(), "Cannot save bitmap to media " +
                        "folder without this permission. Please either grant this permission " +
                        "or use \"Share Bitmap\" instead.");
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                        }, FractviewActivity.SAVE_TO_MEDIA_PERMISSIONS);
            }
            return;
        }

        EnterFilenameDialogFragment fragment = EnterFilenameDialogFragment.newInstance();
        fragment.show(getChildFragmentManager(), EnterFilenameDialogFragment.TAG);
    }

    public void saveBitmap(String filePrefix) {
        SaveBitmapToMediaFragment fragment = SaveBitmapToMediaFragment.newInstance(filePrefix);
        getChildFragmentManager().beginTransaction().add(fragment, SaveBitmapToMediaFragment.SAVE_FRAGMENT_TAG).commit();
    }

    public void shareBitmap() {
        ShareBitmapFragment fragment = ShareBitmapFragment.newInstance();
        getChildFragmentManager().beginTransaction().add(fragment, ShareBitmapFragment.SAVE_FRAGMENT_TAG).commit();
    }

    public boolean setBitmapSizeInKeyFractal(int width, int height) {
        CalculatorWrapper wrapper = calculatorWrappers.get(provider.keyId());
        return wrapper.setBitmapSize(width, height);
    }

    public void removeSaveJob(SaveInBackgroundFragment.SaveJob job) {
        // if someone cancels a wait-for-save dialog
        calculatorWrappers.get(provider.keyId()).removeSaveJob(job);
    }

    public void addSaveJob(SaveInBackgroundFragment.SaveJob job) {
        calculatorWrappers.get(provider.keyId()).addSaveJob(job);
    }

    // ===== History

    public void onBackPressed() {
        // first, check whether any view currently does any editing.
        for(CalculatorWrapper wrapper : calculatorWrappers.values()) {
            if(wrapper.cancelViewEditing()) {
                return;
            }
        }

        // second, undo history in key fractal
        historyBack();
    }

    public void historyBack() {
        if(!provider.historyBack(provider.keyId())) {
            DialogHelper.error(getContext(), "History is empty");
        }
    }

    public void historyForward() {
        if(!provider.historyForward(provider.keyId())) {
            DialogHelper.error(getContext(), "No further items in history");
        }
    }

    public void mergeFractalsFromKey() {
        // FIXME fetch parameters from key fractal and merge them, starting with source code
    }

    public UISettings getUISettings() {
        // FIXME Maintain ui settings.
        // TODO Group things into classes.
        return null;
    }

    // ========== Interactive points ==========
    // FIXME move this!
    public void addInteractivePoint(String key, int id) {
        interactivePoints.add(new InteractivePoint(this, key, id));
        updateInteractivePoints();
        containerViewController.invalidate();
    }

    public boolean isInteractivePoint(String key, int id) {
        for(InteractivePoint pt : interactivePoints) {
            if(pt.is(key, id)) {
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

                calculatorWrappers.values().forEach(CalculatorWrapper::updateInteractivePointsInView);

                return;
            }
        }
    }

    public Iterable<InteractivePoint> interactivePoints() {
        return interactivePoints;
    }

    private void updateInteractivePoints() {
        interactivePoints.removeIf(pt -> !pt.update());

        Iterator<InteractivePoint> it = interactivePoints.iterator();

        for(int index = 0; it.hasNext(); index++) {
            // FIXME keep color fixed.
            it.next().setColorFromWheel(index, interactivePoints.size());
        }

        calculatorWrappers.values().forEach(CalculatorWrapper::updateInteractivePointsInView);
    }

    public FractalData getSelected() {
        return provider.getFractal(provider.keyId()).data();
    }

    public int selectedId() {
        return provider.keyId();
    }

    public void setSelectedId(int selectedId) {
        this.provider.setKeyId(selectedId);
    }
}
