package at.searles.fractview.fractal;

import android.graphics.Bitmap;

import at.searles.math.Scale;

/**
 * This is a sample class so that the emulator works (which does not support RenderScript)
 */
public class DemoFractalDrawer implements FractalDrawer {

    public static final int factor = 2; // 2^INIT_PIX_SIZE = 16

    private Controller controller;
    private int progress;
    private int maxProgress;

    public DemoFractalDrawer(Controller controller) {
        this.controller = controller;
    }

    private Fractal f;
    private Bitmap b;
    private Scale sc;

    @Override
    public void init(Bitmap bitmap, Fractal fractal) {
        this.b = bitmap;
        this.f = fractal;
    }

    @Override
    public void updateBitmap(Bitmap bm) {
        this.b = bm;
    }

    @Override
    public void setScale(Scale sc) {
        this.sc = sc;
    }

    @Override
    public void setFractal(Fractal f) {
        this.f = f;
        this.setScale(f.scale());
    }

    @Override
    public float progress() {
        return ((float) progress) / (float) maxProgress;
    }

    @Override
    public void run() {
        long dur = System.currentTimeMillis();

        int width = b.getWidth();
        int height = b.getHeight();

        maxProgress = width * height;

        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                b.setPixel(x, y, 0xffff0000); // red background = lake
            }
        }

        controller.previewGenerated();

        for(int y = 0; y < height; ++y) {
            for(int x = 0; x < width; ++x) {
                double[] c = sc.scale(x, y); // fixme update Scale to take a double[] array or a Cplx

                double zr = 0, zi = 0;

                /*for(int i = 0; i < 64; ++i) {
                    if(zr * zr + zi * zi >= 4) {
                        if(i < 16) b.setPixel(x, y, 0xff000000 | ((i * 16) << 24));
                        if(i < 32) b.setPixel(x, y, 0xffff0000 | (((i - 16) * 16) << 16));
                        if(i < 48) b.setPixel(x, y, 0xff000000 | (((i - 32) * 16) << 24));
                        else b.setPixel(x, y, 0xffffff00 | (((i - 32) * 16)));

                        break; // next pix.
                    } else {
                        double t = zr;
                        zr = zr * zr - zi * zi + c[0];
                        zi = 2 * zr * t + c[1];
                    }
                }*/

                if(controller.isCancelled()) break;
                progress ++;
            }

            if(controller.isCancelled()) break;

            controller.bitmapUpdated();
        }

        if (!(controller.isCancelled())) {
            dur = System.currentTimeMillis() - dur;
            controller.finished(dur);
        }
    }
}
