package at.searles.fractview.fractal;

import at.searles.fractal.Fractal;

/**
 * Created by searles on 27.02.18.
 */

public interface FractalFragment {
    Fractal get(int i);// returns the i-th fractal.

    Iterable<String> getParameters();

    Object getType(String label);

    <T> T getValue(String label);

    <T> void setValue(String label, T value);

    boolean isDefault(String label);

    void resetToDefault(String label); // throw if it is default.
}
