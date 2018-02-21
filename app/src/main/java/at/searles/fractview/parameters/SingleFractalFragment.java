package at.searles.fractview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.fractview.parameters.FractalProviderFragment;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;

/**
 * Special fragment
 */
public class SingleFractalFragment extends FractalProviderFragment {

    private Fractal fractal;

    /*
    Basic function:

    fractal has source code.

    fractal.parse(): parses sourceCode, creates tree
                     and Map<String, Type> externalTypes;

    fractals.parseExternalExprs():


    A call to 'fractal.parse()' will compile the source
    code and initialize the external-map.

    next, all externals are parsed.
     */

    /**
     * for additional parameters that are only defined
     * in extern expressions.
     */
    private Map<String, List<String>> inlineParameters;

    public SingleFractalFragment() {
        this.inlineParameters = new HashMap<>();
    }

    @Override
    public Fractal get(int index) {
        return fractal;
    }

    @Override
    public String sourceCode() {
        return fractal.sourceCode();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public Iterable<String> parameters() {
        return fractal.parameters();
    }

    @Override
    public Fractal.Type type(String label) {
        return fractal.type(label);
    }

    @Override
    public Object value(String label) {
        return fractal.get(label).value();
    }

    @Override
    public boolean isDefault(String label) {
        return fractal.isDefault(label);
    }

    @Override
    public void reset(String label) {
        fractal.reset(label);
    }

    @Override
    public void setValue(String label, Object value) throws FractalProviderFragment.CannotSetParameterException {
        switch (type(label)) {
            case Scale: {
                Scale sc = (Scale) value;
                fractal.setScale(sc);
            }
            break;
            case Int: {
                fractal.setInt(label, ((Number) value).intValue());
            }
            break;
            case Real: {
                fractal.setReal(label, ((Number) value).doubleValue());
            }
            break;
            case Cplx: {
                fractal.setCplx(label, (Cplx) value);
            }
            break;
            case Expr: {
                // This one is more complicated.
                // Compiling is one here and not in the dialog because I cannot simply
                // pass a Tree as a parcel in case I modify it accordingly.

                // store id in case of error.
                // If backup is null, then the original was used.
                String backup = fractal.isDefault(label) ? null : (String) fractal.get(label).value();

                try {
                    fractal.setExpr(label, value.toString());
                    fractal.parseExternalExpressions();
                    fractal.compile();

                    // compiling was fine...
                    fireStructureModified();
                } catch (CompileException e) { // this includes parsing exceptions now
                    // there was an error. Restore expr for id to original state
                    if (backup == null) {
                        // back to original
                        fractal.reset(label);
                    } else {
                        // back to old value
                        fractal.setExpr(label, backup);
                    }

                    // nothing happened here.
                }
            }
            break;
            case Color: {
                fractal.setColor(label, ((Number) value).intValue());
            }
            break;
            case Bool: {
                fractal.setBool(label, (Boolean) value);
            }
            break;
            case Palette: {
                fractal.setPalette(label, (Palette) value);
            }
            break;
            default:
                throw new IllegalArgumentException("No such type");
        }
    }

    @Override
    public void setSourceCode(String source, boolean resetParameters) throws FractalProviderFragment.CannotSetParameterException {
        Fractal newFractal = fractal.copyNewSource(source, resetParameters);
        setFractal(0, newFractal);
    }

    @Override
    public void setFractal(int index, Fractal fractal) throws FractalProviderFragment.CannotSetParameterException {
        try {
            fractal.parse();
            fractal.parseExternalExpressions();
            fractal.compile();
            setFractal(0, fractal);

            fireStructureModified();
        } catch(CompileException e) {
            throw new CannotSetParameterException(e);
        }
    }
}
