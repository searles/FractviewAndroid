package at.searles.meelan;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

// import at.searles.syntax.ListValue;

public abstract class Tree {

	/**
	 * Linearizes an expression. For this purpose, it adds instructions to program.
	 * The result is written into the target register. If there is no such
	 * register, targetScope indicates in which scope a new register should be created.
	 * To linearize the expression, currentScope is used.
	 *
	 * TargetScope is needed in embedded instructions, eg 1 + {2 - 3}. Here, the
	 * inner scope differs from it.
	 * @param target
	 * @param targetScope
	 * @param currentScope
	 * @param program
	 * @return The value of the expr. If target is set, it returns target.
	 * @throws CompileException Type exception if the result of the expression cannot
	 * be converted to the target register.
     */
	public Value linearizeExpr(Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
		throw new CompileException("not an expression: " + this).addTraceElement(this);
	}

	/**
	 * Linearizes the current tree by appending instructions to program.
	 * @param currentScope
	 * @param program
	 * @throws CompileException
     */
	public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
		throw new CompileException("not a statement: " + this).addTraceElement(this);
	}

	public void linearizeBool(Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
		throw new CompileException("not a bool: " + this).addTraceElement(this);
	}

	public static Tree createBlock(List<Tree> content) {
		if(content.size() == 1) return content.get(0);
		else return new Block(content);
	}

	/**
	 * Calls inline on a list of args. Just here for convenience.
	 * @param args list of trees on which inline should be called
	 * @param table Argument for inline call
     * @param maskVars Argument for inline call
	 * @return List of inlined trees.
	 */
	public static List<Tree> inlineList(List<Tree> args, ScopeTable table, boolean maskVars) throws CompileException {
		List<Tree> inlinedArgs = new LinkedList<>();

		// a bit lazy, only create a new opapp if necessary.
		for(Tree arg : args) {
			inlinedArgs.add(arg.inline(table, maskVars));
		}

		// no need to create something new. Inlining did not change it.
		return inlinedArgs;
	}


	/**
	 * Puts constants into table and replaces them in the remaining code.
	 * @param table Table to contain constants
	 * @param maskVars new variables inside function definitions should not be assigned registers, therefore if true,
	 *                 variables are not replaced by register declarations.
	 * @return Tree without defs (they are replaced by nops).
	 */
	public abstract Tree inline(ScopeTable table, boolean maskVars) throws CompileException;

	public abstract void exportEntries(ExternalData data) throws CompileException;

	/**
	 * Creates a member (struct)
	 * @param ret
	 * @param u
	 * @return
	 */
	public Tree createMember(Tree ret, String u) {
		return new Member(ret, u);
	}

	// from here it is interesting

	public static class Id extends Tree {

		public String id;

		public Id(String id) {
			this.id = id;
		}

		@Override
		public String toString() {
			return id;
		}

		@Override
		public boolean equals(Object o) {
			return o instanceof Id && ((Id) o).id.equals(id);
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			Tree t = table.getId(id, this);

			if(t != null) {
				return t;
			} else {
				// is it a constant?
				// FIXME this is so yuck-style...
				try {
					Constant c = Constant.valueOf(id);
					return c.get();
				} catch(IllegalArgumentException e) {
					// no. Is it an operation?
					try {
						Operation op = Op.valueOf(id);
						return new OpTree(op);
					} catch(IllegalArgumentException e2) {
						throw new CompileException(id + " is not defined").addTraceElement(this);
					}
				}
			}
		}

		@Override
		public void exportEntries(ExternalData data) {}
	}

	public static class Def extends Tree {

		private final String id;
		private final Tree expr;

		public Def(String id, Tree expr) {
			this.id = id;
			// from the parsing process, expr is an expr and not
			// a stmt, hence no worries about the scope.
			this.expr = expr;
		}

		public String toString() {
			return "def " + id + " " + expr;
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			if(table.topLevelScope(id)) throw new CompileException(id +" already defined").addTraceElement(this);

			try {
				Tree inlineExpr = expr.inline(table, maskVars);
				table.addDef(id, inlineExpr);
				// return new Def(id, inlineExpr); // returns this one because
				return null;
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
			// ignore.
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			expr.exportEntries(data);
		}
	}

	public static class Var extends Tree {
		// Variable declarations must either have a type or an initialization.

		String id;
		String type;

		Tree init; // this is an expr

		public Var(String id, String type, Tree init) { // expr may be null
			this.id = id;
			this.type = type;
			this.init = init;
		}

		public String toString() {
			return "var " + id + ": " + type + (init == null ? "" : " := " + init);
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			if(table.topLevelScope(id)) {
				throw new CompileException(id +" already defined").addTraceElement(this);
			}

			try {
				Tree inlineInit = init == null ? null : init.inline(table, maskVars);
				if(maskVars) {
					table.addMasked(id);
					return inlineInit == null ? this : new Var(id, type, inlineInit);
				} else {
					return table.addVar(id, type, inlineInit); // returns a register definition
				}
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			if(init != null) init.exportEntries(data);
		}
	}

	// FIXME Support currying by unifying OpApp and OpTree.
	// FIXME Also TODO: Check whether crash when importing a damaged thing.
	public static class OpTree extends Tree {
		Operation op;

		public OpTree(Operation op) {
			this.op = op;
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) {
			return this;
		}

		public Operation get() {
			return op;
		}

		public String toString() {
			return op.toString();
		}

		@Override
		public void exportEntries(ExternalData data) {} // nothing to do.
	}

	public static class FuncDef extends Tree {

		String id; // for debugging
		List<String> args;
		Tree body;

		public FuncDef(String id, List<String> args, Tree body) {
			// The id is just here for debugging.
			this.id = id;
			this.args = args;
			this.body = body;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();
			sb.append("func (");
			boolean first = true;
			for(String id : args) {
				if(first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(id);
			}
			return sb.append(") ").append(body).toString();
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			try {
				ScopeTable funcTable = table.createFuncTable(args);
				return new FuncDef(id, args, body.inline(funcTable, true)); // mask vars inside funcdefs.
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		public Tree inlineArgs(ScopeTable table, List<Tree> args, boolean maskVars) throws CompileException {
			// args must already be inlined!
			ScopeTable funcTable = table.newScope();

			Iterator<String> i1 = this.args.iterator();
			Iterator<Tree> i2 = args.iterator();

			while (i1.hasNext() && i2.hasNext()) {
				funcTable.addDef(i1.next(), i2.next());
			}

			if (i1.hasNext() || i2.hasNext()) {
				throw new CompileException("call to " + id + " with wrong number of arguments").addTraceElement(this);
			}

			try {
				return body.inline(funcTable, maskVars);
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			body.exportEntries(data);
		}
	}

	public static class Extern extends Tree {
		// Variable declarations must either have a type or an initialization.
		String id;
		String type;

		Tree init;

		public Extern(String id, String type, Tree init) { // expr may be null
			this.id = id;
			this.type = type;
			this.init = init;
		}

		public String toString() {
			return "extern " + id + ": " + type + (init == null ? "" : " := " + init);
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			// table should contain id already but it is not yet inlined.
			table.pullUpExtern(id, maskVars); // therefore, inline it here (variables are masked!)

			return null; // similar to a def
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			data.add(id, type, init);
		}
	}

	// actually only need three kinds of exprs
	public static class App extends Tree {
		// fixme: in Inline map it to an OpApp with a compilable App.
		// that way I immediately find whether a function

		public Tree fn;
		public List<Tree> args;

		App(Tree fn, List<Tree> args) {
			this.fn = fn;
			this.args = args;
		}

		App(String fn, List<Tree> args) {
			this(new Id(fn), args);
		}

		public App(String fn, Tree... args) {
			this(fn, Arrays.asList(args));
		}

		public String toString() {
			StringBuilder sb = new StringBuilder().append(fn).append("(");

			boolean first = true;

			for(Tree arg : args) {
				if(first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(arg);
			}

			sb.append(")");

			return sb.toString();
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			// FIXME: Should I first eval or first inline?
			// inline inserts definitions, but some operations
			// like derive and newton would require a different thing.
			// So, maybe I can delegate the inline-fn to op?
			// But I need defs. So, maybe inline but mask variables?
			// Use case: var z; def power = z; z = 0; z = newton(z^power-1, z)
			// other solution: second argument in derive must be a register after inlining
			// or an id if variables are masked.

			// inline arguments
			List<Tree> argsInlined = inlineList(args, table, maskVars);

			// next, check whether it is a predefined operation
			// fn must be either an Id
			//    ===> if not in scopetable, check predefined operations.
			// well, so simply call equals. Only need to implement if for those two.

			Tree op = fn.inline(table, maskVars);

			try {
				if (op instanceof FuncDef) {
					return ((FuncDef) op).inlineArgs(table, argsInlined, maskVars);
				} else if (op instanceof OpTree) {
					return ((OpTree) op).get().eval(argsInlined);
				} else if(maskVars) {
					// it might be a masked Id or at least contain some.
					// Keep it until further notice
					// but use the inlined arguments.
					return new App(op, argsInlined);
				} else if(argsInlined.size() == 1) {
					// it is a multiplication.
					return Op.mul.eval(Arrays.asList(op, argsInlined.get(0)));
				}
			} catch (CompileException e) {
				// if there is an exception when inlining
				throw e.addTraceElement(this);
			}

			// nothing was returned and no other exception was thrown, hey, this one did not exist!
			throw new CompileException("no such function: " + fn).addTraceElement(this);
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			for(Tree arg : args) arg.exportEntries(data);
		}
	}


	//// ===================== Now there are the linearizable = compilable tree types ============================= ////
	public static class RegDecl extends Tree {
		// RegDecl are needed because I may declare a variable and
		// do the first assignment only inside some inner scope (like an if)

		Value.Reg reg;
		Tree init;

		RegDecl(Value.Reg reg, Tree init) {
			// this is only called from inlining
			this.reg = reg;
			this.init = init;
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			if(init != null) {
				Tree inlinedInit = init.inline(table, maskVars);
				if(inlinedInit == init) return this;
				else return new RegDecl(reg, inlinedInit);
			} else {
				// nothing to inline.
				return this;
			}
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException{
			throw new CompileException("extern entry in register declaration is a bug");
		}

		@Override
		public Value linearizeExpr(Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			throw new CompileException("variable declaration used as an expr...").addTraceElement(this);
		}

		@Override
		public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
			try {
				reg.initDataScope(currentScope);
				if (init != null) {
					init.linearizeExpr(reg, null, currentScope.newScope(), program);
				}
			} catch(CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void linearizeBool(Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			throw new CompileException("variable declaration used as a boolean...").addTraceElement(this);
		}

		public String toString() {
			return reg + " = " + init;
		}
	}

	public static class Block extends Tree {

		List<Tree> block;

		public Block(List<Tree> block) {
			this.block = block;
		}

		public Block(Tree...trees) {
			this(Arrays.asList(trees));
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			for (Tree t : block) {
				sb.append("\n").append(t);
			}

			return sb.toString();
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			List<Tree> l = new LinkedList<Tree>();

			for (Tree t : block) {
				t = t.inline(table, maskVars);

				if (t != null) {
					// ignore null
					if (t instanceof Block) {
						// insert blocks. There are no blocks in blocks because of this.
						// this is mainly because of variable declarations.
						// fixme test this!
						for (Tree u : ((Block) t).block) {
							l.add(u);
						}
					} else {
						l.add(t);
					}
				}
			}

			return createBlock(l);
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			// this one is only called on the outermost block.
			for (Tree t : block) t.exportEntries(data);
		}

		@Override
		public Value linearizeExpr(Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			// all but the last one in this block are statements
			if (block.isEmpty()) {
				throw new CompileException("empty block is not an expression").addTraceElement(this);
			}

			try {
				Iterator<Tree> it = block.iterator();

				for (Tree arg = it.next(); ; arg = it.next()) {
					if (it.hasNext()) {
						// then it is a statement
						arg.linearizeStmt(currentScope, program);
					} else {
						// then it is an expr.
						return arg.linearizeExpr(target, targetScope, currentScope, program);
					}
				}
			} catch(CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
			try {
				for (Tree t : block) {
					t.linearizeStmt(currentScope, program);
				}
			} catch(CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void linearizeBool(Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			// all but the last one in this block are statements
			if (block.isEmpty())
				throw new CompileException("empty block is not a boolean expression").addTraceElement(this);

			Iterator<Tree> it = block.iterator();

			try {
				for (Tree arg = it.next(); ; arg = it.next()) {
					if (it.hasNext()) {
						// then it is a statement
						arg.linearizeStmt(currentScope, program);
					} else {
						// then it is an expr.
						arg.linearizeBool(trueLabel, falseLabel, currentScope, program);
						return;
					}
				}
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}
	}

	public static class Scope extends Tree {
		Tree inner;

		public Scope(Tree inner) {
			this.inner = inner;
		}

		public String toString() {
			return "{" + inner + "}";
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			Tree t = inner.inline(table.newScope(), maskVars);

			if(t instanceof Scope || t instanceof Vec) {
				// no need to have a scope in a scope
				return t;
			} else if(t instanceof Value.Const) {
				// well
				return t;
			} else {
				return new Scope(t);
			}
		}

		@Override
		public Value linearizeExpr(Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			// we create a new inner scope here.
			if(targetScope == null) targetScope = currentScope;
			return inner.linearizeExpr(target, targetScope, currentScope.newScope(), program);
		}

		@Override
		public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
			inner.linearizeStmt(currentScope.newScope(), program);
		}

		@Override
		public void linearizeBool(Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			inner.linearizeBool(trueLabel, falseLabel, currentScope.newScope(), program);
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			inner.exportEntries(data);
		}
	}

	/**
	 * This one is the '.'-operator, like the one in objects or structs
	 */
	public static class Member extends Tree {

		Tree element;
		String member;

		public Member(Tree element, String member) {
			this.element = element;
			this.member = member;
		}

		@Override
		public Value linearizeExpr(Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			// compile argument 0.
			Value v = element.linearizeExpr(null, null, currentScope, program);
			return v.subitem(member).linearizeExpr(target, targetScope, currentScope, program);
		}

		@Override
		public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
			throw new CompileException("accessing a member is not a statement").addTraceElement(this);
		}

		@Override
		public void linearizeBool(Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			throw new CompileException("accessing a member is not a bool").addTraceElement(this);
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			Tree inlinedElement = element.inline(table, maskVars);
			return inlinedElement == element ? this : this.createMember(inlinedElement, member);
		}

		@Override
		public String toString() {
			return element + "." + member;
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			element.exportEntries(data);
		}
	}

	public static class OpApp extends Tree {
		Operation op;
		List<Tree> args;

		public OpApp(Operation op, List<Tree> args) {
			if(op == null) {
				throw new NullPointerException();
			}
			this.op = op;
			this.args = args;
		}

		public String toString() {
			StringBuilder sb = new StringBuilder();

			sb.append(op).append("(");

			boolean first = true;
			for(Tree arg : args) {
				if(first) {
					first = false;
				} else {
					sb.append(", ");
				}
				sb.append(arg);
			}

			sb.append(")");

			return sb.toString();
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			try {
				return op.eval(inlineList(args, table, maskVars));
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public Value linearizeExpr(Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			try {
				return op.linearizeExpr(args, target, targetScope, currentScope, program);
			} catch(CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
			try {
				op.linearizeStmt(args, currentScope, program);
			} catch(CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void linearizeBool(Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			try {
				op.linearizeBool(args, trueLabel, falseLabel, currentScope, program);
			} catch(CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			for(Tree arg : args) arg.exportEntries(data);
		}
	}

	public static class Range extends Tree {
		public final Tree a;
		public final Tree b;

		public Range(Tree a, Tree b) {
			this.a = a;
			this.b = b;
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			try {
				Tree a = this.a.inline(table, maskVars);
				Tree b = this.b.inline(table, maskVars);

				if(a != this.a || b != this.b) return new Range(a, b); else return this;
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			a.exportEntries(data);
			b.exportEntries(data);
		}

		public String toString() {
			return a + " .. " + b;
		}
	}

	public static class Vec extends Tree implements Iterable<Tree> {
		List<Tree> ts;

		public Vec(List<Tree> ts) {
			this.ts = ts;
		}

		@Override
		public Tree inline(ScopeTable table, boolean maskVars) throws CompileException {
			try {
				List<Tree> inlined = inlineList(ts, table, maskVars);

				if (inlined != ts) return new Vec(inlined);
				else return this;
			} catch (CompileException e) {
				throw e.addTraceElement(this);
			}
		}

		// FIXME members!

		/**
		 * labels is a list of uninitlalized labels, one for each element
		 * @param labels
		 * @param currentScope
		 * @param program
		 */
		public void linearizeVecStmt(List<Value.Label> labels,
									 DataScope currentScope, Program program) throws CompileException {
			if(labels.size() != ts.size()) throw new IllegalArgumentException("amount of created labels is wrong");

			Iterator<Value.Label> li = labels.iterator();
			Iterator<Tree> vi = ts.iterator();

			Value.Label endLabel = new Value.Label();

			while(li.hasNext()) {
				program.addLabel(li.next()); // add label
				vi.next().linearizeStmt(currentScope.newScope(), program); // do statement
				Op.__jump.addToProgram(Collections.singletonList((Value) endLabel), currentScope, program); // and jump to end.
			}

			program.addLabel(endLabel);
		}

		/**
		 * labels is a list of uninitlalized labels, one for each element
		 * @param labels
		 * @param currentScope
		 * @param program
		 */
		public Value linearizeVecExprs(List<Value.Label> labels, Value.Reg target, DataScope targetScope,
									   DataScope currentScope, Program program) throws CompileException {
			if(labels.size() != ts.size()) throw new IllegalArgumentException("amount of created labels is wrong");

			Iterator<Value.Label> li = labels.iterator();
			Iterator<Tree> vi = ts.iterator();

			// get register for return value.
			if(target == null) {
				target = new Value.Reg(targetScope == null ? currentScope : targetScope, "$$");
			}

			Value.Label endLabel = new Value.Label();

			while(li.hasNext()) {
				program.addLabel(li.next()); // add label
				vi.next().linearizeExpr(target, null, currentScope.newScope(), program); // do statement
				Op.__jump.addToProgram(Collections.singletonList((Value) endLabel), currentScope, program); // and jump to end.
			}

			program.addLabel(endLabel);

			return target;
		}

		/**
		 * labels is a list of uninitlalized labels, one for each element
		 * @param labels
		 * @param currentScope
		 * @param program
		 */
		public void linearizeVecBool(List<Value.Label> labels, Value.Label trueLabel, Value.Label falseLabel,
									   DataScope currentScope, Program program) throws CompileException {
			if(labels.size() != ts.size()) throw new IllegalArgumentException("amount of created labels is wrong");

			Iterator<Value.Label> li = labels.iterator();
			Iterator<Tree> vi = ts.iterator();

			while(li.hasNext()) {
				program.addLabel(li.next()); // add label
				vi.next().linearizeBool(trueLabel, falseLabel, currentScope.newScope(), program); // do statement
				// jump is not necessary because of true/false-label.
				// Op.__jump.addToProgram(Collections.singletonList(endLabel), currentScope, program); // and jump to end.
			}

			// no need for an end-label.
		}

		public Tree get(int i) {
			return ts.get(i);
		}

		public int size() {
			return ts.size();
		}

		@Override
		public Iterator<Tree> iterator() {
			return ts.iterator();
		}

		@Override
		public void exportEntries(ExternalData data) throws CompileException {
			for(Tree arg : ts) arg.exportEntries(data);
		}

		public String toString() {
			return ts.toString();
		}
	}
}
