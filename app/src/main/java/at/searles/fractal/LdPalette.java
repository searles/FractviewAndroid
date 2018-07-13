package at.searles.fractal;

import java.util.List;

import at.searles.meelan.ops.SystemInstruction;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.types.FunctionType;
import at.searles.meelan.values.Const;

public class LdPalette extends SystemInstruction {

    private static LdPalette singleton;

    public static LdPalette get() {
        if(singleton == null) {
            singleton = new LdPalette();
        }

        return singleton;
    }

    private LdPalette() {}

    @Override
    protected Const evaluate(FunctionType functionType, List<Tree> list) {
        // FIXME
        return null;
    }
}
