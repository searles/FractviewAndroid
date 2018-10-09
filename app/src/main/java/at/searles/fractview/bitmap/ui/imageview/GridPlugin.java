package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;

import at.searles.fractview.bitmap.ui.ScalableImageView;

/**
 * Simple plugin that only shows a grid
 */
public class GridPlugin extends Plugin {
    /**
     * The grid is painted from two kinds of lines. These are the paints
     */
    private static final Paint[] GRID_PAINTS = gridPaints();

    public static final float LEFT_UP_INDICATOR_LENGTH = 40f;

    private boolean showGrid;

    public GridPlugin(ScalableImageView parent, boolean showGrid) {
        super(parent);
        this.showGrid = showGrid;
    }

    @Override
    public void onDraw(@NotNull Canvas canvas) {
        float w = parent.getWidth();
        float h = parent.getHeight();

        float cx = w / 2.f;
        float cy = h / 2.f;

        if(parent.flipBitmap(w, h)) {
            // draw an indicator in the left upper corner of the bitmap
            // which is in this case
            for (Paint gridPaint : GRID_PAINTS) {
                // three lines in total
                canvas.drawLine(
                        w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
                        w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 1.5f,
                        gridPaint
                );

                canvas.drawLine(
                        w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
                        w - LEFT_UP_INDICATOR_LENGTH * 1.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
                        gridPaint
                );

                canvas.drawLine(
                        w - LEFT_UP_INDICATOR_LENGTH * 1.5f, LEFT_UP_INDICATOR_LENGTH * 0.5f,
                        w - LEFT_UP_INDICATOR_LENGTH * 0.5f, LEFT_UP_INDICATOR_LENGTH * 1.5f,
                        gridPaint
                );
            }
        }


        if(showGrid) {
            float bw = parent.scaledBitmapWidth(parent.getWidth(), parent.getHeight());
            float bh = parent.scaledBitmapHeight(parent.getWidth(), parent.getHeight());

            float minlen = Math.min(bw, bh) / 2.f;

            for (int i = 0; i < GRID_PAINTS.length; ++i) {
                Paint gridPaint = GRID_PAINTS[i];

                // outside grid
                canvas.drawLine(0, cy - minlen, w, cy - minlen, gridPaint);
                canvas.drawLine(0, cy + minlen, w, cy + minlen, gridPaint);
                canvas.drawLine(cx - minlen, 0, cx - minlen, h, gridPaint);
                canvas.drawLine(cx + minlen, 0, cx + minlen, h, gridPaint);

                // inside cross
                canvas.drawLine(0, h / 2.f, w, h / 2.f, gridPaint);
                canvas.drawLine(w / 2.f, 0, w / 2.f, h, gridPaint);

                // and a circle inside
                canvas.drawCircle(w / 2.f, h / 2.f, minlen, gridPaint);

                // and also draw quaters with thinner lines
                if(i != 0) {
                    canvas.drawLine(0, cy - minlen / 2.f, w, cy - minlen / 2.f, gridPaint);
                    canvas.drawLine(0, cy + minlen / 2.f, w, cy + minlen / 2.f, gridPaint);
                    canvas.drawLine(cx - minlen / 2.f, 0, cx - minlen / 2.f, h, gridPaint);
                    canvas.drawLine(cx + minlen / 2.f, 0, cx + minlen / 2.f, h, gridPaint);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        // no touch
        return false;
    }


    private static Paint[] gridPaints() {
        Paint[] gridPaints = new Paint[]{new Paint(), new Paint(), new Paint()};

        gridPaints[0].setColor(0xffffffff);
        gridPaints[0].setStyle(Paint.Style.STROKE);
        gridPaints[0].setStrokeWidth(5f);

        gridPaints[1].setColor(0xff000000);
        gridPaints[1].setStyle(Paint.Style.STROKE);
        gridPaints[1].setStrokeWidth(3f);

        gridPaints[2].setColor(0xffffffff);
        gridPaints[2].setStyle(Paint.Style.STROKE);
        gridPaints[2].setStrokeWidth(1f);

        return gridPaints;
    }
}
