package at.searles.fractview.main;

import at.searles.math.Cplx;

public class InteractivePoint {
    private FractalProviderFragment parent;
    private final String id;
    int owner; // this is updated on a regular basis by the provider if a fractal is removed.

    private Class<?> parameterClass;

    private double[] position;

    InteractivePoint(FractalProviderFragment parent, String id, int owner) {
        this.parent = parent;
        this.id = id;
        this.owner = owner;

        this.position = new double[]{0, 0};

        update();
    }

    private void updatePosition(Object value) {
        parameterClass = value.getClass();

        if (parameterClass == Cplx.class) {
            position[0] = ((Cplx) value).re();
            position[1] = ((Cplx) value).im();

            return;
        }

        // FIXME expr.
        throw new IllegalArgumentException("unsupported type");
    }

    public void setValue(double[] newValue) {
        if (parameterClass == Cplx.class) {
            // position is updated indirectly via listener.
            Cplx value = new Cplx(newValue[0], newValue[1]);
            parent.setParameterValue(id, owner, value);

            return;
        }

        // FIXME expr.
        throw new IllegalArgumentException("unsupported type: " + this);
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

        updatePosition(parameterValue);

        return true;
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

    public String toString() {
        return id + "/" + owner;
    }
}
