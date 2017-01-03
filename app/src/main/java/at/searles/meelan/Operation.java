package at.searles.meelan;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface Operation {
	/**
	 * Simplifies operation. If it could not be simplified, 'this' should be
	 * returned. One may assume, that arguments are already fully simplified.
	 * @param arguments of operation
	 * @return simplified tree
	 */
	@NotNull
	Tree eval(List<Tree> arguments);

	/**
	 * Linearizes this operation as an expression and writes the result into target.
	 * If target is null a new register is created in target-scope. If target
	 * is an uninitialized register, it is initialized.
	 * @param args
	 * @param target
	 * @param targetScope
	 * @param currentScope
	 * @param program
	 * @return
	 * @throws CompileException
	 */
	Value linearizeExpr(List<Tree> args, Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException;

	void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) throws CompileException;

	void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException;
}
