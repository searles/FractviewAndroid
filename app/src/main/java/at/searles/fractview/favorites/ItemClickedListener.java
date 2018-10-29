package at.searles.fractview.favorites;

import android.content.Intent;
import android.view.View;
import android.widget.AdapterView;

import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractview.fractal.BundleAdapter;
import at.searles.fractview.ui.DialogHelper;

class ItemClickedListener implements AdapterView.OnItemClickListener {
    private FavoritesListAdapter adapter;

    ItemClickedListener(FavoritesListAdapter adapter) {
        this.adapter = adapter;
    }

    // On click return the selected fractal.
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
        FavoritesActivity activity = (FavoritesActivity) parent.getContext();

        // get bookmark
        FavoriteEntry entry = adapter.getItem(index);

        if (entry.fractal != null) {
            // This is due to a bug in an old version.
            Intent data = new Intent();
            data.putExtra(FavoritesActivity.FRACTAL_INDENT_LABEL, BundleAdapter.toBundle(entry.fractal));
            activity.setResult(1, data);
            activity.finish();
        } else {
            // FIXME better message
            DialogHelper.error(activity, "not available. If you think it should be, please report a bug feedback.");
        }
    }
}
