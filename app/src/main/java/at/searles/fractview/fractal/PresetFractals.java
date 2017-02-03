package at.searles.fractview.fractal;

import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

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
		public Fractal.Parameters parameters;

		private Preset(String title, String filename, String iconFilename, Scale scale, Fractal.Parameters parameters) {
			this.title = title;
			this.filename = filename;
			this.iconFilename = iconFilename;
			this.scale = scale;
			this.parameters = parameters;
		}
	}






	public static final Preset[] PRESETS = new Preset[] {
			/* Title, Source-file, Parameters, Icon-file
			 */
			new Preset("Default", "Default.fv", "Default.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Default / Mandelbrot Wikipedia", "Default.fv", "DefaultMBWiki.png",
					new Scale(1.3, 0, 0, 1.3, -0.7, 0),
					new Fractal.Parameters()
							.add("bailouttransfer", Fractal.Type.Expr, "log(1 + value * (0.42 / 28))")
							.add("laketransfer", Fractal.Type.Expr, "0")
							.add("lakepalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000000}}))
							.add("bailoutpalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}}))),

			new Preset("Default / Celtic", "Default.fv", "DefaultCeltic.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("function", Fractal.Type.Expr, "rabs sqr z + p")),

			new Preset("Default / Burning Ship", "Default.fv", "DefaultBurningShip.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("function", Fractal.Type.Expr, "mandelbrot(abs z, p)")),

			new Preset("Default / Phoenix", "Default.fv", "DefaultPhoenix.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("function", Fractal.Type.Expr, "mandelbrot(z, p.x) + zlast p.y")
					.add("mandelinit", Fractal.Type.Expr, "c")
					.add("juliaset", Fractal.Type.Bool, true)
					.add("juliapoint", Fractal.Type.Cplx, new Cplx(0.5666, -0.5))),

			new Preset("Cczcpaczcp", "Cczcpaczcp.fv", "Cczcpaczcp.png", Scale.createScaled(1), new Fractal.Parameters()),

			new Preset("Julia Map", "JuliaMap.fv", "JuliaMap.png", new Scale(1.3, 0, 0, 1.3, -0.7, 0), new Fractal.Parameters()),

			new Preset("Branching", "Branching.fv", "Branching.png", Scale.createScaled(2), new Fractal.Parameters()),

			// Next, fold: Fractals in which a function is applied for every value in the orbit.

			new Preset("Fold", "Fold.fv", "Fold.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Two Fold", "TwoFold.fv", "TwoFold.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Branching / Curvature Inequality", "Branching.fv", "BranchingCurvature.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("addend", Fractal.Type.Expr, "arcnorm((znext - z) / (z - zlast))")
					.add("interpolate_smooth_i", Fractal.Type.Bool, true)),

			new Preset("Branching / Triange Inequality", "Branching.fv", "BranchingTriangle.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("addend", Fractal.Type.Expr, "{ var t1 = rad z ^ max_power, t2 = rad p, M = abs(t1 - t2), m = t1 + t2; (rad znext - m) / (M - m) }")),

			new Preset("Fold / Branching", "Fold.fv", "FoldBranch.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
			),

			new Preset("Fold / Exponential Smoothing", "Fold.fv", "FoldExp.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /rad(z - znext)) + foldvalue")
					.add("lakevalue", Fractal.Type.Expr, "re foldvalue")
			),

			new Preset("Two Fold / Branching", "TwoFold.fv", "TwoFoldBranch.png", Scale.createScaled(2), new Fractal.Parameters()
					.add("foldfn", Fractal.Type.Expr, "(1 + sin 6 arc znext) / 2 (rad znext + 4) + foldvalue")
					.add("foldfn2", Fractal.Type.Expr, "{ var dz = z - znext; (0.5 + 0.5 sin 6 arc dz) rad dz + foldvalue2 }")
			),

			// .addPalette("bailoutpalette", new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}}))



			// Now orbit traps. Special cases of Fold fractals
			new Preset("Min/Max Orbit Trap", "MinMaxOrbitTrap.fv", "MinMaxOrbitTrap.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Orbit Trap", "OrbitTrap.fv", "OrbitTrap.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Secant", "Secant.fv", "Secant.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Complex Function", "ComplexFn.fv", "ComplexFn.png", Scale.createScaled(4), new Fractal.Parameters()),
			//new Preset("Default 3D", "Default3D.fv", "Default3D.png", Scale.createScaled(2), new Fractal.Parameters()),
			//new Preset("Branching 3D", "Branching3D.fv", "Branching3D.png", Scale.createScaled(2), new Fractal.Parameters()),

			new Preset("Pendulum (3 Magnets)", "Pendulum3.fv", "Pendulum3.png", Scale.createScaled(4), new Fractal.Parameters()),



			//new Preset("Complex Function 3D", "ComplexFn3D.fv", "ComplexFn3D.png", Scale.createScaled(4), new Fractal.Parameters()),


			// Some default fractals:

			new Preset("Orbit Trap / Steiner Circles", "OrbitTrap.fv", "OrbitTrapSteiner.png", new Scale(2, 0, 0, 2, 0, 0), new Fractal.Parameters()
					.add("trapfn", Fractal.Type.Expr, "min(circle(0:0, 3, znext), circle(-2:0, 1, znext), circle(2:0, 1, znext), circle(-1:-1.73205, 1, znext), circle(-1:1.73205, 1, znext), circle(1:-1.73205, 1, znext), circle(1:1.73205, 1, znext))")),


			new Preset("Complex Function / Domain Coloring", "ComplexFn.fv", "ComplexFnDomain.png", Scale.createScaled(4), new Fractal.Parameters()
					.add("transfer", Fractal.Type.Expr, "arcnorm z : (0.6 fract (log rad z / log 2) + 0.0667)")),
	};
}
