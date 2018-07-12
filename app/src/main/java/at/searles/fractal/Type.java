package at.searles.fractal;

public enum Type {
    Int("int"),
    Real("real"),
    Cplx("cplx"),
    Bool("bool"),
    Expr("expr"),
    Color("color"),
    Palette("palette"),
    Scale("scale");

    public final String identifier;

    Type(String identifier) {
        this.identifier = identifier;
    }

    public static Type fromString(String s) {
        for (Type t : Type.values()) {
            if (t.identifier.equals(s)) {
                return t;
            }
        }

        return null;
    }
}
