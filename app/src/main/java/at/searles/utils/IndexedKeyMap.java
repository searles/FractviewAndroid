package at.searles.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * This map is designed for
 */

public class IndexedKeyMap<A> {
    // This is too complex.

    private final Map<String, A> map = new HashMap<>();
    private final Map<String, Integer> indexMap = new HashMap<>();
    private final ArrayList<String> indexed = new ArrayList<>();

    /**
     * If invalid is true, then indexMap is not in sync, thus should
     * not be read.
     */
    private boolean invalid = false;

    public synchronized void add(String key, A value) {
        int index = indexed.size();
        map.put(key, value);
        indexed.add(key);
        indexMap.put(key, index);

        // invalid state remains the same.
    }

    public synchronized void sort() {
        indexed.sort(String::compareToIgnoreCase);
        invalid = true;
    }

    public synchronized void sort(Comparator<String> comparator) {
        indexed.sort(comparator);
        invalid = true;
    }

    protected synchronized void reindex() {
        indexMap.clear();

        for(int i = 0; i < indexed.size(); ++i) {
            indexMap.put(indexed.get(i), i);
        }

        invalid = false;
    }

    public synchronized void remove(int index) {
        String key = indexed.get(index);

        indexed.remove(index);
        map.remove(key);

        invalid = true;
    }

    public int indexAt(String key) {
        if(invalid) reindex();
        return indexMap.get(key);
    }

    public String keyAt(int index) {
        return indexed.get(index);
    }

    public A valueAt(int index) {
        return value(keyAt(index));
    }

    public A value(String key) {
        return map.get(key);
    }

    public int size() {
        return indexed.size();
    }

    public void clear() {
        indexed.clear();
        indexMap.clear();
        indexed.clear();
    }
}
