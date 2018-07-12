package at.searles.fractal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.inlined.FuncDeclaration;
import at.searles.meelan.parser.MeelanEnv;
import at.searles.meelan.symbols.AbstractExternData;
import at.searles.meelan.values.Bool;
import at.searles.meelan.values.CplxVal;
import at.searles.meelan.values.Int;
import at.searles.meelan.values.Label;
import at.searles.meelan.values.Real;
import at.searles.parsing.parser.ParserStream;

public abstract class AbstractParameters extends AbstractExternData {
    private static final Tree DEFAULT = new Int(0);
    private static final String DEFAULT_TYPE = "int";

    @Override
    public abstract void setCustomValue(String id, String type, Object value);

    @Override
    public abstract Object customValue(String id, String type);

    public void setInt(String id, int i) {
        setCustomValue(id, Type.Int.identifier, i);
    }

    public void setReal(String id, double d) {
        data.put(id, new Parameter(Type.Real, d));
    }

    public void setCplx(String id, Cplx c) {
        data.put(id, new Parameter(Type.Cplx, c));
    }

    public void setBool(String id, boolean b) {
        data.put(id, new Parameter(Type.Bool, b));
    }

    public void setExpr(String id, String expr) {
        data.put(id, new Parameter(Type.Expr, expr));
    }

    public void setColor(String id, int color) {
        data.put(id, new Parameter(Type.Color, color));
    }

    public void setPalette(String id, Palette p) {
        data.put(id, new Parameter(Type.Palette, p));
    }

    public void setScale(String id, Scale sc) {
        data.put(id, new Parameter(Type.Scale, sc));
    }

    public void setScale(Scale sc) {
        // The default scale that is used for the zoom
        setScale(SCALE_KEY, sc);
    }


    @Override
    public Tree defaultValue() {
        return DEFAULT;
    }

    @Override
    public String defaultType() {
        return DEFAULT_TYPE;
    }

    @Override
    public Tree convertToInternal(String typeString, Object value) {
        // TODO id!
        Type type = Type.fromString(typeString);

        if (type == null) {
            return null;
        }

        switch (type) {
            case Int:
                return new Int(((Number) value).intValue());
            case Real:
                return new Real(((Number) value).doubleValue());
            case Cplx:
                return new CplxVal((Cplx) value);
            case Bool:
                return new Bool((Boolean) value);
            case Color:
                return new Int(((Number) value).intValue());
            case Expr:
                // extern values must be treated special
                // unless they are constants.
                return Ast.parseExpr(new MeelanEnv(), ParserStream.fromString(String) value));
            case Palette:
                new FuncDeclaration("palette_" + id, Collections.singletonList("__xy"),
                        Op.__ld_palette.eval(Arrays.asList(new Label(paletteIndex), xy)))
                paletteIndex++;
            case Scale:
                // FIXME
                break;
        }

        return null;
    }

    @Override
    public Object convertToValue(String s, Tree tree) throws MeelanException {
        return null;
    }
}
