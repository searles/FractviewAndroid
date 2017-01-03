package at.searles.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * This is a simple extension of a TreeSet that adds lexical ordering.
 * @param <A>
 */
public class LexicalSet<A extends Comparable<A>> extends TreeSet<A> implements Comparable<LexicalSet<A>> {

	public static <A extends Comparable<A>> LexicalSet<A> single(A a) {
		LexicalSet<A> set = new LexicalSet<>();
		set.add(a);
		return set;
	}

	@Override
	public int compareTo(@NotNull LexicalSet<A> that) {
		// lexical order
		Iterator<A> i0 = this.iterator();
		Iterator<A> i1 = that.iterator();

		while(true) {
			if(i0.hasNext()) {
				if(i1.hasNext()) {
					A a0 = i0.next();
					A a1 = i1.next();

					int cmp = a0.compareTo(a1);

					if(cmp != 0) return cmp;
					// case cmp = 0: goto next elements.
				} else {
					// that is shorter
					return 1;
				}
			} else if(i1.hasNext()) {
				// this is shorter
				return -1;
			} else {
				// no new elements
				break;
			}
		}
		// no new elements
		return 0;
	}
}
