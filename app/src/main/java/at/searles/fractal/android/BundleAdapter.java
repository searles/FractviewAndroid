package at.searles.fractal.android;

import android.os.Bundle;

import at.searles.fractal.ExternParameters;
import at.searles.fractal.Fractal;
import at.searles.fractal.Type;
import at.searles.math.Cplx;
import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.symbols.ExternData;

/**
 * This class contains all serializers for parcels. This is in particular important for
 * classes that are not part of the android eco system but still need to be parceled.
 */

public class BundleAdapter {

    private static final String WIDTH_LABEL = "width";
    private static final String HEIGHT_LABEL = "height";

    private static final String INTS_LABEL = "ints";
    private static final String REALS_LABEL = "reals";
    private static final String CPLXS_LABEL = "cplxs";
    private static final String BOOLS_LABEL = "bools";
    private static final String EXPRS_LABEL = "exprs";
    private static final String COLORS_LABEL = "colors";
    private static final String PALETTES_LABEL = "palettes";
    private static final String SCALES_LABEL = "scales";

    public static Bundle dataToBundle(ExternData data) {
        Bundle intBundle = new Bundle();
        Bundle realBundle = new Bundle();
        Bundle cplxBundle = new Bundle();
        Bundle boolBundle = new Bundle();
        Bundle exprBundle = new Bundle();
        Bundle colorBundle = new Bundle();
        Bundle paletteBundle = new Bundle();
        Bundle scaleBundle = new Bundle();

        for(ExternData.Entry entry : data.entries()) {
            if(entry.isCustom()) {
                Object value = entry.get();

                Type type = Type.fromString(entry.type);

                if(type == null) {
                    throw new IllegalArgumentException("no such type: " + entry.type);
                }

                switch (type) {
                    case Int:
                        intBundle.putInt(entry.id, ((Number) value).intValue());
                        break;
                    case Real:
                        realBundle.putDouble(entry.id, ((Number) value).doubleValue());
                        break;
                    case Cplx:
                        cplxBundle.putDoubleArray(entry.id, BundleAdapter.cplxToArray((Cplx) value));
                        break;
                    case Bool:
                        boolBundle.putBoolean(entry.id, (Boolean) value);
                        break;
                    case Expr:
                        exprBundle.putString(entry.id, (String) value);
                        break;
                    case Color:
                        colorBundle.putInt(entry.id, (Integer) value);
                        break;
                    case Palette:
                        paletteBundle.putBundle(entry.id, BundleAdapter.paletteToBundle((Palette) value));
                        break;
                    case Scale:
                        scaleBundle.putDoubleArray(entry.id, BundleAdapter.scaleToArray((Scale) value));
                        break;
                    default:
                        throw new IllegalArgumentException("Did not expect " + type + " in 'data'");
                }
            }
        }

        Bundle bundle = new Bundle();

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

    public static void bundleToData(Bundle bundle, ExternData data) {
        Bundle intBundle = bundle.getBundle(INTS_LABEL);
        Bundle realBundle = bundle.getBundle(REALS_LABEL);
        Bundle cplxBundle = bundle.getBundle(CPLXS_LABEL);
        Bundle boolBundle = bundle.getBundle(BOOLS_LABEL);
        Bundle exprBundle = bundle.getBundle(EXPRS_LABEL);
        Bundle colorBundle = bundle.getBundle(COLORS_LABEL);
        Bundle paletteBundle = bundle.getBundle(PALETTES_LABEL);
        Bundle scaleBundle = bundle.getBundle(SCALES_LABEL);

        if(intBundle != null) for(String key : intBundle.keySet()) {
            data.setCustomValue(key, Type.Int.identifier, intBundle.getInt(key));
        }

        if(realBundle != null) for(String key : realBundle.keySet()) {
            data.setCustomValue(key, Type.Real.identifier, realBundle.getDouble(key));
        }

        if(cplxBundle != null) for(String key : cplxBundle.keySet()) {
            data.setCustomValue(key, Type.Cplx.identifier, arrayToCplx(cplxBundle.getDoubleArray(key)));
        }

        if(boolBundle != null) for(String key : boolBundle.keySet()) {
            data.setCustomValue(key, Type.Bool.identifier, boolBundle.getBoolean(key));
        }

        if(exprBundle != null) for(String key : exprBundle.keySet()) {
            data.setCustomValue(key, Type.Expr.identifier, exprBundle.getString(key));
        }

        if(colorBundle != null) for(String key : colorBundle.keySet()) {
            data.setCustomValue(key, Type.Color.identifier, colorBundle.getInt(key));
        }

        if(paletteBundle != null) for(String key : paletteBundle.keySet()) {
            data.setCustomValue(key, Type.Palette.identifier, BundleAdapter.bundleToPalette(paletteBundle.getBundle(key)));
        }

        if(scaleBundle != null) for(String key : scaleBundle.keySet()) {
            data.setCustomValue(key, Type.Scale.identifier, BundleAdapter.arrayToScale(scaleBundle.getDoubleArray(key)));
        }
    }


    /**
     * Used to put a palette into a bundle
     */
    public static Bundle paletteToBundle(Palette p) {
        Bundle bundle = new Bundle();
        
        bundle.putInt(WIDTH_LABEL, p.width());
        bundle.putInt(HEIGHT_LABEL, p.height());

        bundle.putIntArray(COLORS_LABEL, p.colors());

        return bundle;
    }
    
    public static Palette bundleToPalette(Bundle bundle) {
        int width = bundle.getInt(WIDTH_LABEL);
        int height = bundle.getInt(HEIGHT_LABEL);
        
        int[] data = bundle.getIntArray(COLORS_LABEL);
        
        return new Palette(width, height, data); 
    }
    
    public static double[] cplxToArray(Cplx c) {
        return new double[]{c.re(), c.im()};
    }

    public static Cplx arrayToCplx(double[] d) {
        return new Cplx(d[0], d[1]);
    }

    public static double[] scaleToArray(Scale sc) {
        return new double[]{sc.xx, sc.xy, sc.yx, sc.yy, sc.cx, sc.cy};
    }

    public static Scale arrayToScale(double[] d) {
        return new Scale(d[0], d[1], d[2], d[3], d[4], d[5]);
    }
    
}
