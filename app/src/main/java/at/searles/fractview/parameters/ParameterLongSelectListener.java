package at.searles.fractview.parameters;

import android.view.View;
import android.widget.AdapterView;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.options.OptionsDialog;

public class ParameterLongSelectListener implements AdapterView.OnItemLongClickListener {

    private final FractalProviderFragment provider;

    public ParameterLongSelectListener(FractalProviderFragment provider) {
        this.provider = provider;
    }

    private void edit(FractalProvider.ParameterEntry item, String dialogTitle) {
        new OptionsDialog(dialogTitle, provider, item).show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ParameterAdapter adapter = (ParameterAdapter) parent.getAdapter();

        FractalProvider.ParameterEntry item = adapter.getItem(position);

        String dialogTitle = String.format("Select an option for \"%s\'", item.parameter.description);

        edit(item, dialogTitle);

        return true;
    }

    public interface Action {
        void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item);
        String title();
        boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item);
    }

    public interface CheckableAction {
        boolean isChecked(FractalProviderFragment provider, FractalProvider.ParameterEntry item);
        void setChecked(boolean newValue, FractalProviderFragment provider, FractalProvider.ParameterEntry item);
        String title();
        boolean isApplicable(FractalProviderFragment provider, FractalProvider.ParameterEntry item);
    }
}
