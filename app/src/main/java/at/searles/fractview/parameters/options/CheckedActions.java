package at.searles.fractview.parameters.options;

import at.searles.fractal.ParameterTable;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.provider.FractalProviderFragment;
import at.searles.fractview.provider.view.parameters.ParameterLongSelectListener;
import at.searles.fractview.ui.DialogHelper;

public enum CheckedActions implements ParameterLongSelectListener.CheckableAction {
    EDIT_IN_VIEW {
        @Override
        public boolean isChecked(FractalProviderFragment provider, ParameterTable.Entry item) {
            return provider.isInteractivePoint(item.key, item.owner);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, ParameterTable.Entry item) {
            if(newValue) {
                try {
                    provider.addInteractivePoint(item.key, item.owner);
                } catch (Throwable th) {
                    DialogHelper.error(provider.getContext(), "Point must be a numeric value: " + th.getMessage());
                }
            } else {
                provider.removeInteractivePoint(item.key, item.owner);
            }
        }

        @Override
        public String title() {
            return "Edit in View";
        }

        @Override
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return item.parameter.type == ParameterType.Cplx
                    || item.parameter.type == ParameterType.Expr;
        }
    },
    SHARE_IN_VIEWS {
        @Override
        public boolean isChecked(FractalProviderFragment provider, ParameterTable.Entry item) {
            return provider.isSharedParameter(item.key);
        }

        @Override
        public void setChecked(boolean newValue, FractalProviderFragment provider, ParameterTable.Entry item) {
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
        public boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item) {
            return provider.fractalCount() > 1;
        }
    }
}
