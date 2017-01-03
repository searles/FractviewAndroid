package at.searles.utils;

import java.util.Iterator;

/**
 * Represents a filter for some iterator. Only those elements are returned
 * for which a boolean function is true.
 * @param <E>
 */
public class Filter<E> implements Iterator<E> {

	final Iterator<E> i;
	final Fn<E> fn;

	E next;
	boolean hasNext;

	public Filter(Iterator<E> i, Fn<E> fn) {
		this.i = i;
		this.fn = fn;
		hasNext = advance();
	}

	boolean advance() {
		while(i.hasNext()) {
			next = i.next();
			if(fn.apply(next)) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean hasNext() {
		return hasNext;
	}

	@Override
	public E next() {
		E e = next;
		hasNext = advance();
		return e;
	}

	@Override
	public void remove() {
		i.remove();
	}

	public static interface Fn<E> {
		boolean apply(E e);
	}
}
