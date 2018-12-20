package at.searles.fractview.renderscript;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.renderscript.Allocation;
import android.renderscript.Double2;
import android.renderscript.Element;
import android.renderscript.Matrix4f;
import android.renderscript.RSRuntimeException;
import android.renderscript.RenderScript;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.Fractal;
import at.searles.fractview.ScriptC_fillimage;
import at.searles.fractview.ScriptC_fractal;
import at.searles.fractview.ScriptField_lab_surface;
import at.searles.fractview.ScriptField_palette;
import at.searles.fractview.fractal.DrawerContext;
import at.searles.fractview.fractal.DrawerListener;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

public class RenderScriptDrawerContext implements DrawerContext {
	// Renderscript-Part
	private static final int parallelPixs = 10240; // fixme do some experiments
	public static final int factor = 2; // 2^INIT_PIX_SIZE = 16

	private DrawerListener listener;

	private Bitmap bitmap;

	private Scale scale;

	private RenderScript rs;
	private Allocation rsBitmap = null;

	private ScriptC_fillimage fillScript;
	private ScriptC_fractal script;

	//final Script.LaunchOptions options;

	private Allocation rsTile;

	// allocation for program code
	private Allocation program_alloc = null;

	// for the task-part
	private volatile int maxProgress;
	private volatile int progress;
	private boolean isCancelled;

	public RenderScriptDrawerContext(RenderScript rs, ScriptC_fractal script, ScriptC_fillimage fillScript) {
		this.rs = rs;
		this.script = script;
		this.fillScript = fillScript;
	}

	public void setListener(DrawerListener listener) {
		this.listener = listener;
	}

	public void init() throws RSRuntimeException {
		rsTile = Allocation.createSized(rs, Element.U8_4(rs), parallelPixs);
		script.set_gTileOut(rsTile);
	}

	@Override
	public void setFractal(Fractal fractal) {
		updatePalettes(fractal.palettes());
		updateProgram(fractal.code());
		updateScale(fractal.scale());
	}

	@Override
	public Scale getScale() {
		return this.scale;
	}

	@Override
	public float progress() {
		return progress / ((float) maxProgress);
	}

	@Override
	public void cancel() {
		this.isCancelled = true;
	}


    @Override
	@Deprecated
	public void updateBitmap(Bitmap bm) {
		Allocation newRSBitmap = Allocation.createFromBitmap(rs, bm);

		if (rsBitmap != null) {
			rsBitmap.destroy();
			rsBitmap = null;
		}

		this.rsBitmap = newRSBitmap;

		script.set_gOut(rsBitmap);
		script.set_width(bm.getWidth());
		script.set_height(bm.getHeight());

		fillScript.set_width(bm.getWidth());
		fillScript.set_height(bm.getHeight());
		fillScript.set_gOut(rsBitmap);

		// scale in script depends on image size
		this.bitmap = bm;
//
//		this.width = bm.getWidth();
//		this.height = bm.getHeight();
	}

