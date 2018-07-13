package at.searles.fractview.bitmap.ui;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import at.searles.fractview.R;
import at.searles.fractview.bitmap.BitmapFragmentListener;
import at.searles.fractal.FractalProvider;
import at.searles.math.Scale;

/**
 * This view allows to dynamically interact with a bitmap fragment.
 * It maintains a ScalableImageView, Progress Bars and a Label for
 * Messages. It furthermore provides
 * callbacks for listeners to the bitmap fragment.
 *
 * In the future it will also host the interactive view.
 */

// FractalFragment contains the fractal.
// BitmapFragmentView must call back to FractalFragment about relative scale.
// Then FractalFragment tells BitmapFragment that there is a new fractal.
// Then, BitmapFragment calls BitmapFragmentView.

public class BitmapFragmentView extends FrameLayout {

    private DrawerProgressTask progressTask;
    private ProgressBar drawerProgressBar;

    private ScaleableImageView imageView;
    private FractalProvider.CallBack callback;

    public BitmapFragmentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        inflate(context, R.layout.bitmapfragment_layout, this);

        imageView = (ScaleableImageView) findViewById(R.id.scaleableImageView);

        drawerProgressBar = (ProgressBar) findViewById(R.id.drawerProgressBar);

        drawerProgressBar.setVisibility(View.INVISIBLE); // will be shown maybe later

        imageView.setListener(new ScaleableImageView.Listener() {
            @Override
            public void scaleRelative(Scale sc) {
                if(callback != null) {
                    callback.setScaleRelative(sc);
                } else {
                    Log.e(getClass().getName(), "scale relative but no fractalCallback is set.");
                }
            }
        });
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

    public void startShowProgress(BitmapFragmentAccessor src) {
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

    public BitmapFragmentListener createListener() {
        return new BitmapFragmentListenerImpl(this);
    }

    public void setCallBack(FractalProvider.CallBack callback) {
        this.callback = callback;
    }
}
