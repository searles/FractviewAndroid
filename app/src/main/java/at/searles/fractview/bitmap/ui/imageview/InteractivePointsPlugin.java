package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
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
        // fixme one for each should be enough?
        selectedPaintStroke.setColor(0xffffffff);
        selectedPaintStroke.setStyle(Paint.Style.STROKE);
        selectedPaintStroke.setStrokeWidth(8.f); // fixme dpi!
        selectedPaintFill.setColor(0xff444444);
        selectedPaintFill.setStyle(Paint.Style.FILL);

        unselectedPaintStroke.setColor(0x80ffffff);
        unselectedPaintStroke.setStyle(Paint.Style.STROKE);
        unselectedPaintStroke.setStrokeWidth(8.f); // fixme dpi!
        unselectedPaintFill.setColor(0x80444444);
        unselectedPaintFill.setStyle(Paint.Style.FILL);
    }

    private void drawPoint(@NotNull Canvas canvas, float x, float y, boolean isSelected) {
        float rad = 60; // fixme from DPI!
        float alpha = 45; // alpha being the angle between the horizontal and
        // radius to the begin of the drop ball.

        float sinAlpha = (float) Math.sin(alpha * Math.PI / 180);
        float height = (float) (rad * Math.sqrt((1 + sinAlpha) / (1 - sinAlpha))); // distance x/y to center of drop

        Paint fill = isSelected ? selectedPaintFill : unselectedPaintFill;
        Paint stroke = isSelected ? selectedPaintStroke : unselectedPaintStroke;

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

        canvas.drawPath(path, fill);
        canvas.drawPath(path, stroke);
    }

    /**
     * returns the distance from the center of the point drawing (not the point itself)
     * if x/y is close to it, otherwise Float.MAX_VALUE (not selected)
     */
    private float isSelected(float x, float y, float pointX, float pointY) {
        float rad = 60; // fixme from DPI and larger than drawing!
        float alpha = 45; // alpha being the angle between the horizontal and
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
            point.updateViewPoint();

            drawPoint(canvas, point.viewX(), point.viewY(), draggedPoint == point);
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

    public void addPoint(InteractivePoint pt, float viewX, float viewY, CalculatorView parent) {
        ViewPoint point = new ViewPoint(parent, pt, viewX, viewY);

        points.add(point);
        parent.invalidate();
    }

    private boolean trySelectPoint(float x, float y) {
        if(draggedPoint != null) {
            throw new IllegalStateException("this is embarrasing and should not happen: old point was not released?");
        }

        // find closest point
        ViewPoint closest = null;
        float closestDistance = Float.MAX_VALUE;

        for(ViewPoint point : points) {
            float d = isSelected(x, y, point.viewX(), point.viewY());

            if(closestDistance > d) {
                closestDistance = d;
                closest = point;

                this.dx = point.viewX() - x;
                this.dy = point.viewY() - y;
            }
        }

        if(closestDistance < Float.MAX_VALUE) {
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

    public boolean cancelDragging() {
        if(draggedPoint != null) {
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
