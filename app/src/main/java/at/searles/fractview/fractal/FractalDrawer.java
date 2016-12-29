package at.searles.fractview.fractal;

import android.app.Activity;
import android.graphics.Bitmap;
import at.searles.math.Scale;

public interface FractalDrawer extends Runnable {

	/**
	 * Controller interface for notification callbacks
	 */
	public interface Controller {
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

		/**
		 * The drawer should check this regularly to  find out whether the calculation is cancelled.
		 * @return
		 */
		boolean isCancelled();

		/**
		 * Returns an activity that can be used to eg access the UI-thread.
		 * @return
		 */
		Activity getActivity();
	}

	public void init(Bitmap bitmap, Fractal fractal);

	public void updateBitmap(Bitmap bm);

	public void setScale(Scale sc);

	public void setFractal(Fractal f);

	float progress(); // value between 0 and 1
}
