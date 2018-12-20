package at.searles.fractview.parameters;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import at.searles.fractal.FractalProvider;
import at.searles.fractal.data.ParameterType;
import at.searles.fractview.main.FractalProviderFragment;

public class ParameterAdapter extends BaseAdapter implements ListAdapter, FractalProvider.Listener {

    private static final int BOOL = 0;
    private static final int ELEMENT = 1;

    private final Activity activity;
    private final FractalProviderFragment parent;

    public ParameterAdapter(Activity activity, FractalProviderFragment parent) {
        this.activity = activity;
        this.parent = parent;

        parent.addListener(this);
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public int getCount() {
        return parent.parameterCount();
    }

    @Override
    public FractalProvider.ParameterEntry getItem(int position) {
        return parent.getParameterEntryByIndex(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        // labels + one for each type.
        // fixme more of them (should contain a preview/checkbox for bools)
        return 3;
    }

    @Override
    public int getItemViewType(int position) {
        switch(getItem(position).parameter.type) {
            case Bool:
                return BOOL;
            default:
                return ELEMENT;
        }
    }

    private String description(FractalProvider.ParameterEntry entry) {
        if(entry.owner == -1 || parent.fractalCount() == 1) {
            return entry.parameter.description;
        } else {
            return entry.parameter.description + " (" + entry.owner + ")";
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int viewType = getItemViewType(position);

        FractalProvider.ParameterEntry entry = getItem(position);

        switch (viewType) {
            case BOOL: {
                if (view == null) {
                    view = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_checked, parent, false);
                }

                CheckedTextView text1 = view.findViewById(android.R.id.text1);

                text1.setTypeface(entry.parameter.isDefault ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);

                text1.setText(description(entry));
                text1.setChecked((Boolean) entry.parameter.value);
            }
            break;
            case ELEMENT: {
                if (view == null) {
                    view = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                }

                TextView text1 = view.findViewById(android.R.id.text1);
                text1.setTypeface(entry.parameter.isDefault ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);

                text1.setText(description(entry));

                TextView text2 = view.findViewById(android.R.id.text2);

                text2.setText(getLabelForType(entry.parameter.type));
            }
            break;
        }

        return view;
    }

    private String getLabelForType(ParameterType type) {
        // FIXME move.
        switch (type) {
            case Expr:
                return "Expression";
            case Int:
                return "Integer Number";
            case Real:
                return "Real Number";
            case Cplx:
                return "Complex Number";
            case Color:
                return "Color";
            case Palette:
                return "Palette";
            case Scale:
                return "Scale";
            case Source:
                return "Source";
            default:
                throw new IllegalArgumentException("missing label for " + type);
        }
    }

    @Override
    public void parameterMapUpdated(FractalProvider src) {
        notifyDataSetChanged();
    }
}
