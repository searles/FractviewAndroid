package at.searles.utils;

import java.util.ArrayList;
import java.util.Collections;
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

    /**
     * Finds the first or last entry with the given prefix
     * @param prefix
     * @return
     */
    public int findPrefix(String prefix, int startRange, int endRange, boolean findFirstIndex) {
        // policy, first entry, last entry or any.

        int l = startRange; // l is inclusive
        int r = endRange; // r is inclusive

        while(l <= r) {
            // if we look for the last index, we round up.
            int m = (l + r + (findFirstIndex ? 0 : 1)) / 2;

            int cmp = CharUtil.cmpPrefix(this.keyAt(m), prefix);

            if(cmp > 0) {
                r = m - 1;
            } else if(cmp < 0) {
                l = m + 1;
            } else {
                // the first one is in the interval l .. m, the last one in m .. r
                if(findFirstIndex) {
                    if(l == m) {
                        return l;
                    } else {
                        r = m;
                    }
                } else {
                    if(m == r) {
                        return m;
                    } else {
                        l = m;
                    }
                }
            }
        }

        return -1;
    }


    public synchronized void sort() {
        Collections.sort(indexed, new Comparator<String>() {
            @Override
            public int compare(String s1, String s2) {
                return s1.compareToIgnoreCase(s2);
            }
        });

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
