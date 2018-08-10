package at.searles.fractview.fractal;

/**
 * For updates of Drawers
 */
public interface DrawerListener {
    /**
     * Called when some new part of the image was drawn
     *
     * @param firstUpdate true if this is the first update after a new draw.
     */
    void drawingUpdated(boolean firstUpdate);

    /**
     * Called when the calculation is done.
     */
    void drawingFinished();
}
