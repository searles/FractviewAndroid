package at.searles.fractview.parameters;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.utils.ScaleUtils;
import at.searles.math.Scale;

public enum ScaleOptions implements ParameterLongSelectListener.Action {
    Reset("Reset to Default") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            provider.set(item.key, null);
        }
    },
    CenterOnOrigin("Center on Origin") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.value;
            provider.set(item.key, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
        }
    },
    Orthogonalize("Orthogonalize") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.value;
            provider.set(item.key, ScaleUtils.orthogonalize(scale));
        }
    },
    Straighten("Straighten") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.value;
            provider.set(item.key, ScaleUtils.straighten(scale));
        }
    };

    private final String description;

    ScaleOptions(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
