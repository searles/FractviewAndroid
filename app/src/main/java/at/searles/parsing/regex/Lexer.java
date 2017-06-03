package at.searles.parsing.regex;

import org.jetbrains.annotations.NotNull;

import java.util.TreeMap;

import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Parser;

/**
 * Pretty much a more useable frontend for FSA
 */
public class Lexer {
	private final Acceptor<Void> HIDDEN = new Acceptor<Void>() {
		@Override
		public Void apply(Buffer.Token tok) {
			throw new UnsupportedOperationException();
		}

		public String toString() {
			return "HIDDEN";
		}
	};

	/**
	 * RegexParser for regular expressions to add lexems
	 */
	private final RegexParser regexParser = new RegexParser();

	/**
	 * fsa that accepts our current language. Empty language is not allowed,
	 * add must be called at least once.
	 * FIXME maybe it is better to create a minimal FSA with only one start state?
	 */
	FSA fsa = null;

	/**
	 * Adds acceptor regular expression which should be accepted but for which there is no acceptor.
	 * Typical example are comments or white characters.
	 * @param regex regular expression that should be ignored
	 */
	public void addIgnore(String regex) {
		FSA newFSA = regexParser.parse(regex, HIDDEN);
		if(fsa == null) fsa = newFSA;
		else {
			newFSA.or(fsa); // always use new fsa on lhs, otherwise we tokenPosition the wrong acceptor!
			fsa = newFSA;
			fsa.removeUnusedNodes();
		}
		//System.out.println(fsa.start.fullString());
	}

	/** Adds acceptor regular expression
	 * @param regex The regular expression
	 * @param acceptor acceptor to be called if this regular expression was accepted. If another
	 *                 acceptor also accepts this regular expression, then the LATTER one will be used.
	 *                 Hence, if there are ids for names and keywords, then FIRST ids. Same for integer and real-values.
	 * @throws IllegalArgumentException if the regular expression was invalid.
	 */
	private Acceptor<?> add(String regex, Acceptor<?> acceptor) {
		FSA newFSA = regexParser.parse(regex, acceptor);
		if(fsa == null) fsa = newFSA;
		else {
			newFSA.or(fsa);
			fsa = newFSA;
			fsa.removeUnusedNodes();
		}

		return acceptor;
	}

	/**
	 * This member is used to store regular expressions that were added through the last
	 * item.
	 */
	private final TreeMap<String, Acceptor<String>> directTokens = new TreeMap<>();

	/** Returns an acceptor that was added using "tok" before
	 * @param regex The regular expression that was added. Must be a 1:1 equivalence!
	 * @return Acceptor for the regex, or new acceptor for it.
	 */
	private Acceptor<String> cachedToken(final String regex) {
		Acceptor<String> a;

		if(directTokens.containsKey(regex)) {
			return directTokens.get(regex);
		} else {
			a = new Acceptor<String>() {
				@Override
				public String apply(Buffer.Token tok) {
					return tok.seq().toString();
				}

				public String toString() {
					return regex;
				}
			};

			add(regex, a);
			directTokens.put(regex, a);

			return a;
		}
	}

	/**
	 * This one is useful for single-character-regexes or keywords because no string object is created.
	 * @param regex regular expression to be parsed.
	 * @return A regexParser that checks whether the next token matches the regex and then returns the regex (!)
	 */
	public Parser<String> tok(final String regex) {
		final Acceptor<String> acceptor = cachedToken(regex);

		return new Parser<String>() {
			@Override
			public String parse(Buffer buf) {
				Buffer.Token tok = buf.tok(Lexer.this);
				if(tok.acceptor == acceptor) {
					// we found something
					buf.advanceToNextToken();
					return regex;
				} else {
					return null;
				}
			}

			@Override
			public String toString() {
				return regex;
			}
		};
	}

	/**
	 * Similar to tok, but this one returns the match itself. Useful for simple regular expressions
	 * with some alternatives like "/|\*|%"
	 * @param regex regular expression to be parsed.
	 * @return a regexParser that in case of a match returns the matched string.
	 */
	public Parser<String> match(final String regex) {
		final Acceptor<String> acceptor = cachedToken(regex);

		return new Parser<String>() {
			@Override
			public String parse(Buffer buf) {
				Buffer.Token tok = buf.tok(Lexer.this);
				if(tok.acceptor == acceptor) {
					String ret = tok.seq().toString();
					// we found something
					buf.advanceToNextToken();
					return ret;
				} else {
					return null;
				}
			}

			@Override
			public String toString() {
				return regex;
			}
		};
	}

	/**
	 * Creates a new regexParser that accepts this token type.
	 * == is used for the comparison to avoid problems with lambda expressions. Careful, do not call
	 * this multiple times on the same regex since it is always added to the automaton.
	 * @param regex Regular expression for this acceptor
	 * @param fn Acceptor that should be accepted
	 * @return null if not matched, otherwise the return value of the acceptor (which also may be null!).
	 */
	public <A> Parser<A> match(final String regex, @NotNull final Acceptor<A> fn) {

		add(regex, fn);

		return new Parser<A>() {
			@Override
			public A parse(Buffer buf) {
				Buffer.Token tok = buf.tok(Lexer.this);
				if(tok.acceptor == fn) {
					A a = fn.apply(tok);
					if(a != null) {
						buf.advanceToNextToken();
						return a;
					} else {
						return null;
					}
				} else {
					return null;
				}
			}

			@Override
			public String toString() {
				return regex;
			}
		};
	}

	public Acceptor<?> accept(Buffer buf) {
		// ignore comments, spaces etc...
		Acceptor<?> acceptor = fsa.accept(buf);

		while(acceptor == HIDDEN) {
			buf.advance();
			acceptor = fsa.accept(buf);
		}

		// observe that acceptor might be null
		return acceptor;
	}
}
