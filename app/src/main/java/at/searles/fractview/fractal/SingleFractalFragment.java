package at.searles.fractview.fractal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;

public class SingleFractalFragment extends Fragment implements FractalProvider {

    private static final String FRACTAL_KEY = "asdinoer";

    private Fractal fractal;

    private List<FractalProviderListener> listeners = new LinkedList<>();

    public static SingleFractalFragment newInstance(Fractal fractal) {
        SingleFractalFragment fragment = new SingleFractalFragment();

        Bundle bundle = new Bundle();

        bundle.putBundle(FRACTAL_KEY, BundleAdapter.fractalToBundle(fractal));

        fragment.setArguments(bundle);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        fractal = BundleAdapter.bundleToFractal(getArguments().getBundle(FRACTAL_KEY));

        try {
            fractal.parse();
            fractal.compile();

            for(FractalProviderListener listener : listeners) {
                listener.fractalModified(fractal);
            }
        } catch (CompileException e) {
            e.printStackTrace();
        }
    }

    public void addListener(FractalProviderListener listener) {
        listeners.add(listener);

        if(fractal != null) {
            // and provide with new fractal
            listener.fractalModified(fractal);
        }
    }

    public CallBack createCallback() {
        return new CallBack() {
            @Override
            public void setScaleRelative(Scale sc) {
                setValue(Fractal.SCALE_KEY, fractal.scale().relative(sc));
            }
        };
    }

    @Override
    public Fractal get(int i) {
        return fractal;
    }

    @Override
    public int size() {
        return 1;
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
        // FIXME shouldn't this be immutable? Think of history.
        switch (getType(label)) {
            case Int:
                fractal.setInt(label, (Integer) value);
                break;
            case Real:
                fractal.setReal(label, ((Number) value).doubleValue());
                break;
            case Cplx:
                fractal.setCplx(label, (Cplx) value);
                break;
            case Bool:
                fractal.setBool(label, (Boolean) value);
                break;
            case Expr:
                fractal.setExpr(label, (String) value);
                break;
            case Color:
                fractal.setColor(label, (Integer) value);
                break;
            case Palette:
                fractal.setPalette(label, (Palette) value);
                break;
            case Scale:
                fractal.setScale(label, (Scale) value);
                break;
            default:
                throw new IllegalArgumentException();
        }

        try {
            fractal.parse();
            fractal.compile();
        } catch (CompileException e) {
            e.printStackTrace();
        }

        for(FractalProviderListener listener : listeners) {
            listener.fractalModified(fractal);
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
