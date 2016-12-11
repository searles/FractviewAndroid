package at.searles.fractview.shaders;

/**
 * Created by searles on 06.04.16.
 */

import android.graphics.Bitmap;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.FractalDrawer;
import at.searles.math.Scale;

public class GLDrawer implements FractalDrawer {

	final Controller controller;

	Scale currentScale; // must store it in case the bitmap dimensions change.

	int width = -1;
	int height = -1;
	Bitmap bitmap;

	public GLDrawer(Controller controller) {
		this.controller = controller;
	}


	@Override
	public void init(Bitmap bitmap, Fractal fractal) {

	}

	@Override
	public void updateBitmap(Bitmap bm) {

	}

	@Override
	public void setScale(Scale sc) {

	}

	@Override
	public void setFractal(Fractal f) {

	}

	@Override
	public float progress() {
		return 0;
	}

	@Override
	public void run() {

	}
}
