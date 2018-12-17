package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import at.searles.fractview.bitmap.ui.CalculatorView;
import at.searles.fractview.bitmap.ui.ScalableImageView;
import at.searles.fractview.main.InteractivePoint;

public class InteractivePointsPlugin extends Plugin {

    // This one must be agnostic towards view coordinates.
    // Therefore, it should either store bitmap coordinates
    // or normalized coordinates.

    // Convert normalized to view:
    // parent.screenToNormalized (does this consider current selection?)
    // parent.normalizedToScreen

    private static final float RAD = 50.f;

    private final List<ViewPoint> points;

    private ViewPoint draggedPoint;
    private float dx, dy; // if it was grabbed a bit on the side keep the side

    private final Paint unselectedPaintStroke;
    private final Paint unselectedPaintFill;

    private final Paint selectedPaintStroke;
    private final Paint selectedPaintFill;
    private boolean enabled;

    public InteractivePointsPlugin(ScalableImageView parent) {
        super(parent);

        points = new ArrayList<>(2);

        selectedPaintStroke = new Paint();
        selectedPaintFill = new Paint();
        unselectedPaintStroke = new Paint();
        unselectedPaintFill = new Paint();

        initPaint();
    }

    private void initPaint() {
        selectedPaintStroke.setColor(0xffffffff);
        selectedPaintStroke.setStyle(Paint.Style.STROKE);
        selectedPaintStroke.setStrokeWidth(4.f);
        selectedPaintFill.setColor(0x80676767);
        selectedPaintFill.setStyle(Paint.Style.FILL);
        unselectedPaintStroke.setColor(0x80ffffff);
        unselectedPaintStroke.setStyle(Paint.Style.STROKE);
        unselectedPaintStroke.setStrokeWidth(4.f);
        unselectedPaintFill.setColor(0x80676767);
        unselectedPaintFill.setStyle(Paint.Style.FILL);
    }

    @Override
    public void onDraw(@NotNull Canvas canvas) {
        if(!enabled) return;

        for(ViewPoint point : points) {
            point.updateViewPoint();

            float x = point.viewX();
            float y = point.viewY();

            if(draggedPoint == point) {
                canvas.drawCircle(x, y, RAD * 0.75f + 4, selectedPaintFill);
                canvas.drawCircle(x, y, RAD * 0.75f, selectedPaintStroke);
            } else {
                canvas.drawCircle(x, y, RAD * 0.5f + 2, unselectedPaintFill);
                canvas.drawCircle(x, y, RAD * 0.5f, unselectedPaintStroke);
            }
        }
    }

    @Override
    public boolean onTouchEvent(@NotNull MotionEvent event) {
        if(!enabled) return false;

        int action = event.getActionMasked();

        switch(action) {
            case MotionEvent.ACTION_CANCEL: {
                return releasePoint();
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

//    public void moveViewPointTo(InteractivePoint pt, float viewX, float viewY) {
//        ViewPoint point = findEntry(key);
//        point.moveViewPointTo(viewX, viewY);
//        parent.invalidate();
//    }
//
//    public void moveBitmapPointTo(String key, float bitmapX, float bitmapY) {
//        ViewPoint point = findEntry(key);
//        point.moveBitmapPointTo(bitmapX, bitmapY);
//        parent.invalidate();
//    }

    public void addPoint(InteractivePoint pt, float viewX, float viewY, CalculatorView parent) {
        ViewPoint point = new ViewPoint(parent, pt, viewX, viewY);

        points.add(point);
        parent.invalidate();
    }

    private boolean trySelectPoint(float x, float y) {
        if(draggedPoint != null) {
            throw new IllegalStateException("old point was not released?");
        }

        // find closest point
        ViewPoint closest = null;
        float closestDistance = Float.MAX_VALUE;

        for(ViewPoint point : points) {
            float dx = point.viewX() - x;
            float dy = point.viewY() - y;

            float d = dx * dx + dy * dy;

            if(closestDistance > d) {
                closestDistance = d;
                closest = point;

                this.dx = dx;
                this.dy = dy;
            }
        }

        if(closestDistance <= RAD * RAD) {
            draggedPoint = closest;
            parent.invalidate();
            return true;
        }

        return false;
    }

    private boolean releasePoint() {
        if(draggedPoint != null) {
            // inform others of the update
            draggedPoint.firePointMovedEvent();
            draggedPoint = null;
            parent.invalidate();

            return true;
        } else {
            return false;
        }
    }

    private boolean movePoint(float x, float y) {
        if(draggedPoint != null) {
            draggedPoint.moveViewPointTo(x + dx, y + dy);
            parent.invalidate();
            return true;
        }

        return false;
    }

    public void clear() {
        points.clear();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        private final CalculatorView parent;

        float bitmapPoint[];

        transient float viewPoint[]; // updated when needed.

        private ViewPoint(CalculatorView parent, InteractivePoint pt, float viewX, float viewY) {
            this.pt = pt;
            this.parent = parent;

            this.bitmapPoint = new float[2];
            this.viewPoint = new float[2];

            moveViewPointTo(viewX, viewY);
        }

        void firePointMovedEvent() {
            parent.interactivePointMoved(pt, viewPoint[0], viewPoint[1]);
        }

        void updateViewPoint() {
            // this is called from the onDraw method.
            viewPoint[0] = bitmapPoint[0];
            viewPoint[1] = bitmapPoint[1];

            InteractivePointsPlugin.this.parent.bitmapToView(viewPoint);
        }

        void moveViewPointTo(float viewX, float viewY) {
            bitmapPoint[0] = viewPoint[0] = viewX;
            bitmapPoint[1] = viewPoint[1] = viewY;

            InteractivePointsPlugin.this.parent.viewToBitmap(bitmapPoint);
        }

//        public void moveBitmapPointTo(float bitmapX, float bitmapY) {
//            bitmapPoint[0] = bitmapX;
//            bitmapPoint[1] = bitmapY;
//        }

        float viewX() {
            return viewPoint[0];
        }

        float viewY() {
            return viewPoint[1];
        }
    }
}
