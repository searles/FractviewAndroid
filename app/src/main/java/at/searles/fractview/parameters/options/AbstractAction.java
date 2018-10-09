package at.searles.fractview.parameters.options;

import at.searles.fractview.parameters.ParameterLongSelectListener;

public abstract class AbstractAction implements ParameterLongSelectListener.Action {

    private final String description;

    protected AbstractAction(String description) {
        this.description = description;
    }

    @Override
    public String description() {
        return description;
    }
}
