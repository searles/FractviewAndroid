package at.searles.fractview.provider;

import android.graphics.Color;

import at.searles.fractal.ParserInstance;
import at.searles.math.Cplx;
import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.values.CplxVal;
import at.searles.meelan.values.Int;
import at.searles.meelan.values.Real;

public class InteractivePoint {
    private FractalProviderFragment parent;
    private final String key;
    private final int id; // this is updated on a regular basis by the provider if a fractal is removed.

    private double[] position; // actual position; cached because parsing is expensive.
    private boolean isCplx; // is the backing element a complex number or an expression?

    private int color;

    InteractivePoint(FractalProviderFragment parent, String key, int id) {
        this.parent = parent;
        this.key = key;
        this.id = id;

        this.position = new double[]{0, 0};

        update();
    }

    private boolean updatePosition(Object value) throws MeelanException {
        this.isCplx = value instanceof Cplx;
        if(isCplx) {
            // XXX no cplx in the long run
            position[0] = ((Cplx) value).re();
            position[1] = ((Cplx) value).im();

            return true;
        }

        return pointFromExpr(value.toString());
    }

    private boolean pointFromExpr(String expr) throws MeelanException {
        Tree parsedExpr = ParserInstance.get().parseExpr(expr);
        Tree preprocessedExpr = new Ast(parsedExpr).preprocess(id -> {
            throw new MeelanException("Cannot resolve " + id, parsedExpr);
        });

        if(preprocessedExpr instanceof CplxVal) {
            Cplx num = ((CplxVal) preprocessedExpr).value();
            position[0] = num.re();
            position[1] = num.im();
            return true;
        } else if(preprocessedExpr instanceof Real) {
            Number num = ((Real) preprocessedExpr).value();
            position[0] = num.doubleValue();
            position[1] = 0;
            return true;
        } else if(preprocessedExpr instanceof Int) {
            Number num = ((Int) preprocessedExpr).value();
            position[0] = num.intValue();
            position[1] = 0;
            return true;
        }

        return false;
    }

    public void setValue(double[] newValue) {
        Cplx value = new Cplx(newValue[0], newValue[1]);

        if(isCplx) {
            // position is updated indirectly via listener.
            parent.setParameterValue(key, id, value);
        } else {
            parent.setParameterValue(key, id, value.toString());
        }
    }

    /**
     *
     * @return false if there is no value for this point (ie the point should be deleted)
     */
    public boolean update() {
        Object parameterValue = parent.getParameterValue(key, id);
        return parameterValue != null && updatePosition(parameterValue);

    }

    public double[] position() {
        return position;
    }

    public String key() {
        return key;
    }

    public int id() {
        return id;
    }

    public boolean is(String id, int owner) {
        return this.key.equals(id) && this.id == owner;
    }

    public int color() {
        return this.color;
    }

    public void setColorFromWheel(int index, int count) {
        this.color = Color.HSVToColor(new float[]{360 * index / (float) count, 1, 1});
    }

    public String toString() {
        return key + "/" + id;
    }
}
