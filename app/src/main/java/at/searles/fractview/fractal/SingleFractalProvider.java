package at.searles.fractview.fractal;

import at.searles.fractal.Fractal;
import at.searles.meelan.symbols.ExternData;

public class SingleFractalProvider implements FractalProvider {

    private Fractal fractal;

    public void setFractal(Fractal fractal) {
        this.fractal = fractal;
    }

    @Override
    public Fractal get(int i) {
        return fractal;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public String source() {
        return fractal.sourceCode();
    }

    @Override
    public void setSource(String sourceCode) {
        Fractal newFractal = Fractal.fromValues(sourceCode, this.fractal.data());

        this.fractal = newFractal;
    }

    @Override
    public ExternData data() {
        return fractal.data();
    }
}
