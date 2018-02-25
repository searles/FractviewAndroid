package at.searles.fractal;

import android.graphics.Bitmap;

import at.searles.math.Scale;

public interface Drawer extends Runnable {

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

	public void setFractal(Fractal f);

	public float progress(); // value between 0 and 1

	public void requestEdit();

	public void clearRequestEdit();
}
