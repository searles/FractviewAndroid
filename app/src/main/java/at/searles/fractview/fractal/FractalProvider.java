package at.searles.fractview.fractal;

import at.searles.fractal.Fractal;
import at.searles.math.Scale;

/**
 * Created by searles on 27.02.18.
 */

public interface FractalProvider {
    Fractal get(int i);// returns the i-th fractal.

    /**
     *
     * @return The number of fractals that are provided by this fractalprovider
     */
    int size();

    Iterable<String> getParameters();

    Fractal.Type getType(String label);

    <T> T getValue(String label);

    <T> void setValue(String label, T value);

    boolean isDefault(String label);

    void resetToDefault(String label); // throw if it is default.
    
    interface CallBack {

        void setScaleRelative(Scale sc);
    }
}
