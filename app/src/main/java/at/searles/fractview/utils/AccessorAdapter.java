package at.searles.fractview.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import at.searles.fractview.R;

public class AccessorAdapter<V> extends BaseAdapter {

    private Context context;
    private Accessor<V> accessor;
    private List<String> keyOrder;
    private boolean invalid;

    public AccessorAdapter(Context context, Accessor<V> accessor) {
        this.context = context;
        this.accessor = accessor;
        keyOrder = new ArrayList<>();
    }

    @Override
    public int getCount() {
        return accessor.keys().size();
    }

    @Override
    public Object getItem(int position) {
        validate();
        return accessor.get(keyOrder.get(position));
    }

    private void validate() {
        // TODO update key order if necessary
        if(!invalid) {
            return;
        }

        keyOrder.clear();
        keyOrder.addAll(accessor.keys());

        // TODO fix order.
        Collections.sort(keyOrder);
    }

    @Override
    public long getItemId(int position) {
        // TODO maybe if no icon is there, draw sth different?
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.list_entry_with_icon, parent);
        }

        ImageView iconView = convertView.findViewById(R.id.iconView);
        TextView titleView = convertView.findViewById(R.id.titleView);
        TextView descriptionView = convertView.findViewById(R.id.descriptionView);

        String key = keyOrder.get(position);

        Bitmap icon = accessor.icon(key); // FIXME or drawable?
        String subtitle = accessor.subtitle(key);

        iconView.setImageBitmap(icon);

        titleView.setText(key);
        descriptionView.setText(subtitle != null ? subtitle : "");

        convertView.invalidate();

        return convertView;
    }
}
