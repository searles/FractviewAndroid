package at.searles.math;

/**
 * Created by searles on 17.06.16.
 */
public class Int {
	private int value;

	public Int(int value) {
		this.value = value;
	}

	public int value() {
		return value;
	}

	@Override
	public String toString() {
		return Integer.toString(value);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Int)) return false;

		Int anInt = (Int) o;

		return value == anInt.value;

	}

	@Override
	public int hashCode() {
		return value;
	}
}
