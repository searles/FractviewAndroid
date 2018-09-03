package at.searles.fractview.parameters;

import android.content.DialogInterface;
import android.view.View;
import android.widget.AdapterView;

import at.searles.fractal.FractalProvider;
import at.searles.fractview.ui.DialogHelper;

public class ParameterLongSelectListener implements AdapterView.OnItemLongClickListener {

    private final FractalProvider provider;

    public ParameterLongSelectListener(FractalProvider provider) {
        this.provider = provider;
    }

    private <A extends Action> void edit(A[] actions, FractalProvider.ParameterEntry item, AdapterView parent, String dialogTitle) {
        DialogHelper.showOptionsDialog(parent.getContext(), dialogTitle, descriptions(actions), true, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int which) {
                actions[which].apply(provider, item);
                parent.invalidate(); // FIXME is this the right place?
            }
        });
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        ParameterAdapter adapter = (ParameterAdapter) parent.getAdapter();

        FractalProvider.ParameterEntry item = adapter.getItem(position);

        String dialogTitle = String.format("Select an option for \"%s\'", item.description);

        switch (item.key.type) {
            case Scale: {
                edit(ScaleOptions.values(), item, parent, dialogTitle);
                return true;
            }
            case Expr: {
                edit(ExprOptions.values(), item, parent, dialogTitle);
                return true;
            }
            case Cplx: {
                edit(CplxOptions.values(), item, parent, dialogTitle);
                return true;
            }
            case Int:
            case Real:
            case Bool:
            case Color:
            case Palette: {
                edit(DefaultOnlyOptions.values(), item, parent, dialogTitle);
                return true;
            }
        }

        return false;
    }

    private static <A extends Action> String[] descriptions(A[] actions) {
        String[] options = new String[actions.length];

        for(int i = 0; i < actions.length; ++i) {
            options[i] = actions[i].description();
        }

        return options;
    }

    interface Action {
        void apply(FractalProvider provider, FractalProvider.ParameterEntry item);
        String description();
    }
}
