package at.searles.utils;

public class Pair<A, B> implements Comparable<Pair<A, B>> {
	public final A a;
	public final B b;

	public Pair(A a, B b) {
		if(a == null || b == null) throw new NullPointerException();

		this.a = a;
		this.b = b;
	}

	public String toString() {
		return "(" + a + ", " + b + ")";
	}

	@Override
	public int compareTo(Pair<A, B> that) {
		if(that == null) throw new NullPointerException();

		// use this only if A and B are comparable!
		int cmp = ((Comparable<A>) this.a).compareTo(that.a);
		return cmp != 0 ? cmp : ((Comparable<B>) this.b).compareTo(that.b);
	}
}
