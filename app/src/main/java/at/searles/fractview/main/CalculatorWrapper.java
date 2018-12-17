package at.searles.fractview.main;


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
import at.searles.math.Scale;

/**
 * Initializes renderscript and provides a method to create RenderscriptDrawers. Furthermore
 * maintains views and parameters of views.
 */
public class CalculatorWrapper implements ScalableImageView.Listener {
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
    private CalculatorView view;

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
        this.calculator = new FractalCalculator();

        calculator.setDrawerContext(width, height, drawerContext);

        this.createDrawerTask = null;

        parent.appendInitializedWrapper(this);
    }

    void setIndex(int index) {
        // parent can update index if a calculate wrapper was added or removed.
        this.index = index;
    }

    void startDrawerContextExecution(Fractal fractal) {
        calculator.initializeFractal(fractal);
        calculator.initializeRunLoop();
    }

    @Override
    public void scaleRelative(Scale relativeScale) {
        Scale originalScale = ((Scale) parent.getParameterValue(Fractal.SCALE_LABEL, index));
        Scale absoluteScale = originalScale.relative(relativeScale);
        parent.setParameterValue(Fractal.SCALE_LABEL, index, absoluteScale);
    }

    View createCalculatorView() {
        // TODO: Inflate using layout inflater
        this.view = new CalculatorView(parent.getContext(), null);
        this.view.setWrapper(this);

        ViewGroup.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        this.view.setLayoutParams(layoutParams);

        // the next line sets the bitmap
        view.newBitmapCreated(calculator);

        // let calculator respond to view events.
        calculator.addListener(view);

        return view;
    }

    /**
     * This method is called if and only if the parent's view is destroyed.
     * This happens eg if the screen is rotated.
     */
    void destroyView() {
        calculator.removeListener(this.view);
        this.view.dispose();
        this.view = null;
    }

    public Bitmap bitmap() {
        return calculator.bitmap();
    }

    void dispose() {
        // called from parent.onDestroy
        if(createDrawerTask != null) {
            createDrawerTask.cancel(true);
        }

        this.calculator.dispose();
    }

    public void updateInteractivePointsInView() {
        // Scale might have changed or the point itself
        if(view != null) {
            view.clearPoints();

            float[] normPt = new float[2]; // reuse

            for (InteractivePoint pt : parent.interactivePoints()) {
                calculator.invert(pt.position()[0], pt.position()[1], normPt);
                view.addPoint(pt, normPt[0], normPt[1]);
            }
        }
    }

    public void normToValue(float normX, float normY, double[] dst) {
        calculator.translate(normX, normY, dst);
    }
}
