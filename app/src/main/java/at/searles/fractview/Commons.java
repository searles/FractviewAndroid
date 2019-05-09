package at.searles.fractview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import java.io.ByteArrayOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Commons {

    public static interface KeyAction {
        void apply(String key);
    }

    public static String timestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        return simpleDateFormat.format(new Date());
    }

    public static String fancyTimestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd, HH:mm");
        return simpleDateFormat.format(new Date());
    }

    public static String duration(long ms) {
        StringBuilder sb = new StringBuilder();

        double seconds = ms / 1000.;

        long minutes = ms / 60000;

        if(minutes > 0) {
            seconds -= minutes * 60;
            sb.append(minutes).append(":");
        }

        sb.append(String.format("%02.3f", seconds));

        return sb.toString();
    }



    public static float normX(float bitmapX, int width, int height) {
        float m = Math.min(width, height);
        return bitmapX * 2f / m - width / m;
    }

    public static float normY(float bitmapY, int width, int height) {
        float m = Math.min(width, height);
        return bitmapY * 2f / m - height / m;
    }


    public static float bitmapX(float normX, int width, int height) {
        float m = Math.min(width, height);
        return normX * m / 2f + width / 2f;
    }

    public static float bitmapY(float normY, int width, int height) {
        float m = Math.min(width, height);
        return normY * m / 2f + height / 2f;
    }

    public static boolean isUI() {
        return Looper.getMainLooper().equals(Looper.myLooper());
    }

    public static void uiRun(Runnable runnable) {
        if(isUI()) {
            runnable.run();
        } else {
            new Handler(Looper.getMainLooper()).post(runnable);
        }
    }

//    public static Scale toScale(Tree init) throws MeelanException {
//        if(init instanceof Vec) {
//            Vec vec = (Vec) init;
//
//            if(vec.size() == 3) {
//                double[] scale = new double[6];
//
//                for(int i = 0; i < 3; ++i) {
//                    Tree child = vec.get(i);
//
//                    if(child instanceof Int) {
//                        scale[2 * i] = ((Int) child).value();
//                        scale[2 * i + 1] = 0;
//                    } else if(child instanceof Real) {
//                        scale[2 * i] = ((Real) child).value();
//                        scale[2 * i + 1] = 0;
//                    } else if(child instanceof CplxVal) {
//                        scale[2 * i] = ((CplxVal) child).value().re();
//                        scale[2 * i + 1] = ((CplxVal) child).value().im();
//                    } else {
//                        throw new MeelanException("Components of scale must be complex numbers", init);
//                    }
//                }
//
//                return new Scale(scale[0], scale[1], scale[2], scale[3], scale[4], scale[5]);
//            }
//        }
//
//        throw new MeelanException("Scale must be vector of 3 numbers", init);
//    }

    public static byte[] toPNG(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayBitmapStream);
        return byteArrayBitmapStream.toByteArray();
    }

    public static Bitmap fromPNG(byte[] data) {
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    /**
     * Converts a term to a palette
     */
//    public static Palette toPalette(Tree t) throws MeelanException {
//        int w, h;
//        int colors[];
//
//        if(t instanceof Vec) {
//            Vec vec = (Vec) t;
//
//            if(vec.size() > 0) {
//                Tree element = vec.get(0);
//
//                if(element instanceof Vec) {
//                    // two dimensional palette.
//                    h = vec.size();
//                    w = ((Vec) element).size();
//
//                    colors = new int[w * h];
//
//                    int y = 0;
//
//                    for(Tree row : vec) {
//                        if(row instanceof Vec) {
//                            if(((Vec) row).size() == w) {
//                                for(int x = 0; x < w; ++x) {
//                                    Tree color = ((Vec) row).get(x);
//
//                                    if(color instanceof Int) {
//                                        colors[x + y * w] = ((Int) color).value();
//                                    } else {
//                                        throw new MeelanException("invalid entry in palette", t);
//                                    }
//                                }
//                            }
//                        } else {
//                            throw new MeelanException("invalid row in palette", t);
//                        }
//
//                        y++;
//                    }
//                } else if(element instanceof Int) {
//                    // just one row.
//                    h = 1;
//                    w = vec.size();
//
//                    colors = new int[w];
//
//                    for(int x = 0; x < w; ++x) {
//                        Tree color = vec.get(x);
//
//                        if(color instanceof Int) {
//                            colors[x] = ((Int) color).value();
//                        } else {
//                            throw new MeelanException("invalid entry in palette row");
//                        }
//                    }
//                } else {
//                    throw new MeelanException("invalid row in palette", t);
//                }
//            } else {
//                throw new MeelanException("palette must not be empty", t);
//            }
//        } else if(t instanceof Int) {
//            w = h = 1;
//            colors = new int[1];
//            colors[0] = ((Int) t).value();
//        } else {
//            throw new MeelanException("invalid palette", t);
//        }
//
//        return new Palette(w, h, colors);
//    }


    public static <A> Map<String, A> merge(Map<String, A> primary, Map<String, A> secondary) {
        Map<String, A> merged = new HashMap<>();

        merged.putAll(secondary);
        merged.putAll(primary);

        return merged;
    }



    /**
     * creates a new bitmap with size 64x64 containing the center of the current image
     * @return
     */
    public static Bitmap createIcon(Bitmap original, int iconSize) {
        // FIXME Move somewhere else!
        // create a square icon. Should  only contain central square.
        Bitmap icon = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(icon);

        float scale = ((float) iconSize) / Math.min(original.getWidth(), original.getHeight());

        float w = original.getWidth();
        float h = original.getHeight();

        Matrix transformation = new Matrix();
        transformation.setValues(new float[]{
                scale, 0, (iconSize - scale * w) * .5f,
                0, scale, (iconSize - scale * h) * .5f,
                0, 0, 1,
        });

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(original, transformation, paint);

        return icon;
    }
}
