package at.searles.fractview.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import at.searles.fractview.BitmapFragment;
import at.searles.fractview.R;
import at.searles.math.Scale;

/**
 * This view allows to dynamically interact with a bitmap fragment.
 * It maintains a ScalableImageView, Progress Bars and a Label for
 * Messages. It furthermore provides
 * callbacks for listeners to the bitmap fragment.
 *
 * In the future it will also host the interactive view.
 */

public class BitmapFragmentView extends FrameLayout {

    public static final long PROGRESS_UPDATE_MILLIS = 500; // update the progress bar every ... ms.

    private DrawerProgressTask updateAction;

    // Bitmap fragment is needed for many tasks
    private BitmapFragment bitmapFragment;
    private BitmapFragment.BitmapFragmentListener bitmapFragmentListener;

    private ProgressBar initProgressBar;
    private ProgressBar drawerProgressBar;
    private TextView messageLabel;

    private ScaleableImageView imageView;

    public BitmapFragmentView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        inflate(context, R.layout.bitmapfragment_layout, this);

        updateAction = new DrawerProgressTask();
        imageView = (ScaleableImageView) findViewById(R.id.scaleableImageView);

        initProgressBar = (ProgressBar) findViewById(R.id.initProgressBar);
        drawerProgressBar = (ProgressBar) findViewById(R.id.drawerProgressBar);
        messageLabel = (TextView) findViewById(R.id.messageTextView);

        drawerProgressBar.setVisibility(View.INVISIBLE); // will be shown maybe later

        imageView.setListener(new ScaleableImageView.Listener() {
            @Override
            public void scaleRelative(Scale sc) {
                bitmapFragment.setScaleRelative(sc);
            }
        });
    }

    public void dispose() {
        if(bitmapFragment != null) {
            this.bitmapFragment.removeBitmapFragmentListener(bitmapFragmentListener);
            this.bitmapFragment = null;
        }
    }

    public ScaleableImageView scaleableImageView() {
        return imageView;
    }

    public boolean backButtonAction() {
        // in here there might be more stuff...
        return imageView.backButtonAction();
    }

    /**
     * Class used for updating the view on a regular basis
     */
    class DrawerProgressTask implements Runnable {
        Handler updateHandler;
        boolean updateActionRunning;

        DrawerProgressTask() {
            updateHandler = new Handler();
            updateActionRunning = false;
        }

        void updateProgress() {
            drawerProgressBar.setProgress(
                    (int) (bitmapFragment.progress() * drawerProgressBar.getMax() + 0.5));
        }

        // start update action
        void schedule() {
            // always in UI-thread.
            if(bitmapFragment == null) return;

            if(!updateActionRunning && bitmapFragment.isRunning()) {
                updateProgress();

                // only start if it is not running yet.
                drawerProgressBar.setProgress(0);
                drawerProgressBar.setVisibility(View.VISIBLE);

                updateActionRunning = true;
                updateHandler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
            }
        }

        @Override
        public void run() {
            if(bitmapFragment != null) {
                if (bitmapFragment.isRunning()) {
                    updateProgress();
                    updateHandler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
                } else {
                    drawerProgressBar.setVisibility(View.INVISIBLE);
                    updateActionRunning = false;
                }
            }
        }
    }

    public void setBitmapFragment(BitmapFragment bitmapFragment) {
        if(this.bitmapFragment != null) {
            // in case I ever want to change the bitmap fragment...
            this.bitmapFragment.removeBitmapFragmentListener(bitmapFragmentListener);

            this.bitmapFragment = null;
            this.bitmapFragmentListener = null;
        }

        this.bitmapFragment = bitmapFragment;

        if(bitmapFragment.isInitializing()) {
            // activate spinner
            messageLabel.setText("Hello. This is active");
        } else {
            messageLabel.setVisibility(INVISIBLE);
            initProgressBar.setVisibility(INVISIBLE);
        }

        this.bitmapFragmentListener = new BitmapFragment.BitmapFragmentListener() {
            @Override
            public void initializationFinished() {
                messageLabel.setVisibility(INVISIBLE);
                initProgressBar.setVisibility(INVISIBLE);

                // fixme remove view

                invalidate(); // redraw
            }

            @Override
            public void bitmapUpdated(BitmapFragment src) {
                invalidate();
            }

            @Override
            public void previewGenerated(BitmapFragment src) {
                // can be called from outside the UI-thread!
                Log.d(getClass().getName(), "preview generated");
                imageView.removeLastScale();
                invalidate();
            }

            @Override
            public void calculationStarting(BitmapFragment src) {
                updateAction.schedule();
            }

            @Override
            public void calculationFinished(long ms, BitmapFragment src) {
                // progress bar is hidden in update task.
            }

            @Override
            public void newBitmapCreated(Bitmap bitmap, BitmapFragment src) {
                imageView.setBitmap(bitmap);
                requestLayout();
            }
        };

        bitmapFragment.addBitmapFragmentListener(bitmapFragmentListener);
        updateAction.schedule(); // start progress if necessary
    }


}
