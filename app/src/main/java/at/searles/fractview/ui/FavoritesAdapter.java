package at.searles.fractview.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import at.searles.fractview.R;
import at.searles.fractview.fractal.FavoriteEntry;

import java.util.List;
import java.util.Map;

/**
 * Created by searles on 13.03.16.
 */
public class FavoritesAdapter extends BaseAdapter {

	final Activity context;
	final List<String> labels;
	final Map<String, FavoriteEntry> favorites;

	public FavoritesAdapter(Activity context, List<String> labels, Map<String, FavoriteEntry> favorites) {
		this.context = context;
		this.labels = labels;
		this.favorites = favorites;
	}

	@Override
	public int getCount() {
		return labels.size();
	}

	@Override
	public FavoriteEntry getItem(int index) {
		return favorites.get(labels.get(index));
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

		FavoriteEntry fav = getItem(index);

		iconView.setImageBitmap(fav.icon);
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		titleView.setText(labels.get(index));

		return view;
	}
}
