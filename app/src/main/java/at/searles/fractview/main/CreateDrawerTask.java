package at.searles.fractview.main;

import android.os.AsyncTask;
import android.renderscript.RenderScript;

import at.searles.fractview.ScriptC_fillimage;
import at.searles.fractview.ScriptC_fractal;
import at.searles.fractview.renderscript.RenderScriptDrawer;

/**
 * Task that creates a new drawer
 */
public class CreateDrawerTask extends AsyncTask<Void, Void, Void> {

    private final CalculatorFragment parent; // FIXME longrunning op
    private final RenderScript renderScript;
    private RenderScriptDrawer drawer;

    CreateDrawerTask(RenderScript renderScript, CalculatorFragment parent) {
        this.renderScript = renderScript;
        this.parent = parent;
    }

    @Override
    protected Void doInBackground(Void... ignore) {
        // the following might take some time
        // because it invokes the renderscript
        // compiler
        ScriptC_fractal script = new ScriptC_fractal(renderScript);
        script.set_gScript(script);
        ScriptC_fillimage fillScript = new ScriptC_fillimage(renderScript);
        fillScript.set_gScript(fillScript);

        this.drawer = new RenderScriptDrawer(renderScript, script, fillScript);
        
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        if(!isCancelled()) {
            parent.createDrawerTaskCallback(drawer);
        }
    }
}