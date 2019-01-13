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

//    // for parameters
//    private static final String TYPE_LABEL = "TYPE";
//    private static final String VALUE_LABEL = "VALUE";
//
//    // for fractal data
//    private static final String SOURCE_LABEL = "SOURCE";
//    private static final String PARAMETERS_LABEL = "PARAMETERS";

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

//    public static Bundle parametersToBundle(FractalData data) {
//        Bundle bundle = new Bundle();
//
//        // TODO put an object into a bundle.
//
//        data.forEachParameter((id, value) -> bundle.p);
//
//        for(Map.Entry<String, FractalData.Parameter> entry : data.entrySet()) {
//            Bundle parameter = new Bundle();
//            parameter.putInt(TYPE_LABEL, entry.getValue().type.ordinal());
//
//            Object value = entry.getValue().value;
//
//            switch(entry.getValue().type) {
//                case Int:
//                    parameter.putInt(VALUE_LABEL, ((Number) value).intValue());
//                    break;
//                case Real:
//                    parameter.putDouble(VALUE_LABEL, ((Number) value).doubleValue());
//                    break;
//                case Cplx:
//                    parameter.putDoubleArray(VALUE_LABEL, new double[]{((Cplx) value).re(), ((Cplx) value).im()});
//                    break;
//                case Bool:
//                    parameter.putInt(VALUE_LABEL, ((Boolean) value) ? 1 : 0);
//                    break;
//                case Expr:
//                    parameter.putString(VALUE_LABEL, (String) value);
//                    break;
//                case Color:
//                    parameter.putInt(VALUE_LABEL, (Integer) value);
//                    break;
//                case Palette:
//                    parameter.putBundle(VALUE_LABEL, toBundle((Palette) value));
//                    break;
//                case Scale:
//                    parameter.putDoubleArray(VALUE_LABEL, toArray((Scale) value));
//                    break;
//            }
//
//            bundle.putBundle(entry.getKey(), parameter);
//        }
//
//        return bundle;
//    }
//
//    public static Map<String, FractalData.Parameter> parametersFromBundle(Bundle bundle) {
//        Map<String, FractalData.Parameter> parameters = new HashMap<>();
//
//        for(String id : bundle.keySet()) {
//            Bundle parameter = bundle.getBundle(id);
//
//            if(parameter == null) {
//                throw new NullPointerException();
//            }
//
//            ParameterType type = ParameterType.values()[parameter.getInt(TYPE_LABEL, -1)];
//
//            Object value;
//
//            switch(type) {
//                case Int:
//                    value = parameter.getInt(VALUE_LABEL);
//                    break;
//                case Real:
//                    value = parameter.getDouble(VALUE_LABEL);
//                    break;
//                case Cplx:
//                    double[] cplx = parameter.getDoubleArray(VALUE_LABEL);
//                    value = cplxFromArray(cplx);
//                    break;
//                case Bool:
//                    value = 1 == parameter.getInt(VALUE_LABEL);
//                    break;
//                case Expr:
//                    value = parameter.getString(VALUE_LABEL);
//                    break;
//                case Color:
//                    value = parameter.getInt(VALUE_LABEL);
//                    break;
//                case Palette:
//                    value = paletteFromBundle(parameter.getBundle(VALUE_LABEL));
//                    break;
//                case Scale:
//                    double[] scale = parameter.getDoubleArray(VALUE_LABEL);
//                    value = scaleFromArray(scale);
//                    break;
//                default:
//                    throw new IllegalArgumentException();
//            }
//
//            parameters.put(id, new FractalData.Parameter(type, value));
//        }
//
//        return parameters;
//    }


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
