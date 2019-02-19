package at.searles.fractview.parameters.options;

import at.searles.fractal.ParameterTable;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.provider.FractalProviderFragment;
import at.searles.fractview.provider.view.parameters.ParameterLongSelectListener;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.meelan.values.CplxVal;

public enum Actions implements ParameterLongSelectListener.Action {
    MOVE_TO_ORIGIN {
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {
            Scale scale = (Scale) item.parameter.value;
            provider.setParameterValue(item.key, item.owner, new Scale(scale.xx, scale.xy, scale.yx, scale.yy, 0., 0.));
        }

        @Override
        public String title() {
            return "Move Center to Origin (0:0)";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return item.parameter.type == ParameterType.Scale;
        }
    },
    ORTHOGONALIZE {
        // straightens the current scale
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {
            Scale scale = (Scale) item.parameter.value;
            provider.setParameterValue(item.key, item.owner, scale.createOrthogonal());
        }

        @Override
        public String title() {
            return "Orthogonalize";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return item.parameter.type == ParameterType.Scale;
        }
    },
    MOVE_TO_CENTER_CPLX {
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {
            Scale scale = provider.getFractal(provider.selectedId()).scale();
            provider.setParameterValue(item.key, item.owner, new Cplx(scale.cx, scale.cy));
        }

        @Override
        public String title() {
            return "Move to center of selected view";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return item.parameter.type == ParameterType.Cplx;
        }
    },
    MOVE_TO_CENTER_EXPR {
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {
            Scale scale = provider.getFractal(provider.selectedId()).scale();
            provider.setParameterValue(item.key, item.owner, new CplxVal(new Cplx(scale.cx, scale.cy)));
        }

        @Override
        public String title() {
            return "Move to center of selected view";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return item.parameter.type == ParameterType.Expr;
        }
    },
    SPLIT_FRACTAL {
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {
            String id = item.key;
            Boolean value = (Boolean) item.parameter.value;
            provider.addFromSelected(id, !value);
        }

        @Override
        public String title() {
            return "Split View";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return item.parameter.type == ParameterType.Bool;
        }
    },
    COPY {
        // TODO
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {

        }

        @Override
        public String title() {
            return "Copy";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return true;
        }
    },
    PASTE {
        // TODO
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {

        }

        @Override
        public String title() {
            return "Paste";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return true;
        }
    },
    RESET_TO_DEFAULT {
        @Override
        public void apply(FractalProviderFragment provider, ParameterTable.Entry item) {
            provider.setParameterValue(item.key, item.owner, null);
        }

        @Override
        public String title() {
            return "Reset to Default";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return !item.parameter.isDefault;
        }
    }
}
