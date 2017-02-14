package at.searles.fractview;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import at.searles.fractview.fractal.Fractal;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * Created by searles on 24.01.17.
 */

public class AssetsHelper {
    public static final Scale DEFAULT_SCALE = new Scale(2, 0, 0, 2, 0, 0);

    /**
     * Try to read content of assets-folder
     * @param am The asset manager that should be used
     * @param filename The filename to be read
     * @return The content of the file as a string, null in case of an error
     */
    public static String readSourcecode(AssetManager am, String filename) {
        BufferedReader reader = null;
        String program = null;

        try {
            reader = new BufferedReader(
                    new InputStreamReader(am.open(filename)));

            StringBuilder sb = new StringBuilder();

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                //process line
                sb.append(mLine).append("\n");
            }

            program = sb.toString();
        } catch(IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                    Log.e("PF", "close failed!");
                    e.printStackTrace();
                }
            }
        }

        return program;
    }

    /**
     * Reads an icon from assets
     * @param am Asset Manager to access it
     * @param iconFilename Filename of the icon.
     * @return null if there is no such file. The error message is logged
     */
    public static Bitmap readIcon(AssetManager am, String iconFilename) {
        if(iconFilename == null) return null;

        Bitmap icon = null;
        InputStream is = null;

        try {
            is = am.open("icons/" + iconFilename);
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

    // Create a list of assets and icons that come with it.
    // Read private entries
    public static ProgramAsset e(AssetManager am, String title, String iconFilename, String description, String sourceFilename) {
        String sourceCode = AssetsHelper.readSourcecode(am, sourceFilename);
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        if(sourceCode == null/* || icon == null*/) {
            throw new IllegalArgumentException("bad asset: " + title);
        }

        return new ProgramAsset(title, icon, description, sourceCode);
    }

    // And now for the presets.
    public static class ProgramAsset implements FractalEntry {
        public final String title;
        public final Bitmap icon;
        public final String description;
        public final String source;

        private ProgramAsset(String title, Bitmap icon, String description, String source) {
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.source = source;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public Bitmap icon() {
            return icon;
        }

        @Override
        public String description() {
            return description;
        }
    }

    private static ArrayList<ProgramAsset> _ENTRIES = null;

    public static synchronized ArrayList<ProgramAsset> entries(AssetManager am) {
        if (_ENTRIES == null) {
            // create entries.
            _ENTRIES = new ArrayList<>();

            // grouped : the ones with maxpower
            _ENTRIES.add(e(am, "Default", "default.png", "Basic fractal with bailout and lake coloring", "Default.fv"));
            _ENTRIES.add(e(am, "Julia Map", "juliamap.png", "Variation of \"Default\" that shows a map of julia sets.", "JuliaMap.fv"));
            _ENTRIES.add(e(am, "Branching", "branching.png", "\"Default\" with an addend for average coloring methods for polynom formulas", "Branching.fv"));
            _ENTRIES.add(e(am, "Cczcpaczcp", "ccz.png", "Default with a built-in special formula by Mark R Eggleston, called Cczcpaczcp", "Cczcpaczcp.fv"));

            // the ones with orbit traps
            _ENTRIES.add(e(am, "Orbit Trap", "orbittrap.png", "\"Default\" with an orbit trap", "OrbitTrap.fv"));
            _ENTRIES.add(e(am, "Frame Orbit Trap", "frameorbittrap.png", "\"Default\" with an orbit trap", "FrameOrbitTrap.fv"));
            _ENTRIES.add(e(am, "Min/Max Trap", "minmaxtrap.png", "Picks the maximum distance to the orbit trap", "MinMaxOrbitTrap.fv"));

            // the ones with fold
            _ENTRIES.add(e(am, "Fold", "fold.png", "\"Default\" with a more general addend (fold), also suitable for stripe coloring methods of non-polynomial fractals", "Fold.fv"));
            _ENTRIES.add(e(am, "Two Folds", "twofolds.png", "\"Default\" with two fold functions", "TwoFold.fv"));
            _ENTRIES.add(e(am, "Lake Fold", "lakefold.png", "Draws only the lake of a fractal, thus useful for bounded fractals like Duck or Newton", "Lake.fv"));

            // Special Lake Fold ones
            _ENTRIES.add(e(am, "Newton", "newton.png", "Newton method for root finding fractals", "Newton.fv"));
            _ENTRIES.add(e(am, "Nova", "nova.png", "Nova fractal defined by z - R * (z^power + argument) / (z^power + argument)' + p", "Nova.fv"));
            _ENTRIES.add(e(am, "Secant", "secant.png", "Secant method for root finding fractals", "Secant.fv"));

            // Completely different onces
            _ENTRIES.add(e(am, "Lyapunov", "lyapunov.png", "Lyapunov fractals", "Lyapunov.fv"));

            _ENTRIES.add(e(am, "Pendulum (Multiple Magnets)", "pendulum.png", "Magnetic Pendulum Simulation with 3 Magnets", "Pendulum.fv"));
            _ENTRIES.add(e(am, "Pendulum (3 Magnets)", "pendulum3.png", "Magnetic Pendulum Simulation with 3 Magnets", "Pendulum3.fv"));

            _ENTRIES.add(e(am, "Complex Function", "complexfn.png", "Drawing of Complex function (Color Wheel method by default)", "ComplexFn.fv"));
        }

        return _ENTRIES;
    }

    // Create a list of assets and icons that come with it.
    // Read private entries
    private static ParametersAsset e(AssetManager am, String title, String iconFilename, String description, Fractal.Parameters parameters) {
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        /*if(icon == null) {
            throw new IllegalArgumentException("bad asset: " + title);
        }*/

        return new ParametersAsset(title, icon, description, null, parameters);
    }

    public static ParametersAsset e(AssetManager am, String title, String iconFilename, String description, Scale scale, Fractal.Parameters parameters) {
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        /*if(icon == null) {
            throw new IllegalArgumentException("bad asset: " + title);
        }*/

        return new ParametersAsset(title, icon, description, scale, parameters);
    }

    // And now for the presets.
    public static class ParametersAsset implements FractalEntry {
        public final String title;
        public final Bitmap icon;
        public final String description;
        public final Scale scale; // may be null
        public final Fractal.Parameters parameters;

        private ParametersAsset(String title, Bitmap icon, String description, Scale scale, Fractal.Parameters parameters) {
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.scale = scale;
            this.parameters = parameters;
        }

        @Override
        public String title() {
            return title;
        }

        @Override
        public Bitmap icon() {
            return icon;
        }

        @Override
        public String description() {
            return description;
        }
    }

    private static ArrayList<ParametersAsset> _PARAMETER_ENTRIES = null;

    public static synchronized ArrayList<ParametersAsset> parameterEntries(AssetManager am) {
        if (_PARAMETER_ENTRIES == null) {
            // create entries.
            _PARAMETER_ENTRIES = new ArrayList<>();

            _PARAMETER_ENTRIES.add(e(am, "Mandelbrot Set", "mandelbrot.png", "Mandelbrot SetBurning Ship Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Wikipedia-Settings", "wiki.png", "Parameters from the Wikipedia - Mandelbrot - Entry",
                    new Fractal.Parameters()
                            .add("bailoutvalue", Fractal.Type.Expr, "i + smooth_i")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + value * (0.42 / 28))")
                            .add("laketransfer", Fractal.Type.Expr, "0")
                            .add("lakepalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000000}}))
                            .add("bailoutpalette", Fractal.Type.Palette, new Palette(new int[][]{{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231}})))
            );

            _PARAMETER_ENTRIES.add(e(am, "Burning Ship", "burningship.png", "Burning Ship Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(abs z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Celtic", "celtic.png", "Celtic Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "rabs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Tricorn", "tricorn.png", "Tricorn Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(conj z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Buffalo", "buffalo.png", "Buffalo Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "abs sqr z + p")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Mandel^3", "mandel3.png", "Mandelbrot Set to the power of 3",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z^3 + p")
                            .add("max_power", Fractal.Type.Real, 3)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Mandel^4", "mandel4.png", "Mandelbrot Set to the power of 3",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z^4 + p")
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Lambda", "lambda.png", "Lambda Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "p (1 - z) z")
                            .add("mandelinit", Fractal.Type.Expr, "0.5")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Phoenix Julia Set", "phoenix.png", "Julia Set of the Phoenix Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p.x) + zlast p.y")
                            .add("mandelinit", Fractal.Type.Expr, "c")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(0.5666, -0.5))
            ));

            _PARAMETER_ENTRIES.add(e(am, "Cczcpaczcp", "ccz-2.png", "Buffalo Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "p (z^3 + z^-1)")
                            .add("mandelinit", Fractal.Type.Expr, "{ def alpha = 1; def beta = 3; def gamma = 1; def delta = -1; (-gamma delta / alpha beta)^/(beta - delta)}")
                            .add("max_power", Fractal.Type.Real, 3)
            ));

            _PARAMETER_ENTRIES.add(e(am, "Glynn", "glynn.png", "Glynn fractal (a julia set of mandel^1.6",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z ^ 1.75 + p")
                            .add("max_power", Fractal.Type.Real, 1.75)
                            .add("mandelinit", Fractal.Type.Expr, "0")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(-0.4))
            ));

            _PARAMETER_ENTRIES.add(e(am, "Magnet1", "magnet1.png", "Magnet 1 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr((sqr z + p - 1) / (2z + p - 2))")
                            .add("max_power", Fractal.Type.Real, 2)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Magnet2", "magnet2.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr((z^3 + 3(p - 1)z + (p - 1)(p - 2)) / (3 sqr z + 3(p - 2)z + (p - 1)(p - 2) + 1))")
                            .add("max_power", Fractal.Type.Real, 4)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Mandelbrot Newton", "mandelnewton.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sqr newton(z^3 + p, z) + p")
                            .add("max_power", Fractal.Type.Real, 2)
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            /*_PARAMETER_ENTRIES.add(e(am, "ZtanZ", "Magnet2.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z tan z + p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));*/

            _PARAMETER_ENTRIES.add(e(am, "SinhZ", "sinhz.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "sinh z * p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "I")
            ));

            _PARAMETER_ENTRIES.add(e(am, "CoshZ", "coshz.png", "Magnet 2 Fractal",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "cosh z * p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Inverse Mandel", "recipmandel3.png", "A nice variation for newton",
                    new Scale(2, 0, 0, 2, 0, -1.5),
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "z^-3 + p")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Duck fractal", "duck.png", "A nice variation for newton",
                    new Scale(2, 0, 0, 2, 0, -1.5),
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "log(iabs z + p)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Newton of z^3 + p", "newton3.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(z^3 + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Newton of z^4 - 6 * z^2 - 2 p z + p", "newton4.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(z^4 - 6 * z^2 - 2 p z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "-1")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Newton of sinh z + p", "newtonsinh.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(sinh z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Nova of z^3 - 1", "nova3.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "newton(z^3 - 1, z) + p")
                            .add("mandelinit", Fractal.Type.Expr, "1")
            ));

            _PARAMETER_ENTRIES.add(e(am, "(Lake) Nova z - R (z^power - 1)/(4 * z^3) + p", "nova34.png", "Mandelinit should be \"(p(a n-a)/(a-n))^/n\", provided p does not depend on z.",
                    new Fractal.Parameters()
                            .add("function", Fractal.Type.Expr, "{ def R=3; def power=4; z - R (z^power - 1) / derive(z^4 - 1, z) + p }")
                            .add("mandelinit", Fractal.Type.Expr, "{ def R=3; def power=4; (-(R power-R)/(R-power))^/power}")
            ));

            // Stop Fractal Types.

            // Next: Branching Specialities

            _PARAMETER_ENTRIES.add(e(am, "Curvature Inequality", "curvature.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("addend", Fractal.Type.Expr, "arcnorm((znext - z) / (z - zlast))")
                            //.add("interpolate_smooth_i", Fractal.Type.Bool, true)
            ));

            _PARAMETER_ENTRIES.add(e(am, "Triange Inequality", "triangleinequality.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("addend", Fractal.Type.Expr, "{ var t1 = rad z ^ max_power, t2 = rad p, M = abs(t1 - t2), m = t1 + t2; (rad znext - m) / (M - m) }")
            ));

            // Fold stuff
            _PARAMETER_ENTRIES.add(e(am, "Gaussian Integer Min", "gaussianintmin.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldinit", Fractal.Type.Expr, "bailout")
                            .add("foldfn", Fractal.Type.Expr, "min(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("bailoutvalue", Fractal.Type.Expr, "foldvalue.x")
                            .add("lakevalue", Fractal.Type.Expr, "foldvalue.x")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Gaussian Integer Max", "gaussianintmax.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldinit", Fractal.Type.Expr, "0")
                            .add("foldfn", Fractal.Type.Expr, "max(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("bailoutvalue", Fractal.Type.Expr, "foldvalue.x")
                            .add("lakevalue", Fractal.Type.Expr, "foldvalue.x")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Gaussian Integer (Two-Fold)", "gaussianinttwofold.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldinit", Fractal.Type.Expr, "bailout")
                            .add("foldfn", Fractal.Type.Expr, "min(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("foldinit2", Fractal.Type.Expr, "0")
                            .add("foldfn2", Fractal.Type.Expr, "max(dist(znext, floor(znext + 0.5:0.5)), foldvalue2)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing (Bailout and Lake)", "expsmooth.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing (Bailout)", "expsmoothbailout.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "/cosh rad znext + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing (Lake)", "expsmoothlake.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(/dist(znext, z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing (Two Fold)", "expsmoothtwofold.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "/cosh rad znext + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
                            .add("foldfn2", Fractal.Type.Expr, "/cosh(/dist(znext, z)) + foldvalue2")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue2.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Smooth Drawing (Lake)", "smoothlake.png", "",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "dist(znext, z) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "1.3 atan foldvalue.x / PI"))
            );

            _PARAMETER_ENTRIES.add(e(am, "Branching (Bailout and Lake)", "foldbranching.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc (z - znext)) / (12 + rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Fold-Branching (Bailout)", "foldbranchingbailout.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc znext) / (12 + rad znext) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Fold-Branching (Lake)", "foldbranchinglake.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc(z - znext)) / (12 + /dist(znext, z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Fold-Branching Alternative (Lake)", "foldbranchinglake2.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 4 arc(z - znext)) * (1 - 2 atan(12 + /dist(znext, z)) / PI) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Fold-Branching (Two Fold)", "foldbranchingtwofold.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc znext) / (12 + rad znext) + foldvalue")
                            .add("foldfn2", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc(z - znext)) / (12 + /dist(znext, z)) + foldvalue2")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue2.x)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Exponential Smoothing and Branching (Two Fold)", "twofoldexpsmoothbranch.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x + foldvalue2.x)")
                            .add("foldfn2", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc (z - znext)) / (12 + rad znext + /dist(znext, z)) + foldvalue2")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x + foldvalue2.x)")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + foldvalue2.x) : value")
                            .add("laketransfer", Fractal.Type.Expr, "log(1 + foldvalue2.x) : value")
            ));


            // Next: Orbit Traps
            _PARAMETER_ENTRIES.add(e(am, "Cross Trap", "crosstrap.png", "Orbit Trap of 6 Steiner Circles",
                    new Fractal.Parameters()
                            .add("trapfn", Fractal.Type.Expr, "min(line(0:0, 1:0, znext), line(0:0, 0:1, znext))")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Two Boxes Trap", "twoboxestrap.png", "Orbit Trap of 6 Steiner Circles",
                    new Fractal.Parameters()
                            .add("trapfn", Fractal.Type.Expr, "min(box(-2:-2, 0.5:0.5, znext), box(2:2, -0.5:-0.5, znext))")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Steiner Circles Trap", "steinertrap.png", "Orbit Trap of 6 Steiner Circles",
                    new Fractal.Parameters()
                            .add("trapfn", Fractal.Type.Expr, "min(circle(0:0, 3, znext), circle(-2:0, 1, znext), circle(2:0, 1, znext), circle(-1:-1.73205, 1, znext), circle(-1:1.73205, 1, znext), circle(1:-1.73205, 1, znext), circle(1:1.73205, 1, znext))")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Min/Max Neighbors", "minmaxneighbor.png", "Orbit Trap of 6 Steiner Circles",
                    new Fractal.Parameters()
                            .add("trapfn", Fractal.Type.Expr, "dist(znext, z)")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Min/Max Gaussian Integer", "minmaxgaussianint.png", "A nice variation for newton",
                    new Fractal.Parameters()
                            .add("trapfn", Fractal.Type.Expr, "dist(znext, floor(znext + 0.5:0.5))")
            ));



            // Next Lyapunov Fractals
            _PARAMETER_ENTRIES.add(e(am, "Edge of BA-Lyapunov Fractal", "lyapunovba.png", "Part of Lyapunov fractal called Zirkony Zity",
                    new Scale(1, 0, 0, -1, 3, 3),
                    new Fractal.Parameters()
                            .add("lyastring", Fractal.Type.Expr, "[b,a]")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Edge of BBABA-Lyapunov Fractal", "lyapunovbbaba.png", "Part of Lyapunov fractal called Zirkony Zity",
                    new Scale(1, 0, 0, -1, 3, 3),
                    new Fractal.Parameters()
                            .add("lyastring", Fractal.Type.Expr, "[b,b,a,b,a]")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Zirkony Zity", "zirkony.png", "Part of Lyapunov fractal called Zirkony Zity",
                    new Scale(0.45, 0, 0, -0.3, 3.05, 3.7),
                    new Fractal.Parameters()
                            .add("lyastring", Fractal.Type.Expr, "[a,a,a,a,a,a,b,b,b,b,b,b]")
            ));

            _PARAMETER_ENTRIES.add(e(am, "Domain Coloring", "domain.png", "Domain Coloring for Complex Functions",
                    new Fractal.Parameters()
                            .add("transfer", Fractal.Type.Expr, "arcnorm z : (0.6 fract (log rad z / log 2) + 0.0667)")
            ));
        }

        return _PARAMETER_ENTRIES;
    }
}
