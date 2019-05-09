package at.searles.fractview.favorites;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.support.v4.content.FileProvider;

import com.google.gson.stream.JsonWriter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import at.searles.fractal.entries.FavoriteEntry;
import at.searles.fractal.gson.Serializers;
import at.searles.fractview.Commons;
import at.searles.fractview.ui.DialogHelper;
import at.searles.fractview.utils.CachedKeyAdapter;
import at.searles.fractview.utils.SharedPrefsHelper;
import at.searles.fractview.utils.SharedPrefsHelperException;

/**
 * Common purpose class to access shared preferences
 */
public class FavoritesAccessor implements FractalAccessor {

    public static final String FAVORITES_SHARED_PREF = "favorites";
    private static final String FILE_PROVIDER = "at.searles.fractview.fileprovider";
    private final SharedPreferences prefs;
    private Context context;

    private CachedKeyAdapter<FavoriteEntry> entries;
    private boolean invalid;

    FavoritesAccessor(Context context) {
        // Fetch shared preferences
        this.prefs = context.getSharedPreferences(
                FAVORITES_SHARED_PREF,
                Context.MODE_PRIVATE);
        this.context = context;

        this.entries = new CachedKeyAdapter<>(FavoriteEntry.class);
        invalid = true;
    }

    public int entriesCount() {
        if(invalid) initializeEntries();
        return entries.size();
    }

    private void initializeEntries() {
        this.entries.clear();

        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            this.entries.put(entry.getKey(), (String) entry.getValue());
        }

        invalid = false;
    }

    public FavoriteEntry valueAt(int position) {
        return entries().get(entries.keyAt(position));
    }

    private CachedKeyAdapter<FavoriteEntry> entries() {
        if(invalid) initializeEntries();
        return entries;
    }

    public String keyAt(int position) {
        return entries().keyAt(position);
    }

    public FavoriteEntry value(String key) {
        return entries().get(key);
    }

    // ===== Methods that access the shared preferences =====

    /**
     * @param oldKey The old key
     * @param newKey The new key. Must not be empty
     * @return The new name which is newKey, possibly enriched with some index.
     * @throws SharedPrefsHelperException if there was some mistake.
     */
    String rename(String oldKey, String newKey) {
        invalid = true;
        return SharedPrefsHelper.renameKey(prefs, oldKey, newKey);
    }

    int indexOf(String key) {
        if(invalid) initializeEntries();
        return entries.indexOf(key);
    }

    void deleteEntries(List<String> keys) {
        for(String key : keys) {
            SharedPrefsHelper.removeEntry(context, key, prefs);
            invalid = true;
        }
    }

    void importEntries(FavoritesActivity activity, FavoriteEntry.Collection newEntries) {
        // Find duplicates
        Map<String, FavoriteEntry> nonDuplicates = new HashMap<>();
        Map<String, FavoriteEntry> duplicates = new HashMap<>();

        for (Map.Entry<?, ?> entry : newEntries.entrySet()) {
            FavoriteEntry favEntry = (FavoriteEntry) entry.getValue();

            String key = (String) entry.getKey();

            if (prefs.contains(key)) {
                duplicates.put(key, favEntry);
            } else {
                nonDuplicates.put(key, favEntry);
            }
        }

        if (duplicates.isEmpty()) {
            Set<String> addedKeys = new TreeSet<>();

            // Add non-duplicates
            addMap(nonDuplicates, addedKeys);
            invalid = true;

            activity.selectKeys(addedKeys);
        } else {
            // Ask what to do with duplicates
            DialogHelper.showOptionsDialog(activity, "Pick an option for new entries with already existing keys", new CharSequence[]{
                    "Do not add items with existing keys",
                    "Append index to new entries",
                    "Overwrite existing entries"
            }, false, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Set<String> addedKeys = new TreeSet<>();

                    SharedPreferences.Editor editor = prefs.edit();

                    // Add non-duplicates
                    addMap(nonDuplicates, addedKeys);

                    // and now pick strategy
                    switch (which) {
                        case 0:
                            // Do not add duplicates
                            break; // this is easy.
                        case 1:
                            // add duplicates with index
                            addMapWithIndex(duplicates, addedKeys);
                            break;
                        case 2:
                            // override
                            addMap(duplicates, addedKeys);
                            break;
                    }

                    editor.apply();
                    activity.selectKeys(addedKeys);
                }
            });
        }
    }

    private void addMap(Map<String, FavoriteEntry> map, Set<String> addedKeys) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, FavoriteEntry> entry : map.entrySet()) {
            String key = entry.getKey();
            FavoriteEntry favEntry = entry.getValue();

            // add it
            addedKeys.add(key);
            editor.putString(key, Serializers.serializer().toJson(favEntry));
        }

        editor.apply();
        invalid = true;
    }

    private void addMapWithIndex(Map<String, FavoriteEntry> map, Set<String> addedKeys) {
        SharedPreferences.Editor editor = prefs.edit();
        for (Map.Entry<String, FavoriteEntry> entry : map.entrySet()) {
            String key = SharedPrefsHelper.nextUnusedName(prefs, entry.getKey());
            FavoriteEntry favEntry = entry.getValue();

            // add it
            addedKeys.add(key);
            editor.putString(key, Serializers.serializer().toJson(favEntry));
        }

        editor.apply();
        invalid = true;
    }

    void exportEntries(Context context, List<String> keys) {
        // Fetch map from adapter
        JsonWriter writer = null;

        // Create a map
        // FIXME:
        //
//        Type type = new TypeToken<HashMap<Integer, Employee>>(){}.getType();
//        HashMap<Integer, Employee> clonedMap = gson.fromJson(jsonString, type);
        FavoriteEntry.Collection collection = new FavoriteEntry.Collection();

        for (String key : keys) {
            collection.put(key, entries().get(key));
        }

        try {
            // Write json to temp file.

            // FIXME pick better file name
            File textFile = File.createTempFile("fractview_collection-" + Commons.timestamp(),
                    ".txt", context.getExternalCacheDir()); // extension fv for fractview

            BufferedWriter bw = new BufferedWriter(new FileWriter(textFile));
            writer = new JsonWriter(bw);
            writer.setIndent("  ");

            Serializers.serializer().toJson(collection, FavoriteEntry.Collection.class, writer);

            // Share text file
            Uri contentUri = FileProvider.getUriForFile(context, FILE_PROVIDER, textFile);

            // after it was successfully saved, share it.
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_STREAM, contentUri);
            context.startActivity(Intent.createChooser(share, "Share Collection"));
        } catch (IOException e) {
            e.printStackTrace();
            DialogHelper.error(context, e.getMessage());
        } finally {
            try {
                if (writer != null) writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
