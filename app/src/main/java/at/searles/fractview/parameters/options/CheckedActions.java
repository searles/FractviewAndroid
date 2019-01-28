package at.searles.fractview.parameters.options;

import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.ParameterLongSelectListener;
import at.searles.fractview.ui.DialogHelper;

public enum CheckedActions implements ParameterLongSelectListener.CheckableAction {
    EDIT_IN_VIEW {
        @Override
        public boolean isChecked(FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            return provider.isInteractivePoint(item.id, item.owner);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, FractalProvider.ParameterEntry item) {
            if(newValue) {
                try {
                    provider.addInteractivePoint(item.id, item.owner);
                } catch (Throwable th) {
                    DialogHelper.error(provider.getContext(), "Point must be a numeric value: " + th.getMessage());
                }
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
            return item.parameter.type == ParameterType.Cplx
                    || item.parameter.type == ParameterType.Expr;
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
