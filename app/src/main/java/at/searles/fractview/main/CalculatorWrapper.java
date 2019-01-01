package at.searles.fractview.main;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.renderscript.RenderScript;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import at.searles.fractal.Fractal;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.ui.CalculatorView;
import at.searles.fractview.bitmap.ui.ScalableImageView;
import at.searles.fractview.fractal.DrawerContext;
import at.searles.fractview.saving.SaveInBackgroundFragment;
import at.searles.math.Scale;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers. Furthermore
 * maintains views and parameters of views.
 */
public class CalculatorWrapper implements ScalableImageView.Listener {

    private DrawerProgressTask progressTask;

    private final FractalProviderFragment parent;
    private int index;

    private int width;
    private int height;

    /**
     * Task that will create the drawer.
     */
    private CreateDrawerTask createDrawerTask;

    /**
     * The calculator. Only initialized after drawerTask is finished.
     */
    private FractalCalculator calculator;

    /**
     * The view. Set after it is initialized. Must be unset when
     * the parent view is destroyed.
     */
    private CalculatorView calculatorView;

    CalculatorWrapper(FractalProviderFragment parent, int width, int height) {
        this.parent = parent;
        this.index = -1;

        this.width = width;
        this.height = height;

        // start it.
        createDrawerTask = new CreateDrawerTask(RenderScript.create(parent.getContext()), this);
    }

    void startInitialization() {
        createDrawerTask.execute();
    }

    /**
     * This method is called from the CreateDrawerTask.
     * @param drawerContext the drawerContext that was just created.
     */
    void drawerInitializationFinished(DrawerContext drawerContext) {
        this.createDrawerTask = null;

        this.progressTask = new DrawerProgressTask(this);

        this.calculator = new FractalCalculator(this);
        calculator.setDrawerContext(width, height, drawerContext);

        parent.appendInitializedWrapper(this);
    }

    public int index() {
        return index;
    }

    void setIndex(int index) {
        // parent can update index if a calculate wrapper was added or removed.
        this.index = index;
    }

    void startRunLoop(Fractal fractal) {
        // put life into calculator (this only has to be done once).
        calculator.initializeFractal(fractal);
        calculator.initializeRunLoop();
    }

    @Override
    public void scaleRelative(Scale relativeScale) {
        Scale originalScale = ((Scale) parent.getParameterValue(Fractal.SCALE_LABEL, index));
        Scale absoluteScale = originalScale.relative(relativeScale);
        parent.setParameterValue(Fractal.SCALE_LABEL, index, absoluteScale);
    }

    @SuppressLint("SetTextI18n")
    View createView() {
        // First, create calculatorView
        this.calculatorView = new CalculatorView(parent.getContext(), null);
        this.calculatorView.setWrapper(this);

        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1f);
        this.calculatorView.setLayoutParams(layoutParams);

        // the next line sets the bitmap
        calculatorView.newBitmapCreated(calculator);

        // let calculator respond to view events.
        calculator.addListener(calculatorView);

        return calculatorView;
    }

    /**
     * This method is called if and only if the parent's view is destroyed.
     * This happens eg if the screen is rotated.
     */
    void destroyView() {
        calculator.removeListener(this.calculatorView);
        this.calculatorView = null;
    }

    public Bitmap bitmap() {
        return calculator.bitmap();
    }

    void dispose() {
        // called from parent.onDestroy
        if(createDrawerTask != null) {
            createDrawerTask.cancel(true);
        }

        this.progressTask.destroy();
        this.calculator.destroy();
    }

    public void updateInteractivePointsInView() {
        // Scale might have changed or the point itself
        if(calculatorView != null) {
            calculatorView.clearPoints();

            float[] normPt = new float[2]; // reuse

            for (InteractivePoint pt : parent.interactivePoints()) {
                // FIXME change scale
                float[] tmp = parent.getFractal(index).scale().invScale(pt.position()[0], pt.position()[1]);
                normPt[0] = tmp[0];
                normPt[1] = tmp[1];

                // [bkp] calculator.invert(, normPt);
                calculatorView.addPoint(pt, normPt[0], normPt[1]);
            }
        }
    }

    public void normToValue(float normX, float normY, double[] dst) {
        double[] tmp = parent.getFractal(index).scale().scale(normX, normY);
        // FIXME change scale!

        dst[0] = tmp[0];
        dst[1] = tmp[1];
    }

    public boolean setBitmapSize(int width, int height) {
        return calculator.setSize(width, height);
    }

    public void removeSaveJob(SaveInBackgroundFragment.SaveJob job) {
        calculator.removeIdleJob(job);
    }

    public void addSaveJob(SaveInBackgroundFragment.SaveJob job) {
        calculator.addIdleJob(job, true, false);
    }

    public void updateProgress() {
        if(calculatorView != null) {
            if (calculator.isRunning()) {
                calculatorView.setProgress(calculator.progress());
            } else {
                calculatorView.hideProgress();
            }
        }
    }

    public void onCalculatorStarted() {
        startShowProgress();
    }

    public void startShowProgress() {
        // first update is in the currently running ui-thread.
        progressTask.run();
    }

    public boolean isRunning() {
        return calculator.isRunning();
    }
}
