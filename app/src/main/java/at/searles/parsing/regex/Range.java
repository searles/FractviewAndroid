package at.searles.parsing.regex;

import org.jetbrains.annotations.NotNull;

public class Range implements Comparable<Range> {
	int start;
	int end;

	public Range(int ch) {
		this.start = ch;
		this.end = (ch + 1);
	}

	public Range(int start, int end) {
		this.start = start;
		this.end = end;
	}

	@Override
	public int compareTo(@NotNull Range that) {
		int cmp = Integer.compare(this.start, that.start);

		return cmp != 0 ? cmp : Integer.compare(this.end, that.end);
	}

	public String toString() {
		if(start == end - 1) return Character.toString((char) start);
		else return ((char) start) + "-" + ((char) (end - 1));
	}
}