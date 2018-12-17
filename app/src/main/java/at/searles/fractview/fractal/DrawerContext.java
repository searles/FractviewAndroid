package at.searles.fractview.fractal;

import android.graphics.Bitmap;

import at.searles.fractal.Fractal;
import at.searles.math.Scale;

public interface DrawerContext {

	void setListener(DrawerListener listener);

	/**
	 * Initializes this drawer. Might take some time.
	 */
	void init();

	/**
	 * Creates all necessary data structures
	 * for a new bitmap. Subclass of alloc.
	 * @param newBitmap The bitmap used in the alloc.
	 */
	Alloc createBitmapAlloc(Bitmap newBitmap);

	/**
	 * After a previous call to prepareSetSize, this method actually
	 * replaces the old datastructures by the new ones.
	 * @param alloc A previously generated allocation. Must have been created
	 *              using this, otherwise ClassCast or unexpected behavior.
	 */
	void setBitmapAlloc(Alloc alloc);

	@Deprecated
	void updateBitmap(Bitmap bm);

	void setScale(Scale sc);

	Scale getScale();

	default void translate(float normX, float normY, double[] dst) {
		// FIXME change scale!
		double[] tmp = getScale().scale(normX, normY);
		dst[0] = tmp[0];
		dst[1] = tmp[1];
	}

	default void invert(double x, double y, float[] dst) {
		// FIXME change scale!
		float[] tmp = getScale().invScale(x, y);
		dst[0] = tmp[0];
		dst[1] = tmp[1];
	}

	void setFractal(Fractal f);

	float progress(); // value between 0 and 1

	void cancel();

	void start();

	Bitmap bitmap();

	class Alloc {
		// used as wrapper for internal data structures used for bitmaps.
		public final Bitmap bitmap;

		public Alloc(Bitmap bitmap) {
			this.bitmap = bitmap;
		}
	}
}
