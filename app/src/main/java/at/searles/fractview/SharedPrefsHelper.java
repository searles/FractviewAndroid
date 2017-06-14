package at.searles.fractview;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Map;

/**
 * Helper for Shared Preferences
 */
public class SharedPrefsHelper {

    public enum SaveMethod { Override, FindNext };

    private final SharedPreferences prefs;

    public SharedPrefsHelper(Context context, String prefsName) {
        this.prefs = context.getSharedPreferences(
                prefsName,
                Context.MODE_PRIVATE);
    }

    private String findNextAvailableName(String name) {
        // if title exists, append an index
        if(prefs.contains(name)) {
            int index = 1; // start from 1
            while(prefs.contains(name + " (" + index + ")")) {
                index ++;
            }

            name = name + " (" + index + ")";
        }

        return name;
    }

    /**
     * @param key The key for the provided shared preferences
     * @return a String stored in sharedPreferences
     */
    public String get(String key) {
        return prefs.getString(key, null);
    }

    public void add(String key, String value, SaveMethod method) {
        keyAction(key, new Commons.KeyAction() {
            @Override
            public void apply(String key) {
                add(key, value);
            }
        }, method);
    }

    private void keyAction(String key, Commons.KeyAction action, SaveMethod method) {
        if(prefs.contains(key)) {
            // now, it depends on save method.
            switch (method) {
                case Override:
                    action.apply(key);
                    break;
                case FindNext:
                    action.apply(findNextAvailableName(key));
                    break;
            }
        } else {
            action.apply(key);
        }
    }

    private void add(String key, String value) {
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(key, value);
        edit.apply();
    }

    public void rename(String key, String newKey, SaveMethod method, Context context) {
        if(prefs.contains(key) && !key.equals(newKey)) {
            String value = get(key);

            keyAction(newKey, new Commons.KeyAction() {
                @Override
                public void apply(String newKey) {
                    add(newKey, value);
                    remove(key);
                }
            }, method);
        } else {
            DialogHelper.error(context, "Shared Preferences do not contain " + key);
        }
    }

    public void remove(String key) {
        if(!prefs.contains(key)) {
            Log.e(getClass().getName(), "Trying to remove an inexistent key!");
        } else {
            SharedPreferences.Editor edit = prefs.edit();
            edit.remove(key);
            edit.apply();
        }
    }

    public Map<String, ?> getAll() {
        return prefs.getAll();
    }
}
