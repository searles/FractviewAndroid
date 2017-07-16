package at.searles.meelan;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import at.searles.math.Cplx;
import at.searles.math.Quat;

public enum Op implements Operation {
	// New ops: labs, floor, ceil, round, only_decimals ...
	// the next ones are not compiled and hence do not have a signature
	derive {
		// FIXME: Allow third argument = replacement map.

		/**
		 * returns the derivate of var
		 * @param expr
		 * @param var
		 * @return null if it cannot be derived.
		 */
		Tree derive(Tree expr, Value.Reg var) {
			Tree t = null;

			if(expr == var) t = new Value.Const.Int(1); // D(z) = 1
			else if(expr instanceof Value) t = new Value.Const.Int(0); // D(y) = 0
			else if(expr instanceof Tree.OpApp) {
				Tree.OpApp app = (Tree.OpApp) expr;

				if(app.op instanceof Op) {
					List<Tree> dargs = new LinkedList<>(); // we need the derivatives anyways

					for (Tree arg : app.args) {
						Tree da = derive(arg, var);
						dargs.add(da);
					}

					// FIXME arity check!
					// FIXME Op with arity 0 are not covered yet?

					switch ((Op) app.op) {
						case add:
							if(app.args.size() != 2) break;
							t = add.eval(dargs.get(0), dargs.get(1));
							break;
						case sub:
							if(app.args.size() != 2) break;
							t = sub.eval(dargs.get(0), dargs.get(1));
							break;
						case mul:
							if(app.args.size() != 2) break;
							t = add.eval(mul.eval(dargs.get(0), app.args.get(1)), mul.eval(app.args.get(0), dargs.get(1)));
							break;
						case div:
							if(app.args.size() != 2) break;
							t = div.eval(
									sub.eval(mul.eval(dargs.get(0), app.args.get(1)), mul.eval(app.args.get(0), dargs.get(1))),
									sqr.eval(app.args.get(1)));
							break;
						case pow:
							if(app.args.size() != 2) break;
							// if dathds[0] is 0 then it is const
							// a^b' = exp(ln a*b)' = a^b * (b * da / a + ln a * db)
							// shortcut not really needed.
							Tree m0 = div.eval(mul.eval(app.args.get(1), dargs.get(0)), app.args.get(0));
							Tree m1 = mul.eval(log.eval(app.args.get(0)), dargs.get(1));
							t = mul.eval(pow.eval(app.args.get(0), app.args.get(1)), add.eval(m0, m1));
							break;
						case sqr:
							if(app.args.size() != 1) break;
							t = mul.eval(new Value.Int(2), app.args.get(0), dargs.get(0));
							break;
						case neg:
							if(app.args.size() != 1) break;
							t = neg.eval(dargs.get(0)); break;
						case recip:
							if(app.args.size() != 1) break;
							t = neg.eval(div.eval(dargs.get(0), sqr.eval(app.args.get(0))));
							break;
						case sqrt:
							if(app.args.size() != 1) break;
							t = mul.eval(new Value.Real(0.5), div.eval(dargs.get(0), sqrt.eval(app.args.get(0))));
							break;
						case exp:
							if(app.args.size() != 1) break;
							t = mul.eval(dargs.get(0), exp.eval(app.args.get(0)));
							break;
						case log:
							if(app.args.size() != 1) break;
							t = div.eval(dargs.get(0), app.args.get(0));
							break;
						case sin:
							if(app.args.size() != 1) break;
							t = mul.eval(dargs.get(0), cos.eval(app.args.get(0)));
							break;
						case cos:
							if(app.args.size() != 1) break;
							t = neg.eval(mul.eval(dargs.get(0), sin.eval(app.args.get(0))));
							break;
						case tan:
							if(app.args.size() != 1) break;
							// TODO t =
							break;
						case atan:
							if(app.args.size() != 1) break;
							// TODO t =
							break;
						case sinh:
							if(app.args.size() != 1) break;
							t = mul.eval(dargs.get(0), cosh.eval(app.args.get(0)));
							break;
						case cosh:
							if(app.args.size() != 1) break;
							t = mul.eval(dargs.get(0), sinh.eval(app.args.get(0)));
							break;
						case tanh:
							if(app.args.size() != 1) break;
							// TODO t =
							break;
						case atanh:
							if(app.args.size() != 1) break;
							// TODO t =
							break;
					}
				}
			} else if(expr instanceof Tree.Block) {
				// ensure that there is only one instruction
				if(((Tree.Block) expr).block.size() == 1) {
					t = derive(((Tree.Block) expr).block.get(0), var);
				}
			} else if(expr instanceof Tree.Scope) {
				t =  derive(((Tree.Scope) expr).inner, var);
			}

			if(t == null) {
				// fall-back solution
				t = new Tree.OpApp(derive, Arrays.asList(expr, var));
			}

			return t;
		}

		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			// we only can use this with three elements
			if(arguments.size() == 2) {
				Tree fun = arguments.get(0);
				Tree var = arguments.get(1);

				if(var instanceof Value.Reg) {
					// we are good.
					// fixme don't forget that I should remove Id here because Ids must be replaced by a register
					return derive(fun, (Value.Reg) var);
				}
			}

			// could not get the derivative, an error will be given later.
			return new Tree.OpApp(this, arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String title() {
			return "Derivation of a function.";
		}

		@Override
		public String description() {
			return "Returns the derivate of a function.\n" +
					"Example: derive(x^2, x) would return 2x.\n" +
					"Error: If the function does not have a derivative (eg \"abs\").";
		}
	},
	newton {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			// we only can use this with three elements
			if(arguments.size() == 2) {
				Tree fun = arguments.get(0);
				Tree var = arguments.get(1);

				Tree dfun = derive.eval(arguments);

				Tree t = sub.eval(var, div.eval(fun, dfun));

				return t;
			}

			// an error will be given later.
			return new Tree.OpApp(this, arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String description() {
			return "Generates the formula for the newton approximation.\n" +
					"Example: newton(x^2, x) would return x - x^2/2x.\n" +
					"Error: If the function does not have a derivative (eg abs).";
		}
	},
	solve2 {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			// solve2 solves quadratic equations
			// a*x^2+b*x+c = 0.
			// arguments are a, b, c, x, {1,2}

			if(arguments.size() == 4) {
				Tree a = arguments.get(0);
				Tree b = arguments.get(1);
				Tree c = arguments.get(2);
				Tree index = arguments.get(3);

				// Solution is
				// (b +/- sqrt(b * b - 4 a c)) / 2a

				Tree dis =
						sqrt.eval(
							sub.eval(
									sqr.eval(b), // b^2
									mul.eval(new Value.Int(4), mul.eval(a, c)) // 4ac
							));

				Tree nom = null;

				if(index instanceof Value.Int) {
					if(((Value.Int) index).value == 1) {
						nom = add.eval(neg.eval(b), dis);
					} else if(((Value.Int) index).value == 2) {
						nom = sub.eval(neg.eval(b), dis);
					}
				}

				if(nom != null) {
					// nom / 2d
					Tree result = div.eval(nom, mul.eval(new Value.Int(2), a));
					return result;
				}
			}

			// an error will be given later.
			return new Tree.OpApp(this, arguments);
		}
		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String description() {
			return this.toString() + ": Solves the quadratic equation a * x^2 + b*x + c.\n" +
					"Usage: solve2(a, b, c, index) where \"index\" is in {1, 2}\n" +
					"Example: solve2(1, 0, 1, 2) would return -1.\n";
		}
	},
	// FIXME: solve2, solve3, secant, general_newton
	whileOp {
		@Override
		public void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
			// two possible versions: one argument is like do-while, two arguments is a while.

			if(args.size() == 1) {
				Tree cond = args.get(0);

				Value.Label trueLabel = new Value.Label();
				Value.Label falseLabel = new Value.Label();

				program.addLabel(trueLabel);

				cond.linearizeBool(trueLabel, falseLabel, currentScope, program);

				program.addLabel(falseLabel);
			} else if(args.size() == 2) {
				Tree cond = args.get(0);
				Tree body = args.get(1);

				Value.Label trueLabel = new Value.Label();
				Value.Label falseLabel = new Value.Label();
				Value.Label conditionLabel = new Value.Label();

				// first, jump to the condition.
				__jump.addToProgram(Collections.singletonList(conditionLabel), null, program);

				// but if it is true, we return here.
				program.addLabel(trueLabel);

				body.linearizeStmt(currentScope, program);

				// now the condition
				program.addLabel(conditionLabel);

				cond.linearizeBool(trueLabel, falseLabel, currentScope, program);

				program.addLabel(falseLabel);
			} else {
				throw new CompileException("whileOp must have one or two arguments!");
			}
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return this.toString() + ": Internal while-loop, executed while condition is satisfied. \n" +
					"Usage: whileOp(condition) or whileOp(condition, body)\n";
		}
	},
	ifOp {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			// we only can use this with three elements
			if(arguments.size() == 3) {
				Tree condition = arguments.get(0);
				Tree thenPart = arguments.get(1);
				Tree elsePart = arguments.get(2);

				if(condition instanceof Value.Const.Bool) {
					if (((Value.Const.Bool) condition).value) {
						return thenPart;
					} else {
						return elsePart;
					}
				}
			}

			return unaryEval(arguments);
		}

