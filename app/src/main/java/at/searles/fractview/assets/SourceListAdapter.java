package at.searles.fractview.assets;

import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.searles.fractal.entries.SourceEntry;
import at.searles.fractview.R;

@Deprecated
public class SourceListAdapter extends BaseAdapter {

    private final Activity activity;

    private SourceEntry currentEntry;
    private List<SourceEntry> entries;
    private Map<String, Bitmap> icons;

    SourceListAdapter(Activity activity, String current) {
        this.activity = activity;
        initEntries(current);
    }

    private void initEntries(String currentSourceCode) {
        this.currentEntry = new SourceEntry(null, "Current source", currentSourceCode);

        this.entries = new ArrayList<>();
        this.entries.add(currentEntry);

        // TODO add other entries
        //this.entries = SourceEntry.entries(context.getAssets());

        // TODO init icons
        this.icons = new HashMap<>();
    }

    @Override
    public int getCount() {
        return entries.size();// + customEntries.size();
    }

    @Override
    public SourceEntry getItem(int position) {
        return entries.get(position);
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

        ImageView iconView = (ImageView) convertView.findViewById(R.id.iconView);
        TextView titleView = (TextView) convertView.findViewById(R.id.titleView);
        TextView descriptionView = (TextView) convertView.findViewById(R.id.descriptionView);

        // fixme
//        Bitmap icon = getIcon(index);
//
//        iconView.setImageBitmap(icon);
//
//        titleView.setText(getTitle(index));
//        descriptionView.setText(getDescription(index));

        convertView.invalidate();

        return convertView;
    }
}
