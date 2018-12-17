package at.searles.fractview.parameters.options;

import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;

public enum CheckedActions implements ParameterLongSelectListener.CheckableAction {
    EDIT_IN_VIEW {
        @Override
        public boolean isChecked(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return provider.isInteractivePoint(item.key, item.owner);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            if(newValue) {
                provider.addInteractivePoint(item.key, item.owner);
            } else {
                provider.removeInteractivePoint(item.key, item.owner);
            }
        }

        @Override
        public String title() {
            return "Edit in View";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            // TODO allow for expr
            return item.parameter.type == ParameterType.Cplx;
        }
    },
    SHARE_IN_VIEWS {
        @Override
        public boolean isChecked(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return !provider.isExclusiveParameter(item.key);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            if(newValue) {
                provider.removeExclusiveParameter(item.key);
            } else {
                provider.addExclusiveParameter(item.key);
            }
        }

        @Override
        public String title() {
            return "Share Value in Views";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return true;
        }
    }
}
