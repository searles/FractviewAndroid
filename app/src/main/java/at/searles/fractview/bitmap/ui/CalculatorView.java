package at.searles.fractview.bitmap.ui;

import android.content.Context;
import android.graphics.PointF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import at.searles.fractview.R;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.FractalCalculatorListener;
import at.searles.fractview.bitmap.ui.imageview.InteractivePointsPlugin;
import at.searles.fractview.main.CalculatorFragment;

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

    private CalculatorFragment fragment;
    private DrawerProgressTask progressTask;
    private ProgressBar drawerProgressBar;

    private ScalableImageView imageView;

    private InteractivePointsPlugin interactivePointsPlugin;

    public CalculatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public void setFragment(CalculatorFragment fragment) {
        this.fragment = fragment;
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_fractal_calculator, this);

        imageView = findViewById(R.id.scaleableImageView);
        interactivePointsPlugin = new InteractivePointsPlugin(imageView);
        imageView.addPlugin(interactivePointsPlugin);

        drawerProgressBar = findViewById(R.id.drawerProgressBar);
        drawerProgressBar.setVisibility(View.INVISIBLE); // will be shown maybe later
    }

    public void addPoint(String key, String description, PointF normalizedPoint) {
        PointF screenPoint = imageView.normalizedToScreen(normalizedPoint);

        // Step 2: Set point
        interactivePointsPlugin.addPoint(key, description, screenPoint.x, screenPoint.y,
                new InteractivePointsPlugin.PointListener() {
                    @Override
                    public void pointMoved(String key, float screenX, float screenY) {
                        // FIXME normalized should be done in here.
                        PointF normalizedPoint = imageView.screenToNormalized(new PointF(screenX, screenY));
                        fragment.moveParameterToNormalized(key, normalizedPoint);
                    }
                });
    }

    public void removePoint(String key) {
        interactivePointsPlugin.removePoint(key);
    }

    public void updatePoint(String key, PointF normalizedPoint) {
        // cannot use normalizedToScreen because it uses a different conversion.
        PointF bitmapPoint = scaleableImageView().normalizedToBitmap(normalizedPoint);
        interactivePointsPlugin.moveBitmapPointTo(key, bitmapPoint.x, bitmapPoint.y);
    }

    void setProgress(float progress) {
        drawerProgressBar.setVisibility(VISIBLE);
        drawerProgressBar.setProgress(
                (int) (progress * drawerProgressBar.getMax() + 0.5));
    }

    void hideProgress() {
        drawerProgressBar.setVisibility(INVISIBLE);
    }

    public boolean backButtonAction() {
        // FIXME
        return imageView.backButtonAction();
    }

    public void startShowProgress(FractalCalculator src) {
        progressTask = new DrawerProgressTask(this, src);

        // first update is in the currently running ui-thread.
        progressTask.run();
    }

    public void dispose() {
        // dispose progress task if running
        if(progressTask != null) {
            // TODO: Move this to fractalcalculatorfragment?
            progressTask.dispose();
        }
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
        Log.d(getClass().getName(), "preview generated");
        this.scaleableImageView().removeLastScale();

        // Tell interactivePointPlugin to update its point.
        // FIXME interactivePointsPlugin.movePointTo();

        this.invalidate();
    }

    @Override
    public void drawerStarted(FractalCalculator src) {
        this.startShowProgress(src);
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
