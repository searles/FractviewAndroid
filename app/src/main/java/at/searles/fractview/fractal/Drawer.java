package at.searles.fractview.fractal;

import android.graphics.Bitmap;
import android.graphics.PointF;

import at.searles.fractal.Fractal;
import at.searles.math.Scale;

public interface Drawer {

	public void setListener(DrawerListener listener);

	/**
	 * Initializes this drawer.
	 */
	public void init();

	/**
	 * Creates all necessary data structures
	 * for the given bitmap but keeps the old ones
	 * in place because a calculation might be
	 * still running.
	 * @param newBitmap The bitmap that will replace the current one.
	 * @return true if the datastructures could be allocated successfully.
	 */
	boolean prepareSetSize(Bitmap newBitmap);

	/**
	 * After a previous call to prepareSetSize, this method actually
	 * replaces the old datastructures by the new ones.
	 */
	void applyNewSize();

	@Deprecated
	public void updateBitmap(Bitmap bm);

	public void setScale(Scale sc);

	public Scale getScale();

	public default double[] translate(PointF normPoint) {
		return getScale().scale(normPoint.x, normPoint.y);
	}

	public default PointF invert(double x, double y) {
		float[] point = getScale().invScale(x, y);
		return new PointF(point[0], point[1]);
	}

	public void setFractal(Fractal f);

	public float progress(); // value between 0 and 1

	public void cancel();

	public void start();
}
