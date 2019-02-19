package at.searles.fractview.provider;


import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.renderscript.RenderScript;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import at.searles.fractal.Fractal;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.ui.CalculatorView;
import at.searles.fractview.bitmap.ui.ScalableImageView;
import at.searles.fractview.fractal.DrawerContext;
import at.searles.fractview.main.CreateDrawerTask;
import at.searles.fractview.main.DrawerProgressTask;
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
    private int id;

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

    public CalculatorWrapper(FractalProviderFragment parent, int width, int height) {
        this.parent = parent;

        this.width = width;
        this.height = height;

        // start it.
        createDrawerTask = new CreateDrawerTask(RenderScript.create(parent.getContext()), this);
    }

    public void startInitialization() {
        createDrawerTask.execute();
    }

    /**
     * This method is called from the CreateDrawerTask.
     * @param drawerContext the drawerContext that was just created.
     */
    public void drawerInitializationFinished(DrawerContext drawerContext) {
        this.createDrawerTask = null;

        this.progressTask = new DrawerProgressTask(this);

        this.calculator = new FractalCalculator(this);
        calculator.setDrawerContext(width, height, drawerContext);

        parent.appendInitializedWrapper(this);
    }

    public int id() {
        return id;
    }

    void startRunLoop(int id, Fractal fractal) {
        this.id = id;

        // put life into calculator (this only has to be done once).
        calculator.initializeFractal(fractal);
        calculator.initializeRunLoop();
    }

    @Override
    public void scaleRelative(Scale relativeScale) {
        Scale originalScale = ((Scale) parent.getParameterValue(Fractal.SCALE_LABEL, id));
        Scale absoluteScale = originalScale.createRelative(relativeScale);
        parent.setParameterValue(Fractal.SCALE_LABEL, id, absoluteScale);
    }

    @SuppressLint("SetTextI18n")
    CalculatorView createView() {
        // First, create calculatorView
        this.calculatorView = new CalculatorView(parent.getContext(), null);
        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, 1f);
        this.calculatorView.setLayoutParams(layoutParams);

        this.calculatorView.setWrapper(this);

        return calculatorView;
    }

    /**
     * This method is called if and only if the parent's view is destroyed.
     * This happens eg if the screen is rotated.
     */
    void destroyView() {
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
            calculatorView.updateInteractivePoints();
        }
    }

    public void normToValue(double normX, double normY, double[] dst) {
        parent.getFractal(id).scale().scalePoint(normX, normY, dst);
    }

    public void valueToNorm(double x, double y, double[] dst) {
        parent.getFractal(id).scale().invScalePoint(x, y, dst);
    }

    boolean setBitmapSize(int width, int height) {
        return calculator.setSize(width, height);
    }

    void removeSaveJob(SaveInBackgroundFragment.SaveJob job) {
        calculator.removeIdleJob(job);
    }

    void addSaveJob(SaveInBackgroundFragment.SaveJob job) {
        calculator.addIdleJob(job, true, false);
    }

    /**
     * Called periodically from ProgressTask
     */
    public void updateProgress() {
        if(calculatorView != null) {
            if (calculator.isRunning()) {
                calculatorView.setProgress(calculator.progress());
            }
        }
    }

    public void onDrawerFinished() {
        if(calculatorView != null) {
            calculatorView.hideProgress();
        }
    }

    public void onCalculatorStarted() {
        progressTask.run();
        if(calculatorView != null) {
            calculatorView.drawerStarted();
        }
    }

    public boolean isRunning() {
        return calculator.isRunning();
    }

    public void onPreviewGenerated() {
        if(calculatorView != null) {
            calculatorView.previewGenerated();
        }
    }

    public void onBitmapUpdated() {
        if(calculatorView != null) {
            calculatorView.invalidate();
        }
    }

    public void onNewBitmapCreated() {
        if(calculatorView != null) {
            calculatorView.initBitmap();
        }
    }

    boolean cancelViewEditing() {
        return calculatorView != null && calculatorView.onBackPressed();
    }

    public Iterable<InteractivePoint> interactivePoints() {
        return parent.interactivePoints();
    }
}
