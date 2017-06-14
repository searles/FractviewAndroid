package at.searles.fractview.fractal;

import android.graphics.Bitmap;

import at.searles.math.Scale;

public interface FractalDrawer extends Runnable {

	/**
	 * Controller interface for notification callbacks
	 */
	public interface FractalDrawerListener {
		/**
		 * Called when the first preview of the new image is generated.
		 */
		void previewGenerated();

		/**
		 * Called when some new part of the image was drawn
		 */
		void bitmapUpdated();

		/**
		 * Called when the calculation is done.
		 * @param ms
		 */
		void finished(long ms);
	}

	public void setListener(FractalDrawerListener listener);

	public void init(Bitmap bitmap, Fractal fractal);

	public void updateBitmap(Bitmap bm);

	public void setScale(Scale sc);

	public void setFractal(Fractal f);

	public float progress(); // value between 0 and 1

	public void cancel(); // set cancel flag to true.
}
