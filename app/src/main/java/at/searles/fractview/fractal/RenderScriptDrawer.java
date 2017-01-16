package at.searles.fractview.fractal;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.renderscript.*;
import android.util.Log;
import at.searles.fractview.ScriptC_fillimage;
import at.searles.fractview.ScriptC_fractal;
import at.searles.fractview.ScriptField_lab_surface;
import at.searles.fractview.ScriptField_palette;
import at.searles.fractview.fractal.Fractal;
import at.searles.fractview.fractal.FractalDrawer;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

import java.util.LinkedList;
import java.util.List;

public class RenderScriptDrawer implements FractalDrawer {
	// Renderscript-Part
	public static final int parallelPixs = 10240; // fixme allow setting
	public static final int factor = 2; // 2^INIT_PIX_SIZE = 16

	private FractalDrawer.Controller controller;

	private Scale scale;
	private Bitmap bitmap;
	private int width;
	private int height;

	private final RenderScript rs;
	private Allocation rsBitmap = null;

	private ScriptC_fillimage fillScript;
	private ScriptC_fractal script;

	//final Script.LaunchOptions options;

	private Allocation rsTile;

	/**
	 * the next field is needed to force sync of drawings. It has no other
	 * practical use than to force a copy-operation that waits until the
	 * rs-kernel is done.
	 */
	final byte[] dummy = new byte[4 * parallelPixs]; // one byte per pix

	// allocation for program code
	private Allocation program_alloc = null;

	// allocation for palettes
	private ScriptField_palette rs_palettes;
	private ScriptField_lab_surface rs_palette_data;

	// for the task-part
	private volatile int maxProgress;
	private volatile int progress;

	public RenderScriptDrawer(FractalDrawer.Controller controller) {
		this.controller = controller;

		// initialize renderscript
		this.rs = RenderScript.create(controller.getActivity());
	}

	public void init(Bitmap bitmap, Fractal fractal) throws RSRuntimeException {
		// the following might take some time
		// because it invokes the renderscript
		// compiler
		script = new ScriptC_fractal(rs);
		script.set_gScript(script);
		fillScript = new ScriptC_fillimage(rs);
		fillScript.set_gScript(fillScript);
		rsTile = Allocation.createSized(rs, Element.U8_4(rs), parallelPixs);
		script.set_gTileOut(rsTile);

		// first set fractal (otherwise we do not have a scale)
		setFractal(fractal);
		updateBitmap(bitmap);
	}

	@Override
	public void setFractal(Fractal fractal) {
		Log.d("FD", "Setting fractal");
		updatePalettes(fractal.palettes());
		updateProgram(fractal.code());
		setScale(fractal.scale());
	}

	public void setScale(Scale sc) {
		updateScale(sc);
	}

	@Override
	public float progress() {
		return progress / ((float) maxProgress);
	}

	public void updateBitmap(Bitmap bm) {
		Log.d("RS_D", "updating bitmap");
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

		Log.d("RS_D", "successful allocation");

		// scale in script depends on image size
		this.bitmap = bm;
		this.width = bm.getWidth();
		this.height = bm.getHeight();

		updateScale(null); // if scale was not initialized yet
	}

	// Draw

	final Runnable copyRunnable = new Runnable() {
		@Override
		public void run() {
			rsBitmap.copyTo(bitmap);

			synchronized (this) {
				// inform the guy who is waiting that we are done.
				this.notify();
			}
		}
	};

	public void run() {
		long dur = System.currentTimeMillis();

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
			if (controller.isCancelled()) break;

			// FIXME Idea: AsyncTask for each step.

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

				// fixme synchronization uses some hack here:
				// I access rsTile. Since therefore, the previous forEach-call must
				// have terminated, it waits here.

				rsTile.copyTo(dummy);

				// fixme I should find a better way though!

				progress += tilelength;
				if (controller.isCancelled()) break;
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

			// fixme line before was replaced by the following: try whether this improves crashes.
			try {
				synchronized (copyRunnable) {
					controller.getActivity().runOnUiThread(copyRunnable);
					copyRunnable.wait();
				}
			} catch (InterruptedException e) {
				// someone is terminating us

				// set interrupt thread
				Thread.currentThread().interrupt();

				// and terminate.
				return;
			}

			// done blowing up copy to bitmap.

			// image was updated, tell people about it.
			if (firstCall) {
				script.set_factor(factor); // it was 0 initially.
				controller.previewGenerated();
				firstCall = false;
			} else {
				controller.bitmapUpdated();
			}

			stepsize /= factor;

			lasttotalpix = totalpix;
		}

		if (!(controller.isCancelled())) {
			dur = System.currentTimeMillis() - dur;
			controller.finished(dur);
		}
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
		script.set_len(code.length);

		if (code.length == 0) return;

		program_alloc = intAlloc(code, program_alloc);
		script.bind_program(program_alloc);
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

		rs_palettes = new ScriptField_palette(rs, l.size());
		script.bind_palettes(rs_palettes);


		//} else {
		//	rs_palettes.resize(size);
		// fixme: do I need to rebind?
		//}

		// fixme if (rs_palette_data == null) {
		rs_palette_data = new ScriptField_lab_surface(rs, surfaceCount);
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
		if(scale == null) scale = this.scale; else this.scale = scale;

		double centerX = (width - 1.) / 2.;
		double centerY = (height - 1.) / 2.;
		double factor = 1. / Math.min(centerX, centerY);

		Double2 x = new Double2(scale.xx * factor, scale.xy * factor);
		Double2 y = new Double2(scale.yx * factor, scale.yy * factor);

		Double2 c = new Double2(
				scale.cx - factor * (scale.xx * centerX + scale.yx * centerY),
				scale.cy - factor * (scale.xy * centerX + scale.yy * centerY)
		);

		script.set_xx(x);
		script.set_yy(y);
		script.set_tt(c);
	}

}
