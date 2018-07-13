package at.searles.fractal.predefined;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.fractview.AssetsHelper;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * And now for the demos/presets.
 */
public class ParameterEntry {

    public final String title;
    public final Bitmap icon;
    public final String description;
    public final Map<String, Fractal.Parameter> parameters;

    public ParameterEntry(String title, Bitmap icon, String description, Map<String, Fractal.Parameter> parameters) {
        this.title = title;
        this.icon = icon;
        this.description = description;
        this.parameters = parameters;
    }

    // Create a list of assets and icons that come with it.
    // Read private entries
    private static ParameterEntry createEntry(AssetManager am, String title, String iconFilename, String description, Fractal.ParameterMapBuilder parameters) {
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);
        return new ParameterEntry(title, icon, description, parameters.map());
    }

    private static ArrayList<ParameterEntry> _ENTRIES = null;

    /**
     * @param am At first run, the asset manager must be present to load icons. Afterwards this
     *           parameter can be null.
     * @return
     */
    public static synchronized List<ParameterEntry> entries(AssetManager am) {
        if (_ENTRIES == null) {
            // create entries.
            _ENTRIES = new ArrayList<>();

            _ENTRIES.add(createEntry(am, "Mandelbrot Set", "mandelbrot.png", "Mandelbrot Set (z^2 + p)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Wikipedia-Settings", "wiki.png", "Mandelbrot Set with Parameters from Wikipedia",
                    Fractal.parameterBuilder()
                            .add("bailoutvalue", Fractal.Type.Expr, "i + smooth_i")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + value * (0.42 / 28))")
                            .add("laketransfer", Fractal.Type.Expr, "0")
                            .add("lakepalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000000}}))
                            .add("bailoutpalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}})))
            );

            _ENTRIES.add(createEntry(am, "Burning Ship", "burningship.png", "Burning Ship Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(abs z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Celtic", "celtic.png", "Celtic Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "rabs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Tricorn", "tricorn.png", "Tricorn Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(conj z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Buffalo", "buffalo.png", "Buffalo Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "abs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Perpendicular Mandelbrot", "perpendicular_mandelbrot.png", "Perpendicular Mandelbrot Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(conj rabs z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Perpendicular Burning Ship", "perpendicular_burningship.png", "Perpendicular Burning Ship Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(iabs z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Perpendicular Celtic", "perpendicular_celtic.png", "Perpendicular Celtic Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "conj rabs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Perpendicular Buffalo", "perpendicular_buffalo.png", "Perpendicular Buffalo Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "rabs sqr iabs z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Mandel^3", "mandel3.png", "Mandelbrot Set to the power of 3",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "z^3 + p")
                            .add("max_power", Fractal.Type.Real, 3)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Mandel^4", "mandel4.png", "Mandelbrot Set to the power of 4",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "z^4 + p")
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Lambda", "lambda.png", "Lambda Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "p (1 - z) z")
                            .add("mandelinit", Fractal.Type.Expr, "0.5")
            ));

            _ENTRIES.add(createEntry(am, "Generic Lambda", "generic_lambda.png", "Lambda Fractal with parameterized maximum power",
                    Fractal.parameterBuilder()
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("function", Fractal.Type.Expr, "p (1 - z^(max_power - 1)) z")
                            .add("mandelinit", Fractal.Type.Expr, "/max_power ^ /(max_power - 1)")
            ));

            _ENTRIES.add(createEntry(am, "Simonbrot Normal", "simonbrot_normal.png", "Simonbrot ",
                    Fractal.parameterBuilder()
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("function", Fractal.Type.Expr, "sqr z abs z + p")
                    )
            );

            _ENTRIES.add(createEntry(am, "Simonbrot Inverted", "simonbrot_inverted.png", "Simonbrot with abs/sqr exchanged",
                    Fractal.parameterBuilder()
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("function", Fractal.Type.Expr, "{ var t = sqr z; t * abs t } + p")
                    )
            );

            _ENTRIES.add(createEntry(am, "Phoenix Julia Set", "phoenix.png", "Julia Set of the Phoenix Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p.x) + zlast p.y")
                            .add("mandelinit", Fractal.Type.Expr, "c")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(0.5666, -0.5))
            ));

            _ENTRIES.add(createEntry(am, "Cczcpaczcp", "ccz-2.png", "Special formula by Mark R Eggleston",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "{ def alpha = 1; def beta = 3; def gamma = 1; def delta = -1; p (alpha * z^beta + gamma * z^delta) }")
                            .add("mandelinit", Fractal.Type.Expr, "{ def alpha = 1; def beta = 3; def gamma = 1; def delta = -1; (-gamma delta / alpha beta)^/(beta - delta)}")
                            .add("max_power", Fractal.Type.Real, 3)
            ));

            _ENTRIES.add(createEntry(am, "Glynn", "glynn.png", "Glynn Fractal (Julia Set of z^1.75 + p)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "z ^ 1.75 + p")
                            .add("max_power", Fractal.Type.Real, 1.75)
                            .add("mandelinit", Fractal.Type.Expr, "0")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(-0.4))
            ));

            _ENTRIES.add(createEntry(am, "Magnet1", "magnet1.png", "Magnet 1 Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "sqr((sqr z + p - 1) / (2z + p - 2))")
                            .add("max_power", Fractal.Type.Real, 2)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Magnet2", "magnet2.png", "Magnet 2 Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "sqr((z^3 + 3(p - 1)z + (p - 1)(p - 2)) / (3 sqr z + 3(p - 2)z + (p - 1)(p - 2) + 1))")
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Mandelbrot Newton", "mandelnewton.png", "Mix of Newton Set and Mandelbrot Formula",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "sqr newton(z^3 + p, z) + p")
                            .add("max_power", Fractal.Type.Real, 2)
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "SinhZ", "sinhz.png", "Mandelbrot Set using the Sine Hyperbolicus (non-polynomial formular)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "sinh z * p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "I")
            ));

            _ENTRIES.add(createEntry(am, "CoshZ", "coshz.png", "Mandelbrot Set using the Cosine Hyperbolicus (non-polynomial formular)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "cosh z * p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Inverse Mandel", "recipmandel3.png", "Fractal of z^-3 + p (lake only)",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(2, 0, 0, 2, 0, -1.5))
                            .add("function", Fractal.Type.Expr, "z^-3 + p")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Duck fractal", "duck.png", "Duck fractal (lake only)",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(2, 0, 0, 2, 0, -1.5))
                            .add("function", Fractal.Type.Expr, "log(iabs z + p)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Newton of z^3 + p", "newton3.png", "Newton Set (lake only)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(z^3 + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Newton of z^4 - 6 * z^2 - 2 p z + p", "newton4.png", "Newton Set for a more complex formular (lake only)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(z^4 - 6 * z^2 - 2 p z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "-1")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Newton of sinh z + p", "newtonsinh.png", "Newton Set using the Sine Hyperbolicus (lake only)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(sinh z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Nova of z^3 - 1", "nova3.png", "Nova fractal for power 3 (variation of the newton approximation, lake only)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(z^3 - 1, z) + p")
                            .add("mandelinit", Fractal.Type.Expr, "1")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Nova z - R (z^power - 1)/(4 * z^3) + p", "nova34.png", "Generic Nova Formular (lake only)",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "{ def R=3; def power=4; z - R (z^power - 1) / derive(z^4 - 1, z) + p }")
                            .add("mandelinit", Fractal.Type.Expr, "{ def R=3; def power=4; (-(R power-R)/(R-power))^/power}")
            ));

            // Stop Fractal Types.

            // Next: Branching Specialities

            _ENTRIES.add(createEntry(am, "Curvature Inequality", "curvature.png", "Branching Addend",
                    Fractal.parameterBuilder()
                            .add("addend", Fractal.Type.Expr, "arcnorm((znext - z) / (z - zlast))")
                    //.add("interpolate_smooth_i", Fractal.Type.Bool, true)
            ));

            _ENTRIES.add(createEntry(am, "Triange Inequality", "triangleinequality.png", "Branching Addend",
                    Fractal.parameterBuilder()
                            .add("addend", Fractal.Type.Expr, "{ var t1 = rad z ^ max_power, t2 = rad p, M = abs(t1 - t2), m = t1 + t2; (rad znext - m) / (M - m) }")
            ));

            // Fold stuff
            _ENTRIES.add(createEntry(am, "Gaussian Integer Min", "gaussianintmin.png", "Minimum distance to a gaussian integer (fold)",
                    Fractal.parameterBuilder()
                            .add("foldinit", Fractal.Type.Expr, "bailout")
                            .add("foldfn", Fractal.Type.Expr, "min(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("bailoutvalue", Fractal.Type.Expr, "foldvalue.x")
                            .add("lakevalue", Fractal.Type.Expr, "foldvalue.x")
            ));

            _ENTRIES.add(createEntry(am, "Gaussian Integer Max", "gaussianintmax.png", "Maximum distance to a gaussian integer (fold)",
                    Fractal.parameterBuilder()
                            .add("foldinit", Fractal.Type.Expr, "0")
                            .add("foldfn", Fractal.Type.Expr, "max(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("bailoutvalue", Fractal.Type.Expr, "foldvalue.x")
                            .add("lakevalue", Fractal.Type.Expr, "foldvalue.x")
            ));

            _ENTRIES.add(createEntry(am, "Gaussian Integer Mixed", "gaussianinttwofold.png", "Minimum (bailout) and maximum (lake) distance to a gaussian integer (two-fold)",
                    Fractal.parameterBuilder()
                            .add("foldinit", Fractal.Type.Expr, "bailout")
                            .add("foldfn", Fractal.Type.Expr, "min(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("foldinit2", Fractal.Type.Expr, "0")
                            .add("foldfn2", Fractal.Type.Expr, "max(dist(znext, floor(znext + 0.5:0.5)), foldvalue2)")
            ));

            _ENTRIES.add(createEntry(am, "Distance Estimation (Mandelbrot)", "distance_estimation.png", "Distance estimation for the mandelbrot set (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "2 znext foldvalue + 1")
                            .add("bailoutvalue", Fractal.Type.Expr, "rad znext / rad foldvalue / 2 * log rad znext")
            ));

            // FIXME Lyapunov

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Bailout and Lake)", "expsmooth.png", "Exponential Smoothing for bailout and lake (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Bailout)", "expsmoothbailout.png", "Exponential Smoothing for bailout (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh rad znext + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Lake)", "expsmoothlake.png", "Exponential Smoothing for lake",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(/dist(znext, z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Two Fold)", "expsmoothtwofold.png", "Exponential Smoothing for bailout (first fold) and lake (second fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh rad znext + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
                            .add("foldfn2", Fractal.Type.Expr, "/cosh(/dist(znext, z)) + foldvalue2")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue2.x)")
            ));

            _ENTRIES.add(createEntry(am, "Smooth Drawing (Lake)", "smoothlake.png", "Smooth drawing for the lake based on atan (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "dist(znext, z) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "1.3 atan foldvalue.x / PI"))
            );

            _ENTRIES.add(createEntry(am, "Fold-Branching (Bailout and Lake)", "foldbranching.png", "Branching for fold for bailout and lake (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc (z - znext)) / (12 + rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching (Bailout)", "foldbranchingbailout.png", "Branching for fold for bailout (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc znext) / (12 + rad znext) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Geometry Pattern (Bailout)", "foldgeometrybailout.png", "Branching for fold for bailout (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/line(0:0, 1:0, znext) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching (Lake)", "foldbranchinglake.png", "Branching for fold for lake (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc(z - znext)) / (12 + /dist(znext, z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching Alternative (Lake)", "foldbranchinglake2.png", "Branching based on atan for fold for lake (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 4 arc(z - znext)) * (1 - 2 atan(12 + /dist(znext, z)) / PI) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Geometry Pattern (Lake)", "foldgeometrylake.png", "Branching for fold for bailout (fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/line(0:0, 1:0, /(znext - z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching (Two Fold)", "foldbranchingtwofold.png", "Branching for bailout (first fold) and lake (second fold) (two fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc znext) / (12 + rad znext) + foldvalue")
                            .add("foldfn2", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc(z - znext)) / (12 + /dist(znext, z)) + foldvalue2")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue2.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing and Branching (Two Fold)", "twofoldexpsmoothbranch.png", "Combination of exponential smoothing and branching (two fold)",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x + foldvalue2.x)")
                            .add("foldfn2", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc (z - znext)) / (12 + rad znext + /dist(znext, z)) + foldvalue2")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x + foldvalue2.x)")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + foldvalue2.x) : value")
                            .add("laketransfer", Fractal.Type.Expr, "log(1 + foldvalue2.x) : value")
            ));


            // Next: Orbit Traps
            _ENTRIES.add(createEntry(am, "Cross Trap", "crosstrap.png", "Orbit Trap of a centered cross",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "min(line(0:0, 1:0, znext), line(0:0, 0:1, znext))")
            ));

            _ENTRIES.add(createEntry(am, "Two Boxes Trap", "twoboxestrap.png", "Orbit Trap of two boxes",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "min(box(-2:-2, 0.5:0.5, znext), box(2:2, -0.5:-0.5, znext))")
            ));

            _ENTRIES.add(createEntry(am, "Steiner Circles Trap", "steinertrap.png", "Orbit Trap of 6 Steiner Circles",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "min(circle(0:0, 3, znext), circle(-2:0, 1, znext), circle(2:0, 1, znext), circle(-1:-1.73205, 1, znext), circle(-1:1.73205, 1, znext), circle(1:-1.73205, 1, znext), circle(1:1.73205, 1, znext))")
            ));

            _ENTRIES.add(createEntry(am, "Min/Max Neighbors", "minmaxneighbor.png", "Orbit trap determining the distance of neighbors in the orbit",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "dist(znext, z)")
            ));

            _ENTRIES.add(createEntry(am, "Min/Max Gaussian Integer", "minmaxgaussianint.png", "Orbit trap determining the distance to the next gaussian integer",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "dist(znext, floor(znext + 0.5:0.5))")
            ));



            // Next Lyapunov Fractals
            _ENTRIES.add(createEntry(am, "Edge of BA-Lyapunov Fractal", "lyapunovba.png", "Part of Lyapunov fractal",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(1, 0, 0, -1, 3, 3))
                            .add("lyastring", Fractal.Type.Expr, "[b,a]")
            ));

            _ENTRIES.add(createEntry(am, "Edge of BBABA-Lyapunov Fractal", "lyapunovbbaba.png", "Part of Lyapunov fractal",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(1, 0, 0, -1, 3, 3))
                            .add("lyastring", Fractal.Type.Expr, "[b,b,a,b,a]")
            ));

            _ENTRIES.add(createEntry(am, "Zirkony Zity", "zirkony.png", "Part of Lyapunov fractal called Zirkony Zity",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(0.45, 0, 0, -0.3, 3.05, 3.7))
                            .add("lyastring", Fractal.Type.Expr, "[a,a,a,a,a,a,b,b,b,b,b,b]")
            ));

            _ENTRIES.add(createEntry(am, "Domain Coloring", "domain.png", "Domain Coloring for Complex Functions",
                    Fractal.parameterBuilder()
                            .add("transfer", Fractal.Type.Expr, "arcnorm z : (0.6 fract (log rad z / log 2) + 0.0667)")
            ));
        }

        return _ENTRIES;
    }
}
