package at.searles.fractal;

import java.util.HashMap;
import java.util.Map;

public class RawFractal {
    public final String sourceCode;
    public final Map<String, Parameter> data;

    public RawFractal(String sourceCode) {
        this.sourceCode = sourceCode;
        this.data = new HashMap<>();
    }

    public void add(String id, Type type, Object value) {
        data.put(id, new Parameter(type, value));
    }

    public static class Parameter {
        public final Type type;
        public final Object value;

        private Parameter(Type type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
}
