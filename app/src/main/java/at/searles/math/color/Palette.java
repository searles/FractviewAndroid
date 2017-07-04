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

import at.searles.math.InterpolationMatrix;
import at.searles.math.Matrix4;

public class Palette {

	private final int width;
	private final int height;
	private final int[] colors;

	public Palette(int width, int height, int[] colors) {
		this.width = width;
		this.height = height;

		this.colors = new int[width * height];

		System.arraycopy(colors, 0, this.colors, 0, this.colors.length);
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int argb(int x, int y) {
		return colors[y * width  + x];
	}

	public int[] colors() {
		return colors;
	}

	public String toString() {
		StringBuilder sb = new StringBuilder("[");

		boolean b0 = true;

		for(int y = 0; y < height; ++y) {
			if(b0) b0 = false; // first element in row
			else sb.append(", ");

			boolean b1 = true;
			sb.append("[");

			for(int x = 0; x < width; ++x) {
				if(b1) b1 = false;
				else sb.append(", ");

				sb.append(Colors.toColorString(argb(x, y)));
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


	static LABSurface[][] createSplines(Palette p) {
		// must be a rectangle
		int h = p.height;
		int w = p.width;

		double[][] L = new double[h][w];
		double[][] a = new double[h][w];
		double[][] b = new double[h][w];
		double[][] alpha = new double[h][w];

		for(int y = 0; y < h; ++y) {
			for(int x = 0; x < w; ++x) {
				// fixme shortcut!
				float[] lab = Colors.rgb2lab(Colors.int2rgb(p.argb(x, y)));

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
		LABSurface[][] cs = createSplines(this);
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

}