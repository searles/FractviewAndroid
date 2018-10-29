package at.searles.fractview;

import android.content.Context;
import android.content.SharedPreferences;

import at.searles.fractal.gson.Serializers;
import at.searles.fractview.utils.CharUtil;
import at.searles.fractview.utils.SharedPrefsHelperException;

/**
 * Helper for Shared Preferences
 */
public class SharedPrefsHelper {

    public static String renameKey(SharedPreferences prefs, String oldKey, String newKey) {
        if(newKey.isEmpty()) {
            throw new SharedPrefsHelperException("Name must not be empty");
        }

        if(oldKey.equals(newKey)) {
            return newKey;
        }

        String value = prefs.getString(oldKey, null);

        if(value == null) {
            throw new SharedPrefsHelperException("Content was empty");
        }

        String unusedNewKey = nextUnusedName(prefs, newKey);
        prefs.edit().putString(unusedNewKey, value).remove(oldKey).apply();

        return unusedNewKey;
    }

    public static <A> void putWithUniqueKey(Context context, String key, A element, String preferencesName) {
        String entryString = Serializers.serializer().toJson(element);

        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE);

        String uniqueKey = nextUnusedName(preferences, key);
        preferences.edit().putString(uniqueKey, entryString).apply();
    }

    public static String nextUnusedName(SharedPreferences prefs, String name) {
        while(prefs.contains(name)) {
            name = CharUtil.nextIndex(name);
        }

        return name;
    }

    public static String loadFromSharedPreferences(Context context, String name, String preferencesName) {
        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE);

        return preferences.getString(name, null);
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
