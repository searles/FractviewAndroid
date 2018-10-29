package at.searles.fractview.favorites;

import com.google.gson.JsonSyntaxException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import at.searles.fractal.gson.Serializers;
import at.searles.strings.CharComparators;
import at.searles.strings.NaturalStringComparator;

/**
 * FIXME move to utils
 */

public class CachedKeyAdapter<A> {
    private ArrayList<String> keys;
    private LinkedHashMap<String, String> jsonEntries;
    private HashMap<String, A> entries;

    /**
     * If true, then keys must be updated when it is accessed.
     */
    private boolean invalid = false;
    private Type type;

    public CachedKeyAdapter(Type type) {
        this.type = type;
        keys = new ArrayList<>();
        jsonEntries = new LinkedHashMap<>();
        entries = new HashMap<>();

        invalid = false;
    }

    private void validate() {
        if (invalid) {
            invalid = false;

            keys.clear();
            keys.addAll(jsonEntries.keySet());
            keys.sort(new NaturalStringComparator(CharComparators.CASE_SENSITIVE));
        }
    }

    public void clear() {
        jsonEntries.clear();
        invalid = true;
    }

    public void put(String key, String value) {
        jsonEntries.put(key, value);
        invalid = true;
    }

    public void reset() {
        jsonEntries.clear();
        invalid = true;
    }

    public int size() {
        return jsonEntries.size();
    }

    public String keyAt(int position) {
        validate();
        return keys.get(position);
    }

    public A get(String key) {
        A value = entries.get(key);

        if(value == null) {
            value = load(key);
        }

        return value;
    }

    public int indexOf(String key) {
        validate();
        return keys.indexOf(key);
    }

    private A load(String key) {
        String json = jsonEntries.get(key);

        try {
            A value = Serializers.serializer().fromJson(json, type);
            entries.put(key, value);
            return value;
        } catch (JsonSyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
}
