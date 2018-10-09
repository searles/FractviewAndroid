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

            // FIXME: Show label

            if(draggedPoint == point) {
                canvas.drawCircle(point.x, point.y, RAD * 0.75f + 4, selectedPaintFill);
                canvas.drawCircle(point.x, point.y, RAD * 0.75f, selectedPaintStroke);
            } else {
                canvas.drawCircle(point.x, point.y, RAD * 0.5f + 2, unselectedPaintFill);
                canvas.drawCircle(point.x, point.y, RAD * 0.5f, unselectedPaintStroke);
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

    private class ViewPoint {
        final String key;

        final String label;
        final List<PointListener> listeners;

        float x;
        float y;

        private ViewPoint(String key, String label, float x, float y) {
            this.key = key;
            this.label = label;
            this.listeners = new LinkedList<>();
            this.x = x;
            this.y = y;
        }

        private void setScreenPosition(float x, float y) {
            this.x = x;
            this.y = y;
            parent.invalidate();
        }

        void firePointMovedEvent() {
            for(PointListener listener : listeners) {
                listener.pointMoved(key, x, y);
            }
        }

        void addListener(PointListener listener) {
            this.listeners.add(listener);
        }

        boolean is(String key) {
            return key.equals(this.key);
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

    public void movePointTo(String key, float x, float y) {
        ViewPoint point = findEntry(key);

        point.x = x;
        point.y = y;

        parent.invalidate();
    }

    public void addPoint(String key, String label, float x, float y, PointListener... listeners) {
        ViewPoint point = new ViewPoint(key, label, x, y);

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
            float dx = point.x - x;
            float dy = point.y - y;

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
            draggedPoint.setScreenPosition(x + dx, y + dy);
            parent.invalidate();
            return true;
        }

        return false;
    }

    public void clear() {
        points.clear();
    }

    public interface PointListener {
        void pointMoved(String key, float screenX, float screenY);
    }
}