	@Override
	public Alloc createBitmapAlloc(Bitmap newBitmap) {
		try {
			Allocation newRSBitmap = Allocation.createFromBitmap(rs, newBitmap);
			return new RSAlloc(newBitmap, newRSBitmap);
		} catch(RSRuntimeException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public void setBitmapAlloc(Alloc a) {
		// in ui thread
		if (rsBitmap != null) {
			// clean up
			rsBitmap.destroy();
			rsBitmap = null;
		}

		this.rsBitmap = ((RSAlloc) a).alloc;
		this.bitmap = a.bitmap;

		script.set_gOut(rsBitmap);
		script.set_width(this.bitmap.getWidth());
		script.set_height(this.bitmap.getHeight());

		fillScript.set_width(this.bitmap.getWidth());
		fillScript.set_height(this.bitmap.getHeight());
		fillScript.set_gOut(rsBitmap);
	}

	// Draw

	private final Runnable copyRunnable = new Runnable() {
		@Override
		public void run() {
			rsBitmap.copyTo(bitmap);

			synchronized (this) {
				// inform the guy who is waiting that we are done.
				this.notify();
			}
		}
	};

	public void start() {
		// in ui-thread.
		isCancelled = false;

		new Thread(
				new Runnable() {
					@Override
					public void run() {
						backgroundRenderer();
					}
				}
		).start();
	}

	@Override
	public Bitmap bitmap() {
		return bitmap;
	}

	private void backgroundRenderer() {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();

		script.set_factor(0); // factor is 0 in beginning.

		int stepsize = 1;

		int size = width * height; // number of pixels to be drawn.

		while (stepsize * parallelPixs * factor < size) stepsize *= factor;

		// okay, we have a stepsize.
		// now, we can do stuff.
		int lasttotalpix = 0;
		int pixcount; // number of pixels to be run in the next run.

		boolean firstCall = true;

		progress = 0;
		maxProgress = size;

		while (stepsize > 0) {
			if (isCancelled) break;

			script.set_stepsize(stepsize);

			// pixels to be drawn
			int totalpix = ((width + stepsize - 1) / stepsize) *
					((height + stepsize - 1) / stepsize);

			// how many pixels have to be drawn in this run?
			pixcount = totalpix - lasttotalpix;

			for (int index = 0; index < pixcount; index += parallelPixs) { // partition over pixcount.
				int tilelength = index + parallelPixs >= pixcount ? pixcount - index : parallelPixs;

				script.set_offset(index);
				script.set_tilelength(tilelength);

				// calculate tile (this call cannot be intercepted)
				script.forEach_root(rsTile); // call root

				// wait for kernel
				rs.finish();

				progress += tilelength;
				if (isCancelled) break;
			}

			// fill gaps
			if (stepsize > 1) {
				// Call the script that fills the parts
				// between single pixels by gradients
				fillScript.set_stepsize(stepsize);
				fillScript.forEach_root(rsBitmap, rsBitmap);
			}

			// copy to image
			// rsBitmap.copyTo(bitmap);
			// line before was replaced by the following: try whether this improves crashes.

			try {
				synchronized (copyRunnable) {
					new Handler(Looper.getMainLooper()).post(copyRunnable);
					copyRunnable.wait();
				}
			} catch (InterruptedException e) {
				// someone is terminating us
				Log.e(getClass().getName(), "Interrupted", e);

				// set interrupt thread
				Thread.currentThread().interrupt();

				// and terminate.
				return;
			}

			// done blowing up copy to bitmap.

			// image was updated, tell people about it.
			if (firstCall) {
				script.set_factor(factor); // it was 0 initially.
				listener.drawingUpdated(true);
				firstCall = false;
			} else {
				listener.drawingUpdated(false);
			}

			stepsize /= factor;

			lasttotalpix = totalpix;
		}

		Log.d(getClass().getName(), "drawing finished, cancel=" + isCancelled + ", size=" + size);
		listener.drawingFinished();
	}

	// Update data
	private Allocation intAlloc(int[] array, Allocation alloc) {
		if (alloc == null || alloc.getType().getCount() != array.length) {
			if (alloc != null) alloc.destroy();
			alloc = Allocation.createSized(rs, Element.I32(rs), array.length);
		}

		alloc.copyFrom(array);

		return alloc;
	}

	private void updateProgram(int[] code) {
		script.set_codeLen(code.length);

		if (code.length == 0) return;

		program_alloc = intAlloc(code, program_alloc);
		script.bind_code(program_alloc);
	}


	private void updatePalettes(List<Palette> l) {
		// get size of required data structures
		if (l.isEmpty()) return;

		int surfaceCount = 0;

		List<Palette.Data> data = new LinkedList<>();

		for (Palette palette : l) {
			Palette.Data p = palette.create();
			data.add(p);
			surfaceCount += p.countSurfaces();
		}

		// fixme: only reallocate if more space needed

		// FIXME: do I have to free old memory??? CHECK!!!
		// fixme if (rs_palettes == null) {

		ScriptField_palette rs_palettes = new ScriptField_palette(rs, l.size());
		script.bind_palettes(rs_palettes);

		//} else {
		//	rs_palettes.resize(size);
		// fixme: do I need to rebind?
		//}

		// fixme if (rs_palette_data == null) {
		ScriptField_lab_surface rs_palette_data = new ScriptField_lab_surface(rs, surfaceCount);
		script.bind_palette_data(rs_palette_data);
		// } else {
		// fixme resize not allowed
		//
		/* This method was deprecated in API level 18. RenderScript objects should be immutable once created.
			The replacement is to create a new allocation and copy the contents. This function will throw
			an exception if API 21 or higher is used.*/

		//	rs_palette_data.resize(surfaceCount);
		// fixme: do I need to rebind?
		//}

		// now set values

		int offset = 0;
		int index = 0;

		for (Palette.Data palette : data) {
			rs_palettes.set_w(index, palette.w, true);
			rs_palettes.set_h(index, palette.h, true);
			rs_palettes.set_offset(index, offset, true);

			index++;

			for (int i = 0; i < palette.countSurfaces(); ++i) {
				rs_palette_data.set_L(offset, new Matrix4f(palette.splines[i].L.flts()), true);
				rs_palette_data.set_a(offset, new Matrix4f(palette.splines[i].a.flts()), true);
				rs_palette_data.set_b(offset, new Matrix4f(palette.splines[i].b.flts()), true);
				rs_palette_data.set_alpha(offset, new Matrix4f(palette.splines[i].alpha.flts()), true);

				offset++;
			}
		}
	}

	private void updateScale(Scale scale) {
		this.scale = scale;

		Double2 x = new Double2(scale.xx, scale.xy);
		Double2 y = new Double2(scale.yx, scale.yy);

		Double2 c = new Double2(scale.cx, scale.cy);

		script.set_xx(x);
		script.set_yy(y);
		script.set_tt(c);

		// fixme fetch scale like palettes
	}

	private class RSAlloc extends Alloc {
		final Allocation alloc;

		RSAlloc(Bitmap bitmap, Allocation alloc) {
			super(bitmap);
			this.alloc = alloc;
		}
	}
}
