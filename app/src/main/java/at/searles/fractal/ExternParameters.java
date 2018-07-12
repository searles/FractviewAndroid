package at.searles.fractal;

import java.util.HashMap;
import java.util.Map;

public class ExternParameters extends AbstractParameters {

    private final Map<String, Map<String, Object>> customValues;

    public ExternParameters() {
        customValues = new HashMap<>();
    }

    @Override
    public void setCustomValue(String id, String type, Object value) {
        Map<String, Object> typeTable = customValues.get(type);

        if(typeTable == null) {
            typeTable = new HashMap<>();
            customValues.put(type, typeTable);
        }

        typeTable.put(id, value);
    }

    @Override
    public Object customValue(String id, String type) {
        Map<String, Object> typeTable = customValues.get(type);
        return typeTable == null ? null : typeTable.get(id);
    }
}
