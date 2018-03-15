package at.searles.fractview.saving;

import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.WallpaperManager;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import at.searles.fractview.bitmap.BitmapFragment;
import at.searles.fractview.bitmap.IdleJob;
import at.searles.fractview.ui.DialogHelper;

/**
 * This fragment saves bitmaps. It waits until the corresponding
 * bitmap fragment has finished.
 */

public class SaveFragment extends Fragment {

    private static final String SAVE_FRAGMENT_TAG = "saveFragment";

    private static final String SKIP_CANCEL_FRAGMENT_TAG = "skipCancelTag";
    private static final String WAIT_FRAGMENT_TAG = "waiting";

    public static SaveFragment registerNewInstanceForParent(BitmapFragment bitmapFragment) {
        FragmentManager fm = bitmapFragment.getChildFragmentManager();

        FragmentTransaction fragmentTransaction = fm.beginTransaction();

        SaveFragment saveFragment = newInstance();
        fragmentTransaction.add(saveFragment, SAVE_FRAGMENT_TAG);

        fragmentTransaction.commit();

        return saveFragment;
    }

    private static SaveFragment newInstance() {
        SaveFragment fragment = new SaveFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    private SaveJob job;

    public SaveFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        job = new SaveJob();

        // get bitmap fragment
        BitmapFragment bitmapFragment = (BitmapFragment) getParentFragment();

        if(bitmapFragment.isRunning()) {
            createSkipCancelDialogFragment();
        }

        // add job to bitmap fragment, executed before all
        // others, but do not interrupt the execution.
        bitmapFragment.scheduleIdleJob(job, true, false);
    }


    private void createSkipCancelDialogFragment() {
        DialogFragment dialogFragment = new SkipCancelDialogFragment();
        dialogFragment.show(getChildFragmentManager(), SKIP_CANCEL_FRAGMENT_TAG);
    }

    private void dismissSkipCancelDialogFragment() {
        DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(SKIP_CANCEL_FRAGMENT_TAG);

        if(dialogFragment != null) {
            dialogFragment.dismiss();
        }

        // TODO: also remove from fragmentManager?
    }

    private void createProgressDialogFragment() {
        DialogFragment dialogFragment = new WaitDialogFragment();
        dialogFragment.show(getChildFragmentManager(), WAIT_FRAGMENT_TAG);
    }

    private void dismissProgressDialogFragment() {
        DialogFragment dialogFragment = (DialogFragment) getChildFragmentManager().findFragmentByTag(WAIT_FRAGMENT_TAG);
        dialogFragment.dismiss();
        // TODO: also remove?
    }

    public void onCancel() {
        job.cancel();
    }

    public void onSkip() {
        job.startJob();
    }

    private Bitmap getBitmap() {
        return ((BitmapFragment) getParentFragment()).getBitmap();
    }

    /**
     * Saves the image in the background. As a special tweak, it displays a dialog to
     * save the file only once the calculation is done. This dialog allows skip or cancel. Skip
     * saves the image instantly, Cancel cancels the whole thing.
     * @param imageFile File object in which the image should be saved.
     */
    private void saveImage(File imageFile) throws IOException {
        // Not in UI thread!
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            if (!getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                throw new UnsupportedOperationException("compress not supported!");
            }
        }
    }

    private void shareImageFile(File file) throws IOException {
        // FIXME better create file in a public place.
        // Share text file
        Uri contentUri = FileProvider.getUriForFile(getActivity(), "at.searles.fractview.fileprovider", file);
        // after it was successfully saved, share it.
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("image/png");
        share.putExtra(Intent.EXTRA_STREAM, contentUri);
        startActivity(Intent.createChooser(share, "Share Image"));
    }

    private void saveImageFileToMedia(File file) {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(file);
        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }

    private void setBitmapAsWallpaper(Bitmap bitmap) throws IOException {
        // set bitmap
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(getActivity());
        wallpaperManager.setBitmap(bitmap);
    }

    private class SaveTask extends AsyncTask<Void, Void, Void> {

        private File imageFile;
        private IOException exception;

        @Override
        protected void onPreExecute() {
            if(isCancelled()) {
                return;
            }

            dismissSkipCancelDialogFragment();

            createProgressDialogFragment();

            try {
                imageFile = File.createTempFile("fractview", ".png", getActivity().getExternalCacheDir());
            } catch (IOException e) {
                DialogHelper.error(getContext(), e.getLocalizedMessage());
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(isCancelled()) {
                return;
            }

            dismissProgressDialogFragment();

            if(exception == null) {
                try {
                    shareImageFile(imageFile);
                } catch (IOException e) {
                    exception = e;
                }
            }

            if(exception != null) {
                DialogHelper.error(getContext(), exception.getLocalizedMessage());
            }

            job.onFinished();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(!isCancelled()) {
                try {
                    saveImage(imageFile);
                } catch (IOException e) {
                    e.printStackTrace();
                    this.exception = e;
                }
            }

            return null;
        }
    }

    public class SaveJob extends IdleJob {

        private boolean isCancelled = false;
        private SaveTask task;

        private SaveJob() {
            super(false);
        }

        public void cancel() {
            this.isCancelled = true;
        }

        protected void onStart() {
            if (!isCancelled) {
                task = new SaveTask();
                task.execute();
            } else {
                onFinished();
            }
        }
    }
}
