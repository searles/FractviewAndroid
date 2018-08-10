package at.searles.fractview.utils;

import at.searles.math.Scale;

public class ScaleUtils {
    public static Scale straighten(Scale scale) {
        double xlen = Math.hypot(scale.xx, scale.xy);
        double ylen = Math.hypot(scale.yx, scale.yy);

        return new Scale(xlen, 0, 0, ylen, scale.cx, scale.cy);
    }

    public static Scale orthogonalize(Scale scale) {
        // FIXME Check these
        // Step 1: make x/y-vectors same length
        double xx = scale.xx;
        double xy = scale.xy;

        double yx = scale.yx;
        double yy = scale.yy;

        double lenx = Math.sqrt(xx * xx + xy * xy);
        double leny = Math.sqrt(yx * yx + yy * yy);

        double mlen = Math.max(lenx, leny);

        xx *= mlen / lenx;
        xy *= mlen / lenx;
        yx *= mlen / leny;
        yy *= mlen / leny;

        double vx = (xx + yx) / 2;
        double vy = (xy + yy) / 2;

        double ax = vx + vy;
        double ay = vx - vy;

        return new Scale(ax, ay, -ay, ax, scale.cx, scale.cy);
    }
}
