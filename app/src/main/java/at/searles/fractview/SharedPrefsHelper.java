package at.searles.fractview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import at.searles.fractal.gson.Serializers;

/**
 * Helper for Shared Preferences
 */
public class SharedPrefsHelper {

    // FIXME title names are now out of sync from keys.

    public static <A> void storeInSharedPreferences(Context context, String name, A element, String preferencesName) {
        String entryString = Serializers.serializer().toJson(element);

        Log.d("Shared Pref", "Storing " + entryString);

        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName,
                Context.MODE_PRIVATE);

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

}
