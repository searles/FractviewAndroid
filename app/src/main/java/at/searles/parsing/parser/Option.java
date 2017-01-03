package at.searles.parsing.parser;

public class Option<A> {

	public static <A> Option<A> some(A a) {
		return new Option<A>(a);
	}

	public static <A> Option<A> none() {
		return new Option<A>();
	}

	public final boolean isDef;
	private final A a;

	// we must maintain a position in the parsing stream.
	Option() {
		isDef = false;
		a = null;
	}

	Option(A a) {
		isDef = true;
		this.a = a;
	}

	public A get() {
		return a;
	}

	@Override
	public String toString() {
		return isDef ? a.toString() : "{}";
	}
}
