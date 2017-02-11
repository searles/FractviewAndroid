package at.searles.meelan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import at.searles.parsing.ParsingError;
import at.searles.parsing.parser.Buffer;
import at.searles.parsing.parser.Concat;
import at.searles.parsing.parser.Parser;
import at.searles.parsing.parser.Rep;
import at.searles.parsing.regex.Acceptor;
import at.searles.parsing.regex.Lexer;

/**
 * Parser for the syntax of meelan.
 */
class Syntax {

	private static final Acceptor<Double> realFn = new Acceptor<Double>() {
		@Override
		public Double apply(Buffer.Token tok) {
			return Double.parseDouble(tok.seq().toString());
		}

		public String toString() {
			return "real";
		}
	};

	private static final Acceptor<Integer> integerFn = new Acceptor<Integer>() {
		@Override
		public Integer apply(Buffer.Token tok) {
			return Integer.parseInt(tok.seq().toString());
		}

		public String toString() {
			return "int";
		}
	};

	private static final Acceptor<String> idFn = new Acceptor<String>() {
		@Override
		public String apply(Buffer.Token tok) {
			return tok.seq().toString();
		}

		public String toString() {
			return "id";
		}
	};

	private static final Acceptor<Integer> hexintFn = new Acceptor<Integer>() {
		@Override
		public Integer apply(Buffer.Token tok) {
			// we need long here because numbers like ffff0000 throw an exception because they are too big
			String hexstr = tok.seq().toString().substring(1);

			int alpha, r, g, b;

			switch(hexstr.length()) {
				case 3:
					alpha = 0xff;
					r = Integer.parseInt(hexstr.substring(0, 1), 16) * 0x11;
					g = Integer.parseInt(hexstr.substring(1, 2), 16) * 0x11;
					b = Integer.parseInt(hexstr.substring(2, 3), 16) * 0x11;
					break;
				case 4:
					alpha = Integer.parseInt(hexstr.substring(0, 1), 16) * 0x11;
					r = Integer.parseInt(hexstr.substring(1, 2), 16) * 0x11;
					g = Integer.parseInt(hexstr.substring(2, 3), 16) * 0x11;
					b = Integer.parseInt(hexstr.substring(3, 4), 16) * 0x11;
					break;
				case 6:
					alpha = 0xff;
					r = Integer.parseInt(hexstr.substring(0, 2), 16);
					g = Integer.parseInt(hexstr.substring(2, 4), 16);
					b = Integer.parseInt(hexstr.substring(4, 6), 16);

					break;
				case 8:
					alpha = Integer.parseInt(hexstr.substring(0, 2), 16);
					r = Integer.parseInt(hexstr.substring(2, 4), 16);
					g = Integer.parseInt(hexstr.substring(4, 6), 16);
					b = Integer.parseInt(hexstr.substring(6, 8), 16);

					break;
				default:
					throw new ParsingError("bad format", tok.start, tok.src);
			}

			// this is used for colors, so the following are allowed:
			// #rgb
			// #argb
			// #rrggbb   ==> 0xffrrggbb
			// #aarrggbb ==> 0xaarrggbb

			return alpha << 24 | r << 16 | g << 8 | b;
		}

		@Override
		public String toString() {
			return "hexint";
		}
	};

	private static final Acceptor<String> stringFn = tok -> {
        // This one is pretty much "raw string", except for escaping quotes.
        StringBuilder sb = new StringBuilder(tok.seq().length() - 2); // remove front " and last "

        boolean esc = false;

        for(int i = 1; i < tok.seq().length() - 1; ++i) {
            // pos 0 is " and so is str[str.length - 1]
            if(esc) {
                // next char is the one
                // FIXME If I every want \n \r or \t, this is the place to go.
                sb.append(tok.seq().charAt(i));
                esc = false;
            } else {
                char ch = tok.seq().charAt(i);
                if(ch == '\\') {
                    esc = true;
                } else {
                    sb.append(ch);
                }
            }
        }

        return sb.toString();
    };

	private final static Lexer lexer = new Lexer();

	static {
		// earlier tokens will be parsed first.
		lexer.addIgnore("//[^\\n]*");
		// all kinds of spaces. a0 is mean actually, because it
		// always occurs in HTML
		lexer.addIgnore("[ \\n\\r\\t\\u00a0]+");
	}

	// some very basic things
	private static final Parser<String> id = lexer.match("[A-Za-z_][0-9A-Za-z_]*", idFn); // this one must be added before keywords!
	private static final Parser<Double> real = lexer.match("[0-9]+(\\.[0-9]*)?([eE]-?[0-9]+)?", realFn);
	private static final Parser<Integer> integer = lexer.match("[0-9]+", integerFn); //  the later one overrules the previous one.
	private static final Parser<Integer> hexint = lexer.match("#[0-9a-fA-F]+", hexintFn);