		@Override
		public void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
			if(args.size() == 2) {
				Tree cond = args.get(0);
				Tree thenPart = args.get(1);

				Value.Label trueLabel = new Value.Label();
				Value.Label falseLabel = new Value.Label();

				cond.linearizeBool(trueLabel, falseLabel, currentScope, program);

				program.addLabel(trueLabel);
				thenPart.linearizeStmt(currentScope, program);
				program.addLabel(falseLabel);
			} else if(args.size() == 3) {
				Tree cond = args.get(0);
				Tree thenPart = args.get(1);
				Tree elsePart = args.get(2);

				Value.Label trueLabel = new Value.Label();
				Value.Label falseLabel = new Value.Label();
				Value.Label endLabel = new Value.Label();

				cond.linearizeBool(trueLabel, falseLabel, currentScope, program);

				program.addLabel(trueLabel);
				thenPart.linearizeStmt(currentScope.newScope(), program);

				__jump.addToProgram(Collections.singletonList(endLabel), null, program);

				program.addLabel(falseLabel);
				elsePart.linearizeStmt(currentScope.newScope(), program);

				program.addLabel(endLabel);
			} else {
				throw new CompileException("ifOp must have two or three arguments!");
			}
		}

		@Override
		public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			if(args.size() == 3) {
				Tree cond = args.get(0);
				Tree thenPart = args.get(1);
				Tree elsePart = args.get(2);

				Value.Label condTrueLabel = new Value.Label(); // label for condition
				Value.Label condFalseLabel = new Value.Label(); // label for condition

				// if condition is true then thenPart.linearizeBool(true, false) else elsePart.linearizeBool(true, false)
				cond.linearizeBool(condTrueLabel, condFalseLabel, currentScope, program);

				program.addLabel(condTrueLabel);
				thenPart.linearizeBool(trueLabel, falseLabel, currentScope, program);

				program.addLabel(condFalseLabel);
				elsePart.linearizeBool(trueLabel, falseLabel, currentScope, program);
			} else {
				throw new CompileException("ifOp of bools must have three arguments!");
			}
		}

		@Override
		public Value linearizeExpr(List<Tree> args, Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			// Design problems: It is not possible to combine multiple programs
			// into one program. If definitely should be. Because, anyways, that's
			// what I do in scope! So, why can't I do this?
			// Answer: Because of the program counter. But compilation should
			// not be too complicated requiring multiple passes, thus
			// this design decision is okay even though it means
			// a bit of overhead for ifs.

			if(args.size() == 3) {
				Tree cond = args.get(0);
				Tree thenPart = args.get(1);
				Tree elsePart = args.get(2);

				Value.Label trueLabel = new Value.Label(); // label for condition
				Value.Label falseLabel = new Value.Label(); // label for condition
				Value.Label thenAssignmentLabel = new Value.Label(); // label for condition
				Value.Label endLabel = new Value.Label();

				cond.linearizeBool(trueLabel, falseLabel, currentScope, program);

				program.addLabel(trueLabel);

				// FIXME Check the next lines!
				// FIXME Again, do it.

				Value thenValue = null;
				Value elseValue = null;

				if(target != null && target.type() != null) {
					// if we know the type, we can use it right away
					thenValue = elseValue = target;
				}

				thenValue = thenPart.linearizeExpr(target != null && target.type() != null ? target : null, null, currentScope.newScope(), program);

				__jump.addToProgram(Collections.singletonList(thenAssignmentLabel), null, program);

				program.addLabel(falseLabel);

				elseValue = elsePart.linearizeExpr(target != null && target.type() != null ? target : null, null, currentScope.newScope(), program);

				if(target == null) {
					target = new Value.Reg(targetScope == null ? currentScope : targetScope, "$$");
				}

				if(target.type() == null) {
					// determine maximum type
					if(thenValue.type().canConvertTo(elseValue.type())) {
						target.initType(elseValue.type());
					} else if(elseValue.type().canConvertTo(thenValue.type())) {
						target.initType(thenValue.type());
					} else {
						throw new CompileException("if-expr with incompatible types");
					}
				}

				if(elseValue != target) { // this also means that thenTarget != target
					Op.mov.addToProgram(Arrays.asList(elseValue, target), currentScope, program);
					Op.__jump.addToProgram(Collections.singletonList(endLabel), null, program);
					// no need to jump if the then-assignment does not need any assignment.
				}

				program.addLabel(thenAssignmentLabel);

				if(thenValue != target) { // and elseValue != target
					Op.mov.addToProgram(Arrays.asList(thenValue, target), currentScope, program);
				}

				program.addLabel(endLabel);

				return target;
			} else {
				throw new CompileException("ifOp as expression must have three arguments!");
			}
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return this.toString() + ": Internal if-branch. \n" +
					"Usage: ifOp(condition, thenBody) or ifOp(condition, thenBody, elseBody). " +
					"Only the ternary operator can return values.\n";
		}
	},
	length {
		@NonNull
		@Override
		public Tree eval(List<Tree> args) {
			if(args.size() == 1) {
				Tree range = args.get(0);

				/*if(range instanceof Tree.Range) {
					Tree.Range r = (Tree.Range) range;

					if(r.a instanceof Value.Int && r.b instanceof Value.Int) {
						return new Value.Int(((Value.Int) r.b).value - ((Value.Int) r.a).value);
					}
				} else*/ if(range instanceof Tree.Vec) {
					Tree.Vec r = (Tree.Vec) range;
					return new Value.Int(r.size());
				}
			}

			return super.eval(args);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return this.toString() + ": Internal length operation for ranges and vectors\n" +
					"Usage: length(range or vector).\n";
		}
	},
	select {
		// selects one single argument in the vector that is in argument 2.
		Tree checkArgs(List<Tree> args) throws CompileException {
			if(args.size() != 2) throw new CompileException("select requires two arguments");
			if(args.get(1) instanceof Tree.Vec) {
				return args.get(1);
			} else /*if(args.get(1) instanceof Tree.Range) {
				return args.get(1);
			} else */{
				throw new CompileException("2nd argument to select not a vector: " + args.get(1));
			}
		}

		List<Value.Label> linearize(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
			Tree t = checkArgs(args);
			Tree var = args.get(0);

			if(t instanceof Tree.Vec) {
				Tree.Vec v = (Tree.Vec) t;
				// create one label for all elements in vec

				// add modulo-instruction
				Value index = mod.linearizeExpr(Arrays.asList(var, new Value.Int(v.size())), null, null, currentScope.newScope(), program);

				// the modulo also makes sure that this is an integer!

				List<Value.Label> labels = new ArrayList<>(v.size());

				for (Tree ignored : v) {
					labels.add(new Value.Label());
				}

				// relative jump to label
				List<Value> jRelArgs = new ArrayList<>(labels.size() + 1);
				jRelArgs.add(index);
				jRelArgs.addAll(labels);

				program.addRaw(__jumprel, __jumprel.signatures[0], jRelArgs);

				return labels;
			} else {
				throw new CompileException("Cannot apply select to range yet :(");
			}
		}

		@Override
		public void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
			// this is the difference in all others
			List<Value.Label> labels = linearize(args, currentScope, program);
			((Tree.Vec) args.get(1)).linearizeVecStmt(labels, currentScope, program);
		}

		@Override
		public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			List<Value.Label> labels = linearize(args, currentScope, program);
			((Tree.Vec) args.get(1)).linearizeVecBool(labels, trueLabel, falseLabel, currentScope, program);
		}

		@Override
		public Value linearizeExpr(List<Tree> args, Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
			List<Value.Label> labels = linearize(args, currentScope, program);
			return ((Tree.Vec) args.get(1)).linearizeVecExprs(labels, target, targetScope, currentScope, program);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return this.toString() + ": selects element from a vector. \n" +
					"Usage: select(index, vector) where indices start with 0. If" +
					"index is larger than the vector size \n" +
					"Example: select((condition) or whileOp(condition, body)\n";
		}
	},
	forOp {
		@NonNull
		@Override
		public Tree eval(List<Tree> args) {
			System.out.println("eval in for");
			if(args.size() == 3 && args.get(0) instanceof Tree.Id) {
				// for v in range do body
				// is converted
				//
				// var $forindex$ = 0
				// while {
				//     var v = select($forindex, range)
				//     body
				//     next($forindex$, range.length)
				// }

				// first argument
				String indexId = "$for_index$"; // internal variable
				Tree.Id index = new Tree.Id(indexId);
				Tree.Var indexInit = new Tree.Var(indexId, null, new Value.Int(0));

				// now to the body of the while loop
				Tree.Id v = (Tree.Id) args.get(0);
				Tree range = args.get(1); // can be a range or a vector
				Tree.Var vInit = new Tree.Var(v.id, null,
						select.eval(index, range));

				Tree body = args.get(2);

				Tree next = Op.next.eval(index, Op.length.eval(range));

				Tree whileStmt = whileOp.eval(
						new Tree.Block(vInit, body, next)
				);

				return new Tree.Scope(new Tree.Block(
						indexInit,
						whileStmt
				));
			}

			return super.eval(args);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}


		@Override
		public String description() {
			return this.toString() + ": Internal for-loop.\n" +
					"Usage: forOp(var, vector) or forOp(var, range).\n";
		}
	},
	and {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			for(Tree arg : args) {
				Value.Label nextLabel = new Value.Label();
				arg.linearizeBool(nextLabel, falseLabel, currentScope, program);
				program.addLabel(nextLabel);
			}

			// all were satisfied, thus go to true.
			__jump.addToProgram(Collections.singletonList(trueLabel), currentScope, program);
		}


		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return "Boolean and. Usage: expr and expr.";
		}
	},
	or {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			// very similar to and
			for(Tree arg : args) {
				Value.Label nextLabel = new Value.Label();
				arg.linearizeBool(trueLabel, nextLabel, currentScope, program);
				program.addLabel(nextLabel);
			}

			// non was satisfied, thus go to false.
			__jump.addToProgram(Collections.singletonList(falseLabel), currentScope, program);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return "Boolean or. Usage: expr or expr.";
		}
	},
	not {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			if(args.size() != 1) {
				throw new CompileException("not requires 1 argument");
			}

			args.get(0).linearizeBool(falseLabel, trueLabel, currentScope, program);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			throw new IllegalArgumentException("not a C-instruction");
		}

		@Override
		public String description() {
			return "Logical negation. Usage: \"not expr\".";
		}
	},
	// from here assembler instructions
	mov(
			// this one is special!
			new Signature().r(Type.integer).w(Type.integer),
			new Signature().r(Type.integer).w(Type.real),
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.integer).w(Type.cplx),
			new Signature().r(Type.real).w(Type.cplx),
			new Signature().r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.integer).w(Type.quat),
			new Signature().r(Type.real).w(Type.quat),
			new Signature().r(Type.cplx).w(Type.quat),
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@Override
		void addToProgram(List<Value> args, DataScope currentScope, Program program) throws CompileException {
			Signature signature = Signature.findSignatureStrict(this.signatures, args);


			if(signature == null) {
				throw new CompileException("No signature found for " + this + args);
			}

			program.add(this, signature, args, currentScope);
		}

		@Override
		public void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
			if(args.size() == 2) {
				Tree rvalue = args.get(0);
				Value lvalue = args.get(1).linearizeExpr(null, null, currentScope, program);

				if(!(lvalue instanceof Value.Reg)) {
					throw new CompileException(args.get(1) + " is not an lvalue");
				}

				rvalue.linearizeExpr((Value.Reg) lvalue, null, currentScope, program);
			} else {
				throw new CompileException("assignment must have two arguments!");
			}
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			// mov values[0] values[1]
			Type srcType = signature.get(0).type;
			Type dstType = signature.get(1).type;

			if(srcType == dstType) {
				// no need for explicit conversion
				return generateExprCase("", signature, values);
			} else {
				return generateExprCase("convert_" + dstType.name(), signature, values);
			}
		}

		@Override
		public String description() {
			return "Internal assignment operator. Usage: \"mov source target\".";
		}
	},

	// Part 2: Boolean expressions
	next(new Signature().w(Type.integer).r(Type.integer).label().label()) {
		// replacement for for-loops.
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return "pc = (++" + values.get(0).vmAccessCode(1) +  ") < " + values.get(1).vmAccessCode(2) + " ? "
			+ values.get(2).vmAccessCode(3) + " : " + values.get(3).vmAccessCode(4) + ";";
		}

		@Override
		public String description() {
			return "Logical increment operator.\n" +
					"Usage: next(i, max).\n" +
					"Increments the integer variable i. Returns true if the result" +
					"is smaller than max, otherwise false. Equivalent" +
					"to the C-expression \"(++i) < max\".";
		}
	},
	g(
			new Signature().r(Type.integer).r(Type.integer).label().label(),
			new Signature().r(Type.real).r(Type.real).label().label()
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateCmpCase(">", signature, values);
		}

		@Override
		public String description() {
			return "Greater-than comparison. Usage: g(expr1, expr2). Returns" +
					"true if expr1 > expr 2.";
		}
	},
	ge(
			new Signature().r(Type.integer).r(Type.integer).label().label(),
			new Signature().r(Type.real).r(Type.real).label().label()
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateCmpCase(">=", signature, values);
		}

		@Override
		public String description() {
			return "Greater-equal comparison. \n" +
					"Usage: ge(expr1, expr2). \n" +
					"Returns" +
					"true if expr1 >= expr 2.";
		}	},
	eq(
			new Signature().r(Type.integer).r(Type.integer).label().label(),
			new Signature().r(Type.real).r(Type.real).label().label()
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateCmpCase("==", signature, values);
		}

		@Override
		public String description() {
			return "Equals-to comparison. \n" +
					"Usage: eq(expr1, expr2). \n" +
					"Returns" +
					"true if expr1 is equal to expr 2. Handle with care with " +
					"non-integers because of rounding errors";
		}
	},
	ne(
			new Signature().r(Type.integer).r(Type.integer).label().label(),
			new Signature().r(Type.real).r(Type.real).label().label()
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateCmpCase("!=", signature, values);
		}

		@Override
		public String description() {
			return "Not-equal comparison. \n" +
					"Usage: eq(expr1, expr2). \n" +
					"Returns" +
					"true if expr1 is equal to expr 2. Handle with care with " +
					"non-integers because of rounding errors";
		}
	},
	le(
			new Signature().r(Type.integer).r(Type.integer).label().label(),
			new Signature().r(Type.real).r(Type.real).label().label()
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateCmpCase("<=", signature, values);
		}
	},
	l(
			new Signature().r(Type.integer).r(Type.integer).label().label(),
			new Signature().r(Type.real).r(Type.real).label().label()
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateCmpCase("<", signature, values);
		}
	},
	radless(
			new Signature().r(Type.cplx).r(Type.real).label().label()
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			int[] argIndices = Value.argIndices(values);

			Value z = values.get(0);
			Value bailout = values.get(1);
			Value lessLabel = values.get(2);
			Value nlessLabel = values.get(3);

			return String.format("pc = rad2(%s) < sqr(%s) ? %s : %s;",
					z.vmAccessCode(1),
					bailout.vmAccessCode(argIndices[1]),
					lessLabel.vmAccessCode(argIndices[2]),
					nlessLabel.vmAccessCode(argIndices[3]));
		}
	},
	distless(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.real).label().label()
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			int[] argIndices = Value.argIndices(values);

			Value z = values.get(0);
			Value zz = values.get(1);
			Value bailout = values.get(2);
			Value lessLabel = values.get(3);
			Value nlessLabel = values.get(4);

			return String.format("pc = rad2(%s - %s) < sqr(%s) ? %s : %s;",
					z.vmAccessCode(1),
					zz.vmAccessCode(argIndices[1]),
					bailout.vmAccessCode(argIndices[2]),
					lessLabel.vmAccessCode(argIndices[3]),
					nlessLabel.vmAccessCode(argIndices[4]));
		}
	},
	radrange(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.real).r(Type.real).label().label().label()
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			int[] argIndices = Value.argIndices(values);

			Value z = values.get(0);
			Value zz = values.get(1);
			Value bailout = values.get(2);
			Value epsilon = values.get(3);
			Value bailoutLabel = values.get(4);
			Value epsilonLabel = values.get(5);
			// the true-label, but this one is already set during compilation.
			Value falseLabel = values.get(6);

			return "pc = radrange(" +
					z.vmAccessCode(argIndices[0]) + ", " +
					zz.vmAccessCode(argIndices[1]) + ", " +
					bailout.vmAccessCode(argIndices[2]) + ", " +
					epsilon.vmAccessCode(argIndices[3]) + ", " +
					bailoutLabel.vmAccessCode(argIndices[4]) + ", " +
					epsilonLabel.vmAccessCode(argIndices[5]) + ", " +
					falseLabel.vmAccessCode(argIndices[6]) + ");";
		}

		@Override
		public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			if(args.size() != 6) throw new CompileException("radrange requires 6 arguments");

			Iterator<Tree> ia = args.iterator();

			Tree zTree = ia.next();
			Tree zzTree = ia.next();
			Tree bailoutTree = ia.next();
			Tree epsilonTree = ia.next();
			Tree bailoutStmtTree = ia.next();
			Tree epsilonStmtTree = ia.next();

			Value z = zTree.linearizeExpr(null, null, currentScope, program);
			Value zz = zzTree.linearizeExpr(null, null, currentScope, program);
			Value bailout = bailoutTree.linearizeExpr(null, null, currentScope, program);
			Value epsilon = epsilonTree.linearizeExpr(null, null, currentScope, program);

			// Following structure:
			// If bailout is true, then __jump to label bailoutLabel,
			// If epsilon is true, __jump to label epsilonLabel (both of which __jump to true)
			// otherwise __jump to falseLabel

			// hence, the signature of it is z, zz, bailout, epsilon, bailout-label, epsilon-label, true-label, false-label.

			Value.Label bailoutLabel = new Value.Label();
			Value.Label epsilonLabel = new Value.Label();

			addToProgram(Arrays.asList(z, zz, bailout, epsilon, bailoutLabel, epsilonLabel, falseLabel), currentScope, program);

			// the true label will be ignored.
			//program.add(radrange, z, zz, bailout, epsilon, bailoutLabel, epsilonLabel, falseLabel);

			program.addLabel(bailoutLabel);
			bailoutStmtTree.linearizeStmt(currentScope.newScope(), program);

			__jump.addToProgram(Collections.singletonList(trueLabel), null, program);

			program.addLabel(epsilonLabel);

			epsilonStmtTree.linearizeStmt(currentScope.newScope(), program);
			__jump.addToProgram(Collections.singletonList(trueLabel), null, program);

			// and this is it.
		}

	},

	// Part 3: Mathematics

	add(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			if(arguments.size() == 2) {
				Tree a0 = arguments.get(0);
				Tree a1 = arguments.get(1);
				if(a0 instanceof Value.Int && ((Value.Int) a0).value == 0) return a1;
				if(a1 instanceof Value.Int && ((Value.Int) a1).value == 0) return a0;
				if(a0 instanceof Value.Real && ((Value.Real) a0).value == 0) return a1;
				if(a1 instanceof Value.Real && ((Value.Real) a1).value == 0) return a0;
				if(a0 instanceof Value.CplxVal && ((Value.CplxVal) a0).value.equals(new Cplx(0))) return a1;
				if(a1 instanceof Value.CplxVal && ((Value.CplxVal) a1).value.equals(new Cplx(0))) return a0;
				if(a0 instanceof Value.QuatVal && ((Value.QuatVal) a0).value.equals(new Quat(0, 0, 0, 0))) return a1;
				if(a1 instanceof Value.QuatVal && ((Value.QuatVal) a1).value.equals(new Quat(0, 0, 0, 0))) return a0;
			}

			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	sub(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			if(arguments.size() == 2) {
				Tree a0 = arguments.get(0);
				Tree a1 = arguments.get(1);
				if(a0 instanceof Value.Int && ((Value.Int) a0).value == 0) return neg.eval(a1);
				if(a1 instanceof Value.Int && ((Value.Int) a1).value == 0) return a0;
				if(a0 instanceof Value.Real && ((Value.Real) a0).value == 0) return neg.eval(a1);
				if(a1 instanceof Value.Real && ((Value.Real) a1).value == 0) return a0;
				if(a0 instanceof Value.CplxVal && ((Value.CplxVal) a0).value.equals(new Cplx(0))) return neg.eval(a1);
				if(a1 instanceof Value.CplxVal && ((Value.CplxVal) a1).value.equals(new Cplx(0))) return a0;
				if(a0 instanceof Value.QuatVal && ((Value.QuatVal) a0).value.equals(new Quat(0, 0, 0, 0))) return neg.eval(a1);
				if(a1 instanceof Value.QuatVal && ((Value.QuatVal) a1).value.equals(new Quat(0, 0, 0, 0))) return a0;
			}

			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	mul(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			if(arguments.size() == 2) {
				Tree a0 = arguments.get(0);
				Tree a1 = arguments.get(1);

				// Zero-element:
				if(a0 instanceof Value.Int && ((Value.Int) a0).value == 0) return a0;
				if(a1 instanceof Value.Int && ((Value.Int) a1).value == 0) return a1;
				if(a0 instanceof Value.Real && ((Value.Real) a0).value == 0) return a0;
				if(a1 instanceof Value.Real && ((Value.Real) a1).value == 0) return a1;
				if(a0 instanceof Value.CplxVal && ((Value.CplxVal) a0).value.equals(new Cplx(0))) return a0;
				if(a1 instanceof Value.CplxVal && ((Value.CplxVal) a1).value.equals(new Cplx(0))) return a1;
				if(a0 instanceof Value.QuatVal && ((Value.QuatVal) a0).value.equals(new Quat(0, 0, 0, 0))) return a0;
				if(a1 instanceof Value.QuatVal && ((Value.QuatVal) a1).value.equals(new Quat(0, 0, 0, 0))) return a1;

				// One-element:
				if(a0 instanceof Value.Int && ((Value.Int) a0).value == 1) return a1;
				if(a1 instanceof Value.Int && ((Value.Int) a1).value == 1) return a0;
				if(a0 instanceof Value.Real && ((Value.Real) a0).value == 1) return a1;
				if(a1 instanceof Value.Real && ((Value.Real) a1).value == 1) return a0;
				if(a0 instanceof Value.CplxVal && ((Value.CplxVal) a0).value.equals(new Cplx(1))) return a1;
				if(a1 instanceof Value.CplxVal && ((Value.CplxVal) a1).value.equals(new Cplx(1))) return a0;
				if(a0 instanceof Value.QuatVal && ((Value.QuatVal) a0).value.equals(new Quat(1, 0, 0, 0))) return a1;
				if(a1 instanceof Value.QuatVal && ((Value.QuatVal) a1).value.equals(new Quat(1, 0, 0, 0))) return a0;
			}


			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase("mul", signature, values);
		}
	},
	scalarmul(
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	div(
			// no integer division
			// new Signature().r(Type.integer).r(Type.integer).w(Type.real),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			if(arguments.size() == 2) {
				Tree a0 = arguments.get(0);
				Tree a1 = arguments.get(1);

				// div by 0 is bad.

				if(a0 instanceof Value.Int && ((Value.Int) a0).value == 0) return a0;
				// if(a1 instanceof Value.Int && ((Value.Int) a1).value == 0) return a1;
				if(a0 instanceof Value.Real && ((Value.Real) a0).value == 0) return a0;
				// if(a1 instanceof Value.Real && ((Value.Real) a1).value == 0) return a1;
				if(a0 instanceof Value.CplxVal && ((Value.CplxVal) a0).value.equals(new Cplx(0))) return a0;
				// if(a1 instanceof Value.CplxVal && ((Value.CplxVal) a1).value.equals(new Cplx(0))) return a1;
				if(a0 instanceof Value.QuatVal && ((Value.QuatVal) a0).value.equals(new Quat(0, 0, 0, 0))) return a0;
				// if(a1 instanceof Value.QuatVal && ((Value.QuatVal) a1).value.equals(new Quat(0, 0, 0, 0))) return a1;

				if(a0 instanceof Value.Int && ((Value.Int) a0).value == 1) return recip.eval(a1);
				if(a1 instanceof Value.Int && ((Value.Int) a1).value == 1) return a0;
				if(a0 instanceof Value.Real && ((Value.Real) a0).value == 1) return recip.eval(a1);
				if(a1 instanceof Value.Real && ((Value.Real) a1).value == 1) return a0;
				if(a0 instanceof Value.CplxVal && ((Value.CplxVal) a0).value.equals(new Cplx(1))) return recip.eval(a1);
				if(a1 instanceof Value.CplxVal && ((Value.CplxVal) a1).value.equals(new Cplx(1))) return a0;
				if(a0 instanceof Value.QuatVal && ((Value.QuatVal) a0).value.equals(new Quat(1, 0, 0, 0))) return recip.eval(a1);
				if(a1 instanceof Value.QuatVal && ((Value.QuatVal) a1).value.equals(new Quat(1, 0, 0, 0))) return a0;
			}

			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	mod(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	pow(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).r(Type.integer).w(Type.real),
			new Signature().r(Type.cplx).r(Type.integer).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.integer).w(Type.quat),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			if(signature.get(1).type == Type.integer) {
				return generateExprCase("ipow", signature, values);
			} else {
				return generateExprCase("pow", signature, values);
			}
		}
	},
	min(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	max(
			new Signature().r(Type.integer).r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return binaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	cons(
			new Signature().r(Type.real).r(Type.real).w(Type.cplx),
			new Signature().r(Type.real).r(Type.real).r(Type.real).r(Type.real).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			// create either cplx or quat
			if(arguments.size() == 2 || arguments.size() == 4) {
				List<Value.Const> consts = constValue(arguments);

				if (consts != null) {
					List<Double> reals = new LinkedList<>();

					for (Value.Const c : consts) {
						if(c.type().canConvertTo(Type.real)) {
							Value.Real v = (Value.Real) c.type().convertTo(c, Type.real);
							reals.add(v.value);
						} else {
							reals = null;
							break;
						}
					}

					if(reals != null) {
						if(reals.size() == 2) {
							return new Value.Const.CplxVal(new Cplx(reals.get(0), reals.get(1)));
						} else {
							// reals.size() == 4..
							return new Value.QuatVal(new Quat(reals.get(0), reals.get(1), reals.get(2), reals.get(3)));
						}
					}
				}
			}

			return new Tree.OpApp(this, arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	neg(
			new Signature().r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	recip(
			new Signature().r(Type.integer).w(Type.real),
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	abs(
			new Signature().r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	sqr(
			new Signature().r(Type.integer).w(Type.integer),
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx),
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	conj(
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	exp(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	log(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	sqrt(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	sin(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	cos(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	tan(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	atan(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	sinh(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	cosh(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	tanh(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	atanh(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	floor(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	ceil(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	fract(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	circlefn(
			new Signature().r(Type.real).w(Type.real),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	real2int(
			new Signature().r(Type.real).w(Type.integer)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},

	// some specials for cplx numbers
	re(
			new Signature().r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	im(
			new Signature().r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	mandelbrot(
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	dot(
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	rad2(
			new Signature().r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	rad(
			new Signature().r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	arc(
			new Signature().r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	arcnorm(
			new Signature().r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	dist2(
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	dist(
			new Signature().r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	rabs(
			new Signature().r(Type.cplx).w(Type.cplx)
			) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	iabs(
			new Signature().r(Type.cplx).w(Type.cplx)
			) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	flip(
			new Signature().r(Type.cplx).w(Type.cplx)
			) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	polar(
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	rect(
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},



	// Graphics primitives

	circle(
			new Signature().r(Type.cplx).r(Type.real).r(Type.cplx).w(Type.real)
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	line(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	segment(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	box(
			new Signature().r(Type.cplx).r(Type.cplx).r(Type.cplx).w(Type.real)
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},

	// Translate coordinates

	map(
			new Signature().r(Type.real).r(Type.real).w(Type.cplx),
			new Signature().r(Type.cplx).w(Type.cplx)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	smooth( // bad smooth function, only kept for compatibility
			new Signature().r(Type.cplx).r(Type.real).r(Type.real).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	smoothen(
			new Signature().r(Type.cplx).r(Type.real).r(Type.real).w(Type.real)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	over(
			new Signature().r(Type.quat).r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	lab2rgb(
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	rgb2lab(
			new Signature().r(Type.quat).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	int2rgb(
			new Signature().r(Type.integer).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	rgb2int(
			new Signature().r(Type.quat).w(Type.integer)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	int2lab(
			new Signature().r(Type.integer).w(Type.quat)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},
	lab2int(
			new Signature().r(Type.quat).w(Type.integer)
	) {
		@NonNull
		@Override
		public Tree eval(List<Tree> arguments) {
			return unaryEval(arguments);
		}

		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase(name(), signature, values);
		}
	},


	// internal instructions start with __ so that they cannot be entered in the parser
	__jump(
			new Signature().label()
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return "pc = " + values.get(0).vmAccessCode(1) + ";";
		}
		// is not linearized since it is automatically added to the program
	},
	__jumprel(
			// this one is fun...
			new Signature().r(Type.integer)
	) {
		// needed for selections
		@Override
		String generateCase(Signature signature, List<Value> values) {
			// 2 + because the instruction and argument take position 0 and 1.
			return "pc = is[pc + 2 + " + values.get(0).vmAccessCode(1) + "];";
		}
		// is not linearized since it is automatically added to the program
	},
	__ld_palette(
			//new Signature().label().r(Type.cplx).w(Type.integer), fixme would be nice but leads to problems. Maybe using a different name?
			new Signature().label().r(Type.cplx).w(Type.quat)
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return generateExprCase("palette_lab", signature, values);
		}
	}



	/*print(
			new Signature().r(Type.integer),
			new Signature().r(Type.real),
			new Signature().r(Type.cplx),
			new Signature().r(Type.quat)
	) {
		@Override
		String generateCase(Signature signature, List<Value> values) {
			return "cout << " + values.get(0).vmAccessCode(1) + " << endl; pc += 2; ";
		}

		@Override
		public void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) {
			Value v = args.get(0).linearizeExpr(null, null, currentScope, program);

			if(v instanceof Value.Reg)
				program.add(this, v);
			else
				throw new CompileException("print does not work here.");
		}
	}*/;

	Signature[] signatures;

	int intOffset = -1;
	int variationCount;

	Op(Signature... signatures) {
		this.signatures = signatures;
		this.variationCount = 0;

		for(Signature signature : signatures()) {
			variationCount += signature.combinationCount();
		}
	}

	/*Tree apply(int...args) {
		return null;
	}

	Tree apply(double...args) {
		return null;
	}

	Tree apply(Cplx...args) {
		return null;
	}

	Tree apply(Quat...args) {
		return null;
	}*/

	Tree eval(Tree...args) {
		System.out.println("OP: Called eval for " + this);
		return eval(Arrays.asList(args));
	}

	@NonNull
	@Override
	public Tree eval(List<Tree> args) /*throws CompileException*/ {
		return new Tree.OpApp(this, args);
	}

	@Override
	public void linearizeStmt(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
		throw new CompileException(this + " cannot head a stmt");
	}

	List<Value.Const> constValue(List<Tree> arguments) {
		// used in cons.
		for(Tree t : arguments) {
			if(!(t instanceof Value.Const)) return null;
		}

		List<Value.Const> consts = new LinkedList<>();

		for(Tree t : arguments) {
			consts.add((Value.Const) t);
		}

		return consts;
	}

	Tree binaryEval(Tree t0, Tree t1) {
		// bring to same type
		if(t0 instanceof Value.Const && t1 instanceof Value.Const) {
			Value.Const a0 = (Value.Const) t0;
			Value.Const a1 = (Value.Const) t1;

			Tree ret = null;

			if(a0.type().canConvertTo(a1.type())) {
				a0 = a0.type.convertTo(a0, a1.type());
				ret = a0.apply(this, a1);
			} else if(a1.type().canConvertTo(a0.type())) {
				a1 = a1.type.convertTo(a1, a0.type());
				ret = a0.apply(this, a1);
			}

			if(ret != null) return ret;
		}

		return new Tree.OpApp(this, Arrays.asList(t0, t1));
	}

	Tree binaryEval(List<Tree> args) {
		if(args.size() < 2) return new Tree.OpApp(this, args); // will cause an error later

		Iterator<Tree> i = args.iterator();

		Tree t0 = i.next();

		while(i.hasNext()) {
			Tree t1 = i.next();
			t0 = binaryEval(t0, t1);
		}

		return t0;
	}

	Tree unaryEval(List<Tree> arguments) {
		if (arguments.size() == 1) {
			Tree t0 = arguments.get(0);
			if(t0 instanceof Value.Const) {
				Tree t = ((Value.Const) t0).apply(this);
				if(t != null) return t;
			}
		}

		return new Tree.OpApp(this, arguments);
	}

	static List<Value> linearizeExprArgs(List<Tree> args, DataScope currentScope, Program program) throws CompileException {
		// We create a new scope so that registers would actually be reused.
		//DataScope argScope = currentScope.newScope();

		// fixme FIND Usages bec

		List<Value> result = new ArrayList<>(args.size() + 2); // we might need additional space for labels/results

		for(Tree arg : args) {
			result.add(arg.linearizeExpr(null, null, currentScope, program));
		}

		return result;
	}

	public void linearizeBool(List<Tree> args, Value.Label trueLabel, Value.Label falseLabel, DataScope currentScope, Program program) throws CompileException {
		// this implementation is suitable for comparisons
		List<Value> as = linearizeExprArgs(args, currentScope.newScope(), program);

		as.add(trueLabel);
		as.add(falseLabel);

		addToProgram(as, currentScope, program);
	}

	@Override
	public Value linearizeExpr(List<Tree> args, Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
		// create out register
		if(null == target) {
			if (targetScope != null) target = new Value.Reg(targetScope, "$$");
			else target = new Value.Reg(currentScope, "$$");
		}

		// FIXME this might use up more registers than necessary in case of conversions.


		// use a new scope for intermediate results
		currentScope = currentScope.newScope();

		List<Value> as = linearizeExprArgs(args, currentScope, program);

		as.add(target);

		// current scope is used if some intermediate conversions require it.
		addToProgram(as, currentScope, program);

		return target;
	}

	void addToProgram(List<Value> args, DataScope currentScope, Program program) throws CompileException {
		Signature signature = Signature.findSignature(this.signatures, args);

		if(signature == null) {
			throw new CompileException("No signature found for " + this + args);
		}

		program.add(this, signature, args, currentScope);
	}


	/*static Value linearizeExprOp(Op op, List<Tree> args, Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) {
		if(args.size() == 2) {
			List<Value> as = linearizeExprArgs(args, currentScope, program);

			if(target == null) {
				// we need to store the result in a register
				target = new Value.Reg(targetScope != null ? targetScope : currentScope);
			}

			// add result register to arguments.
			as.add(target);

			// the next line sets the type of the register.
			program.add(op, as.toArray(new Value[as.size()]));

			return target;
		} else {
			throw new CompileException(op + " must have two argument!");
		}
	}

	static Value linearizeExprOp(Op op, List<Tree> args, Value.Reg target, DataScope targetScope, DataScope currentScope, Program program) {
		if(args.size() == 1) {
			List<Value> as = linearizeExprArgs(args, currentScope, program);

			if(target == null) {
				target = new Value.Reg(targetScope != null ? targetScope : currentScope);
			}

			as.add(target);

			program.add(op, as.toArray(new Value[as.size()]));

			return target;
		} else {
			throw new CompileException(op + " must have one argument!");
		}
	}*/

	public Signature[] signatures() {
		return signatures;
	}

	public int intOffset() {
		if(intOffset == -1) {
			if(ordinal() == 0) intOffset = 0;
			else {
				// add to previous intOffset the number of possible parameter combinations.
				Op previous = values()[ordinal() - 1];
				intOffset = previous.intOffset() + previous.variationCount;
			}
		}
		return intOffset;
	}

	public String genereateInterpreter() {
		try {
		// definitely not the fastest, but this is not executed in the program but only in the build-process.
		// Hence, it is of no importance if this takes 2 seconds and is not super-memory-efficient.
		if(signatures.length == 0) return "";

		int index = intOffset();

		StringBuilder sb = new StringBuilder();
		sb.append("\t\t\t///// ").append(this.name()).append("[").append(variationCount).append("]\n");

		for(Signature signature : signatures) {
			int combinationCount = signature.combinationCount();
			for (int j = 0; j < combinationCount; ++j) {
				List<Value> values = signature.valuesFromIndex(j);

				// now we have the values. Generate C-code out of them.

				sb.append("\t\t\tcase ").append(index + j).append(": ").append("/* ");

				for(Value v : values) {
					if(v instanceof Value.Reg) {
						sb.append("reg[").append(v.type).append("]");
					} else {
						sb.append(v.type);
					}

					sb.append(" ");
				}

				sb.append("*/\t");
				sb.append(generateCase(signature, values));
				sb.append(" break;\n");
			}

			index += combinationCount;
		}

		sb.append("\n");

		return sb.toString();
		} catch (CompileException e) {
			e.printStackTrace();
			throw new IllegalArgumentException(e.getMessage()); // should not happen.
		}

	}

	abstract String generateCase(Signature signature, List<Value> values);

	String generateExprCase(String cOp, Signature signature, List<Value> values) {
		int resultIndex = values.size() - 1;
		// values[] = values[0] + values[1].


		// Steps:
		// 1. calculate Argument-Indices
		int indices[] = new int[values.size() + 1];

		indices[0] = 1; // instruction takes the first byte.

		int i = 1;
		for(Value v : values) {
			indices[i] = indices[i - 1] + v.vmCodeSize();
			i++;
		}

		int instructionSize = indices[values.size()];

		// 2. get C-code that converts values.get(i).vmAccessCode(argumentIndex) to signature.get(i)
		List<String> cArgConversion = Value.vmAccessCodes(values, indices); // before: signature.cAccessCodes(values, indices);

		// 3. create code to add both values
		String instruction = cOp + "(";

		// we have to ignore the last element because it is the result.
		Iterator<String> it = cArgConversion.iterator();
		boolean firstElement = true;

		while(it.hasNext()) {
			String n = it.next();

			if(it.hasNext()) {
				if(!firstElement) instruction += ", ";
				else firstElement = false;

				instruction += n;
			}
		}

		instruction += ")";

		// 4. get C-code that converts result from (!) signature.get(2) to values.get(2)

		// FIXME FIXME FIXME (Not needed anymore because now the types match)
		String result = instruction;// signature.get(resultIndex).type.convertToInVM(signature.get(resultIndex).type, instruction);
		//cConvertTo(values.get(resultIndex), instruction);

		// 6. assign to values.get(2).vmAccessCode(argumentIndex).
		return values.get(resultIndex).vmAccessCode(indices[resultIndex]) + " = " + result + "; pc += " + instructionSize + ";";
	}


	/**
	 * To generate the C-code for the renderscript VM
	 * @param infixCOp
	 * @param signature
	 * @param values
     * @return
     */
	String generateCmpCase(String infixCOp, Signature signature, List<Value> values) {
		// Steps:
		// 1. calculate Argument-Indices
		int indices[] = new int[values.size() + 1];

		indices[0] = 1; // instruction takes the first byte.

		int i = 1;
		for(Value v : values) {
			indices[i] = indices[i - 1] + v.vmCodeSize();
			i++;
		}

		int instructionSize = indices[values.size()];

		// 2. get C-code that converts values.get(i).vmAccessCode(argumentIndex) to signature.get(i)
		List<String> cArgConversion = Value.vmAccessCodes(values, indices); // before signature.cAccessCodes(values, indices);

		// 3. create code to add both values
		return "pc = (" + cArgConversion.get(0) + " " + infixCOp + " " + cArgConversion.get(1) + ") ? "
				+ values.get(2).vmAccessCode(indices[2]) + " : " + values.get(3).vmAccessCode(indices[3]) + "; ";
	}

	public String title() {
		throw new UnsupportedOperationException();
	}

	public String description() {
		throw new UnsupportedOperationException();
	}

}
