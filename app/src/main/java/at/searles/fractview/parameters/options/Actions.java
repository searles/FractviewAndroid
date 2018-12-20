package at.searles.fractview.parameters.options;

import at.searles.fractal.Fractal;
import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.math.Scale;

public enum Actions implements ParameterLongSelectListener.Action {
    COPY {
        // TODO
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {

        }

        @Override
        public String title() {
            return "Copy";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return true;
        }
    },
    PASTE {
        // TODO
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {

        }

        @Override
        public String title() {
            return "Paste";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return true;
        }
    },
    MOVE_TO_ORIGIN {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.parameter.value;
            provider.setParameterValue(item.key, item.owner, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
        }

        @Override
        public String title() {
            return "Move Center to Origin (0:0)";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return item.parameter.type == ParameterType.Scale;
        }
    },
    MOVE_TO_CENTER {
        // TODO: Multiple implementations: Cplx/Expr + one for 0 fractal

        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            // TODO: check whether scale is shared or not.
            Scale scale = (Scale) provider.getParameterValue(Fractal.SCALE_LABEL, 0);
            provider.setParameterValue(item.key, item.owner, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
        }

        @Override
        public String title() {
            return "Move to Center";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return item.parameter.type == null; // TODO
        }
    },
    MOVE_TO_CENTER_OF_VIEW_0 {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.parameter.value;
            provider.setParameterValue(item.key, item.owner, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
        }

        @Override
        public String title() {
            return "Move to Center";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return false;
        }
    },
    ORTHOGONALIZE {
        // straightens the current scale
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.parameter.value;
            double s = Math.sqrt(Math.abs(scale.xx * scale.yy - scale.xy * scale.yx));
            Scale newScale = new Scale(s, 0, 0, s, scale.cx, scale.cy);
            provider.setParameterValue(item.key, item.owner, newScale);
        }

        @Override
        public String title() {
            return "Orthogonalize";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return item.parameter.type == ParameterType.Scale;
        }
    },
    RESET_TO_DEFAULT {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            provider.setParameterValue(item.key, item.owner, null);
        }

        @Override
        public String title() {
            return "Reset to Default";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return !item.parameter.isDefault;
        }
    },
    SPLIT_FRACTAL {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            String id = item.key;
            Boolean value = (Boolean) item.parameter.value;
            provider.addFractalFromKey(id, !value);
        }

        @Override
        public String title() {
            return "Split View";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return item.parameter.type == ParameterType.Bool;
        }
    }
}
