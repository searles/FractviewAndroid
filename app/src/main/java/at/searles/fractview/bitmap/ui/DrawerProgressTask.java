package at.searles.fractview.bitmap.ui;

import android.os.Handler;

import at.searles.fractview.bitmap.FractalCalculator;

/**
 * Class used for updating the view on a regular basis
 */
class DrawerProgressTask implements Runnable {

    private static final long PROGRESS_UPDATE_MILLIS = 500; // TODO move to res. update the progress bar every ... ms.

    private final FractalCalculatorView view;
    private final FractalCalculator calculator;
    private final Handler handler;

    private boolean disposed;

    DrawerProgressTask(FractalCalculatorView view, FractalCalculator calculator) {
        this.view = view;
        this.calculator = calculator;
        this.handler = new Handler();
    }

    void dispose() {
        disposed = true;
    }

    @Override
    public void run() {
        if(calculator != null) {
            if (calculator.isRunning()) {
                if(!disposed) {
                    view.setProgress(calculator.progress());
                    handler.postDelayed(this, PROGRESS_UPDATE_MILLIS);
                }
            } else {
                view.hideProgress();
            }
        }
    }
}
