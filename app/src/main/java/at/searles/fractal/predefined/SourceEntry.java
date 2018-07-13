package at.searles.fractal.predefined;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

import at.searles.fractview.AssetsHelper;

/**
 * Main menus
 */
public class SourceEntry {
    public final String title;
    public final Bitmap icon;
    public final String description;
    public final String source;

    public SourceEntry(String title, Bitmap icon, String description, String source) {
        this.title = title;
        this.icon = icon;
        this.description = description;
        this.source = source;
    }

    // Create a list of assets and icons that come with it.
    // Read private entries
    public static SourceEntry createEntry(AssetManager am, String title, String iconFilename, String description, String sourceFilename) {
        String sourceCode = AssetsHelper.readSourcecode(am, sourceFilename);
        Bitmap icon = AssetsHelper.readIcon(am, iconFilename);

        if(sourceCode == null/* || icon == null*/) {
            throw new IllegalArgumentException("bad asset: " + title);
        }

        return new SourceEntry(title, icon, description, sourceCode);
    }

    public static ArrayList<SourceEntry> _ENTRIES = null;

    public static synchronized List<SourceEntry> entries(AssetManager am) {
        if (_ENTRIES == null) {
            // create entries.
            _ENTRIES = new ArrayList<>();

            // grouped : the ones with maxpower
            _ENTRIES.add(createEntry(am, "Default", "default.png", "Basic program for fractals with polynomial functions", "Default.fv"));
            _ENTRIES.add(createEntry(am, "Julia Map", "juliamap.png", "Variation of \"Default\" that shows a map of julia sets.", "JuliaMap.fv"));
            _ENTRIES.add(createEntry(am, "Branching", "branching.png", "\"Default\" with an addend for average coloring methods for polynomial functions", "Branching.fv"));
            _ENTRIES.add(createEntry(am, "Cczcpaczcp", "ccz.png", "\"Default\" with a built-in special formula by Mark R Eggleston, called Cczcpaczcp", "Cczcpaczcp.fv"));

            // the ones with orbit traps
            _ENTRIES.add(createEntry(am, "Orbit Trap", "orbittrap.png", "\"Default\" with an orbit trap", "OrbitTrap.fv"));
            _ENTRIES.add(createEntry(am, "Frame Orbit Trap", "frameorbittrap.png", "\"Default\" with an orbit trap with a transparent pattern", "FrameOrbitTrap.fv"));
            _ENTRIES.add(createEntry(am, "Min/Max Trap", "minmaxtrap.png", "Depictures the minimum and maximum distance to the orbit trap", "MinMaxOrbitTrap.fv"));

            // the ones with fold
            _ENTRIES.add(createEntry(am, "Fold", "fold.png", "Program with an aggregator function (fold), also suitable for non-polynomial fractals", "Fold.fv"));
            _ENTRIES.add(createEntry(am, "Two Folds", "twofolds.png", "\"Fold\" with two fold functions", "TwoFold.fv"));
            _ENTRIES.add(createEntry(am, "Lake Fold", "lakefold.png", "\"Fold\" without bailout for Nova- and Newton fractals", "Lake.fv"));

            // Special Lake Fold ones
            _ENTRIES.add(createEntry(am, "Newton", "newton.png", "Newton method for root finding fractals", "Newton.fv"));
            _ENTRIES.add(createEntry(am, "Nova", "nova.png", "Nova fractal defined by z - R * (z^power + argument) / (z^power + argument)' + p", "Nova.fv"));
            _ENTRIES.add(createEntry(am, "Secant", "secant.png", "Secant method for root finding fractals", "Secant.fv"));

            // Completely different onces
            _ENTRIES.add(createEntry(am, "Lyapunov", "lyapunov.png", "Lyapunov fractals", "Lyapunov.fv"));

            _ENTRIES.add(createEntry(am, "Pendulum (Multiple Magnets)", "pendulum.png", "Magnetic Pendulum Simulation with multiple magnets", "Pendulum.fv"));
            _ENTRIES.add(createEntry(am, "Pendulum (3 Magnets)", "pendulum3.png", "Magnetic Pendulum Simulation with 3 Magnets", "Pendulum3.fv"));

            _ENTRIES.add(createEntry(am, "Complex Function", "complexfn.png", "Complex function (Color Wheel method by default)", "ComplexFn.fv"));
        }

        return _ENTRIES;
    }

}
