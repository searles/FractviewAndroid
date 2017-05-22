package at.searles.parsing.parser;

import java.io.IOException;
import java.io.Reader;

import at.searles.parsing.regex.Acceptor;
import at.searles.parsing.regex.Lexer;

// FIXME allow multiple acceptors in same node.

/**
 * This is also an extension of charsequence to use it nicely in Acceptor.
 */
public abstract class Buffer {

	public static final Acceptor<Void> INVALID_TOKEN = new Acceptor<Void>() {
		@Override
		public Void apply(Token tok) {
			throw new IllegalArgumentException("must not call apply without checking!");
		}
	};

	public static final Acceptor<Void> END_OF_BUF = new Acceptor<Void>() {
		@Override
		public Void apply(Token tok) {
			throw new IllegalArgumentException("must not call apply without checking!");
		}
	};

	public static final class Token {
		public final Buffer src;

		public int start;
		public int end;
		public Acceptor<?> acceptor;
		Lexer lexer;

		Token(Buffer src) {
			this.src = src;
			start = 0;
			end = 0;
			acceptor = null;
		}

		public CharSequence seq() {
			return src.seq();
		}
	}

	/**
	 * The current token. If the token should be somehow preserved,
	 * copy this one because it will be modified for the next token.
	 */
	// FIXME instead of setting a fixed token, I should rather store
	// FIXME which token a certain thing is...
	Token tok; // FIXME Problems with seq because of subclasses.

	private boolean advanceToNextToken = true; // if invalid, then call to tok will advance to next token

	/**
	 * Forces the Buffer to advance to the next token on the next
	 * call to tok(...). This should be called after some parsing rule
	 * accepted the token.
	 */
	public void advanceToNextToken() {
		advanceToNextToken = true;
	}

	public Token tok(Lexer lexer) {
		if(advanceToNextToken) {
			// go to next token
			advance();
		}

		if(tok.lexer != lexer) {
			// different lexer, hence start from 0.
			setToTokenStart();
			advanceToNextToken = true;
		}

		if(advanceToNextToken) {
			tok.acceptor = lexer.accept(this); // sets
			tok.lexer = lexer;
			tok.start = start();
			tok.end = marked() + tok.start;

			advanceToNextToken = false;
		}

		return tok;
	}


	/**
	 * Marks the end of the string represented by the underlying CharSequence
	 */
	public abstract void mark();

	/**
	 * @return the next character from the buffer. -1 if nothing to read.
	 * @throws ReadException Exception is thrown if element could not be read
	 */
	public abstract int next() throws ReadException;
	/*public int pos() {
		return pos;
	}*/

	/**
	 * Removes the range 0..mark from char-buffer and sets mark to start. This is the range that is set
	 * by FSA.accept.
	 */
	public abstract void advance();

	/**
	 * resets position so that it would be read again
	 */
	public abstract void setToTokenStart();

	public abstract CharSequence seq();

	public abstract int start(); // start position

	public abstract int marked(); // position of marked

	public abstract boolean isEmpty(); // true if we reached the end of it

	/** Wrapping IOExceptions to be able to use this.
	 *
	 */
	public static class ReadException extends RuntimeException {
		public ReadException(IOException ioException) {
			super(ioException);
		}
	}

	public static class ReaderAdapter extends Buffer {
		/**
		 * Buffer.
		 */
		char[] buf;

		/**
		 * Number of valid characters in buffer. The pre-read characters are buf[0]..buf[buflen - 1]
		 */
		int len;

		/**
		 * the index of the first character in buffer in reader. Very useful for eg syntax highlighting.
		 */
		int start = 0;

		/**
		 * last returned position in buffer. always less or equal to buflen.
		 * Index of last returned character in stream is start + pos.
		 */
		int pos;

		/**
		 * mark in buffer, always less or equal pos.
		 */
		int mark = -1;

		/**
		 * underlying reader
		 */
		Reader reader;

		/**
		 * true if the reader is done. (Not necessarily the buffer because it may contain characters)
		 */
		boolean eof = false;

