package at.searles.fractview.parameters.options;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.R;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.math.Scale;

public class ScaleOptions {
    public static List<ParameterLongSelectListener.Action> getActions(Context context) {
        return Arrays.asList(
                GenericOptions.createResetAction(context),
                ScaleOptions.createOriginAction(context),
                ScaleOptions.createOrthogonalizeAction(context),
                GenericOptions.createCopyAction(context),
                GenericOptions.createPasteAction(context)
        );
    }

    private static AbstractAction createOriginAction(Context context) {
        return new AbstractAction(context.getString(R.string.set_center_to_origin)) {
            @Override
            public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
                Scale scale = (Scale) item.value;
                provider.setParameter(item.key, item.owner, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
            }
        };
    }

    private static AbstractAction createOrthogonalizeAction(Context context) {
        return new AbstractAction(context.getString(R.string.orthogonalize)) {
            @Override
            public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
                Scale scale = (Scale) item.value;
                double s = Math.sqrt(Math.abs(scale.xx * scale.yy - scale.xy * scale.yx));
                Scale newScale = new Scale(s, 0, 0, s, scale.cx, scale.cy);
                provider.setParameter(item.key, item.owner, newScale);
            }
        };
    }
}
