package at.searles.fractview;

import android.app.Activity;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;

/**
 * This activity fetches a fractal and overrides its data/parameters
 * with a preset value, then it returns the new fractal. Data
 * are always merged here.
 */
public class ParametersListActivity extends Activity {

    private Fractal inFractal;

    // FIXME: First current, then assets, then elements stored in shared preference.
    // FIXME: Show only those, that are compilable with source


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preset_parameters);

        Intent intent = getIntent();
        this.inFractal = BundleAdapter.bundleToFractal(intent.getBundleExtra(SourcesListActivity.FRACTAL_INDENT_LABEL));

        // parse fractal
        try {
            inFractal.parse();
        } catch (CompileException e) {
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }

        ListView lv = (ListView) findViewById(R.id.fractalListView);

        final ParameterListAdapter adapter = new ParameterListAdapter(this, inFractal);

        lv.setAdapter(adapter);

        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                ParameterEntry selected = adapter.getItem(index);

                Fractal outFractal = inFractal.copyNewData(selected.parameters, true);

                Intent data = new Intent();

                data.putExtra(SourcesListActivity.FRACTAL_INDENT_LABEL, BundleAdapter.fractalToBundle(outFractal));
                setResult(1, data);
                finish();
            }
        });

        Button closeButton = (Button) findViewById(R.id.closeButton);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // end this activity.
                ParametersListActivity.this.finish();
            }
        });
    }

    private void initEntries() {
        // Fractal is parsed in onCreate.
        /*
         * Entry 0: Current.
         * Entry 1-n: Presets. Options allow to edit. If edited, then 'current' is set to '[name] (modified)'
         * Entry n+1-m: Parameter sets in preferences.
         * Since not all parameter sets will be useful, they are filtered out. For this purpose,
         * the inFractal's source code is parsed and it is checked whether for all parameters in
         * the current parameter map there exists one corresponding entry in the default data.
         */
    }

    private static class ParameterListAdapter extends FractalListAdapter<ParameterEntry> {

        private ParameterEntry inEntry;
        private ArrayList<ParameterEntry> customEntries;
        // FIXME private final SharedPreferences prefs;

        public ParameterListAdapter(Activity context, Fractal inFractal) {
            super(context);
            inEntry = new ParameterEntry("Current", null, "", inFractal.parameterMap());
            // FIXME put PREFS_NAME into resource file
            /*this.prefs = context.getSharedPreferences(
                    SourceEditorActivity.PREFS_NAME,
                    Context.MODE_PRIVATE);
            initEntries(context.getAssets());
            initializeCustomEntries();*/
        }

        private void initializeCustomEntries() {
            if(this.customEntries == null) {
                this.customEntries = new ArrayList<>();
            } else {
                this.customEntries.clear();
            }

            // No format yet for prefs.

            /*for(String key : prefs.getAll().keySet()) {
                String source = prefs.getString(key, null);

                if(source == null) {
                    Log.e(getClass().getName(), "shared prefs contains entry " + key + " but no string");
                } else {
                    this.customEntries.add(new SourcesListActivity.SourceEntry(key, null, null, source));
                }
            }*/
        }

        @Override
        public int getCount() {
            return 1 + _ENTRIES.size();
        }

        @Override
        public ParameterEntry getItem(int position) {
            if(position == 0) {
                return inEntry;
            } else {
                return _ENTRIES.get(position - 1);
            }
        }

        @Override
        public String getTitle(int position) {
            return getItem(position).title;
        }

        @Override
        public Bitmap getIcon(int position) {
            return getItem(position).icon;
        }

        @Override
        public String getDescription(int position) {
            return getItem(position).description;
        }

        @Override
        public void showOptions(int position) {
            // Edit

            // Rename/Delete if comes from shared preferences

            // Copy to Clipboard
        }
    }


    // ====== Predefined parameter sets ======

    // Create a list of assets and icons that come with it.
    // Read private entries
    private static ParameterEntry createEntry(AssetManager am, String title, String iconFilename, String description, Fractal.ParameterMapBuilder parameters) {
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        /*if(icon == null) {
            throw new IllegalArgumentException("bad asset: " + title);
        }*/

        return new ParameterEntry(title, icon, description, parameters.map());
    }

    // And now for the presets.
    public static class ParameterEntry {
        public final String title;
        public final Bitmap icon;
        public final String description;
        public final Map<String, Fractal.Parameter> parameters;

        private ParameterEntry(String title, Bitmap icon, String description, Map<String, Fractal.Parameter> parameters) {
            this.title = title;
            this.icon = icon;
            this.description = description;
            this.parameters = parameters;
        }
    }

    private static ArrayList<ParameterEntry> _ENTRIES = null;

    public static synchronized void initEntries(AssetManager am) {
        if (_ENTRIES == null) {
            // create entries.
            _ENTRIES = new ArrayList<>();

            _ENTRIES.add(createEntry(am, "Mandelbrot Set", "mandelbrot.png", "Mandelbrot SetBurning Ship Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "Wikipedia-Settings", "wiki.png", "Parameters from the Wikipedia - Mandelbrot - Entry",
                    Fractal.parameterBuilder()
                            .add("bailoutvalue", Fractal.Type.Expr, "i + smooth_i")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + value * (0.42 / 28))")
                            .add("laketransfer", Fractal.Type.Expr, "0")
                            .add("lakepalette", Fractal.Type.Palette, new Palette(1, 1, new int[]{0xff000000}))
                            .add("bailoutpalette", Fractal.Type.Palette, new Palette(5, 1, new int[]{0xff000764, 0xff206bcb, 0xffedffff, 0xffffaa00, 0xff310231})))
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

            _ENTRIES.add(createEntry(am, "Mandel^4", "mandel4.png", "Mandelbrot Set to the power of 3",
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

            _ENTRIES.add(createEntry(am, "Phoenix Julia Set", "phoenix.png", "Julia Set of the Phoenix Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "mandelbrot(z, p.x) + zlast p.y")
                            .add("mandelinit", Fractal.Type.Expr, "c")
                            .add("juliaset", Fractal.Type.Bool, true)
                            .add("juliapoint", Fractal.Type.Cplx, new Cplx(0.5666, -0.5))
            ));

            _ENTRIES.add(createEntry(am, "Cczcpaczcp", "ccz-2.png", "Cczcpaczcp",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "p (z^3 + z^-1)")
                            .add("mandelinit", Fractal.Type.Expr, "{ def alpha = 1; def beta = 3; def gamma = 1; def delta = -1; (-gamma delta / alpha beta)^/(beta - delta)}")
                            .add("max_power", Fractal.Type.Real, 3)
            ));

            _ENTRIES.add(createEntry(am, "Glynn", "glynn.png", "Glynn fractal (a julia set of mandel^1.6)",
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

            _ENTRIES.add(createEntry(am, "Mandelbrot Newton", "mandelnewton.png", "Magnet 2 Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "sqr newton(z^3 + p, z) + p")
                            .add("max_power", Fractal.Type.Real, 2)
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            /*_ENTRIES.add(e(am, "ZtanZ", "Magnet2.png", "Magnet 2 Fractal",
                    new Map<String, Fractal.Parameter>()
                            .add("function", Fractal.Type.Expr, "z tan z + p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));*/

            _ENTRIES.add(createEntry(am, "SinhZ", "sinhz.png", "Magnet 2 Fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "sinh z * p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "I")
            ));

            _ENTRIES.add(createEntry(am, "CoshZ", "coshz.png", "Cosh Z fractal",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "cosh z * p")
                            .add("bailout", Fractal.Type.Real, 32)
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Inverse Mandel", "recipmandel3.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(2, 0, 0, 2, 0, -1.5))
                            .add("function", Fractal.Type.Expr, "z^-3 + p")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Duck fractal", "duck.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(2, 0, 0, 2, 0, -1.5))
                            .add("function", Fractal.Type.Expr, "log(iabs z + p)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Newton of z^3 + p", "newton3.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(z^3 + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "c")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Newton of z^4 - 6 * z^2 - 2 p z + p", "newton4.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(z^4 - 6 * z^2 - 2 p z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "-1")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Newton of sinh z + p", "newtonsinh.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(sinh z + p, z)")
                            .add("mandelinit", Fractal.Type.Expr, "0")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Nova of z^3 - 1", "nova3.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "newton(z^3 - 1, z) + p")
                            .add("mandelinit", Fractal.Type.Expr, "1")
            ));

            _ENTRIES.add(createEntry(am, "(Lake) Nova z - R (z^power - 1)/(4 * z^3) + p", "nova34.png", "Mandelinit should be \"(p(a n-a)/(a-n))^/n\", provided p does not depend on z.",
                    Fractal.parameterBuilder()
                            .add("function", Fractal.Type.Expr, "{ def R=3; def power=4; z - R (z^power - 1) / derive(z^4 - 1, z) + p }")
                            .add("mandelinit", Fractal.Type.Expr, "{ def R=3; def power=4; (-(R power-R)/(R-power))^/power}")
            ));

            // Stop Fractal Types.

            // Next: Branching Specialities

            _ENTRIES.add(createEntry(am, "Curvature Inequality", "curvature.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("addend", Fractal.Type.Expr, "arcnorm((znext - z) / (z - zlast))")
                    //.add("interpolate_smooth_i", Fractal.Type.Bool, true)
            ));

            _ENTRIES.add(createEntry(am, "Triange Inequality", "triangleinequality.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("addend", Fractal.Type.Expr, "{ var t1 = rad z ^ max_power, t2 = rad p, M = abs(t1 - t2), m = t1 + t2; (rad znext - m) / (M - m) }")
            ));

            // Fold stuff
            _ENTRIES.add(createEntry(am, "Gaussian Integer Min", "gaussianintmin.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldinit", Fractal.Type.Expr, "bailout")
                            .add("foldfn", Fractal.Type.Expr, "min(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("bailoutvalue", Fractal.Type.Expr, "foldvalue.x")
                            .add("lakevalue", Fractal.Type.Expr, "foldvalue.x")
            ));

            _ENTRIES.add(createEntry(am, "Gaussian Integer Max", "gaussianintmax.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldinit", Fractal.Type.Expr, "0")
                            .add("foldfn", Fractal.Type.Expr, "max(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("bailoutvalue", Fractal.Type.Expr, "foldvalue.x")
                            .add("lakevalue", Fractal.Type.Expr, "foldvalue.x")
            ));

            _ENTRIES.add(createEntry(am, "Gaussian Integer (Two-Fold)", "gaussianinttwofold.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldinit", Fractal.Type.Expr, "bailout")
                            .add("foldfn", Fractal.Type.Expr, "min(dist(znext, floor(znext + 0.5:0.5)), foldvalue)")
                            .add("foldinit2", Fractal.Type.Expr, "0")
                            .add("foldfn2", Fractal.Type.Expr, "max(dist(znext, floor(znext + 0.5:0.5)), foldvalue2)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Bailout and Lake)", "expsmooth.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Bailout)", "expsmoothbailout.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh rad znext + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Lake)", "expsmoothlake.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(/dist(znext, z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing (Two Fold)", "expsmoothtwofold.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh rad znext + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x)")
                            .add("foldfn2", Fractal.Type.Expr, "/cosh(/dist(znext, z)) + foldvalue2")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue2.x)")
            ));

            _ENTRIES.add(createEntry(am, "Smooth Drawing (Lake)", "smoothlake.png", "",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "dist(znext, z) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "1.3 atan foldvalue.x / PI"))
            );

            _ENTRIES.add(createEntry(am, "Branching (Bailout and Lake)", "foldbranching.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc (z - znext)) / (12 + rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching (Bailout)", "foldbranchingbailout.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc znext) / (12 + rad znext) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching (Lake)", "foldbranchinglake.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc(z - znext)) / (12 + /dist(znext, z)) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching Alternative (Lake)", "foldbranchinglake2.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 4 arc(z - znext)) * (1 - 2 atan(12 + /dist(znext, z)) / PI) + foldvalue")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
            ));

            _ENTRIES.add(createEntry(am, "Fold-Branching (Two Fold)", "foldbranchingtwofold.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc znext) / (12 + rad znext) + foldvalue")
                            .add("foldfn2", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc(z - znext)) / (12 + /dist(znext, z)) + foldvalue2")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(1 + foldvalue.x)")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue2.x)")
            ));

            _ENTRIES.add(createEntry(am, "Exponential Smoothing and Branching (Two Fold)", "twofoldexpsmoothbranch.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("foldfn", Fractal.Type.Expr, "/cosh(rad znext + /dist(znext, z)) + foldvalue")
                            .add("bailoutvalue", Fractal.Type.Expr, "log(E^2 + foldvalue.x + foldvalue2.x)")
                            .add("foldfn2", Fractal.Type.Expr, "(0.5 + 0.5 cos 6 arc (z - znext)) / (12 + rad znext + /dist(znext, z)) + foldvalue2")
                            .add("lakevalue", Fractal.Type.Expr, "log(1 + foldvalue.x + foldvalue2.x)")
                            .add("bailouttransfer", Fractal.Type.Expr, "log(1 + foldvalue2.x) : value")
                            .add("laketransfer", Fractal.Type.Expr, "log(1 + foldvalue2.x) : value")
            ));


            // Next: Orbit Traps
            _ENTRIES.add(createEntry(am, "Cross Trap", "crosstrap.png", "Orbit Trap of 6 Steiner Circles",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "min(line(0:0, 1:0, znext), line(0:0, 0:1, znext))")
            ));

            _ENTRIES.add(createEntry(am, "Two Boxes Trap", "twoboxestrap.png", "Orbit Trap of 6 Steiner Circles",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "min(box(-2:-2, 0.5:0.5, znext), box(2:2, -0.5:-0.5, znext))")
            ));

            _ENTRIES.add(createEntry(am, "Steiner Circles Trap", "steinertrap.png", "Orbit Trap of 6 Steiner Circles",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "min(circle(0:0, 3, znext), circle(-2:0, 1, znext), circle(2:0, 1, znext), circle(-1:-1.73205, 1, znext), circle(-1:1.73205, 1, znext), circle(1:-1.73205, 1, znext), circle(1:1.73205, 1, znext))")
            ));

            _ENTRIES.add(createEntry(am, "Min/Max Neighbors", "minmaxneighbor.png", "Orbit Trap of 6 Steiner Circles",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "dist(znext, z)")
            ));

            _ENTRIES.add(createEntry(am, "Min/Max Gaussian Integer", "minmaxgaussianint.png", "A nice variation for newton",
                    Fractal.parameterBuilder()
                            .add("trapfn", Fractal.Type.Expr, "dist(znext, floor(znext + 0.5:0.5))")
            ));



            // Next Lyapunov Fractals
            _ENTRIES.add(createEntry(am, "Edge of BA-Lyapunov Fractal", "lyapunovba.png", "Part of Lyapunov fractal called Zirkony Zity",
                    Fractal.parameterBuilder()
                            .add(Fractal.SCALE_KEY, Fractal.Type.Scale, new Scale(1, 0, 0, -1, 3, 3))
                            .add("lyastring", Fractal.Type.Expr, "[b,a]")
            ));

            _ENTRIES.add(createEntry(am, "Edge of BBABA-Lyapunov Fractal", "lyapunovbbaba.png", "Part of Lyapunov fractal called Zirkony Zity",
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
    }
}
