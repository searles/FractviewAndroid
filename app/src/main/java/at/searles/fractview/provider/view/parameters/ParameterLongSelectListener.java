package at.searles.fractview.provider.view.parameters;

import android.view.View;
import android.widget.AdapterView;

import at.searles.fractal.ParameterTable;
import at.searles.fractview.parameters.options.OptionsDialog;
import at.searles.fractview.provider.FractalProviderFragment;

public class ParameterLongSelectListener implements AdapterView.OnItemLongClickListener {

    private final FractalProviderFragment provider;

    public ParameterLongSelectListener(FractalProviderFragment provider) {
        this.provider = provider;
    }

    private void edit(ParameterTable.Entry item, String dialogTitle) {
        new OptionsDialog(dialogTitle, provider, item).show();
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ParameterAdapter adapter = (ParameterAdapter) parent.getAdapter();

        ParameterTable.Entry item = adapter.getItem(position);

        String dialogTitle = String.format("Select an option for \"%s\'", item.parameter.description);

        edit(item, dialogTitle);

        return true;
    }

    public interface Action {
        void apply(FractalProviderFragment provider, ParameterTable.Entry item);
        String title();
        boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item);
    }

    public interface CheckableAction {
        boolean isChecked(FractalProviderFragment provider, ParameterTable.Entry item);
        void setChecked(boolean newValue, FractalProviderFragment provider, ParameterTable.Entry item);
        String title();
        boolean isApplicable(FractalProviderFragment provider, ParameterTable.Entry item);
    }
}
