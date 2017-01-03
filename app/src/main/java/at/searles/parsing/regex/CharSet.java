package at.searles.parsing.regex;

import java.util.Iterator;
import java.util.TreeSet;

public class CharSet implements Iterable<Range> {

	public static CharSet single(char ch) {
		CharSet charset = new CharSet();
		charset.set.add(new Range(ch));
		return charset;
	}

	public static CharSet range(Range range) {
		CharSet charset = new CharSet();
		charset.set.add(range);
		return charset;
	}

	public static CharSet all() {
		CharSet charset = new CharSet();
		charset.set.add(new Range(Character.MIN_VALUE, Character.MAX_VALUE + 1));
		return charset;
	}

	public CharSet invert() {
		CharSet charset = new CharSet();

		int start = Character.MIN_VALUE;

		for(Range r : this) {
			charset.set.add(new Range((char) start, r.start));
			start = r.end;
		}

		if(start <= Character.MAX_VALUE) {
			charset.set.add(new Range((char) start, Character.MAX_VALUE + 1));
		}

		return charset;
	}

	public void add(Range range) {
		Range lo = set.floor(range);

		// merge if necessary
		if(lo == null || lo.end < range.start) {
			// check higher
			Range hi = set.ceiling(range);

			if(hi == null || hi.start > range.end) {
				// add it.
				set.add(range);
			} else {
				// remove the higher one
				set.remove(hi);
				// and create acceptor new range and add it.
				add(new Range(range.start, Math.max(range.end, hi.end)));
			}
		} else {
			// remove the lower one
			set.remove(lo);
			// and create acceptor new range and add it.
			add(new Range(Math.min(range.start, lo.start), Math.max(range.end, lo.end)));
		}
	}

	TreeSet<Range> set = new TreeSet<>();

	@Override
	public Iterator<Range> iterator() {
		return set.iterator();
	}


	/*public void invert() {
		for(int i = 0; i < content.length; ++i) content[i] = !content[i];
	}*/

	public String toString() {
		StringBuilder sb = new StringBuilder();

		for(Range r : this) sb.append(r.toString());

		return "[" + sb.toString() + "]";
	}

	/*@Nullable
	public CharSet intersect(CharSet that) {
		CharSet intersect = new CharSet(); // only create new objects if necessary.

		for(int i = 0; i < 128; ++i) {
			// create intersection and remove commons
			intersect.content[i] = this.content[i] && that.content[i];
		}

		return intersect;
	}

	public CharSet minus(CharSet that) {
		CharSet minus = new CharSet();

		for(int i = 0; i < 128; ++i) {
			minus.content[i] = this.content[i] && !that.content[i];
		}

		return minus;
	}*/

	/*public boolean isEmpty() {
		for(int i = 0; i < 128; ++i) {
			if(content[i]) return false;
		}

		return true;
	}*/
}
