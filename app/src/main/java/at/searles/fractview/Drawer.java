package at.searles.fractal;

import android.graphics.Bitmap;

import at.searles.math.Scale;

public interface Drawer extends Runnable {

	/**
	 * Controller interface for notification callbacks
	 */
	public interface DrawerListener {
		/**
		 * Called when some new part of the image was drawn
		 *
		 * @param firstUpdate true if this is the first update
		 */
		void bitmapUpdated(boolean firstUpdate);

		/**
		 * Called when the calculation is done.
		 */
		void finished();
	}

	public void setListener(DrawerListener listener);

	public void init(Bitmap bitmap, Fractal fractal);

	public void updateBitmap(Bitmap bm);

	public void setScale(Scale sc);

	public void setFractal(Fractal f);

	public float progress(); // value between 0 and 1

	public void requestEdit();

	public void clearRequestEdit();
}
