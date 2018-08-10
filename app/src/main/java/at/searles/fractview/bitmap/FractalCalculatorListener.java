package at.searles.fractview.bitmap;

/**
 * FractalCalculator does some callbacks to the activity that
 * created this FractalCalculator. It uses the following interface
 * for this purpose.
 */
public interface FractalCalculatorListener {
    /**
     * The view and progress bars should be updated because
     * the bitmap changed
     */
    void bitmapUpdated(FractalCalculator src);

    /**
     * The view should be updated and the view matrices reset
     * because a first preview was generated in the bitmap.
     * TODO: Maybe merge this one with drawingUpdated?
     */
    void previewGenerated(FractalCalculator src);

    /**
     * We will now start a new calc. This one is called from the UI-thread.
     */
    void drawerStarted(FractalCalculator src);

    /**
     * Called when the calculation is finished (and it was not cancelled)
     * @param ms milliseconds
     */
    void drawerFinished(long ms, FractalCalculator src);

    /**
     * Called after a new bitmap was created.
     */
    void newBitmapCreated(FractalCalculator src);
}