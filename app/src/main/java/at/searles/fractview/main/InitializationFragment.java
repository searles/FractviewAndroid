package at.searles.fractview.main;


import android.app.Fragment;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.lang.ref.WeakReference;

import at.searles.fractview.ScriptC_fillimage;
import at.searles.fractview.ScriptC_fractal;
import at.searles.fractview.renderscript.RenderScriptDrawer;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers
 */
public class InitializationFragment extends Fragment {

    public static final String TAG = "rsFragment";
    private RenderScript rs;

    private ScriptC_fillimage fillScript;
    private ScriptC_fractal script;

    /**
     * This flag is only modified in the UI thread.
     */
    private boolean isInitializing;
    private ProgressDialog progressDialog;

    public InitializationFragment() {
        this.isInitializing = true;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment InitializationFragment.
     */
    public static InitializationFragment newInstance() {
        return new InitializationFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setRetainInstance(true);

        if(this.rs != null) {
            // nothing to do anymore.
            return;
        }

        // initialize renderscript
        this.rs = RenderScript.create(getActivity());

        launchAsyncInitialize();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        if(isInitializing) {
            this.progressDialog = createInitDialog();
            progressDialog.show();
        }

        return null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (this.progressDialog != null) {
            dismissInitDialog();
        }
    }

    public ProgressDialog createInitDialog() {
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Please wait while scripts are initialized.");
        progressDialog.setCancelable(false);
        return progressDialog;
    }

    private void dismissInitDialog() {
        if(this.progressDialog != null) {
            this.progressDialog.dismiss();
        }

        this.progressDialog = null;
    }

    private void launchAsyncInitialize() {
        // async run initScripts
        new InitializeTask(this).execute();
    }

    private static class InitializeTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<InitializationFragment> parent;

        InitializeTask(InitializationFragment parent) {
            this.parent = new WeakReference<>(parent);
        }

        @Override
        protected Void doInBackground(Void...ignored) {
            InitializationFragment initializationFragment = parent.get();

            if(initializationFragment != null) {
                initializationFragment.initScripts();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void ignored) {
            InitializationFragment initializationFragment = parent.get();

            if(initializationFragment != null) {
                initializationFragment.isInitializing = false;
                initializationFragment.dismissInitDialog();
                initializationFragment.initializationFinished();
            }
        }
    }

    private void initializationFinished() {
        FractalFragment parent = (FractalFragment) getParentFragment();

        if(parent != null) {
            parent.createCalculators(this);
        }
    }

    private void initScripts() {
        // the following might take some time
        // because it invokes the renderscript
        // compiler
        script = new ScriptC_fractal(rs);
        script.set_gScript(script);
        fillScript = new ScriptC_fillimage(rs);
        fillScript.set_gScript(fillScript);
    }

    public RenderScriptDrawer createDrawer() {
        if(isInitializing) {
            throw new IllegalArgumentException("cannot create drawer while initializing");
        }

        return new RenderScriptDrawer(rs, script, fillScript);
    }
}
