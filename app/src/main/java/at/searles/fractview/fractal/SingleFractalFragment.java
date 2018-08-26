package at.searles.fractview.fractal;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;

import at.searles.fractal.FractalExternData;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.FractalData;
import at.searles.math.Scale;

public class SingleFractalFragment extends Fragment {

    private static final String FRACTAL_KEY = "asdinoer";

    private FractalProvider provider;

    public static SingleFractalFragment newInstance(FractalData fractal) {
        SingleFractalFragment fragment = new SingleFractalFragment();

        Bundle bundle = new Bundle();

        bundle.putBundle(FRACTAL_KEY, BundleAdapter.toBundle(fractal));

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

        FractalData fractal = BundleAdapter.fractalFromBundle(savedInstanceState.getBundle(FRACTAL_KEY));

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

    public CallBack createCallback(String label) {
        return new CallBack() {
            @Override
            public void setScaleRelative(Scale sc) {
                Scale absoluteScale = ((Scale) provider.get(label).data().value(FractalExternData.SCALE_LABEL)).relative(sc);
                provider.set(FractalExternData.SCALE_KEY, label, absoluteScale);
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
