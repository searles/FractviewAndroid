package at.searles.fractview.fractal;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.widget.Toast;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Class to manage preset fractals and their default values.
 */
public class PresetFractals {
	public static final Scale INIT_SCALE = new Scale(2, 0, 0, 2, 0, 0);

	public static class Preset {
		public String title;
		public String filename;
		String iconFilename;
		public Scale scale;
		public Parameters parameters;

		private Preset(String title, String filename, String iconFilename, Scale scale, Parameters parameters) {
			this.title = title;
			this.filename = filename;
			this.iconFilename = iconFilename;
			this.scale = scale;
			this.parameters = parameters;
		}
	}

	/**
	 * Try to read content of assets-folder
	 * @param activity The activity that wants to read it
	 * @param filename The filename to be read
     * @return The content of the file as a string
	 * @throws IOException in case of an error.
     */
	public static String readSourcecode(Activity activity, String filename) throws IOException {
		BufferedReader reader = null;
		String program = null;
		try {
			reader = new BufferedReader(
					new InputStreamReader(activity.getAssets().open(filename)));

			StringBuilder sb = new StringBuilder();

			String mLine;
			while ((mLine = reader.readLine()) != null) {
				//process line
				sb.append(mLine).append("\n");
			}

			program = sb.toString();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					//log the exception
					Log.e("PF", "close failed: " + e.getMessage());
				}
			}
		}

		return program;
	}

	public static Bitmap readIcon(Activity activity, Preset preset) {
		Bitmap icon = null;
		InputStream is = null;
		try {
			is = activity.getAssets().open(preset.iconFilename);
			icon = BitmapFactory.decodeStream(is);
		} catch (IOException e) {
			e.printStackTrace();
			//Toast.makeText(this, "Could not read icon", Toast.LENGTH_LONG).show();
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch(IOException e) {
					e.printStackTrace();
				}
			}
		}

		return icon;
	}





	public static final Preset[] PRESETS = new Preset[] {
			/* Title, Source-file, Parameters, Icon-file
			 */
			new Preset("Default", "Default.fv", "Default.png", Scale.createScaled(2), new Parameters()),

			new Preset("Default / Mandelbrot Wikipedia", "Default.fv", "DefaultMBWiki.png",
					new Scale(1.3, 0, 0, 1.3, -0.7, 0),
					new Parameters()
							.addExpr("bailouttransfer", "log(1 + value * (0.42 / 28))")
							.addExpr("laketransfer", "0")
							.addPalette("lakepalette", new Palette(new int[][]{{0xff000000}}))
							.addPalette("bailoutpalette", new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}}))),

			new Preset("Default / Celtic", "Default.fv", "DefaultCeltic.png", Scale.createScaled(2), new Parameters()
					.addExpr("function", "rabs sqr z + p")),

			new Preset("Default / Burning Ship", "Default.fv", "DefaultBurningShip.png", Scale.createScaled(2), new Parameters()
					.addExpr("function", "mandelbrot(abs z, p)")),

			new Preset("Default / Phoenix", "Default.fv", "DefaultPhoenix.png", Scale.createScaled(2), new Parameters()
					.addExpr("function", "mandelbrot(z, p.x) + zlast p.y")
					.addExpr("mandelinit", "c")
					.addBool("juliaset", true)
					.addCplx("juliapoint", new Cplx(0.5666, -0.5))),

			new Preset("Cczcpaczcp", "Cczcpaczcp.fv", "Cczcpaczcp.png", Scale.createScaled(1), new Parameters()),

			new Preset("Julia Map", "JuliaMap.fv", "JuliaMap.png", new Scale(1.3, 0, 0, 1.3, -0.7, 0), new Parameters()),

			new Preset("Branching", "Branching.fv", "Branching.png", Scale.createScaled(2), new Parameters()),

			new Preset("Branching / Curvature Inequality", "Branching.fv", "BranchingCurvature.png", Scale.createScaled(2), new Parameters()
					.addExpr("addend", "arcnorm((znext - z) / (z - zlast))")
					.addBool("interpolate_smooth_i", true)),

			new Preset("Branching / Triange Inequality", "Branching.fv", "BranchingTriangle.png", Scale.createScaled(2), new Parameters()
					.addExpr("addend", "{ var t1 = rad z ^ max_power, t2 = rad p, M = abs(t1 - t2), m = t1 + t2; (rad znext - m) / (M - m) }")),

			// Next, fold: Fractals in which a function is applied for every value in the orbit.

			new Preset("Fold", "Fold.fv", "Fold.png", Scale.createScaled(2), new Parameters()),

			new Preset("Fold / Branching", "Fold.fv", "FoldBranch.png", Scale.createScaled(2), new Parameters()
					.addExpr("foldfn", "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
			),

			new Preset("Fold / Exponential Smoothing", "Fold.fv", "FoldExp.png", Scale.createScaled(2), new Parameters()
					.addExpr("foldfn", "/cosh(rad znext + /rad(z - znext)) + foldvalue")
					.addExpr("lakevalue", "re foldvalue")
			),

			new Preset("Two Fold", "TwoFold.fv", "TwoFold.png", Scale.createScaled(2), new Parameters()),

			new Preset("Two Fold / Branching", "TwoFold.fv", "TwoFoldBranch.png", Scale.createScaled(2), new Parameters()
					.addExpr("foldfn", "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
					.addExpr("foldfn2", "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue2 }")
			),

			// .addPalette("bailoutpalette", new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}}))


			new Preset("Two Fold / Magnet 1", "TwoFold.fv", "TwoFoldMagnet1.png", new Scale(3, 0, 0, 3, 1, 0), new Parameters()
					.addExpr("function", "sqr((sqr z + p - 1) / (2z + p - 2))")
					/*.addPalette("bailoutpalette", new Palette(
							new int[][]{{0xff00ff88, 0xff008800, 0xffffff88, 0xffff8800, 0xffff2200, 0xff000088}}))
					.addPalette("lakepalette", new Palette(
							new int[][]{
									{0xff00ff88, 0xff008800, 0xffffff88, 0xffff8800, 0xffff2200, 0xff000088},
									{0xff008800, 0xff000000, 0xffffffff, 0xffffffaa, 0xff880000, 0xff000044}
							}))*/
			),

			new Preset("Two Fold / Magnet 2", "TwoFold.fv", "TwoFoldMagnet2.png", new Scale(2, 0, 0, 2, 1, 0), new Parameters()
					.addExpr("function", "sqr((z^3 + 3(p - 1)z + (p - 1)(p - 2)) / (3 sqr z + 3(p - 2)z + (p - 1)(p - 2) + 1))")
					/*.addPalette("bailoutpalette", new Palette(
							new int[][]{{0xffaa88ff, 0xff440044, 0xffffaa44, 0xffffff88, 0xffff4400, 0xff000088}}))
					.addPalette("lakepalette", new Palette(
							new int[][]{
									{0xffaa88ff, 0xff440044, 0xffffaa44, 0xffffff88, 0xffff4400, 0xff000088},
									{0xff6622ff, 0xff000000, 0xffffcc88, 0xffffffff, 0xffaa2200, 0xff0088ff},
							}))*/
			),


			// Now orbit traps. Special cases of Fold fractals
			new Preset("Min/Max Orbit Trap", "MinMaxOrbitTrap.fv", "MinMaxOrbitTrap.png", Scale.createScaled(2), new Parameters()),

			new Preset("Orbit Trap", "OrbitTrap.fv", "OrbitTrap.png", Scale.createScaled(2), new Parameters()),


			// For root finding there are 3 presets:
			new Preset("Root Finding", "RootFinding.fv", "RootFinding.png", new Scale(2, 0, 0, 2, 0, -1.5), new Parameters()),

			new Preset("Newton", "Newton.fv", "Newton.png", Scale.createScaled(2), new Parameters()),

			new Preset("Newton / SinH", "Newton.fv", "NewtonSinH.png", Scale.createScaled(5), new Parameters()
					.addExpr("function", "sinh z + p")
					.addExpr("mandelinit", "0")),

			new Preset("Secant", "Secant.fv", "Secant.png", Scale.createScaled(2), new Parameters()),

			new Preset("Complex Function", "ComplexFn.fv", "ComplexFn.png", Scale.createScaled(4), new Parameters()),
			//new Preset("Default 3D", "Default3D.fv", "Default3D.png", Scale.createScaled(2), new Parameters()),
			//new Preset("Branching 3D", "Branching3D.fv", "Branching3D.png", Scale.createScaled(2), new Parameters()),

			new Preset("Pendulum (3 Magnets)", "Pendulum3.fv", "Pendulum3.png", Scale.createScaled(4), new Parameters()),

			new Preset("Lyapunov", "Lyapunov.fv", "Lyapunov.png", new Scale(0.45, 0, 0, -0.3, 3.05, 3.7), new Parameters()),



			//new Preset("Complex Function 3D", "ComplexFn3D.fv", "ComplexFn3D.png", Scale.createScaled(4), new Parameters()),


			// Some default fractals:

			new Preset("Orbit Trap / Steiner Circles", "OrbitTrap.fv", "OrbitTrapSteiner.png", new Scale(2, 0, 0, 2, 0, 0), new Parameters()
					.addExpr("trapfn", "min(circle(0:0, 3, znext), circle(-2:0, 1, znext), circle(2:0, 1, znext), circle(-1:-1.73205, 1, znext), circle(-1:1.73205, 1, znext), circle(1:-1.73205, 1, znext), circle(1:1.73205, 1, znext))")),


			new Preset("Complex Function / Domain Coloring", "ComplexFn.fv", "ComplexFnDomain.png", Scale.createScaled(4), new Parameters()
					.addExpr("transfer", "arcnorm z : (0.6 fract (log rad z / log 2) + 0.0667)")),






	};

}
