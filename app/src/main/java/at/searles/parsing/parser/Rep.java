package at.searles.parsing.parser;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

/**
 * This is a repetition
 */
public class Rep<A> implements Iterable<A> {

	LinkedList<A> elements;

	public static <A> Rep<A> rep(A first, Rep<A> tail) {
		Rep<A> rep = new Rep<A>();
		rep.add(first);
		for(A a : tail) rep.add(a);
		return rep;
	}

	public Rep() {
		this.elements = new LinkedList<A>();
	}

	public void add(A a) {
		this.elements.add(a);
	}

	@Override
	public Iterator<A> iterator() {
		return elements.iterator();
	}

	public Iterator<A> revIterator() {
		return new Iterator<A>() {
			ListIterator<A> i = elements.listIterator(elements.size());

			@Override
			public boolean hasNext() {
				return i.hasPrevious();
			}

			@Override
			public A next() {
				return i.previous();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public boolean isEmpty() {
		return elements.isEmpty();
	}

	public int size() {
		return elements.size();
	}

	public List<A> asList() {
		return elements;
	}

	public String toString() {
		return elements.toString();
	}
}
