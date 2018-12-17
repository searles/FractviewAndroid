package at.searles.fractview.main;

import at.searles.math.Cplx;

public class InteractivePoint {
    private FractalProviderFragment parent;
    private final String id;
    int owner; // this is updated on a regular basis by the provider if a fractal is removed.

    private Class<?> parameterClass;

    private double[] position;
    private boolean invalid; // update position at next access

    InteractivePoint(FractalProviderFragment parent, String id, int owner) {
        this.parent = parent;
        this.id = id;
        this.owner = owner;

        this.position = new double[]{0, 0};
        this.invalid = true;
    }

    private void updatePosition(Object value) {
        parameterClass = value.getClass();

        if (parameterClass == Cplx.class) {
            position[0] = ((Cplx) value).re();
            position[1] = ((Cplx) value).im();
        }

        // FIXME expr.
        throw new IllegalArgumentException("unsupported type");
    }

    public void updateValue(double[] newValue) {
        if (parameterClass == Cplx.class) {
            // position is updated indirectly via listener.
            Cplx value = new Cplx(newValue[0], newValue[1]);
            parent.setParameterValue(id, owner, value);
        }

        // FIXME expr.
        throw new IllegalArgumentException("unsupported type");
    }

    private void validate() {
        if (invalid) {
            Object parameterValue = parent.getParameterValue(id, owner);

            if (parameterValue == null) {
                // remove yourself.
                parent.removeInteractivePoint(this);
            }

            updatePosition(parameterValue);

            invalid = false;
        }
    }

    public double[] position() {
        validate();
        return position;
    }

    public String id() {
        return id;
    }

    public int owner() {
        return owner;
    }

    public void invalidate() {
        this.invalid = true;
    }

    public boolean is(String id, int owner) {
        return this.id.equals(id) && this.owner == owner;
    }
}
