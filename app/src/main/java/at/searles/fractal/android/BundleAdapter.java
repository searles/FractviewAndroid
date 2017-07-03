package at.searles.fractal.android;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.HashMap;
import java.util.Map;

import at.searles.fractal.Fractal;
import at.searles.fractview.Commons;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * This class contains all serializers for parcels. This is in particular important for
 * classes that are not part of the android eco system but still need to be parceled.
 */

public class BundleAdapter {

    /**
     * Used to put a palette into a bundle
     */
    static public Bundle paletteToBundle(Palette palette) {
        Bundle bundle = new Bundle();
        
        bundle.putInt(WIDTH_LABEL, p.width());
        bundle.putInt(HEIGHT_LABEL, p.height());

        int[] data = p.colors();

        bundle.putIntArray(COLORS_LABEL, data);
    }
    
    static public Palette bundleToPalette(Bundle bundle) {
        int width = bundle.getInt(WIDTH_LABEL);
        int height = bundle.getInt(HEIGHT_LABEL);
        
        int[] data = bundle.getIntArray(COLORS_LABEL);
        
        return new Palette(width, height, data); 
    }
    
    static double[] cplxToArray(Cplx c) {
        return new double[]{c.re(), c.im()};
    }
    
    static Cplx arrayToCplx(double[] d) {
        return new Cplx(d[0], d[1]);
    }
    
    static double[] scaleToArray(Scale sc) {
        return new double[]{sc.xx(), sc.xy(), sc.yx(), sc.yy(), sc.cx(), sc.cy()};
    }
    
    static Scale arrayToScale(double[] d) {
        return new Scale(d[0], d[1], d[2], d[3], d[4], d[5]);
    }
    
    static public Bundle fractalToBundle(Fractal fractal) {
        Bundle bundle = new Bundle();
        bundle.putString(SOURCE_LABEL, fractal.sourceCode());
        
        // one bundle for each type. Less space.
        Bundle intBundle = new Bundle();
        Bundle realBundle = new Bundle();
        Bundle cplxBundle = new Bundle();
        Bundle boolBundle = new Bundle();
        Bundle exprBundle = new Bundle();
        Bundle colorBundle = new Bundle();
        Bundle paletteBundle = new Bundle();
        Bundle scaleBundle = new Bundle();
        
        for(Map.Entry<String, Fractal.Parameter> entry: fractal.nonDefaultParameters()) {
            String key = entry.getKey();
            type = entry.getValue().type();
            value = entry.getValue().value();
            
            switch (type) {
            case Int:
                intBundle.putInt(key, (Integer) value);
                break;
            case Real:
                realBundle.putDouble((Double) value);
                break;
            case Cplx:
                cplxBundle.putDoubleArray(key, cplxToArray((Cplx) value));
                break;
            case Bool:
                boolBundle.putBoolean((Boolean) value);
                break;
            case Expr:
                exprBundle.putString((String) value);
                break;
            case Color:
                colorBundle.putInt((Integer) value);
                break;
            case Palette:
                paletteBundle.putBundle(key, paletteToBundle((Palette) value);
                break;
            case Scale:
                scaleBundle.putDoubleArray(key, scaleToArray((Scale) value));
                break;
            default:
                throw new IllegalArgumentException("Did not expect " + type + " in 'data'");
            }
        }
                                        
        bundle.putBundle(INTS_LABEL, intBundle);
        bundle.putBundle(REALS_LABEL, realBundle);
        bundle.putBundle(CPLXS_LABEL, cplxBundle);
        bundle.putBundle(BOOLS_LABEL, boolBundle);
        bundle.putBundle(EXPRS_LABEL, exprBundle);
        bundle.putBundle(COLORS_LABEL, colorBundle);
        bundle.putBundle(PALETTES_LABEL, paletteBundle);
        bundle.putBundle(SCALES_LABEL, scaleBundle);
                                        
        return bundle;
    }
                                        
    static public Fractal bundleToFractal(Bundle b) {
        String sourceCode = b.getString(SOURCE_LABEL);
        
        Map<String, Fractal.Parameter> data = new HashMap<String, Fractal.Parameter>();
        
        Bundle intBundle = bundle.getBundle(INTS_LABEL);
        Bundle realBundle = bundle.getBundle(REALS_LABEL);
        Bundle cplxBundle = bundle.getBundle(CPLXS_LABEL);
        Bundle boolBundle = bundle.getBundle(BOOLS_LABEL);
        Bundle exprBundle = bundle.getBundle(EXPRS_LABEL);
        Bundle colorBundle = bundle.getBundle(COLORS_LABEL);
        Bundle paletteBundle = bundle.getBundle(PALETTES_LABEL);
        Bundle scaleBundle = bundle.getBundle(SCALES_LABEL);
        
        for(String key : intBundle.keySet()) {
            data.put(key, new Parameter(Type.Int, intBundle.getInt(key)));
        }

        for(String key : realBundle.keySet()) {
            data.put(key, new Parameter(Type.Real, realBundle.getInt(key)));
        }

        for(String key : cplxBundle.keySet()) {
            data.put(key, new Parameter(Type.Cplx, arrayToCplx(cplxBundle.getInt(key))));
        }

        for(String key : boolBundle.keySet()) {
            data.put(key, new Parameter(Type.Bool, intBundle.getBoolean(key)));
        }

        for(String key : exprBundle.keySet()) {
            data.put(key, new Parameter(Type.Expr, exprBundle.getString(key)));
        }

        for(String key : colorBundle.keySet()) {
            data.put(key, new Parameter(Type.Color, colorBundle.getInt(key)));
        }

        for(String key : paletteBundle.keySet()) {
            data.put(key, new Parameter(Type.Palette, bundleToPalette(paletteBundle.getBundle(key))));
        }

        for(String key : scaleBundle.keySet()) {
            data.put(key, new Parameter(Type.Scale, arrayToScale(scaleBundle.getDoubleArray(key))));
        }

        return new Fractal(sourceCode, data);
    }
}
