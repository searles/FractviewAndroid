package at.searles.math;

public class Interpolation {
	public static class Cubic {
		/** Calculates the first derivative of the cubic spline in the point y1 where the distance
		 * of all points is 1.
		 * @param zs
		 * @param zs2
		 * @param zs3
		 * @return
		 */
		public static double slope(double zs, double zs2, double zs3) {
			double d0 = zs2 - zs;
			double d1 = zs3 - zs2;
	
			double q0 = (double) Math.sqrt(d0 * d0 + 1);
			double q1 = (double) Math.sqrt(d1 * d1 + 1);
	
			return (d0 * q1 + d1 * q0) / (q0 + q1);
		}
	
		public static double y(double x, double y0, double y1, double m0, double m1) {
			double a = (2 * y0 + m0 - 2 * y1 + m1);
			double b = (-3 * y0 - 2 * m0 + 3 * y1 - m1);
			double c = m0;
			double d = y0;
	
			return ((a * x + b) * x + c) * x + d;
		}
	
		public static double yNoSlope(double x, double y0, double y1, double y2, double y3) {
			return y(x, y1, y2, slope(y0, y1, y2), slope(y1, y2, y3));
		}
	
		public static double solveLin(double a, double b) {
			return -b / a;
		}
	}
	
	public static class Linear {
		public static double x(double y, double y0, double y1) {
			return (y - y0) / (y1 - y0);
		}
	}
}
