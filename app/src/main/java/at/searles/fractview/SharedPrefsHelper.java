package at.searles.fractview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import at.searles.fractal.gson.Serializers;
import at.searles.fractview.ui.DialogHelper;

/**
 * Helper for Shared Preferences
 */
public class SharedPrefsHelper {

    // FIXME key names are now out of sync from keys.

    public static <A> void storeInSharedPreferences(Context context, String name, A element, String preferencesName) {
        String entryString = Serializers.serializer().toJson(element);

        Log.d("Shared Pref", "Storing " + entryString);

        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE);

        storeInSharedPreferences(name, entryString, preferences);
    }

    public static void storeInSharedPreferences(String name, String entryString, SharedPreferences preferences) {
        if(preferences.contains(name)) {
            for(int i = 1;; ++i) {
                String indexedName = name + "(" + i + ")";
                if(!preferences.contains(indexedName)) {
                    name = indexedName;
                    break;
                }
            }
        }

        preferences.edit().putString(name, entryString).apply();
    }

    public static String loadFromSharedPreferences(Context context, String name, String preferencesName) {
        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE);

        return preferences.getString(name, null);
    }

    /**
     *
     * @param context
     * @param oldKey
     * @param newKey
     * @param prefs
     * @return true if successfully renamed
     */
    public static boolean renameKey(Context context, String oldKey, String newKey, SharedPreferences prefs) {
        // Name did not change, nothing to do.
        if(oldKey.equals(newKey)) return true;

        if(!oldKey.isEmpty()) {
            String value = prefs.getString(newKey, null);

            if(value != null) {
                storeInSharedPreferences(newKey, value, prefs);
                prefs.edit().remove(oldKey).apply();
                return true;
            } else {
                DialogHelper.error(context, "Content was empty");
            }
        } else {
            DialogHelper.error(context, "Name must not be empty");
        }

        return false;
    }

    public static boolean removeEntry(Context context, String key, SharedPreferences prefs) {
        if(prefs.contains(key)) {
            prefs.edit().remove(key).apply();
            return true;
        } else {
            return false;
        }
    }
}
