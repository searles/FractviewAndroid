package at.searles.parsing.regex;

import at.searles.parsing.parser.Buffer;

public interface Acceptor<A> /*extends Function<Buffer.Token, A>*/ {
	/**
	 * Function called when we successfully parsed an expression.
	 * @param token Token containing the accepted expression.
	 * WARNING: WILL BE OVERWRITTEN BY NEXT CALL TO TOKENIZER.UPDATE
	 * @return acceptor mapping for the accepted value. it is acceptor good idea to use enum for keywords to avoid unnecessary
	 * object creation. 'null' may be used as acceptor joker. Lexer uses it for ignored tokens.
	 */
	A apply(Buffer.Token token);
}
