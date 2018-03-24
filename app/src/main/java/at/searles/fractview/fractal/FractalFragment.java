package at.searles.fractview.fractal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

public class FractalFragment extends Fragment implements FractalProvider {

    private static final String FRACTAL_KEY = "asdinoer";

    public FractalFragment newInstance(Fractal fractal) {
        FractalFragment fragment = new FractalFragment();

        Bundle bundle = new Bundle();

        bundle.putBundle(FRACTAL_KEY, BundleAdapter.fractalToBundle(fractal));

        fragment.setArguments(bundle);

        return fragment;
    }

    private Fractal fractal;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        fractal = BundleAdapter.bundleToFractal(getArguments().getBundle(FRACTAL_KEY));
    }

    @Override
    public Fractal get(int i) {
        return fractal;
    }

    @Override
    public Iterable<String> getParameters() {
        return fractal.parameters();
    }

    @Override
    public Fractal.Type getType(String label) {
        return fractal.get(label).type();
    }

    @Override
    public <T> T getValue(String label) {
        //noinspection unchecked
        return (T) fractal.get(label).value();
    }

    @Override
    public <T> void setValue(String label, T value) {
        switch (getType(label)) {
            case Int:
                fractal.setInt(label, (Integer) value);
                return;
            case Real:
                fractal.setReal(label, ((Number) value).doubleValue());
                return;
            case Cplx:
                fractal.setCplx(label, (Cplx) value);
                return;
            case Bool:
                fractal.setBool(label, (Boolean) value);
                return;
            case Expr:
                fractal.setExpr(label, (String) value);
                return;
            case Color:
                fractal.setColor(label, (Integer) value);
                return;
            case Palette:
                fractal.setPalette(label, (Palette) value);
                return;
            case Scale:
                fractal.setScale(label, (Scale) value);
                return;
            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public boolean isDefault(String label) {
        return fractal.isDefault(label);
    }

    @Override
    public void resetToDefault(String label) {
        fractal.setToDefault(label);
    }
}
