package at.searles.math;

public class InterpolationMatrix {
	// Look it up in wikipedia: hermite spline interpolation, extension to 2d
	
	public static Matrix4 create(
			double f00, double f10, double f01, double f11,
			double fx00, double fx10, double fx01, double fx11,
			double fy00, double fy10, double fy01, double fy11,
			double fxy00, double fxy10, double fxy01, double fxy11) {
		
		double alpha[] = {f00, f10, f01, f11,
				fx00, fx10, fx01, fx11, 
				fy00, fy10, fy01, fy11, 
				fxy00, fxy10, fxy01, fxy11};

		double Ainv[][] = {
				{1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{-3, 3, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{2, -2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, -2, -1, 0, 0},
				{0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 1, 1, 0, 0},
				{-3, 0, 3, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, -2, 0, -1, 0},
				{9, -9, -9, 9, 6, 3, -6, -3, 6, -6, 3, -3, 4, 2, 2, 1},
				{-6, 6, 6, -6, -3, -3, 3, 3, -4, 4, -2, 2, -2, -2, -1, -1},
				{2, 0, -2, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0},
				{0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 1, 0, 1, 0},
				{-6, 6, 6, -6, -4, -2, 4, 2, -3, 3, -3, 3, -2, -1, -2, -1},
				{4, -4, -4, 4, 2, 2, -2, -2, 2, -2, 2, -2, 1, 1, 1, 1}
		};
		
		double as[][] = new double[4][4];
		
		for(int i = 0; i < 16; ++i) {
			int y = i / 4;
			int x = i % 4;
			as[y][x] = 0.0;
			
			for(int j = 0; j < 16; ++j) {
				as[y][x] += alpha[j] * Ainv[i][j];
			}
		}

		// FIXME: I think this one is transposed?
		return new Matrix4(as);
	}
	
	public static Matrix4[][] create(double[][] zs) {
		// there are two slopes, one in y and one in x
		// (theoretically there is also one diagonally but we keep that one at 0)

		int h = zs.length;
		int w = zs[0].length;

		double mx[][] = new double[h][w];
		double my[][] = new double[h][w];
		
		// Calculate slopes
		for(int y = 1; y < h - 1; ++y) {
			for(int x = 0; x < w; ++x) {
				my[y][x] = Interpolation.Cubic.slope(zs[y-1][x], zs[y][x], zs[y+1][x]);
			}
		}

		for(int y = 0; y < h; ++y) {
			for(int x = 1; x < w - 1; ++x) {
				mx[y][x] = Interpolation.Cubic.slope(zs[y][x-1], zs[y][x], zs[y][x+1]);
			}
		}

		// first the borders
		for(int y = 0; y < h; ++y) {
			// on the left interpolate from 1-to-last to next
			mx[y][0] = Interpolation.Cubic.slope(zs[y][w - 1], zs[y][0], zs[y][w < 2 ? 0 : 1]); // leftmost
			
			// on the right (max is because w-2 is negative if w == 1.
			mx[y][w - 1] = Interpolation.Cubic.slope(zs[y][w < 2 ? 0 : (w - 2)], zs[y][w - 1], zs[y][0]); // rightmost
		}

		for(int x = 0; x < w; ++x) {
			// on top, interpolate from last row to next.
			my[0][x] = Interpolation.Cubic.slope(zs[h-1][x], zs[0][x], zs[h < 2 ? 0 : 1][x]); // top
			my[h - 1][x] = Interpolation.Cubic.slope(zs[h < 2 ? 0 : (h - 2)][x], zs[h - 1][x], zs[0][x]); // bottom
		}
		
		// Calculate surfaces
		Matrix4[][] matrices = new Matrix4[h][w];
		
		for(int y = 0; y < h - 1; ++y) {
			for(int x = 0; x < w - 1; ++x) {
				matrices[y][x] = create(
						zs[y][x], zs[y][x + 1], zs[y + 1][x], zs[y + 1][x + 1],
						mx[y][x], mx[y][x + 1], mx[y + 1][x], mx[y + 1][x + 1],
						my[y][x], my[y][x + 1], my[y + 1][x], my[y + 1][x + 1],
						0, 0, 0, 0
				);
			}
		}

		// rightmost and bottom surface are missing
		int below, right;

		below = 0;
		right = 0;
		
		for(int x = 0; x < w - 1; ++x) {
			// bottom: take below from top
			matrices[(h - 1)][x] = create(
					zs[(h - 1)][x], zs[(h - 1)][(x + 1)], zs[below][x], zs[below][(x + 1)], 
					mx[(h - 1)][x], mx[(h - 1)][(x + 1)], mx[below][x], mx[below][(x + 1)], 
					my[(h - 1)][x], my[(h - 1)][(x + 1)], my[below][x], my[below][(x + 1)], 
					0, 0, 0, 0
			);
		}

		for(int y = 0; y < h - 1; ++y) {
			matrices[y][(w - 1)] = create(
					zs[y][(w - 1)], zs[y][right], zs[(y + 1)][(w - 1)], zs[(y + 1)][right], 
					mx[y][(w - 1)], mx[y][right], mx[(y + 1)][(w - 1)], mx[(y + 1)][right], 
					my[y][(w - 1)], my[y][right], my[(y + 1)][(w - 1)], my[(y + 1)][right], 
					0, 0, 0, 0
			);
		}
			
		// corner needs special treatment
		matrices[(h - 1)][(w - 1)] = create(
				zs[(h - 1)][(w - 1)], zs[(h - 1)][right], zs[below][(w - 1)], zs[below][right], 
				mx[(h - 1)][(w - 1)], mx[(h - 1)][right], mx[below][(w - 1)], mx[below][right], 
				my[(h - 1)][(w - 1)], my[(h - 1)][right], my[below][(w - 1)], my[below][right], 
				0, 0, 0, 0
		);
		
		return matrices;
	}
}
