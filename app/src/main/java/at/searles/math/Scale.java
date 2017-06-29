package at.searles.math;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.Arrays;

public class Scale {

	// Indices
	public static final int XX = 0;
	public static final int XY = 1;
	public static final int YX = 2;
	public static final int YY = 3;
	public static final int CX = 4;
	public static final int CY = 5;

	public static Scale createScaled(double sc) {
		return new Scale(sc, 0, 0, sc, 0, 0);
	}


	/*
	 * If this was a matrix, it would be
	 * m[0] = xx, m[1] = yx, m[2] = cx
	 * m[3] = xy, m[4] = yy, m[5] = cy
	 */
	public final double data[];

	public Scale(double xx, double xy, double yx, double yy, double cx, double cy) {
		data = new double[6];
		data[XX] = xx;
		data[XY] = xy;
		data[YX] = yx;
		data[YY] = yy;
		data[CX] = cx;
		data[CY] = cy;
	}

	public boolean equals(Object o) {
		if(o instanceof Scale) {
			for(int i = 0; i < 6; ++i) {
				if(data[i] != ((Scale) o).data[i]) return false;
			}

			return true;
		} else {
			return false;
		}
	}

	public static Scale fromMatrix(float...m) {
		return new Scale(m[0], m[3], m[1], m[4], m[2], m[5]);
	}

	/*public double[] scale(float x, float y) {
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
	}*/

	/**
	 * applies the matrix m relative to this scale.
	 * m must contain 6 values.
	 * @param that matrix to be post-contatenated with this scale
	 * @return
	 */
	public Scale relative(Scale that) {
		double xx = data[XX] * that.data[XX] + data[YX] * that.data[XY];
		double xy = data[XY] * that.data[XX] + data[YY] * that.data[XY];

		double yx = data[XX] * that.data[YX] + data[YX] * that.data[YY];
		double yy = data[XY] * that.data[YX] + data[YY] * that.data[YY];

		double cx = data[XX] * that.data[CX] + data[YX] * that.data[CY] + data[CX];
		double cy = data[XY] * that.data[CX] + data[YY] * that.data[CY] + data[CY];

		return new Scale(xx, xy, yx, yy, cx, cy);
	}

	public String toString() {
		return Arrays.toString(data);
	}

	public JsonElement serialize() {
		JsonArray array = new JsonArray();

		for(int i = 0; i < 6; ++i)
			array.add(data[i]);

		return array;
	}

	public static Scale deserialize(JsonElement json) throws JsonParseException {
		JsonArray array = (JsonArray) json;

		double data[] = new double[6];

		for(int i = 0; i < 6; ++i)
			data[i] = array.get(i).getAsDouble();

		return new Scale(data[0], data[1], data[2], data[3], data[4], data[5]);
	}

	public double xx() {
		return data[XX];
	}

	public double xy() {
		return data[XY];
	}

	public double yx() {
		return data[YX];
	}

	public double yy() {
		return data[YY];
	}

	public double cx() {
		return data[CX];
	}

	public double cy() {
		return data[CY];
	}

	// ======= GSON Adapter ========
	public static class JsonAdapter implements JsonDeserializer<Scale>, JsonSerializer<Scale> {
		@Override
		public Scale deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return Scale.deserialize(json);
		}

		@Override
		public JsonElement serialize(Scale src, Type typeOfSrc, JsonSerializationContext context) {
			return src.serialize();
		}
	}
}
