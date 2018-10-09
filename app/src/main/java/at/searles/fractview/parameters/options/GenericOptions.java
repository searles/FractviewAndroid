package at.searles.fractview.parameters.options;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.R;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;

public class GenericOptions {

    public static List<ParameterLongSelectListener.Action> getActions(Context context) {
        return Arrays.asList(
                GenericOptions.createResetAction(context),
                GenericOptions.createCopyAction(context),
                GenericOptions.createPasteAction(context)
        );
    }

    static AbstractAction createResetAction(Context context) {
        return new AbstractAction(context.getString(R.string.reset)) {
            @Override
            public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
                reset(provider, item);
            }
        };
    }

    static AbstractAction createCopyAction(Context context) {
        return new AbstractAction(context.getString(R.string.copy)) {
            @Override
            public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
                copy(context, provider, item);
            }
        };
    }

    static AbstractAction createPasteAction(Context context) {
        return new AbstractAction(context.getString(R.string.paste)) {
            @Override
            public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
                paste(context, provider, item);
            }
        };
    }

    private static void reset(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
        provider.setParameter(item.key, item.owner, null);
    }

    private static void copy(Context context, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
        // TODO
    }

    private static void paste(Context context, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
        // TODO
    }
}
