package at.searles.fractview.fractal;

import android.os.Bundle;

import at.searles.fractal.data.FractalData;
import at.searles.fractal.gson.Serializers;
import at.searles.math.Scale;
import at.searles.math.color.Palette;

/**
 * This class contains all serializers for parcels. This is in particular important for
 * classes that are not part of the android eco system but still need to be parceled.
 */

public class BundleAdapter {

    // for palette
    private static final String WIDTH_LABEL = "width";
    private static final String HEIGHT_LABEL = "height";
    private static final String PALETTE_LABEL = "palette";

    private static final String FRACTAL_DATA_JSON = "fds";

    public static Bundle toBundle(FractalData fractal) {
        String json = Serializers.serializer().toJson(fractal);
        Bundle bundle = new Bundle();
        bundle.putString(FRACTAL_DATA_JSON, json);
        return bundle;
    }

    public static FractalData fractalFromBundle(Bundle bundle) {
        String json = bundle.getString(FRACTAL_DATA_JSON);
        return Serializers.serializer().fromJson(json, FractalData.class);
    }

    /**
     * Used to put a palette into a bundle. Faster and easier than json.
     */
    public static Bundle toBundle(Palette p) {
        Bundle bundle = new Bundle();
        
        bundle.putInt(WIDTH_LABEL, p.width());
        bundle.putInt(HEIGHT_LABEL, p.height());

        bundle.putIntArray(PALETTE_LABEL, p.colors());

        return bundle;
    }
    
    public static Palette paletteFromBundle(Bundle bundle) {
        int width = bundle.getInt(WIDTH_LABEL);
        int height = bundle.getInt(HEIGHT_LABEL);
        
        int[] data = bundle.getIntArray(PALETTE_LABEL);
        
        return new Palette(width, height, data); 
    }
    
//    public static double[] toArray(Cplx c) {
//        return new double[]{c.re(), c.im()};
//    }
//
//    public static Cplx cplxFromArray(double[] d) {
//        return new Cplx(d[0], d[1]);
//    }
//
    public static double[] toArray(Scale sc) {
        return new double[]{sc.xx, sc.xy, sc.yx, sc.yy, sc.cx, sc.cy};
    }

    public static Scale scaleFromArray(double[] d) {
        return new Scale(d[0], d[1], d[2], d[3], d[4], d[5]);
    }
}
