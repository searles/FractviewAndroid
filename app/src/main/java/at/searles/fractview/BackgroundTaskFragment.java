package at.searles.fractview;

import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

/**
 * This class is used for AsyncTasks that for a short time stall
 * the UI with a progress dialog. Typical example: Something is
 * stored or loaded and it might take a few seconds.
 */
public abstract class BackgroundTaskFragment<Result> extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("BMF", "onCreate");

        super.onCreate(savedInstanceState);
        setRetainInstance(true); // preserve this one on rotation

        launch();
    }

    private void launch() {
        createTask().execute();
    }

    public abstract AsyncTask<Void, Void, Result> createTask();

    public String createProgressDialog(String title, String message) {
        // Create a new dialog fragment that hosts a
        // uncancelable progress dialog.
        return "TODO";
    }

    public void dismissProgressDialog(String id) {
    }
}
