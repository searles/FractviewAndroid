package at.searles.fractview.saving;

import android.app.DialogFragment;
import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import at.searles.fractview.bitmap.BitmapFragment;
import at.searles.fractview.bitmap.IdleJob;

/**
 * This fragment saves bitmaps. It waits until the corresponding
 * bitmap fragment has finished.
 */

public abstract class SaveInBackgroundFragment extends Fragment {

    public static final String SAVE_FRAGMENT_TAG = "saveFragment";

    protected static final String SKIP_CANCEL_FRAGMENT_TAG = "skipCancelTag";
    protected static final String WAIT_FRAGMENT_TAG = "waiting";


    private SaveJob job;

    public SaveInBackgroundFragment() {
    }

    protected BitmapFragment bitmapFragment() {
        // get bitmap fragment
        return (BitmapFragment) getParentFragment();
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

    protected Bitmap getBitmap() {
        return ((BitmapFragment) getParentFragment()).bitmap();
    }



    protected abstract void prepareSaveInUIThread();

    protected abstract void asyncSaveInBackground();

    protected abstract void postSaveInUIThread();

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

            prepareSaveInUIThread();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if(isCancelled()) {
                return;
            }

            dismissProgressDialogFragment();

            postSaveInUIThread();

            job.onFinished();
        }

        @Override
        protected Void doInBackground(Void... params) {
            if(!isCancelled()) {
                asyncSaveInBackground();
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

    /**
     * Saves the image in the background. Not directly called here, but
     * used in multiple subclasses.
     * @param imageFile File object in which the image should be saved.
     */
    protected void saveImage(File imageFile) throws IOException {
        // Not in UI thread!
        try (FileOutputStream fos = new FileOutputStream(imageFile)) {
            if (!getBitmap().compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                throw new UnsupportedOperationException("compress not supported!");
            }
        }
    }
}
