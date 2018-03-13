package at.searles.fractview.saving;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.WallpaperManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import at.searles.fractview.Commons;
import at.searles.fractview.bitmap.BitmapFragment;
import at.searles.fractview.bitmap.IdleJob;
import at.searles.fractview.ui.DialogHelper;

/**
 * This fragment saves bitmaps. It waits until the corresponding
 * bitmap fragment has finished.
 */

public class SaveFragment extends Fragment {

    // once the fragment is created there is
    // * After the selection create idle-job
    // * schedule non-interrupting idle-job that performs save-op.
    // * if bitmap fragment is running show skip/cancel
    //   + skip: start asynctask in idle-job.
    //   + cancel: set cancel flag in idle-job, then run it (actually doing nothing).
    // * once, idle-job starts, hide skip/cancel dialog and show '... saving' dialog
    // done.

    public static SaveFragment newInstance() {
        SaveFragment fragment = new SaveFragment();

        Bundle args = new Bundle();
        fragment.setArguments(args);

        return fragment;
    }

    public SaveFragment() {
    }

    private BitmapFragment getBitmapFragment() {
        // TODO
    }

    private Bitmap getBitmap() {
        return getBitmapFragment().getBitmap();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        IdleJob job = new IdleJob() {
            @Override
            public boolean imageIsModified() {
                // no need to redraw the image if it is saved.
                return false;
            }

            @Override
            public AsyncTask<Void, Void, Void> task() {
                return new SaveTask();
            }
        };

        // get bitmap fragment
        BitmapFragment bitmapFragment = getBitmapFragment();

        if(bitmapFragment.isRunning()) {
            // FIXME Create dialog fragment for skip/cancel

            // If cancel, set cancel flag in job.
        }

        // add job to bitmap fragment, executed before all
        // others, but do not interrupt the execution.
        bitmapFragment.scheduleIdleJob(job, true, false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        switch(stage) {
            case WAITING_FOR_RENDER: {
                // Can cancel and skip
                builder.setTitle("Please wait...");
                builder.setMessage("Image will be saved after rendering is finished. " +
                        "To save immediately, select \"Save Now\".");
                builder.setNeutralButton("Save Now", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performSaveOperation();
                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dispose();
                    }
                });
            } break;
            case SAVING: {
                builder.setTitle("Please wait...");
                builder.setMessage("Saving image");
                builder.setCancelable(false);
            } break;
        }

        simpleDialog = builder.show();


        // create dialog that is currently appropriate
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    private class SaveTask extends AsyncTask<Void, Void, Void> {

        private File imageFile;
        private IOException exception;

        @Override
        protected void onPreExecute() {
            if(isCancelled()) {
                return;
            }

            // TODO hide skip/cancel dialog if it exists.

            // TODO show progress dialog

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

            // TODO hide dialog fragment

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
}