	// regex: "([^\"](\[\"])?)*". In RegexParser, \ is escaped as \\.
	// this gives "([^\\"](\\[\\"])?)*"
	// and since this is a Java string we also need to escape all " and \. Uff...
	public static final Parser<String> str = lexer.match("\"([^\\\\\"]*(\\\\[\\\\\"])?)*\"", stringFn); // Strings are in the most simple format: \" is " and \\ is \, everything else is normal.
	public static final Parser<Boolean> bool =
			lexer.tok("true").adapter(s -> true)
			.or(lexer.tok("false").adapter(s -> false));

	// Expressions and Statements
	public static final Parser.ParserReference<Tree> expr = new Parser.ParserReference<>();
	public static final Parser.ParserReference<Tree> stmt = new Parser.ParserReference<>();

	// A program is a sequence of statements
	static final Parser<Tree> program = stmt.thenLeft(lexer.tok(";").opt()).rep(true).adapter(
			stmts -> Tree.createBlock(stmts.asList()));

		/*
	Parser:

	need: expr, stmt.

	primary = id | int | real | hex | bool | [ expr (, expr)* ] | ( expr )
	postfix = primary (. ident)*
	parameters = ( <expr> (, <expr>)* ) | <app>
	app = <postfix> <parameters>?
	term = { (stmt ;?)* } | <app> // this one is separated from app because {..} cannot be followed by arguments without a separator.
	unexpr = [-/] <unexpr> | <term>
	ifexpr = <unexpr> if <expr> else <unexpr>
	consexpr = ifexpr (: ifexpr)*
	powexpr = consexpr (^ consexpr)*
	mulexpr = powexpr ( [*%/] powexpr)*
	sumexpr = mulexpr ( [+-] mulexpr)*
	compexpr = sumexpr ((<>==) sumexpr)?
	literal = not <literal> | <compexpr>
	orexpr = literal (or literal)*
	andexpr = orexpr (and orexpr)*
	assignexpr = andexpr (= assignexpr)?
	expr = assignexpr

	ifstmt = if <expr> then <stmt> (else <stmt>)?
	whilestmt = while <expr> (do <stmt>)?
	defstmt = def <id> = <expr>
	externstmt =  extern <id> <id> = <expr>
	varstmt = var ( <id> <id>? (= <expr>)? )+
	arguments = <id> | ( (<id> (, <id>)*)? )?
	funcstmt = func <id> <arguments> <stmt> // eg func sin x {bla}

	stmt = ifstmt | whilestmt | defstmt | externstmt | varstmt | arguments | funcstmt | expr
	 */

	private static final Parser<Tree> block = lexer.tok("{").thenRight(program).thenLeft(lexer.tok("}")).adapter(
			Tree.Scope::new
	);

	private static final Parser<List<Tree>> parameterList = expr.then(lexer.tok(",").thenRight(expr).rep(true)).adapter(
			t -> Rep.rep(t.a, t.b).asList()
	);

	static final Parser<? extends Tree> vector = lexer.tok("\\[").thenRight(parameterList).thenLeft(lexer.tok("\\]")).adapter(
			Tree.Vec::new
	);

	private static final Parser<Tree> parens = lexer.tok("\\(").thenRight(expr).thenLeft(lexer.tok("\\)"));

	private static final Parser<Tree> primary =
			parens.or(integer.or(hexint).adapter(Value.Const.Int::new)
	).or(real.adapter(Value.Real::new)).or(id.adapter(Tree.Id::new)).or(block).or(vector)
			.or(bool.adapter(Value.Bool::new))
			.or(str.adapter(Value.StringVal::new)); // already annotated

	private static final Parser<Tree> postfix = primary.then(lexer.tok("\\.").thenRight(id).rep(true)).adapter(
			(t) -> {
					Tree ret = t.a;

					for(String u : t.b) {
						ret = ret.createMember(ret, u);
					}

					return ret;
				}
	);

	private static final Parser.ParserReference<Tree> app = new Parser.ParserReference<>();

	private static final Parser<List<Tree>> parameters =
			lexer.tok("\\(").thenRight(parameterList.opt()).thenLeft(lexer.tok("\\)")).adapter(
					t -> t.isDef ? t.get() : new LinkedList<Tree>()
			).or(app.adapter(Collections::singletonList)
	);

