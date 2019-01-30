package at.searles.fractview.bitmap.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import at.searles.fractview.R;
import at.searles.fractview.bitmap.ui.imageview.InteractivePointsPlugin;
import at.searles.fractview.main.CalculatorWrapper;

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

public class CalculatorView extends FrameLayout {

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

    /**
     * Initializes the wrapper. Also fetches and sets bitmap.
     */
    public void setWrapper(CalculatorWrapper wrapper) {
        this.wrapper = wrapper;
        initBitmap();
        interactivePointsPlugin.setWrapper(wrapper);
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

    public boolean onBackPressed() {
        return interactivePointsPlugin.cancelDragging() || imageView.onBackPressed();
    }

    public ScalableImageView scaleableImageView() {
        return imageView;
    }

    public void previewGenerated() {
        // can be called from outside the UI-thread!
        this.scaleableImageView().removeLastScale();
        interactivePointsPlugin.setEnabled(true);
        wrapper.updateInteractivePointsInView(); // because image matrix changed.
        this.invalidate();
    }

    public void drawerStarted() {
        // wait until preview is shown
        interactivePointsPlugin.setEnabled(false);
    }

    public void initBitmap() {
        this.scaleableImageView().setBitmap(wrapper.bitmap());
        this.requestLayout();
    }

    public void updateInteractivePoints() {
        interactivePointsPlugin.updatePoints();
    }
}
