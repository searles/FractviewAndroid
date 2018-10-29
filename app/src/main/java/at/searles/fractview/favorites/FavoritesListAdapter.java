package at.searles.fractview.favorites;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractview.Commons;
import at.searles.fractview.R;

/**
 * This adapter manages all fractal entries. The main item is a
 * TreeMap that represents all Favorite entries.
 */
public class FavoritesListAdapter extends BaseAdapter {

    private final FavoritesActivity activity;
    private final FavoritesAccessor accessor;

    FavoritesListAdapter(FavoritesActivity activity, FavoritesAccessor accessor) {
        this.activity = activity;
        this.accessor = accessor;
    }

    @Override
    public int getCount() {
        return accessor.entriesCount();
    }

    @Override
    public FavoriteEntry getItem(int position) {
        return accessor.valueAt(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            // fixme avoid passing null
            convertView = activity.getLayoutInflater().inflate(R.layout.list_entry_with_icon, null);
        }

        ImageView iconView = convertView.findViewById(R.id.iconView);
        TextView titleView = convertView.findViewById(R.id.titleView);
        TextView descriptionView = convertView.findViewById(R.id.descriptionView);

        String key = accessor.keyAt(position);
        FavoriteEntry entry = accessor.value(key);

        Bitmap icon = Commons.fromPNG(entry.icon);

        iconView.setImageBitmap(icon);

        titleView.setText(key);
        descriptionView.setText(entry.description);

        convertView.invalidate();

        return convertView;
    }
}