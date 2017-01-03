package at.searles.parsing.parser;

public class Wrapper<T> {
	// wrappers can be used to pass a value on in the parser chain.
	public T t;

	public T set(T t) {
		this.t = t;
		return t;
	}

	public T get() {
		return t;
	}
}
