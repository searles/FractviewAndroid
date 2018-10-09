package at.searles.fractview.parameters;

import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;

import java.util.List;
import java.util.stream.Collectors;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.main.FractalProviderFragment;
import at.searles.fractview.parameters.options.GenericOptions;
import at.searles.fractview.parameters.options.ScaleOptions;
import at.searles.fractview.ui.DialogHelper;

public class ParameterLongSelectListener implements AdapterView.OnItemLongClickListener {

    private final FractalProviderFragment provider;

    public ParameterLongSelectListener(FractalProviderFragment provider) {
        this.provider = provider;
    }

    private void edit(List<Action> actions, FractalProvider.ParameterEntry item, AdapterView parent, String dialogTitle) {
        DialogHelper.showOptionsDialog(parent.getContext(), dialogTitle, descriptions(actions), true, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                actions.get(which).apply(provider, item);
                parent.invalidate(); // FIXME is this the right place?
            }
        });
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ParameterAdapter adapter = (ParameterAdapter) parent.getAdapter();

        FractalProvider.ParameterEntry item = adapter.getItem(position);

        String dialogTitle = String.format("Select an option for \"%s\'", item.description);

        switch (item.type) {
            case Scale: {
                edit(ScaleOptions.getActions(parent.getContext()), item, parent, dialogTitle);
                return true;
            }
            default: {
                edit(GenericOptions.getActions(parent.getContext()), item, parent, dialogTitle);
                return true;
            }
        }
    }

    private static String[] descriptions(List<Action> actions) {
        List<String> descriptions = actions.stream().map(Action::description).collect(Collectors.toList());

        return descriptions.toArray(new String[descriptions.size()]);
    }

    public interface Action {
        void apply(FractalProviderFragment provider, FractalProvider.ParameterEntry item);
        String description();
    }
}
