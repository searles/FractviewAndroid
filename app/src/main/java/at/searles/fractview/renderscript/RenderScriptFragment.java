package at.searles.fractview.renderscript;


import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.renderscript.RenderScript;
import android.util.Log;

import java.util.LinkedList;

import at.searles.fractview.ScriptC_fillimage;
import at.searles.fractview.ScriptC_fractal;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers
 */
public class RenderScriptFragment extends Fragment {

    private static final String DIALOG_FRAGMENT_TAG = "RenderScriptFragmentDialog";

    private RenderScript rs;

    private ScriptC_fillimage fillScript;
    private ScriptC_fractal script;

    /**
     * This flag is only modified in the UI thread.
     */
    private boolean isInitializing;
    private LinkedList<RenderScriptListener> listeners;

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

        showInitDialog();
        launchAsyncInitialize();
    }

    public void showInitDialog() {
        Log.d(getClass().getName(), "show init dialog");
        InitProgressDialogFragment dialogFragment = new InitProgressDialogFragment();
        dialogFragment.show(getActivity().getFragmentManager(), DIALOG_FRAGMENT_TAG);
    }

    private void dismissInitDialog() {
        Log.d(getClass().getName(), "dismiss init dialog");
        InitProgressDialogFragment dialogFragment =
                (InitProgressDialogFragment) getActivity().getFragmentManager()
                        .findFragmentByTag(DIALOG_FRAGMENT_TAG);

        // Dismiss DialogFragment
        dialogFragment.dismissAllowingStateLoss();
    }

    private void launchAsyncInitialize() {
        // async run initScripts
        new AsyncTask<Void, Void, Void>(){
            @Override
            protected Void doInBackground(Void...ignored) {
                initScripts();
                return null;
            }

            @Override
            protected void onPostExecute(Void ignored) {
                isInitializing = false;
                dismissInitDialog();

                // Tell others
                for(RenderScriptListener listener : listeners) {
                    listener.rsInitializationFinished(RenderScriptFragment.this);
                }
            }
        }.execute();
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
