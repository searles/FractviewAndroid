package at.searles.parsing.parser;

// data signs used in the parser combinators
public class Concat<A, B> {
	public final A a;
	public final B b;

	public Concat(A a, B b) {
		this.a = a;
		this.b = b;
	}

	public String toString() {
		return a.toString() + b.toString();
	}
}