	static {
		app.set(postfix.then(parameters.rep(true)).adapter(
				t -> {
                    Tree ret = t.a;

                    for(List<Tree> arg : t.b) {
                        ret = new Tree.App(ret, arg);
                    }

                    return ret;
                }));
	}

	static final Parser<List<String>> args =
			id.adapter(Collections::singletonList)
					.or(lexer.tok("\\(").thenRight(
							id.then(lexer.tok(",").thenRight(id).rep(true)).opt())
							.thenLeft(lexer.tok("\\)")).adapter(
			vars -> {
                List<String> ret = new LinkedList<>();
                if(vars.isDef) {
                    ret.add(vars.get().a);
                    for(String a : vars.get().b) {
                        ret.add(a);
                    }
                }

                return ret;
            }
	));

	private static final Parser.ParserReference<Tree> term = new Parser.ParserReference<>();

	private static final Parser<Tree> lambda =  lexer.tok("lambda").thenRight(args.opt()).then(term).adapter(
			t -> {
				List<String> args = t.a.isDef ? t.a.get() : Collections.emptyList();
				return new Tree.FuncDef("$lambda$", args, t.b);
			}
	);

	static {
		term.set(block.or(app).or(lambda));
	}

	// trailing sign.
	private static final Parser.ParserReference<Tree> unexpr = new Parser.ParserReference<>();

	static {
		unexpr.set(lexer.tok("-").or(lexer.tok("/")).then(unexpr).adapter(
				t -> {
					Op op = t.a.charAt(0) == '-' ? Op.neg : Op.recip;
					return op.eval(Collections.singletonList(t.b));
				}).or(term)
		);
	}



	private static final Parser.ParserReference<Tree> ifexpr = new Parser.ParserReference<>();

	static {
		ifexpr.set(unexpr.then(lexer.tok("if").thenRight(expr).then(lexer.tok("else").thenRight(ifexpr)).opt()).adapter(
				t -> {
					if (t.b.isDef) return Op.ifOp.eval(Arrays.asList(t.b.get().a, t.a, t.b.get().b));
					else return t.a;
				}
		));
	}

	// list constructor
	private static final Parser<Tree> consexpr = ifexpr.then(lexer.tok(":").thenRight(ifexpr).rep(true))
			.adapter(t -> {
				if (t.b.isEmpty()) {
					return t.a;
				} else {
					return Op.cons.eval(Rep.rep(t.a, t.b).asList());
				}
			});

	private static final Parser<Tree> pow = consexpr.then(lexer.tok("^").thenRight(consexpr).rep(true))
			.adapter(t -> {
				Tree ret = t.a;

				for(Tree exp : t.b) {
					ret = Op.pow.eval(Arrays.asList(ret, exp));
				}

				return ret;
			});

	// wow, that was some annoying bug: Have written - instead of %
	private static final Parser<Tree> prod = pow.then((lexer.match("[%*]").or(lexer.tok("/"))).then(pow).rep(true))
			// FIXME Here is the reason why /1 does not work
			// TODO BUGFIX
			.adapter(ts -> {
				Tree ret = ts.a;

				for (Concat<String, Tree> t : ts.b) {
					switch (t.a.charAt(0)) {
						case '/':
							ret = Op.div.eval(Arrays.asList(ret, t.b));
							break;
						case '%':
							ret = Op.mod.eval(Arrays.asList(ret, t.b));
							break;
						case '*': // this one just leaves \\*
							ret = Op.mul.eval(Arrays.asList(ret, t.b));
							break;
						default:
							throw new AssertionError("bug.");
					}
				}

				return ret;
			});

	private static final Parser<Tree> sum = prod.then((lexer.tok("-").or(lexer.tok("\\+"))).then(prod).rep(true))
			.adapter(ts -> {
				Tree ret = ts.a;

				for (Concat<String, Tree> t : ts.b) {
					switch (t.a.charAt(0)) {
						case '-':
							ret = Op.sub.eval(Arrays.asList(ret, t.b));
							break;
						default: // this is \\+
							ret = Op.add.eval(Arrays.asList(ret, t.b));
							break;
					}
				}
				return ret;
			});

	private static final Parser<Op> compOp =
			lexer.tok("==").adapter(s -> Op.eq)
	.or(lexer.tok(">=").adapter(s -> Op.ge))
	.or(lexer.tok("=<").adapter(s -> Op.le))
	.or(lexer.tok("><").adapter(s -> Op.ne))
	.or(lexer.tok(">").adapter(s -> Op.g))
	.or(lexer.tok("<").adapter(s -> Op.l));

