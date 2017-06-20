package at.searles.fractview;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by searles on 18.06.17.
 */

public class SavePlugin implements BitmapFragmentPlugin, BitmapFragment.BitmapFragmentListener {

    private static final int WAITING_FOR_RENDER = 0;
    private static final int SAVING = 1;

    private interface Action {
        void apply(BitmapFragment fragment);
    }

    public static SavePlugin createShare() {
        return new SavePlugin((fragment) -> {
            try {
                File imageFile = File.createTempFile("fractview", ".png", fragment.getActivity().getExternalCacheDir());
                SavePlugin.saveImage(imageFile, fragment);

                Commons.uiRun(() -> {
                    // Share image
                    Uri contentUri = Uri.fromFile(imageFile);
                    // after it was successfully saved, share it.
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/png");
                    share.putExtra(Intent.EXTRA_STREAM, contentUri);
                    fragment.startActivity(Intent.createChooser(share, "Share Image"));
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static SavePlugin createSave(File file) {
        return new SavePlugin((fragment) -> {
            SavePlugin.saveImage(file, fragment);
            // this is executed after saving was successful
            // Add it to the gallery
            Commons.uiRun(() -> {
                Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri contentUri = Uri.fromFile(file);
                mediaScanIntent.setData(contentUri);
                fragment.getActivity().sendBroadcast(mediaScanIntent);
            });
        });
    }

    public static SavePlugin createSetWallpaper() {
        return new SavePlugin((fragment) -> {
            // set bitmap
            WallpaperManager wallpaperManager = WallpaperManager.getInstance(fragment.getActivity());
            try {
                wallpaperManager.setBitmap(fragment.getBitmap());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private BitmapFragment fragment;
    private final Action action;

    private SavePlugin(Action action) {
        Log.d(getClass().getName(), "constructor");

        this.action = action;
    }

    @Override
    public void init(BitmapFragment fragment) {
        Log.d(getClass().getName(), "init");

        if(this.fragment != null) {
            Log.e(getClass().getName(), "fragment is not null!");
        }

        this.fragment = fragment;

        fragment.addBitmapFragmentPlugin(this);

        if(fragment.isRunning()) {
            stage = WAITING_FOR_RENDER;
        } else {
            stage = SAVING;
        }

        if(fragment.getActivity() != null) {
            attachContext(fragment.getActivity());
        }

        if(fragment.isRunning()) {
            // Saving will be called from 'finished'
            fragment.addBitmapFragmentListener(this);
        } else {
            performSaveOperation();
        }

        /*View waitView = view.setBusyView();

        Button cancelButton = (Button) waitView.findViewById(R.id.cancelButton);
        Button skipButton = (Button) waitView.findViewById(R.id.skipButton);

        cancelButton.setOnClickListener((l) -> dispose());
        skipButton.setOnClickListener((l) -> performSaveOperation(fragment.getBitmap()));
    */}

    private AlertDialog simpleDialog = null;
    private int stage = -1;

    private void createDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        switch(stage) {
            case WAITING_FOR_RENDER: {
                // Can cancel and skip
                builder.setTitle("Rendering not finished");
                builder.setMessage("Image will be saved after rendering is finished.");
                builder.setNeutralButton("Save Now", (dialog, which) -> performSaveOperation());
                builder.setNegativeButton("Cancel", (dialog, which) -> dispose());
            } break;
            case SAVING: {
                builder.setTitle("Saving image");
                builder.setMessage("Please wait...");
                builder.setCancelable(false);
            } break;
        }

        simpleDialog = builder.show();
    }

    @Override
    public void attachContext(Activity context) {
        Log.d(getClass().getName(), "attachContext");
        createDialog(context);
    }

    @Override
    public void detach() {
        simpleDialog = null;
    }

        // for task logic
    private void dispose() {
        Log.d(getClass().getName(), "dispose");

        // the following might fail because rendering
        // has already finished
        fragment.removeBitmapFragmentListener(this);

        fragment.removeBitmapFragmentPlugin(this);

        if(simpleDialog != null) {
            Log.d(getClass().getName(), "dismissing dialog");
            simpleDialog.dismiss();
        }
    }

    private void performSaveOperation() {
        Log.d(getClass().getName(), "performSaveOperation");

        if(stage == WAITING_FOR_RENDER) {
            Log.d(getClass().getName(), "switching stages");

            stage = SAVING;

            // there was a waiting stage and thus an old dialog.
            // maybe unless we are in a funny moment
            // where the activity is just recreated...
            if(simpleDialog != null) {
                Context context = simpleDialog.getContext();
                simpleDialog.dismiss();
                createDialog(context);
            }
        } else {
            Log.d(getClass().getName(), "not switching stages");
        }

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                action.apply(fragment);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                dispose(); // remove all listeners
            }
        }.execute();
    }

    // ==================================================================
    // ================= Save/Share/Set as Wallpaper ====================
    // ==================================================================

    /**
     * Saves the image in the background. As a special tweak, it displays a dialog to
     * save the file only once the calculation is done. This dialog allows skip or cancel. Skip
     * saves the image instantly, Cancel cancels the whole thing.
     * @param imageFile File object in which the image should be saved.
     */
    private static void saveImage(final File imageFile, BitmapFragment fragment) {
        // Not in UI thread!
        Bitmap bitmap = fragment.getBitmap();

        String errorMsg;
        try {
            FileOutputStream fos = new FileOutputStream(imageFile);

            if (bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                // Successfully written picture
                fos.close();
                return;
            } else {
                errorMsg = "Error calling \"compress\".";
                fos.close();
            }
        } catch(IOException e) {
            errorMsg = e.getLocalizedMessage();
        }

        String finalErrorMsg = errorMsg;
        Commons.uiRun(() -> DialogHelper.error(fragment.getActivity(), finalErrorMsg));
    }



    // from BitmapFragmentListener
    @Override
    public void drawerFinished(long ms, BitmapFragment src) {
        if(fragment != src) {
            Log.e(getClass().getName(), "fragment is not src in drawerFinished!");
        }

        performSaveOperation();
    }

    @Override
    public void initializationFinished() {}

    @Override
    public void bitmapUpdated(BitmapFragment src) {}

    @Override
    public void previewGenerated(BitmapFragment src) {}

    @Override
    public void drawerStarted(BitmapFragment src) {}

    @Override
    public void newBitmapCreated(Bitmap bitmap, BitmapFragment src) {}
}
