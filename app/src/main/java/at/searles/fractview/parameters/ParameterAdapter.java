package at.searles.fractview.parameters;

import android.app.Activity;
import android.graphics.Typeface;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.TextView;

import at.searles.fractal.FractalProvider;

public class ParameterAdapter extends BaseAdapter implements ListAdapter {

    private static final int BOOL = 0;
    private static final int ELEMENT = 1;

    private final Activity activity;
    private final FractalProvider provider;

    public ParameterAdapter(Activity activity, FractalProvider provider) {
        this.activity = activity;
        this.provider = provider;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public int getCount() {
        return provider.parameterCount();
    }

    @Override
    public FractalProvider.ParameterEntry getItem(int position) {
        return provider.getParameter(position);
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
        switch(getItem(position).key.type) {
            case Bool:
                return BOOL;
            default:
                return ELEMENT;
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int viewType = getItemViewType(position);

        FractalProvider.ParameterEntry entry = getItem(position);

        switch (viewType) {
            case BOOL: {
                if (view == null)
                    view = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_checked, parent, false);

                CheckedTextView text1 = (CheckedTextView) view.findViewById(android.R.id.text1);

                text1.setTypeface(entry.isDefault ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);

                text1.setText(entry.description);
                text1.setChecked((Boolean) entry.value);
            }
            break;
            case ELEMENT: {
                if (view == null) {
                    view = activity.getLayoutInflater().inflate(android.R.layout.simple_list_item_2, parent, false);
                }

                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setTypeface(entry.isDefault ? Typeface.DEFAULT : Typeface.DEFAULT_BOLD);

                text1.setText(entry.description);

                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                switch (entry.key.type) {
                    case Expr:
                        text2.setText("Expression");
                        break;
                    case Int:
                        text2.setText("Integer Number");
                        break;
                    case Real:
                        text2.setText("Real Number");
                        break;
                    case Cplx:
                        text2.setText("Complex Number");
                        break;
                    case Color:
                        text2.setText("Color");
                        break;
                    case Palette:
                        text2.setText("Palette");
                        break;
                    case Scale:
                        text2.setText("Scale");
                        break;
                    default:
                        Log.e(getClass().getName(), "missing label for " + entry.key.type);
                }
            }
            break;
        }

        return view;
    }
}