	private static final Parser<Tree> cmpExpr = sum.then(compOp.then(sum).opt())
			.adapter(t -> {
				if (t.b.isDef) {
					return t.b.get().a.eval(Arrays.asList(t.a, t.b.get().b));
				} else {
					return t.a;
				}
			});

	/**
	 * literal is a reference because its definition uses literal itself.
	 */
	private static final Parser.ParserReference<Tree> literal = new Parser.ParserReference<>();

	static {
		literal.set(lexer.tok("not").thenRight(literal).adapter(
				arg -> Op.not.eval(Collections.singletonList(arg))
		).or(cmpExpr));
	}

	private static final Parser<Tree> andExpr = literal.then(lexer.tok("and").thenRight(literal).rep(true))
			.adapter(t -> {
				Tree ret = t.a;

				for (Tree lit : t.b) {
					ret = Op.and.eval(Arrays.asList(ret, lit));
				}

				return ret;
			});

	private static final Parser<Tree> orExpr = andExpr.then(lexer.tok("or").thenRight(andExpr).rep(true))
			.adapter(t -> {
				Tree ret = t.a;

				for (Tree lit : t.b) {
					ret = Op.or.eval(Arrays.asList(ret, lit));
				}

				return ret;
			});

	private static final Parser<Tree> rangeExpr = orExpr.then(lexer.tok("to").thenRight(orExpr).opt())
			.adapter(t -> t.b.isDef ? new Tree.Range(t.a, t.b.get()) : t.a);

	private static final Parser<Tree> assignExpr = rangeExpr.then(lexer.tok("=").thenRight(rangeExpr).opt()).adapter(
			t -> t.b.isDef ? Op.mov.eval(t.b.get(), t.a) : t.a);

	// and that is it. we are finally ready for expr.
	static { expr.set(assignExpr); }

	private static final Parser<Tree> ifstmt = lexer.tok("if").thenRight(expr)
			.then(lexer.tok("then").thenRight(stmt))
			.then(lexer.tok("else").thenRight(stmt).opt())
			.adapter(t -> {
				if (t.b.isDef) {
					// if-else
					return Op.ifOp.eval(Arrays.asList(t.a.a, t.a.b, t.b.get()));
				} else {
					return Op.ifOp.eval(Arrays.asList(t.a.a, t.a.b));
				}
			});

	private static final Parser<Tree> whilestmt = lexer.tok("while").thenRight(expr).then(lexer.tok("do").thenRight(stmt).opt()).adapter(
		t -> t.b.isDef ?
				Op.whileOp.eval(Arrays.asList(t.a, t.b.get())) :
				Op.whileOp.eval(Collections.singletonList(t.a)));

	private static final Parser<Tree> defstmt = lexer.tok("def").thenRight(id).then(lexer.tok("=").opt().thenRight(expr)).adapter(
		t -> new Tree.Def(t.a, t.b));


	private static final Parser<Tree.Extern> externstmt = lexer.tok("extern").thenRight(id).then(id).then(lexer.tok("=").thenRight(expr)).adapter(
		t -> new Tree.Extern(t.a.a, t.a.b, t.b));

	// this is not top-level.
	private static final Parser<Tree> vardecl = id.then(id.opt()).then(lexer.tok("=").thenRight(expr).opt()).adapter(
		t -> new Tree.Var(t.a.a, t.a.b.isDef ? t.a.b.get() : null, t.b.isDef ? t.b.get() : null));

	// A block does not lose its scope
	private static final Parser<Tree> varstmt = lexer.tok("var").thenRight(vardecl.then(lexer.tok(",").thenRight(vardecl).rep(true))).adapter(
		t -> Tree.createBlock(Rep.rep(t.a, t.b).asList()));

	private static final Parser<Tree> forstmt = lexer.tok("for").thenRight(id)
			.then(lexer.tok("in").thenRight(expr)).then(lexer.tok("do").thenRight(stmt))
			.adapter(t -> Op.forOp.eval(new Tree.Id(t.a.a), t.a.b, t.b));

	private static final Parser<Tree.Def> funcstmt = lexer.tok("func").thenRight(id).then(args.opt()).then(stmt).adapter(
			t -> {
				int size = t.a.b.isDef ? t.a.b.get().size() : 0;
				List<String> args = new ArrayList<>(size);


				if (size > 0) {
					// transfer arguments in list into array
					args.addAll(t.a.b.get());
				}

				// it is in fact a def a func sth...
				return new Tree.Def(t.a.a, new Tree.FuncDef(t.a.a, args, t.b));
			}
	);

	static {
		stmt.set(forstmt.or(ifstmt).or(whilestmt).or(defstmt).or(externstmt).or(varstmt).or(funcstmt).or(expr));
	}
}
