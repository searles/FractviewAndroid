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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import at.searles.math.Scale;
import at.searles.math.color.Palette;
import at.searles.meelan.CompileException;
import at.searles.meelan.Tree;
import at.searles.meelan.Value;

/**
 * Created by searles on 11.06.17.
 */

public class Commons {

    public static interface KeyAction {
        void apply(String key);
    }

    public static String timestamp() {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HH-mm");
        return simpleDateFormat.format(new Date());
    }

    public static String duration(long ms) {
        StringBuilder sb = new StringBuilder();

        double seconds = ms / 1000.;

        long minutes = ms / 60000;

        if(minutes > 0) {
            seconds -= minutes * 60;
            sb.append(minutes + ":");
        }

        sb.append(String.format("%02.3f", seconds));

        return sb.toString();
    }


    /**
     * Matrices to convert coordinates into value that is
     * independent from the bitmap-size. Normized always
     * contains the square -1,-1 - 1-1 with 0,0 in the middle
     * but also keeps the ratio of the image.
     */
    public static Matrix bitmap2norm(int width, int height) {
        float m = Math.min(width, height);

        Matrix ret = new Matrix();

        ret.setValues(new float[]{
                2f / m, 0f, -width / m,
                0f, 2f / m, -height / m,
                0f, 0f, 1f
        });

        return ret;
    }

    public static float normX(float bitmapX, int width, int height) {
        float m = Math.min(width, height);
        return bitmapX * 2f / m - width / m;
    }

    public static float normY(float bitmapY, int width, int height) {
        float m = Math.min(width, height);
        return bitmapY * 2f / m - height / m;
    }

    /**
     * Inverse of bitmap2norm
     */
    public static Matrix norm2bitmap(int width, int height) {
        float m = Math.min(width, height);

        Matrix ret = new Matrix();

        ret.setValues(new float[]{
                m / 2f, 0f, width / 2f,
                0f, m / 2f, height / 2f,
                0f, 0f, 1f
        });

        return ret;
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

    public static Scale toScale(Tree init) {
        throw new IllegalArgumentException("not yet implemented");
    }

    public static byte[] toPNG(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayBitmapStream);
        return byteArrayBitmapStream.toByteArray();
    }

    public static Bitmap fromPNG(byte[] data) {
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        return BitmapFactory.decodeByteArray(data, 0, data.length);
    }

    /**
     * Converts a term to a palette
     * @param t
     * @return
     * @throws CompileException
     */
    public static Palette toPalette(Object t) throws CompileException {
        if(t == null) {
            throw new CompileException("Missing list value");
        }

        LinkedList<List<Integer>> p = new LinkedList<List<Integer>>();

        int w = 0, h = 0;

        if(t instanceof Tree.Vec) {
            for(Tree arg : (Tree.Vec) t) {
                List<Integer> row = new LinkedList<Integer>();

                if(arg instanceof Tree.Vec) {
                    for(Tree item : (Tree.Vec) arg) {
                        if(item instanceof Value.Int) {
                            row.add(((Value.Int) item).value);
                        } else {
                            throw new CompileException("int was expected here");
                        }
                    }
                } else if(arg instanceof Value.Int) {
                    row.add(((Value.Int) arg).value);
                } else {
                    throw new CompileException("int was expected here");
                }

                if(row.isEmpty()) {
                    throw new CompileException("no empty row allowed in palette");
                }

                if(w < row.size()) w = row.size();


                p.add(row);
                h++;
            }
        } else if(t instanceof Value.Int) {
            w = h = 1;
            p.add(Collections.singletonList(((Value.Int) t).value));
        }

        // p now contains lists of lists, h and w contain width and height.
        int[] colors = new int[h * w];

        int y = 0;

        for(List<Integer> row : p) {
            int i = 0;

            for(Integer color : row) {
                colors[i++] = color;
            }
        }

        return new Palette(w, h, colors);
    }


    public static <A> Map<String, A> merge(Map<String, A> primary, Map<String, A> secondary) {
        Map<String, A> merged = new HashMap<>();

        merged.putAll(secondary);
        merged.putAll(primary);

        return merged;
    }



    private static final int ICON_LEN = 64;

    /**
     * creates a new bitmap with size 64x64 containing the center of the current image
     * @return
     */
    private static Bitmap createIcon(Bitmap original) {
        // FIXME Move somewhere else!
        // create a square icon. Should  only contain central square.
        Bitmap icon = Bitmap.createBitmap(ICON_LEN, ICON_LEN, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(icon);

        float scale = ((float) ICON_LEN) / Math.min(original.getWidth(), original.getHeight());

        float w = original.getWidth();
        float h = original.getHeight();

        Matrix transformation = new Matrix();
        transformation.setValues(new float[]{
                scale, 0, (ICON_LEN - scale * w) * .5f,
                0, scale, (ICON_LEN - scale * h) * .5f,
                0, 0, 1,
        });

        Paint paint = new Paint();
        paint.setFilterBitmap(true);

        canvas.drawBitmap(original, transformation, paint);

        return icon;
    }

    private static byte[] getBitmapBinary(Bitmap bitmapPicture) {
        // FIXME Move somewhere else!
        // Thanks to http://mobile.cs.fsu.edu/converting-images-to-json-objects/
        ByteArrayOutputStream byteArrayBitmapStream = new ByteArrayOutputStream();
        bitmapPicture.compress(Bitmap.CompressFormat.PNG, 100, byteArrayBitmapStream);
        return byteArrayBitmapStream.toByteArray();
    }

    /**
     * Convert a Base64 encoded icon to a bitmap
     * @return
     */
    private static Bitmap getBitmapFromBinary(byte[] binaryData) {
        // Thanks to http://mobile.cs.fsu.edu/converting-images-to-json-objects/
        return BitmapFactory.decodeByteArray(binaryData, 0, binaryData.length);
    }

}
