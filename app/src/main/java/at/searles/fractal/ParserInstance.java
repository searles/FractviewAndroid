package at.searles.fractal;

import at.searles.meelan.MeelanException;
import at.searles.meelan.compiler.Ast;
import at.searles.meelan.optree.Tree;
import at.searles.meelan.parser.MeelanEnv;
import at.searles.meelan.parser.MeelanParser;
import at.searles.parsing.lexer.TokStream;
import at.searles.parsing.parser.ParserStream;

public class ParserInstance {

    private static ParserInstance singleton = null;

    public static ParserInstance get() {
        if(singleton == null) {
            singleton = new ParserInstance();
        }

        return singleton;
    }

    private MeelanEnv env;
    private MeelanParser parser;

    private ParserInstance() {
        this.env = new MeelanEnv();
        this.parser = new MeelanParser();
    }

    private Tree parseExpr(String sourceCode) {
        ParserStream stream = new ParserStream.fromString(sourceCode);

        Tree tree = parser.parseExpr(env, stream);

        if(!stream.isEmpty()) {
            // TODO 2018-07-11: There should be some warning in this case.
            // Proposal: Throw exception with tree and catch it.
            throw new MeelanException("not fully parsed!", tree);
        }

        return tree;
    }

    public Ast parseSource(String sourceCode) {
        ParserStream stream = ParserStream.fromString(sourceCode);
        return Ast.parse(env, stream);
    }
}
