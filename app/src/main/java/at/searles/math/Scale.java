package at.searles.math;

public class Scale {

	/*public static PointF bToN(float x, float y, float width, float height) {
		float centerX = (width - 1.f) / 2.f;
		float centerY = (height - 1.f) / 2.f;

		float scale = 1.f / Math.min(centerX, centerY);

		return new PointF(
				(x - centerX) * scale,
				(y - centerY) * scale
		);
	}

	public static Matrix bToN(float width, float height) {
		float cx = (width - 1.f) / 2.f;
		float cy = (height - 1.f) / 2.f;

		float sc = 1.f / Math.min(cx, cy);

		float tx = -cx * sc;
		float ty = -cy * sc;

		Matrix m = new Matrix();
		m.setValues(new float[]{
				sc, 0f, tx,
				0f, sc, ty,
				0f, 0f, 1f
		});

		return m;
	}

	public static PointF nToB(float x, float y, int width, int height) {
		float centerX = (width - 1.f) / 2.f;
		float centerY = (height - 1.f) / 2.f;

		float scale = Math.min(centerX, centerY);

		return new PointF(
				x * scale + centerX,
				y * scale + centerY
		);
	}

	public static Matrix nToB(float width, float height) {
		float cx = (width - 1.f) / 2.f;
		float cy = (height - 1.f) / 2.f;

		float sc = Math.min(cx, cy);

		Matrix m = new Matrix();
		m.setValues(new float[]{
				sc, 0f, cx,
				0f, sc, cy,
				0f, 0f, 1f
		});

		return m;
	}*/


	public static Scale createScaled(double sc) {
		return new Scale(sc, 0, 0, sc, 0, 0);
	}


	/*
	 * If this was a matrix, it would be
	 * m[0] = xx, m[1] = yx, m[2] = cx
	 * m[3] = xy, m[4] = yy, m[5] = cy
	 */
	public final double xx, xy, yx, yy, cx, cy;

	public Scale(double xx, double xy, double yx, double yy, double cx, double cy) {
		this.xx = xx;
		this.xy = xy;
		this.yx = yx;
		this.yy = yy;
		this.cx = cx;
		this.cy = cy;
	}

	public boolean equals(Object o) {
		if(o instanceof Scale) {
			Scale sc = (Scale) o;
			return sc.xx == xx && sc.xy == xy && sc.yx == yx && sc.yy == yy && sc.cx == cx && sc.cy == cy;
		} else {
			return false;
		}
	}

	public static Scale fromMatrix(double...m) {
		return new Scale(m[0], m[3], m[1], m[4], m[2], m[5]);
	}

	// fixme, again, why didn't I simply use a matrix here???
	// fixme just because renderscript does not have double-matrices?
	public static Scale fromMatrix(float...m) {
		return new Scale(m[0], m[3], m[1], m[4], m[2], m[5]);
	}

	public double[] scale(float x, float y) {
		return new double[]{
				xx * x + yx * y + cx,
				xy * x + yy * y + cy};
	}

	public float[] invScale(double x, double y) {
		x -= cx;
		y -= cy;

		double det = xx * yy - xy * yx;
		double detX = x * yy - y * yx;
		double detY = xx * y - xy * x;

		return new float[]{(float) (detX / det), (float) (detY / det)};
	}

	public Scale zoom(float px, float py, double factor) {
		double ncx = xx * px + yx * py + cx;
		double ncy = xy * px + yy * py + cy;

		return new Scale(xx * factor, xy * factor, yx * factor, yy * factor, ncx, ncy);
	}

	/**
	 * applies the matrix m relative to this scale.
	 * m must contain 6 values.
	 * @param that matrix to be post-contatenated with this scale
	 * @return
	 */
	public Scale relative(Scale that) {
		double m0 = xx * that.xx + yx * that.xy;
		double m1 = xx * that.yx + yx * that.yy;
		double m2 = xx * that.cx + yx * that.cy + cx;
		double m3 = xy * that.xx + yy * that.xy;
		double m4 = xy * that.yx + yy * that.yy;
		double m5 = xy * that.cx + yy * that.cy + cy;

		return new Scale(m0, m3, m1, m4, m2, m5);
	}

	/*public Scale orthogonal() {
		double lenx = Math.sqrt(xx * xx + xy * xy);
		double leny = Math.sqrt(yx * yx + yy * yy);

		// so, length is normalized. Now, take the diagonal
		double dx = (xx / lenx + yx / leny);
		double dy = (xy / lenx + yy / leny);

		// This is the diagonal is from the center to the lower right corner.

		// Now, find vectors v1, v2 st v1 * v2 = 0 and v1 + v2 = len.
		// don't forget, they are same length!
		// hence, vx - vy = dx, vx + vy = dy
		// therefore 2 vx = dx + dy, vy = dy - dx.
		double vx = (dx + dy) / 2.;
		double vy = (dx - dy) / 2.;

		double len = Math.max(lenx, leny);

		vx *= len;
		vy *= len;

		// longer/shorter
		if(xx * yy - yx * xy > 0)
			return new Scale(vx, vy, vy, -vx, cx, cy);
		else
			return new Scale(vy, -vx, vx, vy, cx, cy);
	}*/

	public String toString() {
		return xx + ":" + xy + " " + yx + ":" + yy + " " + cx + ":" + cy;
	}


}
