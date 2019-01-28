package at.searles.fractview.main;

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
    private final String id;
    int owner; // this is updated on a regular basis by the provider if a fractal is removed.

    private double[] position; // actual position; cached because parsing is expensive.
    private boolean isCplx; // is the backing element a complex number or an expression?

    private int color;

    InteractivePoint(FractalProviderFragment parent, String id, int owner) {
        this.parent = parent;
        this.id = id;
        this.owner = owner;

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
            parent.setParameterValue(id, owner, value);
        } else {
            parent.setParameterValue(id, owner, value.toString());
        }
    }

    /**
     *
     * @return false if there is no value for this point (ie the point should be deleted)
     */
    public boolean update() {
        if(!parent.parameterExists(id, owner)) {
            // this check is necessary because parameterValue might generalize owner.
            return false;
        }

        Object parameterValue = parent.getParameterValue(id, owner);

        if (parameterValue == null) {
            throw new NullPointerException("no parameter although parameterExists was true...");
        }

        return updatePosition(parameterValue);
    }

    public double[] position() {
        return position;
    }

    public String id() {
        return id;
    }

    public int owner() {
        return owner;
    }

    public boolean is(String id, int owner) {
        return this.id.equals(id) && this.owner == owner;
    }

    public int color() {
        return this.color;
    }

    public void setColorFromWheel(int index, int count) {
        this.color = Color.HSVToColor(new float[]{ });
    }

    public String toString() {
        return id + "/" + owner;
    }
}
