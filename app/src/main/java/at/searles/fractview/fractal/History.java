package at.searles.fractview.fractal;

import java.util.LinkedList;

import at.searles.fractal.data.FractalData;

public class History {
    private FractalData current;
    private LinkedList<FractalData> past;

    public History() {
        this.current = null;
        this.past = new LinkedList<>();
    }

    public void addToHistory(FractalData fractal) {
        if(fractal == null) {
            throw new NullPointerException("fractal in history must not be null");
        }

        if(current != null) {
            // add current to history list
            past.addLast(current);
        }

        this.current = fractal;
    }

    public boolean isEmpty() {
        return past.isEmpty();
    }

    public FractalData removeLast() {
        this.current = null;
        return past.removeLast();
    }
}
