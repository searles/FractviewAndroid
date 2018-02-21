package at.searles.fractview;

import android.content.Context;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckedTextView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

import at.searles.fractal.Fractal;

/**
 * Adapter for listviews for extern meelan parameters
 */
public class ParameterAdapter extends BaseAdapter implements ListAdapter {

    // The following use different views (and listeners)
    private static final int BOOL = 0;
    private static final int ELEMENT = 1;

    /**
     * Inflater is needed for view
     */
    private final LayoutInflater inflater;

    private final ArrayList<String> labels;
    private final ArrayList<Fractal.Type> types;

    private final FractalProviderFragment fragment;

    public ParameterAdapter(FractalProviderFragment fragment) {
        this.fragment = fragment;
        inflater = (LayoutInflater) fragment.getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // fill elements with elements :)
        labels = new ArrayList<>();
        types = new ArrayList<>();
        init();
    }

    /**
     * Fill elements-list with content.
     */
    private void init() {
        labels.clear();
        types.clear();

        for (String label : fragment.parameters()) {
            labels.add(label);
            types.add(fragment.type(label));
        }
    }


    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    public int getCount() {
        return labels.size();
    }

    @Override
    public String getItem(int position) {
        return labels.get(position);
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
        // fixme more?
        switch (types.get(position)) {
            case Bool:
                return BOOL;
            default:
                return ELEMENT;
        }
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        int viewType = getItemViewType(position);

        String label = labels.get(position);
        Fractal.Type type = types.get(position);

        switch (viewType) {
            case BOOL: {
                if (view == null)
                    view = inflater.inflate(android.R.layout.simple_list_item_checked, parent, false);

                CheckedTextView text1 = (CheckedTextView) view.findViewById(android.R.id.text1);

                if (!fragment.isDefault(label)) {
                    //Log.d("PA", e.label + " is not default");
                    text1.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    //Log.d("PA", e.label + " is default");
                    text1.setTypeface(Typeface.DEFAULT);
                }

                text1.setText(label);
                text1.setChecked((Boolean) fragment.value(label));
            }
            break;
            case ELEMENT: {
                if (view == null)
                    view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                text1.setText(label);

                // if not isDefaultValue set bold.
                if (!fragment.isDefault(label)) {
                    //Log.d("PA", e.label + " is not default");
                    text1.setTypeface(Typeface.DEFAULT_BOLD);
                } else {
                    //Log.d("PA", e.label + " is default");
                    text1.setTypeface(Typeface.DEFAULT);
                }

                TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                switch (type) {
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
                        Log.e(getClass().getName(), "missing label");
                }
            }
            break;
        }

        return view;
    }

    public ListView.OnItemClickListener createOnClickListener(Context context) {
        return new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(types.get(position) == Fractal.Type.Bool) {
                    String label = labels.get(position);
                    boolean newValue = !(Boolean) fragment.value(label);

                    fragment.setValue(label, newValue);

                    ((CheckedTextView) view).setChecked(newValue);

                    notifyDataSetChanged();

                    return;
                } else {
                    fragment.startEditorForParameter(labels.get(position));
                }
            }
        };
    }

    public ListView.OnItemLongClickListener createOnLongClickListener() {
        return new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                fragment.startOptionsEditor(labels.get(position));
                return true;
            }
        };
    }
}
