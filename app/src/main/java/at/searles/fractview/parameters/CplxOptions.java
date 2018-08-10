package at.searles.fractview.parameters;

import at.searles.fractal.FractalProvider;

enum CplxOptions implements ParameterLongSelectListener.Action {
    Reset("Reset to Default") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            provider.set(item.key, null);
        }
    },
    Center("Set to Center") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            // TODO: Replace by on-screen edit.
        }
    }
    ;
    private final String description;

    CplxOptions(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

}
