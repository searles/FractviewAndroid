package at.searles.fractview.parameters.options;

import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.meelan.values.CplxVal;

public enum Actions implements ParameterLongSelectListener.Action {
    MOVE_TO_ORIGIN {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.parameter.value;
            provider.setParameterValue(item.id, item.owner, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
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
    ORTHOGONALIZE {
        // straightens the current scale
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = (Scale) item.parameter.value;
            provider.setParameterValue(item.id, item.owner, scale.createOrthogonal());
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
    MOVE_TO_CENTER_CPLX {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = provider.getFractal(provider.keyIndex()).scale();
            provider.setParameterValue(item.id, item.owner, new Cplx(scale.cx, scale.cy));
        }

        @Override
        public String title() {
            return "Move to center of selected view";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return item.parameter.type == ParameterType.Cplx;
        }
    },
    MOVE_TO_CENTER_EXPR {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            Scale scale = provider.getFractal(provider.keyIndex()).scale();
            provider.setParameterValue(item.id, item.owner, new CplxVal(new Cplx(scale.cx, scale.cy)));
        }

        @Override
        public String title() {
            return "Move to center of selected view";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return item.parameter.type == ParameterType.Expr;
        }
    },
    SPLIT_FRACTAL {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            String id = item.id;
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
    },
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
    RESET_TO_DEFAULT {
        @Override
        public void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            provider.setParameterValue(item.id, item.owner, null);
        }

        @Override
        public String title() {
            return "Reset to Default";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return !item.parameter.isDefault;
        }
    }
}
