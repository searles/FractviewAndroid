package at.searles.fractview.saving;

import android.app.WallpaperManager;
import android.os.Bundle;

import java.io.IOException;

import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.ui.DialogHelper;

public class SetWallpaperFragment extends SaveInBackgroundFragment {

    private WallpaperManager wallpaperManager;
    private IOException exception;

    public static SetWallpaperFragment newInstance() {
        SetWallpaperFragment fragment = new SetWallpaperFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void prepareSaveInUIThread() {
        this.wallpaperManager = WallpaperManager.getInstance(getActivity());
    }

    @Override
    protected void asyncSaveInBackground() {
        try {
            wallpaperManager.setBitmap(((FractalProviderFragment) getParentFragment()).getBitmap());
        } catch (IOException e) {
            this.exception = e;
        }
    }

    @Override
    protected void postSaveInUIThread() {
        if(exception != null) {
            DialogHelper.error(getActivity(), exception.getLocalizedMessage());
        }
    }
}
