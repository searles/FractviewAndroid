package at.searles.fractview.parameters.options;

import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.meelan.values.CplxVal;
import at.searles.meelan.values.Int;
import at.searles.meelan.values.Real;

public enum CheckedActions implements ParameterLongSelectListener.CheckableAction {
    EDIT_IN_VIEW {
        @Override
        public boolean isChecked(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return provider.isInteractivePoint(item.id, item.owner);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            if(newValue) {
                provider.addInteractivePoint(item.id, item.owner);
            } else {
                provider.removeInteractivePoint(item.id, item.owner);
            }
        }

        @Override
        public String title() {
            return "Edit in View";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            // TODO
            return item.parameter.type == ParameterType.Cplx
                    || (item.parameter.type == ParameterType.Expr
                        && (
                            item.parameter.value instanceof Int
                            || item.parameter.value instanceof Real
                            || item.parameter.value instanceof CplxVal
                        )
                    );
        }
    },
    SHARE_IN_VIEWS {
        @Override
        public boolean isChecked(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return provider.isSharedParameter(item.id);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            if(newValue) {
                provider.removeExclusiveParameter(item.id);
            } else {
                provider.addExclusiveParameter(item.id);
            }
        }

        @Override
        public String title() {
            return "Share Value in Views";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return provider.fractalCount() > 1;
        }
    }
}
