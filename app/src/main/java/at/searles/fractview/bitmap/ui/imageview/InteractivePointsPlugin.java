package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import at.searles.fractview.bitmap.ui.ScalableImageView;
import at.searles.fractview.main.CalculatorWrapper;
import at.searles.fractview.main.InteractivePoint;

public class InteractivePointsPlugin extends Plugin {
    private static final float BALLON_RADIUS_INCH = 0.15f; // makes a diameter of 0.3 inch
    private static final float BALLON_ANGLE = 50.f; // the larger the more distance

    // This one must be agnostic towards view coordinates.
    // Therefore, it should either store bitmap coordinates
    // or normalized coordinates.

    // Convert normalized to view:
    // parent.screenToNormalized (does this consider current selection?)
    // parent.normalizedToScreen

    private final List<ViewPoint> points;
    private CalculatorWrapper wrapper;
    private final ScalableImageView parent;

    private ViewPoint draggedPoint;
    private float dx, dy; // if it was grabbed a bit on the side keep the side

    private final Paint paintStroke;
    private final Paint paintFill;

    private boolean enabled;

    private final float ballonRadius; // 1/3 inch

    public InteractivePointsPlugin(ScalableImageView parent) {

        this.ballonRadius = parent.getContext().getResources().getDisplayMetrics().densityDpi * BALLON_RADIUS_INCH;

        this.parent = parent;

        points = new ArrayList<>(2);

        paintStroke = new Paint();
        paintFill = new Paint();

        initPaint();

        this.enabled = true;
    }

    public void setWrapper(CalculatorWrapper wrapper) {
        this.wrapper = wrapper;
        updatePoints();
    }

    public void updatePoints() {
        points.clear();

        if(wrapper != null) {
            for (InteractivePoint pt : wrapper.interactivePoints()) {
                double[] normPt = new double[2];
                wrapper.valueToNorm(pt.position()[0], pt.position()[1], normPt);
                points.add(new ViewPoint(pt, normPt));
            }

            parent.invalidate();
        }
    }

    private void initPaint() {
        paintStroke.setColor(0xffffffff);
        paintStroke.setStyle(Paint.Style.STROKE);
        paintStroke.setStrokeWidth(ballonRadius / 12.f);
        paintFill.setColor(0xff444444);
        paintFill.setStyle(Paint.Style.FILL);
    }

    private void drawPoint(@NotNull Canvas canvas, float x, float y, int color, boolean isSelected) {
        float rad = ballonRadius;
        float alpha = BALLON_ANGLE;

        float sinAlpha = (float) Math.sin(alpha * Math.PI / 180);
        float height = (float) (rad * Math.sqrt((1 + sinAlpha) / (1 - sinAlpha))); // distance x/y to center of drop

        updatePaintColor(color, isSelected);

        float rad2 = rad * sinAlpha / (1 - sinAlpha); // radius of pointy part

        Path path = new Path();

        path.moveTo(x, y);
        path.arcTo(x - 2 * rad2, y - rad2, x, y + rad2, 0F, alpha - 90F, false);
        path.arcTo(x - rad, y - height - rad, x + rad, y - height + rad,
                90 + alpha, 360 - 2 * alpha, false);
        path.arcTo(x, y - rad2, x + 2 * rad2, y + rad2, 270 - alpha, alpha - 90, false);
        path.close();

        path.addCircle(x, y - height, rad / 2, Path.Direction.CW);

        path.setFillType(Path.FillType.EVEN_ODD);

        canvas.drawPath(path, paintFill);
        canvas.drawPath(path, paintStroke);
    }

    private void updatePaintColor(int color, boolean isSelected) {
        if(!isSelected) {
            color = color & 0x7FFFFFFF;
        }

        paintFill.setColor(color);
    }

    /**
     * returns the distance from the center of the point drawing (not the point itself)
     * if x/y is close to it, otherwise Float.MAX_VALUE (not selected)
     */
    private float isSelected(float x, float y, float pointX, float pointY) {
        float rad = ballonRadius;
        float alpha = BALLON_ANGLE; // alpha being the angle between the horizontal and
        // radius to the begin of the drop ball.

        float sinAlpha = (float) Math.sin(alpha * Math.PI / 180);
        float height = (float) (rad * Math.sqrt((1 + sinAlpha) / (1 - sinAlpha))); // distance x/y to center of drop

        // left top rectangle
        float dx = Math.abs(x - pointX);
        float dy = Math.abs(y - (pointY - height));

        float d = Math.max(dx, dy);

        if(d < rad) {
            return d;
        }

        return Float.MAX_VALUE;
    }

