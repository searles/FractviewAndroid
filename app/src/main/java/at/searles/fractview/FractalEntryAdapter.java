package at.searles.fractview;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.searles.fractal.FractalLabel;

/**
 * Adapter for fractals with bitmaps
 */
public class FractalEntryAdapter extends BaseAdapter {

	private final Activity context;
	private final List<String> keys;
	private final Map<String, FractalLabel> entries;

	public FractalEntryAdapter(Activity context) {
		this.context = context;

		keys = new ArrayList<>();
		this.entries = new HashMap<>();
	}

    public void setData(Map<String, ? extends FractalLabel> entryMap) {
		keys.clear();
		entries.clear();

		keys.addAll(entryMap.keySet());
		Collections.sort(keys);

		this.entries.putAll(entryMap);
	}

	public void setData(List<? extends FractalLabel> entryList) {
		keys.clear();
		entries.clear();

		for(FractalLabel entry : entryList) {
			keys.add(entry.title());
			this.entries.put(entry.title(), entry);
		}
	}

	@Override
	public int getCount() {
		return entries.size();
	}

	@Override
	public FractalLabel getItem(int index) {
		return entries.get(keys.get(index));
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

		FractalLabel entry = getItem(index);

		iconView.setImageBitmap(entry.icon());
		iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
		titleView.setText(entry.title());

		return view;
	}
}
