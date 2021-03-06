package at.searles.fractview;


import android.app.Activity;
import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Adapter for all kinds of fractal entries. Which fractal
 * is determined by the concrete implementation.
 */
public abstract class FractalListAdapter<A> extends BaseAdapter {

    private final Activity context;

    protected FractalListAdapter(Activity context) {
        this.context = context;
    }

    @Override
    public abstract int getCount();

    @Override
    public abstract A getItem(int position);
    
    public abstract Bitmap getIcon(int position);

    public abstract String getTitle(int position);
    
    public abstract String getDescription(int position);

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int index, View view, ViewGroup parent) {
        if (view == null) {
            // fixme avoid passing null
            view = context.getLayoutInflater().inflate(R.layout.list_entry_with_icon, null);
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.iconView);
        TextView titleView = (TextView) view.findViewById(R.id.titleView);
        TextView descriptionView = (TextView) view.findViewById(R.id.descriptionView);

        Bitmap icon = getIcon(index);

        iconView.setImageBitmap(icon);

        titleView.setText(getTitle(index));
        descriptionView.setText(getDescription(index));

        view.invalidate();

        return view;
    }

    private void setIcon(ImageView imageView, Bitmap bitmap, int targetSize){
    }
}
