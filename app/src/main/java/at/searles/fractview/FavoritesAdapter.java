package at.searles.fractview;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter for fractals with bitmaps
 */
public class FavoritesAdapter extends BaseAdapter {

	private final Activity context;
	private final List<? extends FractalEntry> entries;

	public FavoritesAdapter(Activity context, List<? extends FractalEntry> entries) {
		this.context = context;
		this.entries = entries;
	}

	@Override
	public int getCount() {
		return entries.size();
	}

	@Override
	public FractalEntry getItem(int index) {
		return entries.get(index);
	}

	@Override
	public long getItemId(int pos) {
		return pos;
	}

	@Override
	public View getView(int index, View view, ViewGroup parent) {
		if (view == null) {
			// fixme avoid passing null
			view = context.getLayoutInflater().inflate(R.layout.favorite_entry_layout, null);
		}

		ImageView iconView = (ImageView) view.findViewById(R.id.iconView);
		TextView titleView = (TextView) view.findViewById(R.id.titleView);

		FractalEntry entry = getItem(index);

		iconView.setImageBitmap(entry.icon());
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		titleView.setText(entry.title());

		return view;
	}
}
