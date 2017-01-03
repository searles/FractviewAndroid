package at.searles.parsing;

import at.searles.parsing.parser.Buffer;

public class ParsingError extends RuntimeException {

	int pos;
	Buffer src;

	/**
	 * @param msg
	 * @param pos
	 * @param src
	 */
	public ParsingError(String msg, int pos, Buffer src) {
		super(msg);
		this.pos = pos;
		this.src = src;
	}

	public int pos() {
		return pos;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " at " + pos + " but " + src.seq() + " was found";
	}
}
