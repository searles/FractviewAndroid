package at.searles.fractview.bitmap.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import at.searles.fractview.R;
import at.searles.fractview.bitmap.FractalCalculator;
import at.searles.fractview.bitmap.FractalCalculatorListener;

/**
 * This view allows to dynamically interact with a bitmap fragment.
 * It maintains a ScalableImageView, Progress Bars and a Label for
 * Messages. It furthermore provides
 * callbacks for listeners to the bitmap fragment.
 *
 * In the future it will also host the interactive view.
 */

// FractalFragment contains the fractal.
// FractalCalculatorView must call back to FractalFragment about relative scale.
// Then FractalFragment tells FractalCalculator that there is a new fractal.
// Then, FractalCalculator calls FractalCalculatorView.

public class FractalCalculatorView extends FrameLayout implements FractalCalculatorListener {

    private DrawerProgressTask progressTask;
    private ProgressBar drawerProgressBar;

    private ScaleableImageView imageView;

    public FractalCalculatorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.view_fractal_calculator, this);

        imageView = (ScaleableImageView) findViewById(R.id.scaleableImageView);

        drawerProgressBar = (ProgressBar) findViewById(R.id.drawerProgressBar);

        drawerProgressBar.setVisibility(View.INVISIBLE); // will be shown maybe later
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
        // TODO Can't I register for this?
        // in here there might be more stuff...
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
            progressTask.dispose();
        }
    }

    public ScaleableImageView scaleableImageView() {
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
