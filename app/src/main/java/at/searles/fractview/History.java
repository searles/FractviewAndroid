package at.searles.fractview;

import java.util.LinkedList;

import at.searles.fractview.fractal.Fractal;

/**
 * Created by searles on 21.05.17.
 */

public class History {
    private Fractal current;
    private LinkedList<Fractal> past;

    public History() {
        this.current = null;
        this.past = new LinkedList<>();
    }

    public void addToHistory(Fractal fractal) {
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

    public Fractal removeLast() {
        this.current = null;
        return past.removeLast();
    }
}
