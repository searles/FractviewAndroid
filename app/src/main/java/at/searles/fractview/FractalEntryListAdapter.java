package at.searles.fractview;


import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import at.searles.fractal.FractalEntry;
import at.searles.utils.IndexedKeyMap;

/**
 * Created by searles on 04.07.17.
 */

public class FractalEntryListAdapter extends BaseAdapter {

    private final IndexedKeyMap<FractalEntry> list;
    private final Map<String, Bitmap> iconCache;
    private final Activity context;

    public FractalEntryListAdapter(Activity context) {
        this.context = context;
        this.list = new IndexedKeyMap<>();
        this.iconCache = new HashMap<>();
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public FractalEntry getItem(int position) {
        return list.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int index, View view, ViewGroup parent) {
        if (view == null) {
            // fixme avoid passing null
            view = context.getLayoutInflater().inflate(R.layout.fractal_entry_layout, null);
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.iconView);
        TextView titleView = (TextView) view.findViewById(R.id.titleView);
        TextView descriptionView = (TextView) view.findViewById(R.id.descriptionView);

        FractalEntry entry = getItem(index);

        Bitmap icon;

        if(iconCache.containsKey(entry.title())) {
            icon = iconCache.get(entry.title());
        } else {
            icon = BitmapFactory.decodeByteArray(entry.iconBinary(), 0, entry.iconBinary().length);
            iconCache.put(entry.title(), icon);
        }

        iconView.setImageBitmap(icon);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        titleView.setText(entry.title());
        descriptionView.setText(entry.description());

        return view;
    }
}
