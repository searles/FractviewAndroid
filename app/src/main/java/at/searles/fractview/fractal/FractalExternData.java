package at.searles.fractview.fractal;

import java.util.HashMap;

import at.searles.fractal.Type;
import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.parser.MeelanEnv;
import at.searles.meelan.symbols.AbstractExternData;
import at.searles.parsing.lexer.TokStream;
import at.searles.parsing.parser.ParserStream;

public class FractalExternData extends AbstractExternData {

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
    public Tree convertToInternal(Entry entry, Object o) {
        // TODO
        return null;
    }

    @Override
    public Object convertToValue(String s, Tree tree) throws MeelanException {
        // TODO
        return null;
    }

}
