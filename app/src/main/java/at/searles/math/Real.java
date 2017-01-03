package at.searles.math;

/**
 * Created by searles on 17.06.16.
 */
public class Real {
	private double value;

	public Real(double value) {
		this.value = value;
	}

	public double value() {
		return this.value;
	}

	@Override
	public String toString() {
		return Double.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Real real = (Real) o;

		return Double.compare(real.value, value) == 0;

	}

	@Override
	public int hashCode() {
		long temp = Double.doubleToLongBits(value);
		return (int) (temp ^ (temp >>> 32));
	}
}