    @Override
    public void onDraw(@NotNull Canvas canvas) {
        if(!enabled) return;

        for(ViewPoint point : points) {
            point.updateViewPt();

            int color = point.color();

            drawPoint(canvas, point.viewPt[0], point.viewPt[1], color, false);

            if(point.draggedPosition != null) {
                drawPoint(canvas, point.draggedPosition[0], point.draggedPosition[1], color, true);
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        if(!enabled) return false;

        int action = event.getActionMasked();

        switch(action) {
            case MotionEvent.ACTION_CANCEL: {
                // same as back pressed.
                return cancelDragging();
            }
            case MotionEvent.ACTION_DOWN: {
                return event.getPointerId(0) == 0 && trySelectPoint(event.getX(), event.getY());
            }
            case MotionEvent.ACTION_UP: {
                return releasePoint();
            }
            case MotionEvent.ACTION_MOVE: {
                int index = event.getActionIndex();
                int id = event.getPointerId(index);

                return id == 0 && movePoint(event.getX(), event.getY());
            }
            default:
                Log.e("Unknown Action", event.toString());
                return false;
        }
    }

    private boolean trySelectPoint(float x, float y) {
        assert draggedPoint == null;

        // find closest point
        ViewPoint closest = null;
        float closestDistance = Float.MAX_VALUE;

        for(ViewPoint point : points) {
            float d = isSelected(x, y, point.viewPt[0], point.viewPt[1]);

            if(closestDistance > d) {
                closestDistance = d;
                closest = point;

                this.dx = point.viewPt[0] - x;
                this.dy = point.viewPt[1] - y;
            }
        }

        if(closestDistance < Float.MAX_VALUE) {
            draggedPoint = closest;
            draggedPoint.initDragging();
            parent.invalidate();
            return true;
        }

        return false;
    }

    private boolean releasePoint() {
        if(draggedPoint != null) {
            // inform others of the update
            float[] normPt = new float[2];
            double[] selectedPt = new double[2];
            parent.screenToNormalized(draggedPoint.draggedPosition[0], draggedPoint.draggedPosition[1], normPt);
            wrapper.normToValue(normPt[0], normPt[1], selectedPt);

            draggedPoint.pt.setValue(selectedPt);

            draggedPoint = null;
            parent.invalidate();

            return true;
        } else {
            return false;
        }
    }

    private boolean movePoint(float x, float y) {
        if(draggedPoint != null) {
            draggedPoint.dragPointTo(x + dx, y + dy);
            parent.invalidate();
            return true;
        }

        return false;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean cancelDragging() {
        if(draggedPoint != null) {
            draggedPoint.cancelDragging();
            draggedPoint = null;
            parent.invalidate();
            return true;
        }

        return false;
    }

    private class ViewPoint {
        /**
         * This one stores listeners to events for the given point in addition to
         * the point projected onto the bitmap (which is the ground truth) and
         * the point projected onto the view. The bitmap point is translated via
         * the imageMatrix into the viewPoint, hence it takes current selections
         * into consideration.
         */

        final InteractivePoint pt;

        final double normPt[]; // updated when needed.
        float draggedPosition[]; // null if not needed.

        float[] viewPt; // updated on every call.

        private ViewPoint(InteractivePoint pt, double normPt[]) {
            this.pt = pt;
            this.normPt = normPt;

            this.viewPt = new float[2];

            this.draggedPosition = null;
        }

        void updateViewPt() {
            InteractivePointsPlugin.this.parent.normalizedToBitmap((float) normPt[0], (float) normPt[1], viewPt);
            InteractivePointsPlugin.this.parent.bitmapToView(viewPt[0], viewPt[1], viewPt);
        }

        void initDragging() {
            this.draggedPosition = new float[]{viewPt[0], viewPt[1]};
        }

        void dragPointTo(float viewX, float viewY) {
            if(draggedPosition != null) {
                this.draggedPosition[0] = viewX;
                this.draggedPosition[1] = viewY;
            }
        }

        public int color() {
            return pt.color();
        }

        void cancelDragging() {
            this.draggedPosition = null;
        }
    }
}
