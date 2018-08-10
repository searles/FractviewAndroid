package at.searles.fractview.parameters;

import at.searles.fractal.FractalProvider;

enum DefaultOnlyOptions implements ParameterLongSelectListener.Action {
    Reset("Reset to Default") {
        @Override
        public void apply(FractalProvider provider, FractalProvider.ParameterEntry item) {
            provider.set(item.key, null);
        }
    }
    ;
    private final String description;

    DefaultOnlyOptions(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }

}
