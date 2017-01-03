package at.searles.math;

import java.util.Arrays;

public class Matrix4 {

	/* 		float f00, float f10, float f01, float f11,
			float fx00, float fx10, float fx01, float fx11,
			float fy00, float fy10, float fy01, float fy11,
			float fxy00, float fxy10, float fxy01, float fxy11
	*/

	double[][] m = new double[4][4];

	public Matrix4(double[][] m) {
		this.m = m;
	}

	public double[] dbls() {
		double[] array = new double[16];

		for(int y = 0; y < 4; ++y) {
			System.arraycopy(m[y], 0, array, y * 4, 4);
		}

		return array;
	}

	public float[] flts() {
		float[] array = new float[16];

		for(int y = 0; y < 4; ++y) {
			for(int x = 0; x < 4; ++x) {
				array[y * 4 + x] = (float) m[y][x];
			}
		}

		return array;
	}

	public String toString() {
		return Arrays.toString(m);
	}
}
