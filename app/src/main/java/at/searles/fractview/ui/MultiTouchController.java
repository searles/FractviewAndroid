package at.searles.fractview.ui;

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Pair;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

public class MultiTouchController {

	private Matrix matrix;
	private Map<Integer, Pair<PointF, PointF>> points;
	private boolean rotationLock; // the matrix should not rotate the image.

	public MultiTouchController(boolean rotationLock) {
		points = new TreeMap<>();
		matrix = new Matrix(); // identity matrix
		this.rotationLock = rotationLock;
	}

	private Matrix createCurrentMatrix() {
		if(points.size() >= 3) {
			Iterator<Pair<PointF, PointF>> i = points.values().iterator();
			Pair<PointF, PointF> pq0 = i.next();
			Pair<PointF, PointF> pq1 = i.next();
			Pair<PointF, PointF> pq2 = i.next();
			// all other points are ignored.

			// Get the matrix
			// ( a b tx )
			// ( c d ty )
			// ( 0 0  1 )
			// m * p0 = q0
			// m * p1 = q1
			// m * p2 = q2
			float p0x = pq0.first.x;
			float p0y = pq0.first.y;
			float p1x = pq1.first.x;
			float p1y = pq1.first.y;
			float p2x = pq2.first.x;
			float p2y = pq2.first.y;
			float q0x = pq0.second.x;
			float q0y = pq0.second.y;
			float q1x = pq1.second.x;
			float q1y = pq1.second.y;
			float q2x = pq2.second.x;
			float q2y = pq2.second.y;

			float det = p0x * p1y + p1x * p2y + p2x * p0y -
							(p0x * p2y + p1x * p0y + p2x * p1y);

			float detA = q0x * p1y + q1x * p2y + q2x * p0y - (q0x * p2y + q1x * p0y + q2x * p1y);
			float detB = p0x * q1x + p1x * q2x + p2x * q0x - (p0x * q2x + p1x * q0x + p2x * q1x);
			float detTx = p0x * p1y * q2x + p1x * p2y * q0x + p2x * p0y * q1x - (p0x * p2y * q1x + p1x * p0y * q2x + p2x * p1y * q0x);

			float detC = q0y * p1y + q1y * p2y + q2y * p0y - (q0y * p2y + q1y * p0y + q2y * p1y);
			float detD = p0x * q1y + p1x * q2y + p2x * q0y - (p0x * q2y + p1x * q0y + p2x * q1y);
			float detTy = p0x * p1y * q2y + p1x * p2y * q0y + p2x * p0y * q1y - (p0x * p2y * q1y + p1x * p0y * q2y + p2x * p1y * q0y);

			float a = detA / det;
			float b = detB / det;
			float tx = detTx / det;
			float c = detC / det;
			float d  = detD / det;
			float ty  = detTy / det;

			if(rotationLock) {
				// if it is a rotation lock these two are set to 0.
				b = c = 0.f;
			}

			Matrix m = new Matrix();
			m.setValues(new float[]{a, b, tx, c, d, ty, 0.f, 0.f, 1.f});
			return m;
		} else if(points.size() == 2) {
			// Get the matrix
			// ( r string tx )
			// (-string r ty )
			// ( 0 0  1 )
			// m * p0 = q0
			// m * p1 = q1

			Iterator<Pair<PointF, PointF>> i = points.values().iterator();
			Pair<PointF, PointF> pq0 = i.next();
			Pair<PointF, PointF> pq1 = i.next();

			float p0x = pq0.first.x;
			float p0y = pq0.first.y;
			float p1x = pq1.first.x;
			float p1y = pq1.first.y;

			float q0x = pq0.second.x;
			float q0y = pq0.second.y;
			float q1x = pq1.second.x;
			float q1y = pq1.second.y;

			float dpx = p0x - p1x;
			float dpy = p0y - p1y;
			float dqx = q0x - q1x;
			float dqy = q0y - q1y;

			float det = dpx * (-dpx) - dpy * dpy;

			float detR = dqx * (-dpx) - dqy * dpy;
			float detS = dpx * dqy - dpy * dqx;

			float r = detR / det;
			float s = detS / det;

			float tx = q0x - r * p0x - s * p0y;
			float ty = q0y - r * p0y + s * p0x;

			if(rotationLock) {
				// if it is rotation lock I allow rotations
				// around 90 degrees with 2 fingers.
				if(Math.abs(r) > Math.abs(s)) {
					s = 0.f;
				} else {
					r = 0.f;
				}
			}

			Matrix m = new Matrix();
			m.setValues(new float[]{r, s, tx, -s, r, ty, 0.f, 0.f, 1.f});
			return m;
		} else if(points.size() == 1) {
			Pair<PointF, PointF> pq = points.values().iterator().next(); // fixme what is the best way?

			// Get the matrix m
			// ( 1 0 tx )
			// ( 0 1 ty )
			// ( 0 0  1 )
			// such that
			// m * juliaAddend = q

			float tx = pq.second.x - pq.first.x;
			float ty = pq.second.y - pq.first.y;

			Matrix m = new Matrix();
			m.setValues(new float[]{1.f, 0.f, tx, 0.f, 1.f, ty, 0.f, 0.f, 1.f});
			return m;
		} else {
			return new Matrix();
		}
	}

	public Matrix getMatrix() {
		Matrix m = createCurrentMatrix();
		if(!m.preConcat(matrix)) {
			// FIXME: Is this correct?
			throw new IllegalArgumentException("could not concat matrices");
		}
		return m;
	}

	private void commit() {
		// commit current changes.
		this.matrix = getMatrix();

		// reset points
		for(Map.Entry<Integer, Pair<PointF, PointF>> p : points.entrySet()) {
			p.setValue(new Pair<>(p.getValue().second, p.getValue().second));
		}
	}

	public void addPoint(int pointerId, PointF pos) {
		if(points.containsKey(pointerId)) {
			throw new IllegalArgumentException("trying to add a point with an already existing index");
		} else {
			commit();

			if(pos == null) throw new NullPointerException();

			points.put(pointerId, new Pair<>(pos, pos));
		}
	}

	public void movePoint(int pointerId, PointF pos) {
		if (!points.containsKey(pointerId)) {
			throw new IllegalArgumentException("trying to update non-existent index");
		} else {
			Pair<PointF, PointF> entry = points.get(pointerId);
			points.put(pointerId, new Pair<>(entry.first, pos));
		}
	}

	public PointF removePoint(int pointerId) {
		if(!points.containsKey(pointerId)) {
			throw new IllegalArgumentException("trying to remove non-existent index");
		} else {
			commit(); // commit overrides points
			Pair<PointF, PointF> p = points.remove(pointerId);
			// after commit hence juliaAddend.first == juliaAddend.second.
			if(p == null) throw new AssertionError("remove returned null");
			return p.second;
		}
	}

	public boolean isDone() {
		return points.isEmpty();
	}

	public String toString() {
		String s = "";
		for(Pair<PointF, PointF> pair : points.values()) {
			s += pair.first + " -> " + pair.second + "; ";
		}

		return s;
	}
}
