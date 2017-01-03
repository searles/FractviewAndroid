package at.searles.parsing.parser;

import at.searles.parsing.ParsingError;

/**
 * This class represents parser rules, similar to Scala. It reads tokenizer
 * of type S and returns items of type D. If S and D implement
 * the TokenPosition-interface, the positions of the returned elements
 * will be set accordingly.
 * @param <D>
 */
public abstract class Parser<D> {

	public static interface Fn<A, B> {
		B apply(A a);
	}
	
	/**
	 * Parse buf and return some D
	 * @param buf a buffer from which things are read
	 * @return value accepted by the parser.
	 */
	public abstract D parse(Buffer buf);



	/**
	 * An adapter to convert the return value of a parser to a different type. If the result type extends
	 * the TokenPosition.Annotated-Interface, then the corresponding setTokenPosition-method will be called.
	 * @param fn The Fn to map the input type to the output type
	 * @param <T> The output type
	 * @return the mapped result.
	 */
	public <T> Parser<T> adapter(final Fn<D, T> fn) {
		final Parser<D> parent = Parser.this;

		return new Parser<T>() {
			@Override
			public T parse(Buffer buf) {
				D val = parent.parse(buf);

				if(val == null) return null; // could not parse.

				return fn.apply(val);
			}

			public String toString() {
				return parent.toString();
			}
		};
	}

	/**
	 * Since parsers can call themselves, we need to use parsers before we defined their rule.
	 * (example: expr = term '+' expr). Hence, we provide this one to use a parser before it is defined.
	 * It can be simply put into this wrapper later.
	 * @param <D>
	 */
	public static class ParserReference<D> extends Parser<D> {

		Parser<D> wrapped = null;

		/**
		 * Sets this parser to call the parser in the argument
		 * @param parser Parser to be wrapped
		 * @return the wrapped parser in the argument.
		 */
		public Parser<D> set(Parser<D> parser) {
			this.wrapped = parser;
			return parser;
		}

		@Override
		public D parse(Buffer buf) {
			// it should be initialized already.
			return wrapped.parse(buf);
		}

		@Override
		public String toString() {
			return "..."; // wrapped.toString();
		}

		public Parser<D> get() {
			return wrapped;
		}
	}

	/**
	 * Simple concatenation of this and tail. Returns a pair of the return type of this and tail if successfully parsed.
	 * If this was successful but tail wasn't a parser exception is thrown (unless this is an empty option or repetition).
	 * @param tail following parser
	 * @param <E> Type of the tail parser
	 * @return a concatenation of the parsers
	 */
	public <E> Parser<Concat<D, E>> then(final Parser<E> tail) {
		final Parser<D> parent = this;

		return new Parser<Concat<D, E>>() {
			@Override
			public Concat<D, E> parse(Buffer buf) {
				int start = buf.start();
				D ret0 = parent.parse(buf);

				if(ret0 == null) return null; // this parser failed.

				int mid = buf.start(); // get position in buffer.

				E ret1 = tail.parse(buf);

				if (ret1 == null) {
					if(start == mid) return null; // if first one did not consume anything, it is fine.

					// Things like -? expr are not allowed!
					throw new ParsingError(tail + " expected!", start, buf);
				}

				return new Concat<D, E>(ret0, ret1);
			}

			@Override
			public String toString() {
				return parent.toString() + tail.toString();
			}
		};
	}


	/**
	 * Short for a mapped then that ignores the right return value
	 * @param tail
	 * @param <E>
	 * @return
	 */
	public <E> Parser<D> thenLeft(final Parser<E> tail) {
		return then(tail).adapter((c) -> c.a);
	}

	/**
	 * Short for a mapping of then that ignores the left value
	 * @param tail
	 * @param <E>
	 * @return
	 */
	public <E> Parser<E> thenRight(final Parser<E> tail) {
		return then(tail).adapter((c) -> c.b);
	}

	/**
	 * This parser pastes the content of the first parser into the Fn in the parameter
	 * to get another parser whose value is then returned.
	 * @return
	 */
	public <E> Parser<E> into(Fn<D, Parser<E>> tailFn) {
		final Parser<D> parent = Parser.this;

		return new Parser<E>() {
			@Override
			public E parse(Buffer buf) {
				int start = buf.start();

				D d = parent.parse(buf);

				int mid = buf.start();

				if(d != null) {
					Parser<E> tail = tailFn.apply(d);
					E e = tail.parse(buf);

					if (e == null) {
						if(start == mid) return null; // nothing was consumed, thus it is fine.

						// otherwise error because second one did not match
						throw new ParsingError(tail + " expected!", start, buf);
					}

					return e;
				} else {
					return null;
				}
			}
		};
	}

	/**
	 * Returns a parser for options
	 * @return The parser.
	 */
	public Parser<Option<D>> opt() {
		final Parser<D> parent = Parser.this;

		return new Parser<Option<D>>() {
			@Override
			public Option<D> parse(Buffer buf) {
				D ret = parent.parse(buf);

				return ret == null ? Option.<D>none() : Option.some(ret);
			}

			public String toString() {
				return parent + "?";
			}
		};
	}

	/**
	 * Returns a parser for repetitions of elements.
	 * @param mayBeEmpty if true, then also empty repetitions are allowed.
	 * @return The parser
	 */
	public Parser<Rep<D>> rep(final boolean mayBeEmpty) {
		final Parser<D> parent = Parser.this;

		return new Parser<Rep<D>>() {
			@Override
			public Rep<D> parse(Buffer buf) {
				Rep<D> ret = new Rep<D>();

				for(D d = parent.parse(buf); d != null; d = parent.parse(buf)) {
					ret.add(d);
				}

				if(mayBeEmpty || !ret.isEmpty()) {
					return ret;
				} else {
					return null;
				}
			}


			public String toString() {
				return parent + "*";
			}
		};
	}

	@SafeVarargs
	public final Parser<D> or(final Parser<? extends D>... alt) {
		if(alt.length == 0) return this;

		final Parser<D> parent = Parser.this;

		return new Parser<D>() {
			@Override
			public D parse(Buffer buf) {
				D ret = parent.parse(buf);

				// no need to update the position. It has been done before.
				if(ret == null) {
					for (Parser<? extends D> anAlt : alt) {
						D d = anAlt.parse(buf);
						if (d != null) return d;
					}

					return null;
				} else {
					return ret;
				}
			}

			public String toString() {
				String s = "(" + parent;

				for(Parser<? extends D> p : alt) {
					s += " | " + p;
				}

				return s + ")";
			}
		};
	}
}