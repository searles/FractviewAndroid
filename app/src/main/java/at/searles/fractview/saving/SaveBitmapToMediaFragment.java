package at.searles.fractview.saving;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;

import at.searles.fractview.ui.DialogHelper;

public class SaveBitmapToMediaFragment extends SaveInBackgroundFragment {

    private static final String FILENAME_PREFIX_KEY = "filename";

    private File imageFile;
    private IOException exception;

    public static SaveInBackgroundFragment newInstance(String filenamePrefix) {
        SaveInBackgroundFragment fragment = new SaveBitmapToMediaFragment();

        Bundle args = new Bundle();

        args.putString(FILENAME_PREFIX_KEY, filenamePrefix);

        fragment.setArguments(args);

        return fragment;
    }

    private void saveImageFileToMedia() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(imageFile);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    @Override
    protected void prepareSaveInUIThread() {
        String filenamePrefix = getArguments().getString(FILENAME_PREFIX_KEY);
        File directory = EnterFilenameDialogFragment.getMediaDirectory();

        imageFile = new File(directory, filenamePrefix + EnterFilenameDialogFragment.FILE_EXTENSION);
    }

    @Override
    protected void asyncSaveInBackground() {
        try {
            saveImage(imageFile);
        } catch (IOException e) {
            this.exception = e;
        }
    }

    @Override
    protected void postSaveInUIThread() {
        if(exception == null) {
            saveImageFileToMedia();
        } else {
            DialogHelper.error(getActivity(), exception.getMessage());
        }
    }
}
