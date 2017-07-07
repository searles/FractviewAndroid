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

    // FIXME Move this into some dedicated other class private final IndexedKeyMap<FractalEntry> map;
    // private final Map<String, Bitmap> iconCache;
    private final Activity context;

    public FractalEntryListAdapter(Activity context) {
        this.context = context;
        this.map = new IndexedKeyMap<>();
        this.iconCache = new HashMap<>();
    }

    /*public void add(FractalEntry entry) {
        // FIXME: Flag whether it should be kept sorted.
        map.add(entry.title(), entry);
    }

    public void remove(int index) {
        String key = map.keyAt(index);
        map.remove(index);
        iconCache.remove(key);
    }*/

    @Override
    public int getCount() {
        return map.size();
    }

    @Override
    public abstract Fractal getItem(int position);
    
    public abstract Bitmap getIcon(int position);

    public abstract String getTitle(int position);
    
    public abstract String getDescription(int position);
    
    public abstract void showOptions(int position);

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
        Button optionsButton = (Button) view.findViewById(R.id.optionsButton);
        
        FractalEntry entry = getItem(index);

        Bitmap icon = getIcon(index);

        /*if(iconCache.containsKey(entry.title())) {
            icon = iconCache.get(entry.title());
        } else {
            // decode here but keep it in cache
            icon = BitmapFactory.decodeByteArray(entry.iconBinary(), 0, entry.iconBinary().length);
            iconCache.put(entry.title(), icon);
        }*/

        // FIXME Can be null!
        iconView.setImageBitmap(icon);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        titleView.setText(entry.title());
        descriptionView.setText(entry.description());

        return view;
    }
}
