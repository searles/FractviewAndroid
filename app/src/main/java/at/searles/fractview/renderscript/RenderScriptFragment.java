package at.searles.fractview.renderscript;


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
import java.util.LinkedList;

import at.searles.fractview.ScriptC_fillimage;
import at.searles.fractview.ScriptC_fractal;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers
 */
public class RenderScriptFragment extends Fragment {

    private RenderScript rs;

    private ScriptC_fillimage fillScript;
    private ScriptC_fractal script;

    /**
     * This flag is only modified in the UI thread.
     */
    private boolean isInitializing;
    private LinkedList<RenderScriptListener> listeners;
    private ProgressDialog progressDialog;

    public RenderScriptFragment() {
        this.isInitializing = true;
        this.listeners = new LinkedList<>();
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment RenderScriptFragment.
     */
    public static RenderScriptFragment newInstance() {
        return new RenderScriptFragment();
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

        private WeakReference<RenderScriptFragment> parent;

        InitializeTask(RenderScriptFragment parent) {
            this.parent = new WeakReference<>(parent);
        }

        @Override
        protected Void doInBackground(Void...ignored) {
            RenderScriptFragment renderScriptFragment = parent.get();

            if(renderScriptFragment != null) {
                renderScriptFragment.initScripts();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void ignored) {
            RenderScriptFragment renderScriptFragment = parent.get();

            if(renderScriptFragment != null) {
                renderScriptFragment.isInitializing = false;
                renderScriptFragment.dismissInitDialog();
                renderScriptFragment.fireInitializationFinishedEvent();
            }
        }
    }

    private void fireInitializationFinishedEvent() {
        // Tell others
        for(RenderScriptListener listener : listeners) {
            listener.rsInitializationFinished(RenderScriptFragment.this);
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

    public boolean isInitializing() {
        return isInitializing;
    }

    public void addListener(RenderScriptListener listener) {
        this.listeners.add(listener);
    }
}
