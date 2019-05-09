package at.searles.fractview.palettes;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.HashMap;
import java.util.Map;

import at.searles.fractal.entries.FavoriteEntry;

public class PalettesListAdapter extends BaseAdapter {

    private final Activity activity;

//    final SharedPreferences prefs;
//
//    private IndexedKeyMap<String> jsonEntries;
    private Map<String, FavoriteEntry> entries;

    public PalettesListAdapter(Activity activity) {
        this.activity = activity;

//        // Fetch shared preferences
//        this.prefs = activity.getSharedPreferences(
//                FAVORITES_SHARED_PREF,
//                Context.MODE_PRIVATE);
//
//        this.jsonEntries = new IndexedKeyMap<>();
        this.entries = new HashMap<>();

//        initializeAdapter();
    }

//    protected void initializeAdapter() {
//        this.entries.clear();
//        this.jsonEntries.clear();
//
//        for (String key : this.prefs.getAll().keySet()) {
//            String value = this.prefs.getString(key, null);
//
//            if (value != null) {
//                this.jsonEntries.add(key, value);
//            } else {
//                Log.e(getClass().getName(), "Value for key " + key + " was null!");
//            }
//        }
//
//        this.jsonEntries.sort();
//        notifyDataSetChanged();
//    }

    @Override
    public int getCount() {
        return 0;// jsonEntries.size();
    }

    public int getKeyIndex(String key) {
        return 0;//jsonEntries.indexAt(key);
    }

    @Override
    public FavoriteEntry getItem(int position) {
//        String key = jsonEntries.keyAt(position);
//
//        FavoriteEntry entry = this.entries.get(key);
//
//        if (entry == null) {
//            String json = jsonEntries.valueAt(position);
//
//            try {
//                entry = Serializers.serializer().fromJson(json, FavoriteEntry.class);
//                entry.setKey(key);
//                this.entries.put(key, entry);
//            } catch (Throwable th) {
//                entry = null;
//            }
//        }
//
//        return entry;
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}