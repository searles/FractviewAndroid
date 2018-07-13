package at.searles.fractview.fractal;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import at.searles.fractal.LdPalette;
import at.searles.fractal.ParserInstance;
import at.searles.fractal.Type;
import at.searles.math.Cplx;
import at.searles.meelan.MeelanException;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.optree.inlined.FuncDeclaration;
import at.searles.meelan.optree.inlined.Id;
import at.searles.meelan.symbols.AbstractExternData;
import at.searles.meelan.values.Bool;
import at.searles.meelan.values.CplxVal;
import at.searles.meelan.values.Int;
import at.searles.meelan.values.Real;

public class FractalExternData extends AbstractExternData {

    private static final String TEMP_VAR = "__xy";

    private final HashMap<String, Object> customObjects;

    public FractalExternData() {
        customObjects = new HashMap<>();
    }

    @Override
    public boolean setCustomValue(String id, Object o) {
        customObjects.put(id, o);

        return entry(id) != null;
    }

    @Override
    public Object customValue(String id) {
        return customObjects.get(id);
    }

    @Override
    public Object defaultValue(String s) {
        return 0;
    }

    @Override
    public String defaultType(String s) {
        return Type.Int.identifier;
    }

    @Override
    public Tree convertToInternal(Entry entry, Object value) {
        Type type = Type.fromString(entry.type);

        if(type == null) {
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
                return new Int((Integer) value);
            case Expr:
                // This may throw a MeelanException!
                return ParserInstance.get().parseExpr((String) value);
            case Palette:
                // first occurrence of this palette.
                return registerPalette(entry.id);
            case Scale:
                // first occurrence of this palette.
                return registerScale(entry.id);
        }

        throw new IllegalArgumentException("missing case: " + type);
    }

    private int paletteCount() {
        int paletteCount = 0;
        for(Entry ignored : entriesOfType(Type.Palette.identifier)) {
            paletteCount++;
        }
        return paletteCount;
    }

    private Tree registerPalette(String id) {
        return new FuncDeclaration("palette_" + id, Collections.singletonList(TEMP_VAR),
                LdPalette.get().apply(Arrays.asList(new Int(paletteCount()), new Id(TEMP_VAR))));
    }

    private Tree registerScale(String id) {
        throw new MeelanException("scale is not yet supported", null);
    }

    @Override
    public Object convertToValue(String typeString, Tree tree) throws MeelanException {
        Type type = Type.fromString(typeString);

        if(type == null) {
            return null;
        }

        switch (type) {
            // TODO
            case Int:
                break;
            case Real:
                break;
            case Cplx:
                break;
            case Bool:
                break;
            case Expr:
                break;
            case Color:
                break;
            case Palette:
                break;
            case Scale:
                break;
        }

        throw new IllegalArgumentException("missing case: " + type);
    }

}
