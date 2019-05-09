package at.searles.fractview.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import java.util.Set;
import java.util.WeakHashMap;

import at.searles.fractal.gson.Serializers;

// FIXME not necessarily sharedPrefs?
public class SharedPreferencesAccessor<V> implements Accessor<V> {

    private Class<V> type;
    private final SharedPreferences prefs;

    private final WeakHashMap<String, V> cache;

    public static <V> SharedPreferencesAccessor<V> create(String name, Context context, Class<V> type) {
        SharedPreferences prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE);
        return new SharedPreferencesAccessor<>(prefs, type);
    }

    public SharedPreferencesAccessor(SharedPreferences prefs, Class<V> type) {
        this.type = type;
        this.prefs = prefs;

        this.cache = new WeakHashMap<>();
    }

    @Override
    public boolean exists(String key) {
        return this.prefs.contains(key);
    }

    @Override
    public void add(String key, V value) {
        cache.remove(key);
        String json = Serializers.serializer().toJson(value);
        this.prefs.edit().putString(key, json).apply();
    }

    @Override
    public void remove(String key) {
        cache.remove(key);
        this.prefs.edit().remove(key).apply();
    }

    @Override
    public void rename(String oldKey, String newKey) {
        cache.remove(oldKey);
        cache.remove(newKey);

        String stringValue = this.prefs.getString(oldKey, null);
        this.prefs.edit().remove(oldKey).putString(newKey, stringValue).apply();
    }

    @Override
    public V get(String key) {
        V v = cache.get(key);

        if(v != null) {
            return v;
        }

        String json = this.prefs.getString(key, null);

        if(json == null) {
            return null;
        }

        return Serializers.serializer().fromJson(json, type);
    }

    @Override
    public Bitmap icon(String key) {
        // TODO abstract
        return null;
    }

    @Override
    public String subtitle(String key) {
        // TODO abstract
        return null;
    }

    @Override
    public Set<String> keys() {
        return prefs.getAll().keySet();
    }
}
