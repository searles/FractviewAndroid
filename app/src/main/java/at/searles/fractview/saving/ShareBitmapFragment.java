package at.searles.fractview.saving;

import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.IOException;

import at.searles.fractview.ui.DialogHelper;

public class ShareBitmapFragment extends SaveInBackgroundFragment {

    private File imageFile;
    private IOException exception;

    public static ShareBitmapFragment newInstance() {
        return new ShareBitmapFragment();
    }

    @Override
    protected void prepareSaveInUIThread() {
        try {
            imageFile = File.createTempFile("fractview", ".png", getActivity().getExternalCacheDir());
        } catch (IOException e) {
            DialogHelper.error(getActivity(), e.getLocalizedMessage());
        }
    }

    @Override
    protected void asyncSaveInBackground() {
        try {
            saveImage(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
            this.exception = e;
        }
    }

    @Override
    protected void postSaveInUIThread() {
        if(exception == null) {
            try {
                shareImageFile(imageFile);
            } catch (IOException e) {
                exception = e;
            }
        }

        if(exception != null) {
            DialogHelper.error(getActivity(), exception.getLocalizedMessage());
        }
    }

    private void shareImageFile(File file) throws IOException {
        // Share text file
        Uri contentUri = FileProvider.getUriForFile(getActivity(), "at.searles.fractview.fileprovider", file);
        // after it was successfully saved, share it.
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(share, "Share Image")); // FIXME
    }
}
