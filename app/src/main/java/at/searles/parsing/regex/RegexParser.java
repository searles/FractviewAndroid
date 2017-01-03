package at.searles.parsing.regex;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import at.searles.utils.Counter;
import at.searles.utils.LexicalSet;
import at.searles.utils.Table;

/**
 * RegexParser for regular expressions
 */
class RegexParser {

	Counter counter = new Counter();

	// fixme Table can be replaced by TreeBidiMap in Apache Commons
	// fixme but what is a replacement for LexicalSet?
	Table<FSA.Node, LexicalSet<FSA.Node>> dfaTable = new Table<>();

	FSA parse(@NotNull String regex, @NotNull Acceptor acceptor) {
		ParserInstance pi = new ParserInstance(regex, acceptor);
		FSA fsa = pi.regex();

		if(!pi.s.isEmpty()) throw new IllegalArgumentException(pi.s + " was not parsed");

		return fsa;
	}

	class ParserInstance {
		String s;
		Acceptor acceptor;

		ParserInstance(@NotNull String regex, @NotNull Acceptor acceptor) {
			this.s = regex;
			this.acceptor = acceptor;
		}

		@NotNull
		FSA regex() {
			FSA fsa = seq();
			while (s.startsWith("|")) {
				s = s.substring(1).trim(); // cut away leading |
				FSA fsa1 = seq();
				fsa.or(fsa1); // combine these two
			}
			return fsa;
		}

		@NotNull
		FSA seq() {
			FSA fsa = rep(); // may return null

			if (fsa == null) throw new IllegalArgumentException("bad regex");

			for (FSA fsa1 = rep(); fsa1 != null; fsa1 = rep()) {
				fsa.concat(fsa1);
			}

			return fsa;
		}

		@Nullable
		FSA rep() {
			FSA fsa = token();

			if (fsa == null) return null; // nothing

			if (!s.isEmpty()) {
				switch (s.charAt(0)) {
					case '*':
						s = s.substring(1).trim();
						fsa.rep(true);
						break;
					case '+':
						s = s.substring(1).trim();
						fsa.rep(false);
						break;
					case '?':
						s = s.substring(1).trim();
						fsa.opt();
						break;
				}
			}

			return fsa;
		}

		@Nullable
		FSA token() {
			if (s.startsWith("(")) {
				s = s.substring(1).trim();
				FSA fsa = regex();

				if (!s.startsWith(")")) throw new IllegalArgumentException("missing )");

				s = s.substring(1).trim();
				return fsa;
			} else if (s.isEmpty() || s.startsWith("(") || s.startsWith(")") ||
					s.startsWith("*") || s.startsWith("+") || s.startsWith("?") ||
					s.startsWith("|")) {
				return null; // no match
			} else if (s.startsWith("[")) {
				s = s.substring(1);
				CharSet set = charsets();
				if (!s.startsWith("]")) throw new IllegalArgumentException("missing ]");
				s = s.substring(1);
				return new FSA(counter, dfaTable, acceptor, set);
			} else if (s.startsWith(".")) {
				s = s.substring(1);
				return new FSA(counter, dfaTable, acceptor, CharSet.all());
			} else {
				// some character.
				CharSet set = CharSet.single(character());
				s = s.trim(); // in character() I do not trim because spaces may be useful.
				return new FSA(counter, dfaTable, acceptor, set); // creates acceptor start node
			}
		}

		@NotNull
		CharSet charsets() {
			// following grammar:
			// ^?(CHAR ('-' CHAR)?)+
			// where CHAR is acceptor concrete character. \n is newline, \\uXXXX is unicode etc...
			boolean invert = false;

			if (s.startsWith("^")) {
				invert = true;
				s = s.substring(1).trim();
			}

			Range r = range();
			CharSet set = CharSet.range(r);

			while (!s.startsWith("]")) {
				set.add(range());
			}

			return invert ? set.invert() : set;
		}

		@NotNull
		Range range() {
			char start = character();

			int end;

			if (s.charAt(0) == '-') {
				s = s.substring(1);
				end = character() + 1;
			} else {
				end = start + 1;
			}

			return new Range(start, end);
		}

		char character() {
			char ch = s.charAt(0);
			s = s.substring(1);
			if(ch == '\\') {
				return escapedChar();
			} else {
				return (char) ch;
			}
		}

		char escapedChar() {
			char ch = s.charAt(0);
			s = s.substring(1);

			if(ch == 'u') {
				// it is a unicode
				String hex = s.substring(0, 4);
				s = s.substring(4);

				return (char) Integer.parseInt(hex, 16);
			} else {
				switch(ch) {
					case 'n': return '\n';
					case 'r': return '\r';
					case 't': return '\t';
					case '\\': return '\\';
					// fixme others?
					default: return ch; // includes ], -, ...
				}
			}
		}
	}
}
