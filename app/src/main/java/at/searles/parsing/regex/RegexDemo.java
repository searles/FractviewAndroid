package at.searles.parsing.regex;

public class RegexDemo {
	public static void main(String...args) {
		Lexer l = new Lexer();

		l.match("[a-z]+");
		l.tok("me\\r");

		System.out.println(l.fsa);
	}
}
