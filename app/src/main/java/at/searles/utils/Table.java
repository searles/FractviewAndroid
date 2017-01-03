package at.searles.utils;

import java.util.TreeMap;

public class Table<A, B> {
	// this is a map in both directions
	TreeMap<A, B> lr = new TreeMap<>();
	TreeMap<B, A> rl = new TreeMap<>();

	public void add(A a, B b) {
		if(lr.put(a, b) != null || rl.put(b, a) != null) throw new IllegalArgumentException();
	}

	public boolean containsL(A a) {
		return lr.containsKey(a);
	}

	public boolean containsB(B b) {
		return rl.containsKey(b);
	}

	public B r(A a) {
		return lr.get(a);
	}

	public A l(B b) {
		return rl.get(b);
	}
}
