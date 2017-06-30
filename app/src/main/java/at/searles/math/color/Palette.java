/*
 * This file is part of FractView.
 *
 * FractView is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FractView is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with FractView.  If not, see <http://www.gnu.org/licenses/>.
 */
package at.searles.math.color;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;

import at.searles.math.InterpolationMatrix;
import at.searles.math.Matrix4;

public class Palette {

	private final int width;
	private final int height;
	private final int[][] colors;

	public Palette(int[][] colors) {
		this.height = colors.length;
		this.width = colors[0].length;

		this.colors = new int[height][width];

		for (int y = 0; y < height; ++y) {
			System.arraycopy(colors[y], 0, this.colors[y], 0, width);
		}
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int argb(int x, int y) {
		return colors[y][x];
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		boolean b0 = true;

		for(int[] row : colors) {
			if(b0) b0 = false; // first element in row
			else sb.append(", ");

			boolean b1 = true;
			sb.append("[");

			for(int c : row) {
				if(b1) b1 = false;
				else sb.append(", ");

				sb.append(Colors.toColorString(c));
			}

			sb.append("]");
		}

		sb.append("]");

		return sb.toString();
	}


	public static class LABSurface {
		public final Matrix4 L;
		public final Matrix4 a;
		public final Matrix4 b;
		public final Matrix4 alpha;

		private LABSurface(Matrix4 L, Matrix4 a, Matrix4 b, Matrix4 alpha) {
			this.L = L;
			this.a = a;
			this.b = b;
			this.alpha = alpha;
		}
	}


	static LABSurface[][] createSplines(int[][] argbs) {
		// must be a rectangle
		int h = argbs.length;
		int w = argbs[0].length;

		double[][] L = new double[h][w];
		double[][] a = new double[h][w];
		double[][] b = new double[h][w];
		double[][] alpha = new double[h][w];

		for(int y = 0; y < h; ++y) {
			for(int x = 0; x < w; ++x) {
				// fixme shortcut!
				float[] lab = Colors.rgb2lab(Colors.int2rgb(argbs[y][x]));

				L[y][x] = lab[0];
				a[y][x] = lab[1];
				b[y][x] = lab[2];
				alpha[y][x] = lab[3];
			}
		}

		Matrix4[][] LSpline = InterpolationMatrix.create(L);
		Matrix4[][] aSpline = InterpolationMatrix.create(a);
		Matrix4[][] bSpline = InterpolationMatrix.create(b);
		Matrix4[][] alphaSpline = InterpolationMatrix.create(alpha);

		LABSurface[][] cs = new LABSurface[h][w];

		for(int y = 0; y < h; ++y) {
			for(int x = 0; x < w; ++x) {
				cs[y][x] = new LABSurface(LSpline[y][x], aSpline[y][x], bSpline[y][x], alphaSpline[y][x]);
			}
		}

		return cs;
	}

	public Palette.Data create() {
		LABSurface[][] cs = createSplines(colors);
		return new Data(cs);
	}

	public static class Data {
		public final int w;
		public final int h;
		public final LABSurface[] splines;

		private Data(LABSurface[][] splines) {
			this.w = splines[0].length;
			this.h = splines.length;

			this.splines = new LABSurface[h * w];

			for(int y = 0; y < h; ++y) {
				for(int x = 0; x < w; ++x) {
					this.splines[y * w + x] = splines[y][x];
				}
			}
		}

		public int countSurfaces() {
			return splines.length;
		}
	}

	public static final String WIDTH_LABEL = "width";
	public static final String HEIGHT_LABEL = "height";
	public static final String COLORS_LABEL = "colors";

	public JsonElement serialize() {
		JsonObject object = new JsonObject();

		object.addProperty(WIDTH_LABEL, width);
		object.addProperty(HEIGHT_LABEL, height);

		JsonArray array = new JsonArray();

		for(int y = 0; y < height(); ++y) {
			for(int x = 0; x < width(); ++x) {
				array.add(argb(x, y));
			}
		}

		object.add(COLORS_LABEL, array);

		return object;
	}

	public static Palette deserialize(JsonElement json)
			throws JsonParseException {
		JsonObject object = (JsonObject) json;

		int width = object.get(WIDTH_LABEL).getAsInt();
		int height = object.get(HEIGHT_LABEL).getAsInt();

		JsonArray array = object.getAsJsonArray(COLORS_LABEL);

		int colors[][] = new int[height][width];

		for(int y = 0; y < height; ++y) {
			for(int x = 0; x < width; ++x) {
				colors[y][x] = array.get(x + y * width).getAsInt();
			}
		}

		return new Palette(colors);
	}

	// ======= GSON Adapter ========
	public static class JsonAdapter implements JsonDeserializer<Palette>, JsonSerializer<Palette> {
		@Override
		public Palette deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return Palette.deserialize(json);
		}

		@Override
		public JsonElement serialize(Palette src, Type typeOfSrc, JsonSerializationContext context) {
			return src.serialize();
		}
	}
}