package at.searles.fractview.bitmap.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import at.searles.fractview.R;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.FractalCalculatorListener;
import at.searles.fractview.bitmap.ui.imageview.InteractivePointsPlugin;
import at.searles.fractview.main.CalculatorWrapper;
import at.searles.fractview.main.InteractivePoint;

/**
 * This view allows to dynamically interact with a bitmap fragment.
 * It maintains a ScalableImageView, Progress Bars and a Label for
 * Messages. It furthermore provides
 * callbacks for listeners to the bitmap fragment.
 *
 * In the future it will also host the interactive view.
 */

// FractalProviderFragment contains the fractal.
// FractalCalculatorView must call back to FractalProviderFragment about relative scale.
// Then FractalProviderFragment tells FractalCalculator that there is a new fractal.
// Then, FractalCalculator calls FractalCalculatorView.

public class CalculatorView extends FrameLayout implements FractalCalculatorListener {

    private CalculatorWrapper wrapper;
    private ProgressBar drawerProgressBar;

    private ScalableImageView imageView;

    private InteractivePointsPlugin interactivePointsPlugin;

    public CalculatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_fractal_calculator, this);

        imageView = findViewById(R.id.scaleableImageView);
        interactivePointsPlugin = new InteractivePointsPlugin(imageView);
        imageView.addPlugin(interactivePointsPlugin);

        drawerProgressBar = findViewById(R.id.drawerProgressBar);
        drawerProgressBar.setVisibility(View.INVISIBLE); // will be shown maybe later

        imageView.setListener(scale -> wrapper.scaleRelative(scale));
    }

    public void setWrapper(CalculatorWrapper wrapper) {
        this.wrapper = wrapper;
    }

    // interactive points section.

    public void addPoint(InteractivePoint pt, float normX, float normY) {
        float[] screenPt = new float[2];
        imageView.normalizedToScreen(normX, normY, screenPt);

        // Step 2: Set point
        interactivePointsPlugin.addPoint(pt, screenPt[0], screenPt[1], this);

        scaleableImageView().invalidate();
    }

    public void interactivePointMoved(InteractivePoint pt, float screenX, float screenY) {
        float[] normPt = new float[2];
        double[] value = new double[2];

        imageView.screenToNormalized(screenX, screenY, normPt);
        wrapper.normToValue(normPt[0], normPt[1], value);
        pt.setValue(value);
    }

    public void clearPoints() {
        interactivePointsPlugin.clear();
    }

    // ================

    public void setProgress(float progress) {
        drawerProgressBar.setVisibility(VISIBLE);
        drawerProgressBar.setProgress(
                (int) (progress * drawerProgressBar.getMax() + 0.5));
    }

    public void hideProgress() {
        drawerProgressBar.setVisibility(INVISIBLE);
    }

    public boolean backButtonAction() {
        // FIXME
        return imageView.backButtonAction();
    }

    public ScalableImageView scaleableImageView() {
        return imageView;
    }

    @Override
    public void bitmapUpdated(FractalCalculator src) {
        this.invalidate();
    }

    @Override
    public void previewGenerated(FractalCalculator src) {
        // can be called from outside the UI-thread!
        this.scaleableImageView().removeLastScale();
        interactivePointsPlugin.setEnabled(true);
        wrapper.updateInteractivePointsInView(); // because image matrix changed.
        this.invalidate();
    }

    @Override
    public void drawerStarted(FractalCalculator src) {
        // wait until preview is shown
        interactivePointsPlugin.setEnabled(false);
    }

    @Override
    public void drawerFinished(long ms, FractalCalculator src) {
        // progress bar is hidden in update task.
    }

    @Override
    public void newBitmapCreated(FractalCalculator src) {
        this.scaleableImageView().setBitmap(src.bitmap());
        this.requestLayout();
    }

}
