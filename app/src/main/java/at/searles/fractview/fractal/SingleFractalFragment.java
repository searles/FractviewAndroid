package at.searles.fractview.fractal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.LinkedList;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.math.Scale;

public class SingleFractalFragment extends Fragment {

    private static final String FRACTAL_KEY = "asdinoer";

    private LinkedList<Fractal> history = new LinkedList<>(); // TODO???

    private boolean mustIssueWarningOnEmptyHistory = true;

    private FractalProvider provider;

    public static SingleFractalFragment newInstance(Fractal fractal) {
        SingleFractalFragment fragment = new SingleFractalFragment();

        Bundle bundle = new Bundle();

        bundle.putBundle(FRACTAL_KEY, fractal.toBundle());

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

        Fractal fractal = Fractal.fromBundle(savedInstanceState.getBundle(FRACTAL_KEY));

        this.provider = FractalProvider.singleFractal(fractal);

//        for(FractalProviderListener listener : listeners) {
//            listener.fractalModified(fractal);
//        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.d(getClass().getName(), "onSaveInstanceState");

//        outState.putBundle(FRACTAL_KEY, fractal().toBundle());
    }

//    /**
//     * Adds a listener. If the fractal is already set in this
//     * fragment, the fractalModified method is immediately called.
//     * @param listener The listener to be added
//     */
//    public void addListener(FractalProviderListener listener) {
//        listeners.add(listener);
//
//        if(fractal() != null) {
//            // and provide with new fractal
//            listener.fractalModified(fractal());
//        }
//    }

    interface CallBack {
        void setScaleRelative(Scale sc);
    }

    public CallBack createCallback() {
        return new CallBack() {
            @Override
            public void setScaleRelative(Scale sc) {
                Scale absoluteScale = ((Scale) provider.value(Fractal.SCALE_KEY)).relative(sc);
                provider.set(Fractal.SCALE_KEY, absoluteScale);
            }
        };
    }

//    public void setFractal(Fractal newFractal) {
//        setFractal(newFractal, true);
//    }
//
//    public void setFractal(Fractal newFractal, boolean addOldFractalToHistory) {
//        Log.d(getClass().getName(), "set fractal");
//
//        if(fractal != null && addOldFractalToHistory) {
//            history.add(fractal);
//        }
//
//        this.fractal = newFractal;
//
//        fireFractalChangedEvent();
//    }
//
//    public boolean historyBack() {
//        if(history.isEmpty()) {
//            if(mustIssueWarningOnEmptyHistory) {
//                mustIssueWarningOnEmptyHistory = false;
//                DialogHelper.info(getActivity(), "Last element in history. \nPush \'back\' to exit fractview.");
//                return true;
//            }
//
//            return false;
//        }
//
//        Fractal lastEntry = history.removeLast();
//
//        setFractal(lastEntry, false);
//
//        return true;
//    }
}
