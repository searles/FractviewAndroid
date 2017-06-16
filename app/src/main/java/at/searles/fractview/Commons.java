package at.searles.fractview;

import android.graphics.Matrix;
import android.os.Handler;
import android.os.Looper;

import java.text.SimpleDateFormat;
import java.util.Date;

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
}
