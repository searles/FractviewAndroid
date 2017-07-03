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

public class ParcelSerializers {

    /**
     * Used to put a palette into a bundle
     */
    static public Bundle paletteToBundle(Palette palette) {
        Bundle bundle = new Bundle();
        
        bundle.putInt(WIDTH_LABEL, p.width());
        bundle.putInt(HEIGHT_LABEL, p.height());

        int[] array = new int[p.width() * p.height()];

        for (int y = 0; y < p.height(); ++y) {
            for (int x = 0; x < p.width(); ++x) {
                array[x + y * p.width()] = p.argb(x, y);
            }
        }
        
        bundle.putIntArray(COLORS_LABEL, array);
    }
    
    static public Palette bundleToPalette(Bundle bundle) {
        int width = bundle.getInt(WIDTH_LABEL);
        int height = bundle.getInt(HEIGHT_LABEL);
        
        int[] array = 
    }
    

    public static class PaletteWrapper implements Parcelable {

        //public final String label;
        public final Palette p;

        PaletteWrapper(/*String label,*/ Palette p) {
            //this.label = label;
            this.p = p;
        }


        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            //parcel.writeString(label);
            writePaletteToParcel(p, parcel);
        }

        public static final Creator<PaletteWrapper> CREATOR
                = new Creator<PaletteWrapper>() {
            public PaletteWrapper createFromParcel(Parcel in) {
                return new PaletteWrapper(in);
            }

            public PaletteWrapper[] newArray(int size) {
                return new PaletteWrapper[size];
            }
        };


        public static void writePaletteToParcel(Palette p, Parcel parcel) {

        }

        public static Palette readPalette(Parcel in) {
            int w = in.readInt();
            int h = in.readInt();

            int data[][] = new int[h][w];

            for (int y = 0; y < h; ++y) {
                for (int x = 0; x < w; ++x) {
                    data[y][x] = in.readInt();
                }
            }

            return new Palette(data);
        }

        /**
         * Now, writeParcel in reverse
         * @param parcel The palette in a parcel
         */
        private PaletteWrapper(Parcel parcel) {
            //label = parcel.readString();
            p = readPalette(parcel);
        }
    }

    public static class FractalParcelable implements Parcelable {
        final Fractal fractal;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            // First source code
            parcel.writeString(sourceCode);

            // Next scale
            parcel.writeDouble(scale.xx());
            parcel.writeDouble(scale.xy());
            parcel.writeDouble(scale.yx());
            parcel.writeDouble(scale.yy());
            parcel.writeDouble(scale.cx());
            parcel.writeDouble(scale.cy());

            // Next data.
            parcel.writeInt(data.size());

            for (Map.Entry<String, Fractal.Parameter> entry : data.entrySet()) {
                parcel.writeString(entry.getKey());

                Fractal.Type type = entry.getValue().type;
                Object o = entry.getValue().object;

                parcel.writeInt(type.ordinal());

                switch (type) {
                    case Int:
                        parcel.writeInt((Integer) o);
                        break;
                    case Real:
                        parcel.writeDouble((Double) o);
                        break;
                    case Cplx:
                        parcel.writeDouble(((Cplx) o).re());
                        parcel.writeDouble(((Cplx) o).im());
                        break;
                    case Bool:
                        parcel.writeInt((Boolean) o ? 1 : 0);
                        break;
                    case Expr:
                        parcel.writeString((String) o);
                        break;
                    case Color:
                        parcel.writeInt((Integer) o);
                        break;
                    case Palette:
                        Commons.writePaletteToParcel((Palette) o, parcel);
                        break;
                    default:
                        throw new IllegalArgumentException("Did not expect " + type + " in 'data'");
                }
            }
        }

        public static final Parcelable.Creator<Fractal> CREATOR =
                new Parcelable.Creator<Fractal>() {
                    public Fractal createFromParcel(Parcel in) {
                        String sourceCode = in.readString();

                        double xx = in.readDouble();
                        double xy = in.readDouble();
                        double yx = in.readDouble();
                        double yy = in.readDouble();
                        double cx = in.readDouble();
                        double cy = in.readDouble();

                        Scale sc = new Scale(xx, xy, yx, yy, cx, cy);

                        Map<String, Fractal.Parameter> data = new HashMap<>();

                        for (int size = in.readInt(); size > 0; --size) {
                            String id = in.readString();

                            Fractal.Type type = Fractal.Type.values()[in.readInt()];

                            switch (type) {
                                case Int:
                                case Color: // Fall though
                                    data.put(id, new Fractal.Parameter(type, in.readInt()));
                                    break;
                                case Real:
                                    data.put(id, new Fractal.Parameter(type, in.readDouble()));
                                    break;
                                case Cplx: {
                                    double re = in.readDouble();
                                    double im = in.readDouble();
                                    data.put(id, new Fractal.Parameter(type, new Cplx(re, im)));
                                }
                                break;
                                case Bool:
                                    data.put(id, new Fractal.Parameter(type, in.readInt() == 1));
                                    break;
                                case Expr:
                                    data.put(id, new Fractal.Parameter(type, in.readString()));
                                    break;
                                case Palette: {
                                    data.put(id, new Fractal.Parameter(type, Commons.readPalette(in)));
                                }
                                break;
                                default:
                                    throw new IllegalArgumentException("Did not expect " + type + " in 'data'");
                            }

                        }

                        return new Fractal(sc, sourceCode, data);
                    }

                    public Fractal[] newArray(int size) {
                        return new Fractal[size];
                    }
                };
    }
}
