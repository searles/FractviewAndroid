package at.searles.fractal;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.searles.fractview.fractal.FractalProviderListener;
import at.searles.meelan.symbols.ExternData;

/**
 * There is one source file for a fractal provider. It is compiled and that way,
 * the instance of ExternData in the fractalprovider is filled.
 */
public class FractalProvider {

    private static final String SEPARATOR = "::";

    /**
     * Listeners
     */
    private Map<String, List<FractalProviderListener>> listeners;

    /**
     * Which parameters should not be shared?
     */
    private HashSet<String> individualParameters; // without '::'.

    /**
     * Which parameters do exist? Includes '::'.
     */
    private LinkedHashMap<String, ExternData.Entry> entries;

    private LinkedHashMap<String, Object> values;

    /**
     * Key fractals. These are initialized in constructor and remain fixed.
     */
    private LinkedHashMap<String, Fractal> fractals;

    public static FractalProvider singleFractal(Fractal fractal) {
        FractalProvider fractalProvider = new FractalProvider(Collections.emptyList());

        fractalProvider.fractals.put("", fractal);
        for(ExternData.Entry entry : fractal.data().entries()) {
            Object value = fractal.data().customValue(entry.id);

            if(value != null) {
                fractalProvider.values.put(entry.id, value);
            }
        }

        return fractalProvider;
    }

    private FractalProvider(Collection<String> individualParameters) {
        this.individualParameters = new HashSet<>(individualParameters);
        this.fractals = new LinkedHashMap<>();
        this.listeners = new HashMap<>();
        this.entries = new LinkedHashMap<>();
        this.values = new LinkedHashMap<>();
    }

    /**
     * This one is called from the editor parameter list.
     * @param value null if it should be reset to default.
     */
    public void set(String id, Object value) {
        if(isAnnotated(id)) {
            putIntoValues(id, value);

            String label = label(id);
            putValueIntoFractal(id(id), label, value);
        } else {
            putIntoValues(id, value);
            putValueIntoAllFractals(id, value);
        }
    }

    /**
     * This one is called from on-screen editors.
     * @param label is ignored if it is a non-individual parameter.
     */
    public void set(String id, String label, Object value) {
        // check whether it should be set in all fractals
        if(individualParameters.contains(id)) {
            // only set in label
            String annotatedId = annotatedId(id, label);

            // update 'values'
            putIntoValues(annotatedId, value);

            // update fractal
            putValueIntoFractal(id, label, value);
        } else {
            putIntoValues(id, value);
            putValueIntoAllFractals(id, value);
        }
    }

    private void putIntoValues(String id, Object value) {
        if(value == null) {
            values.remove(id);
        } else {
            values.put(id, value);
        }
    }

    private void putValueIntoAllFractals(String id, Object value) {
        for(String label : fractals.keySet()) {
            putValueIntoFractal(id, label, value);
        }
    }

    private void putValueIntoFractal(String id, String label, Object value) {
        Fractal fractal = fractals.get(label);
        if(fractal.data().setCustomValue(id, value)) {
            // TODO 2018-07-11: Some parameters do not require recompilation.
            // Eg palette is only set in data.

            // recompile fractal
            fractal.compile();

            // notify others
            fireFractalChanged(label);

            // invalidate.
            invalidate();
        }
    }

    private void invalidate() {
        entries = null; // this one must be reset.
    }

    /**
     * Get fractal. Label might be further specified, eg in a video with a frame
     * parameter.
     * @param label
     * @return
     */
    public Fractal get(String label) {
        return fractals.get(label);
    }

    private String annotatedId(String id, String label) {
        return id + SEPARATOR + label;
    }

    private boolean isAnnotated(String id) {
        return id.contains(SEPARATOR);
    }

    private String id(String id) {
        return id.substring(0, id.indexOf(SEPARATOR));
    }

    private String label(String id) {
        return id.substring(id.indexOf(SEPARATOR) + SEPARATOR.length());
    }

    private void initializeEntries() {
        entries.clear();

        LinkedHashMap<String, ExternData.Entry> genericParameters = new LinkedHashMap<>();

        for(Map.Entry<String, Fractal> fractal : fractals.entrySet()) {
            for(ExternData.Entry entry : fractal.getValue().data().entries()) {
                // TODO 2018-07-11: Some parameters are hardcoded. These
                // could be write-protected in the UI? Or do not insert them here?

                if(individualParameters.contains(entry.id)) {
                    entries.put(annotatedId(entry.id, fractal.getKey()), entry);
                } else {
                    genericParameters.put(entry.id, entry);
                }
            }
        }

        entries.putAll(genericParameters);
    }

    public Iterable<String> parameterIds() {
        if(entries == null) {
            initializeEntries();
        }

        return entries.keySet();
    }

    public String getDescription(String id) {
        if(entries == null) {
            initializeEntries();
        }

        ExternData.Entry entry = entries.get(id);

        int separatorPos = id.indexOf(SEPARATOR);

        if(separatorPos != -1) {
            String label = id.substring(separatorPos + SEPARATOR.length());
            return String.format("%s [%s]", entry.description, label);
        }

        return entry.description;
    }

    public Type getType(String id) {
        if(entries == null) {
            initializeEntries();
        }

        return Type.fromString(entries.get(id).type);
    }

    public boolean isDefault(String id) {
        return values.containsKey(id);
    }

    public Object value(String id) {
        if(entries == null) {
            initializeEntries();
        }

        Object value = values.get(id);
        return value != null ? value : entries.get(id).defaultValue;
    }

    private void fireFractalChanged(String label) {
        List<FractalProviderListener> listenerList = this.listeners.get(label);

        if(listenerList == null) {
            return;
        }

        Fractal fractal = fractals.get(label);

        for(FractalProviderListener listener : listenerList) {
            listener.fractalModified(fractal);
        }
    }

    public void addListener(String label, FractalProviderListener listener) {
        List<FractalProviderListener> listenerList = this.listeners.get(label);

        if(listenerList == null) {
            listenerList = new LinkedList<>();
            this.listeners.put(label, listenerList);
        }

        listenerList.add(listener);
    }

    public void removeListener(String label, FractalProviderListener listener) {
        List<FractalProviderListener> listenerList = listeners.get(label);

        if(listenerList != null) {
            listenerList.remove(listener);
        }
    }
}
