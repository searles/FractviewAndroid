package at.searles.fractview.fractal;

import android.app.Fragment;

/**
 * Created by searles on 27.02.18.
 */

public class FractalFragment extends Fragment {
    Fractal get(i) returns the i-th fractal.

    Iterable<String> getParameters()

    getType(String label)

    getValue(String label)

    setValue(String label, Object value)

    isDefault(String label)

    resetToDefault(String label) // throw if it is default.
}