		final CharSequence seq = new CharSequence() {
			@Override
			public int length() {
				return mark;
			}

			@Override
			public char charAt(int i) {
				return buf[i];
			}

			@Override
			public CharSequence subSequence(int start, int end) {
				return toString().substring(start, end);
			}

			@Override
			public String toString() {
				return String.copyValueOf(buf, 0, mark);
			}
		};


		/**
		 * Creates this class that has pretty much the same purpose as bufferedreader, but the buffer'string size
		 * can be adjusted.
		 * @param reader Reader from which characters are read.
		 * @param minCapacity Size that is used to initialize the buffer. Will be extended.
		 */
		public ReaderAdapter(Reader reader, int minCapacity) {
			this.buf = new char[minCapacity];
			this.reader = reader;
			this.start = 0;
			this.pos = 0;
			this.len = 0;
			this.mark = 0;

			this.tok = new Token(this);
		}

		public ReaderAdapter(Reader reader) {
			this(reader, 64);
		}

		public void setToTokenStart() {
			// kind-of reset.
			pos = 0;
			mark = -1;
		}

		public void mark() {
			// mark current position
			this.mark = pos;
		}

		public int next() throws ReadException {
			if(pos == buf.length) {
				// array is too small, reallocate
				char[] newbuf = new char[buf.length * 2];
				System.arraycopy(buf, 0, newbuf, 0, buf.length);
				this.buf = newbuf;
			}

			if(pos < len) {
				// we take a char from the buffer
				return buf[pos++];
			} else {
				// pos == buflen
				int ch;

				try {
					ch = reader.read();
				} catch (IOException e) {
					throw new ReadException(e);
				}

				// put it into buf unless it is empty
				if(ch == -1) {
					eof = true;
					return -1;
				} else {
					buf[len++] = (char) ch;
					pos++;
					return ch;
				}
			}
		}

		public void advance() {
			if(mark < 0) throw new IllegalArgumentException("mark must be >= 0");

			if(mark > 0) {
				// shift buf from mark..pos to 0..pos-mark.
				System.arraycopy(buf, mark, buf, 0, pos - mark);
				start += mark; // next start-position
				len -= mark;
			}

			// and reset.
			setToTokenStart();
		}

		public int start() {
			return start;
		}

		public CharSequence seq() {
			return seq;
		}

		public int marked() {
			return start + mark;
		}

		public boolean isEmpty() {
			return eof && len == 0;
		}
	}

	public static class StringAdapter extends Buffer {

		public final CharSequence string;

		int offset = 0;
		int toklen = 0;
		int readlen = 0;

		public StringAdapter(CharSequence s) {
			this.string = s;
			this.tok = new Token(this);
		}

		/**
		 * Returns the remaining part of the Buffer
		 * @return
		 */
		public CharSequence tail() {
			return string.subSequence(offset, string.length());
		}

		@Override
		public void setToTokenStart() {
			toklen = readlen = 0;
		}

		@Override
		public void mark() {
			toklen = readlen;
		}

		@Override
		public int next() throws ReadException {
			int index = offset + readlen;

			if(index >= string.length()) {
				return -1;
			} else {
				readlen++;
				return string.charAt(index);
			}
		}

		@Override
		public void advance() {
			offset += toklen;
			setToTokenStart();
		}

		@Override
		public CharSequence seq() {
			return string.subSequence(offset, offset + toklen);
		}

		@Override
		public int start() {
			return offset;
		}

		@Override
		public int marked() {
			return toklen;
		}

		@Override
		public boolean isEmpty() {
			return offset >= string.length();
		}

		public String toString() {
			// FIXME The compiler will be replaced,
			// FIXME so a dirty fix for this is not a big deal.
			if(1==1/*advanceToNextToken*/) {
				return string.subSequence(0, offset + marked()) + "___" +
						string.subSequence(offset, string.length());
			} else {
				return string.subSequence(0, offset) + "___"
						+ string.subSequence(offset, offset + marked())
						+ "___" + string.subSequence(offset + marked(), string.length());
			}
		}
	}
}
