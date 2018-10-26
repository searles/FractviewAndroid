package at.searles.fractview.bitmap.ui.imageview;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.searles.fractview.bitmap.ui.ScalableImageView;

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
        for(ViewPoint point : points) {
            point.updateViewPoint();

            // FIXME: Show label

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

    private ViewPoint findEntry(String key) {
        // XXX if there are ever more than 3 points, consider using a map.
        for(ViewPoint point : points) {
            if(point.is(key)) {
                return point;
            }
        }

        throw new IllegalArgumentException("no such entry: " + key);
    }

    public void moveViewPointTo(String key, float viewX, float viewY) {
        ViewPoint point = findEntry(key);
        point.moveViewPointTo(viewX, viewY);
        parent.invalidate();
    }

    public void moveBitmapPointTo(String key, float bitmapX, float bitmapY) {
        ViewPoint point = findEntry(key);
        point.moveBitmapPointTo(bitmapX, bitmapY);
        parent.invalidate();
    }

    public void addPoint(String key, String label, float viewX, float viewY, PointListener... listeners) {
        ViewPoint point = new ViewPoint(key, label, new float[2]);
        point.moveViewPointTo(viewX, viewY);

        for(PointListener l : listeners) {
            point.addListener(l);
        }

        points.add(point);

        parent.invalidate();
    }

    public void removePoint(String key) {
        Iterator<ViewPoint> it = points.iterator();

        while(it.hasNext()) {
            ViewPoint point = it.next();

            if(point.is(key)) {
                it.remove();
                return;
            }
        }

        throw new IllegalArgumentException("not found " + key);
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

    public interface PointListener {
        void pointMoved(String key, float normX, float normY);
    }


    private class ViewPoint {
        final String key;

        final String label;
        final List<PointListener> listeners;

        float bitmapPoint[];

        transient float viewPoint[]; // updated when needed.

        private ViewPoint(String key, String label, float bitmapPoint[]) {
            this.key = key;
            this.label = label;
            this.listeners = new LinkedList<>();
            this.bitmapPoint = bitmapPoint;

            this.viewPoint = new float[2];
        }

        void firePointMovedEvent() {
            for(PointListener listener : listeners) {
                listener.pointMoved(key, viewPoint[0], viewPoint[1]);
            }
        }

        void addListener(PointListener listener) {
            this.listeners.add(listener);
        }

        boolean is(String key) {
            return key.equals(this.key);
        }

        void updateViewPoint() {
            // this is called from the onDraw method.
            viewPoint[0] = bitmapPoint[0];
            viewPoint[1] = bitmapPoint[1];

            parent.bitmapToView(viewPoint);
        }

        void moveViewPointTo(float viewX, float viewY) {
            bitmapPoint[0] = viewPoint[0] = viewX;
            bitmapPoint[1] = viewPoint[1] = viewY;

            parent.viewToBitmap(bitmapPoint);
        }

        public void moveBitmapPointTo(float bitmapX, float bitmapY) {
            bitmapPoint[0] = bitmapX;
            bitmapPoint[1] = bitmapY;
        }

        float viewX() {
            return viewPoint[0];
        }

        float viewY() {
            return viewPoint[1];
        }
    }
}
