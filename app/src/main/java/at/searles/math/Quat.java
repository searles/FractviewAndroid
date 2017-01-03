package at.searles.math;

public class Quat {

	private double s0;
	private double s1;
	private double s2;
	private double s3;

	public double s0() { return s0; }
	public double s1() { return s1; }
	public double s2() { return s2; }
	public double s3() { return s3; }

	public Quat(double s0, double s1, double s2, double s3) {
		this.s0 = s0;
		this.s1 = s1;
		this.s2 = s2;
		this.s3 = s3;
	}

	public Quat(Quat q) {
		this(q.s0, q.s1, q.s2, q.s3);
	}

	public Quat() {
		this(0, 0, 0, 0);
	}

	public String toString() {
		return "[" + s0 + ", " + s1 + ", " + s2 + ", " + s3 + "]";
	}

	public Quat add(Quat a0, Quat a1) {
		this.s0 = a0.s0 + a1.s0;
		this.s1 = a0.s1 + a1.s1;
		this.s2 = a0.s2 + a1.s2;
		this.s3 = a0.s3 + a1.s3;
		return this;
	}

	public Quat sub(Quat a0, Quat a1) {
		this.s0 = a0.s0 - a1.s0;
		this.s1 = a0.s1 - a1.s1;
		this.s2 = a0.s2 - a1.s2;
		this.s3 = a0.s3 - a1.s3;
		return this;
	}

	public Quat mul(Quat a0, Quat a1) {
		// fixme use quad-multiplication?
		this.s0 = a0.s0 * a1.s0;
		this.s1 = a0.s1 * a1.s1;
		this.s2 = a0.s2 * a1.s2;
		this.s3 = a0.s3 * a1.s3;
		return this;
	}

	public Quat div(Quat a0, Quat a1) {
		this.s0 = a0.s0 / a1.s0;
		this.s1 = a0.s1 / a1.s1;
		this.s2 = a0.s2 / a1.s2;
		this.s3 = a0.s3 / a1.s3;
		return this;
	}

	public Quat min(Quat a0, Quat a1) {
		this.s0 = Math.min(a0.s0, a1.s0);
		this.s1 = Math.min(a0.s1, a1.s1);
		this.s2 = Math.min(a0.s2, a1.s2);
		this.s3 = Math.min(a0.s3, a1.s3);
		return this;
	}

	public Quat max(Quat a0, Quat a1) {
		this.s0 = Math.max(a0.s0, a1.s0);
		this.s1 = Math.max(a0.s1, a1.s1);
		this.s2 = Math.max(a0.s2, a1.s2);
		this.s3 = Math.max(a0.s3, a1.s3);
		return this;
	}

	public Quat mod(Quat a0, Quat a1) {
		this.s0 = a0.s0 % a1.s0;
		this.s1 = a0.s1 % a1.s1;
		this.s2 = a0.s2 % a1.s2;
		this.s3 = a0.s3 % a1.s3;
		return this;
	}

	public Quat pow(Quat a0, Quat a1) {
		this.s0 = Math.pow(a0.s0, a1.s0);
		this.s1 = Math.pow(a0.s1, a1.s1);
		this.s2 = Math.pow(a0.s2, a1.s2);
		this.s3 = Math.pow(a0.s3, a1.s3);
		return this;
	}

	public Quat neg(Quat a) {
		this.s0 = -a.s0;
		this.s1 = -a.s1;
		this.s2 = -a.s2;
		this.s3 = -a.s3;
		return this;
	}

	public Quat rec(Quat a) {
		this.s0 = 1. / a.s0;
		this.s1 = 1. / a.s1;
		this.s2 = 1. / a.s2;
		this.s3 = 1. / a.s3;
		return this;
	}

	public Quat abs(Quat a) {
		this.s0 = Math.abs(a.s0);
		this.s1 = Math.abs(a.s1);
		this.s2 = Math.abs(a.s2);
		this.s3 = Math.abs(a.s3);
		return this;
	}

	public Quat sqr(Quat a) {
		this.s0 = a.s0 * a.s0;
		this.s1 = a.s1 * a.s1;
		this.s2 = a.s2 * a.s2;
		this.s3 = a.s3 * a.s3;
		return this;
	}
}
