package at.searles.fractview;


import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import at.searles.fractal.Fractal;

/**
 * Adapter for all kinds of fractal entries. Which fractal
 * is determined by the concrete implementation.
 */

public abstract class FractalEntryListAdapter extends BaseAdapter {

    private final Activity context;

    public FractalEntryListAdapter(Activity context) {
        this.context = context;
    }

    @Override
    public abstract int getCount();

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

        Bitmap icon = getIcon(index);
        iconView.setImageBitmap(icon);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        titleView.setText(getTitle(index));
        descriptionView.setText(getDescription(index));

        return view;
    }
}
