package at.searles.fractview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import at.searles.fractal.gson.Serializers;
import at.searles.fractview.ui.DialogHelper;
import at.searles.fractview.utils.CharUtil;

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

    /**
     *
     * @return the actual key under which this is stored
     */
    public static String storeInSharedPreferences(String name, String entryString, SharedPreferences preferences) {
        while(preferences.contains(name)) {
            name = CharUtil.nextIndex(name);
        }

        preferences.edit().putString(name, entryString).apply();

        return name;
    }

    public static String loadFromSharedPreferences(Context context, String name, String preferencesName) {
        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE);

        return preferences.getString(name, null);
    }

    /**
     *
     * @return true if successfully renamed
     */
    public static String renameKey(Context context, String oldKey, String newKey, SharedPreferences prefs) {
        // Name did not change, nothing to do.
        if(oldKey.equals(newKey)) return newKey;

        if(!newKey.isEmpty()) {
            String value = prefs.getString(oldKey, null);

            if(value != null) {
                newKey = storeInSharedPreferences(newKey, value, prefs);
                prefs.edit().remove(oldKey).apply();
                return newKey;
            } else {
                DialogHelper.error(context, "Content was empty");
            }
        } else {
            DialogHelper.error(context, "Name must not be empty");
        }

        return null;
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
