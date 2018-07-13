package at.searles.fractal;

import at.searles.meelan.ops.InstructionSet;

public class FractviewInstructionSet extends InstructionSet {

    private static FractviewInstructionSet singleton;

    public static FractviewInstructionSet get() {
        if(singleton == null) {
            singleton = new FractviewInstructionSet();
        }

        return singleton;
    }

    private FractviewInstructionSet() {
        init();
    }

    private void init() {
        // TODO
    }
}
