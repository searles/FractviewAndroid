package at.searles.fractview.fractal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;

import at.searles.fractal.Fractal;
import at.searles.fractal.android.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;

public class SingleFractalFragment extends Fragment implements FractalProvider {

    private static final String FRACTAL_KEY = "asdinoer";

    private Fractal fractal;

    private List<FractalProviderListener> listeners = new LinkedList<>();

    private LinkedList<Fractal> history = new LinkedList<>();
    private boolean mustIssueWarningOnEmptyHistory = true;

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

        Log.d(getClass().getName(), "onCreate");

        if(savedInstanceState == null) {
            savedInstanceState = getArguments();
        }

        fractal = BundleAdapter.bundleToFractal(savedInstanceState.getBundle(FRACTAL_KEY));

        try {
            fractal.parse();
            fractal.compile();

            for(FractalProviderListener listener : listeners) {
                listener.fractalModified(fractal);
            }
        } catch (CompileException e) {
            e.printStackTrace();
            // FIXME do something useful!
        }

        System.out.println(fractal.scale());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(getClass().getName(), "onSaveInstanceState");

        outState.putBundle(FRACTAL_KEY, BundleAdapter.fractalToBundle(fractal));
    }

    /**
     * Adds a listener. If the fractal is already set in this
     * fragment, the fractalModified method is immediately called.
     * @param listener The listener to be added
     */
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

    public void setFractal(Fractal newFractal) {
        setFractal(newFractal, true);
    }

    public void setFractal(Fractal newFractal, boolean addOldFractalToHistory) {

        Log.d(getClass().getName(), "set fractal");

        if(fractal != null && addOldFractalToHistory) {
            history.add(fractal);
        }

        this.fractal = newFractal;

        try {
            fractal.parse();
            fractal.compile();
        } catch (CompileException e) {
            e.printStackTrace();
        }

        System.out.println(fractal.scale());

        fireFractalChangedEvent();
    }

    private void fireFractalChangedEvent() {
        for(FractalProviderListener listener : listeners) {
            listener.fractalModified(fractal);
        }
    }

    @Override
    public <T> T getValue(String label) {
        //noinspection unchecked
        return (T) fractal.get(label).value();
    }

    @Override
    public <T> void setValue(String label, T value) {
        Fractal newFractal = this.fractal.copy();

        switch (getType(label)) {
            case Int:
                newFractal.setInt(label, (Integer) value);
                break;
            case Real:
                newFractal.setReal(label, ((Number) value).doubleValue());
                break;
            case Cplx:
                newFractal.setCplx(label, (Cplx) value);
                break;
            case Bool:
                newFractal.setBool(label, (Boolean) value);
                break;
            case Expr:
                newFractal.setExpr(label, (String) value);
                break;
            case Color:
                newFractal.setColor(label, (Integer) value);
                break;
            case Palette:
                newFractal.setPalette(label, (Palette) value);
                break;
            case Scale:
                newFractal.setScale(label, (Scale) value);
                break;
            default:
                throw new IllegalArgumentException();
        }

        setFractal(newFractal);
    }

    @Override
    public boolean isDefault(String label) {
        return fractal.isDefault(label);
    }

    @Override
    public void resetToDefault(String label) {
        fractal.setToDefault(label);
    }

    public Fractal fractal() {
        return fractal;
    }

    public boolean historyBack() {
        if(history.isEmpty()) {
            if(mustIssueWarningOnEmptyHistory) {
                mustIssueWarningOnEmptyHistory = false;
                DialogHelper.info(getActivity(), "Last element in history. \nPush \'back\' to exit fractview.");
                return true;
            }

            return false;
        }

        Fractal lastEntry = history.removeLast();

        setFractal(lastEntry, false);

        return true;
    }
}
