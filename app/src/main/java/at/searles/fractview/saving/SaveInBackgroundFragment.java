package at.searles.fractview.saving;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;

import at.searles.fractview.bitmap.BitmapFragment;
import at.searles.fractview.bitmap.IdleJob;

/**
 * This fragment saves bitmaps. It waits until the corresponding
 * bitmap fragment has finished.
 */

public abstract class SaveInBackgroundFragment extends Fragment {

    private enum Status { Waiting, Saving, Done }

    public static final String SAVE_FRAGMENT_TAG = "saveFragment";

    private SaveJob job;
    private Dialog dialog;
    private Status status;

    public SaveInBackgroundFragment() {
        this.dialog = null;
        this.status = Status.Waiting;
    }

    protected BitmapFragment bitmapFragment() {
        // get bitmap fragment
        return (BitmapFragment) getParentFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        // We do not save the state because if the app is terminated
        // then there is no bitmap left. The rendering will restart
        // and the Skip/Cancel-dialog will show.

        job = new SaveJob();

        // get bitmap fragment
        BitmapFragment bitmapFragment = (BitmapFragment) getParentFragment();

        // add job to bitmap fragment, executed before all
        // others, but do not interrupt the execution.
        bitmapFragment.scheduleIdleJob(job, true, false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        // show dialog if necessary
        switch(this.status) {
            case Waiting:
                this.dialog = createSkipCancelDialog();
                break;
            case Saving:
                this.dialog = createProgressDialog();
                break;
            default:
                this.dialog = null;
        }

        if(this.dialog != null) {
            this.dialog.show();
        }

        return null;
    }

    @Override
    public void onDestroyView() {
        // remove dialog if it exists
        super.onDestroyView();
        if (dialog != null) {
            this.dialog.dismiss();
            this.dialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if(this.status == Status.Saving) {
            // special case: The async saving task might already save the image.
            // Therefore, do not recreate the fragment.
            this.status = Status.Done;
            deleteFragmentFromParent();
        }
    }

    private void deleteFragmentFromParent() {
        bitmapFragment().getChildFragmentManager().beginTransaction().remove(this).commit();
    }

    private void terminate() {
        this.status = Status.Done;
        deleteFragmentFromParent();

        if(dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
    }

    private void switchWaitingToSaving() {
        if(this.status != Status.Waiting) {
            throw new IllegalArgumentException("status is " + status + " but it should be 'Waiting'");
        }

        this.status = Status.Saving;

        if(this.dialog != null) {
            this.dialog.dismiss();
            this.dialog = null;

            this.dialog = createProgressDialog();
            this.dialog.show();
        }
    }

    protected Bitmap getBitmap() {
        return ((BitmapFragment) getParentFragment()).bitmap();
    }

    protected abstract void prepareSaveInUIThread();

    protected abstract void asyncSaveInBackground();

    protected abstract void postSaveInUIThread();

    private static class SaveTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<SaveInBackgroundFragment> parent;

        SaveTask(SaveInBackgroundFragment parent) {
            this.parent = new WeakReference<>(parent);
        }

        @Override
        protected void onPreExecute() {
            SaveInBackgroundFragment saveInBackgroundFragment = parent.get();

            if(saveInBackgroundFragment != null) {
                saveInBackgroundFragment.switchWaitingToSaving();
                saveInBackgroundFragment.prepareSaveInUIThread();
            }
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            SaveInBackgroundFragment saveInBackgroundFragment = parent.get();

            if(saveInBackgroundFragment != null) {
                saveInBackgroundFragment.postSaveInUIThread();
                saveInBackgroundFragment.deleteFragmentFromParent();
                saveInBackgroundFragment.job.onFinished();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            SaveInBackgroundFragment saveInBackgroundFragment = parent.get();

            if(saveInBackgroundFragment != null) {
                parent.get().asyncSaveInBackground();
            }

            return null;
        }
    }

    public class SaveJob extends IdleJob {

        private SaveTask task;

        private SaveJob() {
            super(false);
        }

        protected void onStart() {
            task = new SaveTask(SaveInBackgroundFragment.this);
            task.execute();
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

    private void onCancel() {
        bitmapFragment().removeIdleJob(job);
        terminate();
    }

    private void onSkip() {
        bitmapFragment().removeIdleJob(job);
        job.startJob();
    }

    private AlertDialog createSkipCancelDialog() {
        android.app.AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setTitle("Rendering not finished...");
        builder.setMessage("The image will be saved when the rendering is finished.");

        builder.setNeutralButton("Skip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onSkip();
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                onCancel();
            }
        });

        return builder.create();
    }

    public ProgressDialog createProgressDialog() {
        ProgressDialog dialog = new ProgressDialog(getActivity());

        dialog.setTitle("Please wait...");
        dialog.setMessage("Saving image...");
        dialog.setIndeterminate(true);

        return dialog;
    }
}
