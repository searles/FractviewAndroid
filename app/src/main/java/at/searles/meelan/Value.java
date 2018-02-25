package at.searles.meelan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import at.searles.math.Cplx;
import at.searles.math.Quat;

/**
 * This class represents (possibly read-only-) values that can be encoded as an integer.
 * It is an abstract class because of static methods
 */
public abstract class Value extends Tree {

	Type type; // may be initialized only later [eg in registers], hence be prepared to find null in here.

	protected Value(Type type) {
		this.type = type; // may be null!
	}

	/**
	 *
	 * @return the integers that are generated out of this value for the final bytecode
	 */
	public abstract int[] generateVMCode();

	/**
	 * @return Number of integers that are occupied by this value.
	 */
	public abstract int vmCodeSize(); // numbers of integers occupied by this in instruction

	/**
	 * To generate the C-interpreter
	 * @param argIndex starting index of the integer relative to the program counter
	 * @return C-Code that fetches this value.
	 */
	public abstract String vmAccessCode(int argIndex);

	/**
	 * Returns the C-Code to access these elements and converts all read-values
	 * to their proper type in C-code.
	 * @param values
	 * @param argIndices
	 * @return
	 */
	public static List<String> vmAccessCodes(List<Value> values, int[] argIndices) {
		// c-code to convert values.get(i) to type signature.get(i)

		// all dss are same size except for argIndices (one more for the final size)
		Iterator<Value> itV = values.iterator();

		List<String> ret = new ArrayList<>(values.size());

		for(int i = 0; itV.hasNext(); ++i) {
			Value v = itV.next();
			ret.add(v.vmAccessCode(argIndices[i]));
		}

		return ret;
	}


	public abstract Type type();

	/**
	 * For struct-like access.
	 * @param id Id of the accessor
	 * @return
	 * @throws CompileException
	 */
	public Value subitem(String id) throws CompileException {
		throw new CompileException(this + " has no element " + id).addTraceElement(this);
	}

	@Override
	public Tree inline(ScopeTable table, boolean maskVars) {
		return this;
	}

	@Override
	public void linearizeStmt(DataScope currentScope, Program program) throws CompileException {
		throw new CompileException(this + " is not a statement").addTraceElement(this);
	}

	@Override
	public void linearizeBool(Label trueLabel, Label falseLabel, DataScope currentScope, Program program) throws CompileException {
		// fixme well, it may be...
		throw new CompileException(this + " is not a bool").addTraceElement(this);
	}

	@Override
	public Value linearizeExpr(Reg target, DataScope targetScope, DataScope currentScope, Program program) throws CompileException {
		if(target != null || targetScope != null) {
			if(target == null) target = new Reg(targetScope, "$$");
			// if it must be copied to a specific register
			// FIXME could be easier.
			return Op.mov.linearizeExpr(Collections.singletonList((Tree) this), target, targetScope, currentScope, program);
		} else {
			return this;
		}
	}

	/**
	 * Returns the starting indices of the values in the parameter.
	 * @param values arguments
	 * @return array of indices
	 */
	public static int[] argIndices(List<Value> values) {
		int[] argIndices = new int[values.size() + 1];

		argIndices[0] = 1;
		for (int i = 1; i < values.size() + 1; ++i) argIndices[i] = argIndices[i - 1] + values.get(i - 1).vmCodeSize();

		return argIndices;
	}

	public abstract Value addConversion(Sort sort, DataScope currentScope, Program program) throws CompileException;


	@Override
	public void exportEntries(ExternalData data) throws CompileException {
		// do nothing
	}

	/**
	 * Class for labels in the program.
	 * The position is set later.
	 */
	public static class Label extends Const {

		int position;

		public Label() {
			super(Type.label);
			this.position = -1; // default for label meaning that it will be initialized later.
		}

		public Label(int position) {
			this();
			this.position = position;
		}

		public String toString() {
			return String.format("l[%04d]", position);
		}

		@Override
		public int[] generateVMCode() {
			if(position == -1) throw new IllegalArgumentException("all labels must be set to an appropriate position!");
			return new int[]{position};
		}

		@Override
		public int vmCodeSize() {
			return 1; // labels are one integer containing an address
		}

		@Override
		public String vmAccessCode(int argIndex) {
			return "is[pc + " + argIndex + "]"; // just simply take the next integer
		}

		@Override
		public Const apply(Op op, Const a1) {
			return null;
		}

		@Override
		public Const apply(Op op) {
			return null;
		}

		@Override
		public Type type() {
			return Type.label;
		}

		@Override
		public Value addConversion(Sort sort, DataScope currentScope, Program program) {
			if(sort.type != Type.label) throw new IllegalArgumentException();
			return this;
		}

		public void setPosition(int pos) {
			// post-initializing label indices
			if (position != -1) {
				throw new IllegalArgumentException("position already set");
			}

			this.position = pos;
		}
	}


	/**
	 * These instances of Value represent variables.
	 */
	public static class Reg extends Value {

		static int regCount = 0;

		//final int index;
		//final Scope scope;

		// Scope in which this register resides
		DataScope dataScope = null;

		int dataIndex = -1; // index in data array

		String id; // for debugging

		public Reg(DataScope scope, String id) {
			super(null); // will be initialized later.
			this.dataScope = scope;

			// id is just for debugging.
			this.id = id + "[" + (regCount++) + "]";
		}

		public String toString() {
			String s = "reg";
			if(type != null) s += ":" + type;
			if(dataIndex != -1) s += "[" + dataIndex + "]";
			return s;
		}

		public Value subitem(String id) throws CompileException {
			// FIXME: Hardcoded, well, not so nice...
			if(type == Type.cplx) {
				if(id.equals("x")) {
					Reg subReg = new Reg(null, id);
					subReg.type = Type.real;
					subReg.dataIndex = dataIndex;
					return subReg;
				} else if(id.equals("y")) {
					Reg subReg = new Reg(null, id);
					subReg.type = Type.real;
					subReg.dataIndex = dataIndex + 2; // because one double takes two ints.
					return subReg;
				}
			} else if(type == Type.quat) {
				switch (id) {
					case "a": {
						Reg subReg = new Reg(null, id);
						subReg.type = Type.real;
						subReg.dataIndex = dataIndex;
						return subReg;
					}
					case "b": {
						Reg subReg = new Reg(null, id);
						subReg.type = Type.real;
						subReg.dataIndex = dataIndex + 2;
						return subReg;
					}
					case "c": {
						Reg subReg = new Reg(null, id);
						subReg.type = Type.real;
						subReg.dataIndex = dataIndex + 4;
						return subReg;
					}
					case "d": {
						Reg subReg = new Reg(null, id);
						subReg.type = Type.real;
						subReg.dataIndex = dataIndex + 6;
						return subReg;
					}
				}
			}

			return super.subitem(id); // may throw execption
		}

		@Override
		public Value addConversion(Sort sort, DataScope scope, Program program) throws CompileException {
			if(sort.type == type) return this; // no need to convert.

			// target scope must not be null here!
			Reg target = new Reg(scope, "$$");
			target.initType(sort.type);

			try {
				Op.mov.addToProgram(Arrays.asList(this, target), null, program);
			} catch (CompileException e) {
				throw new IllegalArgumentException("this should not happen");
			}


			return target;
		}

		@Override
		public Type type() {
			return type;
		}

		@Override
		public int[] generateVMCode() {
			if(dataIndex == -1) throw new IllegalArgumentException("uninitialized register");
			return new int[]{dataIndex}; // the type is stored in the instruction name.
		}

		@Override
		public int vmCodeSize() {
			return 1; // registers have size 1 (address in the data-array)
		}

		@Override
		public String vmAccessCode(int argIndex) {
			// generate C-code to get this variable.
			return "(* (" + type.typeNameInVM() + "*) &data[is[pc + " + argIndex + "]])";
		}

		public void initDataScope(DataScope scope) throws CompileException {
			// The data scope is initialized once a register declaration is reached.
			if(dataScope == null) {
				this.dataScope = scope;

				if (type != null) {
					dataIndex = dataScope.nextDataIndex(type);
				}
			} else {
				throw new IllegalArgumentException("Register " + id + " initialized twice!");
			}
		}

		static int MAX_MEM = 0;

		/**
		 * Sets the type of the register. This must happen before any other scope is entered, so right
		 * after the variable declaration has been taken care of. If this does not happen then, then two
		 * variables might share the same register leading to undetermined behaviour!
		 * @param type Type with which the variable should be initialized. Must not be null.
		 */
		public boolean initType(Type type) throws CompileException {
			if(type == null) return false;

			if (this.type != null) {
				// if it is al
				if(this.type != type) return false;
				// otherwise everything is fine.
			} else {
				// set type of register
				this.type = type;

				if(dataIndex == -1 && dataScope != null) {
					dataIndex = dataScope.nextDataIndex(type);
				}
			}

			return true;
		}
	}


	/**
	 * Represents constants, the counterpart of registers.
	 */
	public abstract static class Const extends Value {

		protected Const(Type type) {
			super(type);
			if(type == null) throw new NullPointerException();
		}

		@Override
		public Type type() {
			return type;
		}

		@Override
		public int vmCodeSize() {
			return type.size();
		}

		@Override
		public String vmAccessCode(int argIndex) {
			return "(* (" + type.typeNameInVM() + "*) &is[pc + " + argIndex + "])"; // hardcoded inside the code segment
			// should be slightly faster this way because of cache lines
		}

		public abstract Const apply(Op op, Const a1);

		public abstract Const apply(Op op);

		@Override
		public Value addConversion(Sort sort, DataScope currentScope, Program program) {
			return type.convertTo(this, sort.type);
		}

		/*public Const convertTo(Type tokenPosition) {
			switch(type) {
				case i:
					switch (tokenPosition) {
						case i:
							return this;
						case f:
							return Const.flt(data[0]);
						case d:
							return Const.dbl(data[0]);
					}
				case f:
					switch (tokenPosition) {
						case f:
							return this;
						case d:
							return Const.dbl(itof(data[0]));
					}
				case d:
					switch (tokenPosition) {
						case d:
							return this;
					}
				case f2:
					switch (tokenPosition) {
						case f2:
							return this;
					}
				case f4:
					switch (tokenPosition) {
						case f4:
							return this;
					}
				case d2:
					switch (tokenPosition) {
						case d2:
							return this;
						default:
					}
			}

			throw new CompileException("cannot convert " + type + " to " + tokenPosition);
		}*/
	}

	/*public static interface IntConst {
		Int intConst();
	}

	public static interface RealConst {
		Real realConst();
	}

	public static interface CplxConst {
		CplxVal cplxConst();
	}

	public static interface QuatConst {
		QuatVal quatConst();
	}*/

	public static class Int extends Const /*implements IntConst, RealConst, CplxConst, QuatConst*/ {

		public int value;

		public Int(int value) {
			super(Type.integer);
			this.value = value;
		}

		@Override
		public Const apply(Op op, Const a1) {
			// type check is done before
			int a = ((Int) a1).value;
			switch(op) {
				case add: return new Int(this.value + a);
				case sub: return new Int(this.value - a);
				case mul: return new Int(this.value * a);
				case mod: return a == 0 ? null : new Int(this.value % a); // fixme div by 0?
				case min: return new Int(Math.min(this.value, a));
				case max: return new Int(Math.max(this.value, a));
				case g: return new Bool(value > a);
				case ge: return new Bool(value >= a);
				case eq: return new Bool(value == a);
				case ne: return new Bool(value != a);
				case le: return new Bool(value <= a);
				case l: return new Bool(value < a);
				default:
					return Real.applyBinary(op, this.value, a);
			}
		}

		@Override
		public Const apply(Op op) {
			switch(op) {
				case neg: return new Int(-this.value);
				case abs: return new Int(Math.abs(value));
				case sqr: return new Int(value * value);
				default:
					return Real.applyUnary(op, value);
			}
		}

		@Override
		public int[] generateVMCode() {
			return new int[]{ value };
		}

		@Override
		public String toString() {
			return "int[" + value + "]";
		}
/*
		@Override
		public Int intConst() {
			return this;
		}

		@Override
		public Real realConst() {
			return new Real(value);
		}

		@Override
		public CplxVal cplxConst() {
			return new CplxVal(new Cplx(value, 0));
		}

		@Override
		public QuatVal quatConst() {
			return new QuatVal(new Quat(value, 0, 0, 0));
		}*/
	}


	public static class Real extends Const /*implements RealConst, CplxConst, QuatConst*/ {

		public static int[] dtoi(double d) {
			long l = Double.doubleToRawLongBits(d);
			// beware of big endian systems [are there any?]
			return new int[]{(int) (l & 0x0ffffffffl), (int) (l >> 32)};
		}

		public double value;

		public Real(double value) {
			super(Type.real);
			this.value = value;
		}

		@Override
		public int[] generateVMCode() {
			return dtoi(value);
		}

		@Override
		public String toString() {
			return "real[" + value + "]";
		}

		static Const applyBinary(Op op, double a0, double a1) {
			switch(op) {
				case add: return new Real(a0 + a1);
				case sub: return new Real(a0 - a1);
				case mul: return new Real(a0 * a1);
				case div: return new Real(a0 / a1);
				case mod: return new Real(a0 % a1);
				case pow: return new Real(Math.pow(a0, a1));
				case min: return new Real(Math.min(a0, a1));
				case max: return new Real(Math.max(a0, a1));
				case g: return new Bool(a0 > a1);
				case ge: return new Bool(a0 >= a1);
				case eq: return new Bool(a0 == a1);
				case ne: return new Bool(a0 != a1);
				case le: return new Bool(a0 <= a1);
				case l: return new Bool(a0 < a1);
				default:
					return CplxVal.applyBinary(op, new Cplx(a0), new Cplx(a1));
			}
		}

		static Const applyUnary(Op op, double a) {
			switch(op) {
				case neg: return new Real(-a);
				case recip: return new Real(1. / a);
				case abs: return new Real(Math.abs(a));
				case sqr: return new Real(a * a);
				case sqrt: return new Real(Math.sqrt(a));
				case log: return new Real(Math.log(a));
				case exp: return new Real(Math.exp(a));
				case sin: return new Real(Math.sin(a));
				case cos: return new Real(Math.cos(a));
				case tan: return new Real(Math.tan(a));
				case atan: return new Real(Math.atan(a));
				case sinh: return new Real(Math.sinh(a));
				case cosh: return new Real(Math.cosh(a));
				case tanh: return new Real(Math.tanh(a));
				// fixme case atanh: return new Real(Math.atanh);
				case floor: return new Real(Math.floor(a));
				case ceil: return new Real(Math.ceil(a));
				case fract: return new Real(a - Math.floor(a));
				default:
					return CplxVal.applyUnary(op, new Cplx(a, 0));
			}
		}


		@Override
		public Const apply(Op op, Const a1) {
			return applyBinary(op, this.value, ((Real) a1).value);
		}

		@Override
		public Const apply(Op op) {
			return applyUnary(op, this.value);
		}

		/*@Override
		public Real realConst() {
			return this;
		}

		@Override
		public CplxVal cplxConst() {
			return new CplxVal(new Cplx(value, 0));
		}

		@Override
		public QuatVal quatConst() {
			return new QuatVal(new Quat(value, 0, 0, 0));
		}*/
	}


	public static class CplxVal extends Const /*implements CplxConst, QuatConst*/ {

		public Cplx value;

		public CplxVal(Cplx value) {
			super(Type.cplx);
			this.value = value;
		}

		public Value subitem(String id) throws CompileException {
			if(id.equals("x")) {
				return new Real(value.re());
			} else if(id.equals("y")) {
				return new Real(value.im());
			}

			return super.subitem(id);
		}

		@Override
		public int[] generateVMCode() {
			int[] res = Real.dtoi(value.re());
			int[] ims = Real.dtoi(value.im());
			return new int[]{ res[0], res[1], ims[0], ims[1] };
		}

		@Override
		public String toString() {
			return "cplx[" + value + "]";
		}

		static Const applyBinary(Op op, Cplx a0, Cplx a1) {
			// FIXME switch to op.apply(a0, a1);

			switch(op) {
				case add: return new CplxVal(new Cplx().add(a0, a1));
				case sub: return new CplxVal(new Cplx().sub(a0, a1));
				case mul: return new CplxVal(new Cplx().mul(a0, a1));
				case div: return new CplxVal(new Cplx().div(a0, a1));
				case mod: return new CplxVal(new Cplx().mod(a0, a1));
				case pow: return new CplxVal(new Cplx().pow(a0, a1));
				case min: return new CplxVal(new Cplx().min(a0, a1));
				case max: return new CplxVal(new Cplx().max(a0, a1));
				default:
					return QuatVal.applyBinary(op, new Quat(a0.re(), a0.im(), 0, 0), new Quat(a1.re(), a1.im(), 0, 0));
			}
		}

		static Const applyUnary(Op op, Cplx a) {
			switch(op) {
				case neg: return new CplxVal(new Cplx().neg(a));
				case recip: return new CplxVal(new Cplx().rec(a));
				case abs: return new CplxVal(new Cplx().abs(a));
				case conj: return new CplxVal(new Cplx().conj(a));
				case sqr: return new CplxVal(new Cplx().sqr(a));
				case sqrt: return new CplxVal(new Cplx().sqrt(a));
				case log: return new CplxVal(new Cplx().log(a));
				case exp: return new CplxVal(new Cplx().exp(a));
				case sin: return new CplxVal(new Cplx().sin(a));
				case cos: return new CplxVal(new Cplx().cos(a));
				case tan: return new CplxVal(new Cplx().tan(a));
				case atan: return new CplxVal(new Cplx().atan(a));
				case sinh: return new CplxVal(new Cplx().sinh(a));
				case cosh: return new CplxVal(new Cplx().cosh(a));
				case tanh: return new CplxVal(new Cplx().tanh(a));
				// fixme case atanh: return new Real(Math.atanh);
				case floor: return new CplxVal(new Cplx().floor(a));
				case ceil: return new CplxVal(new Cplx().ceil(a));
				case fract: return new CplxVal(new Cplx().fract(a));
				case re: return new Real(a.re());
				case im: return new Real(a.im());
				// fixme case mandelbrot: return new CplxVal(new Cplx().fract(a));
				// case dot: return new Real(new Cplx().fract(a));
				case rad2: return new Real(a.rad2());
				case rad: return new Real(a.rad());
				case arc: return new Real(a.arc());
				case arcnorm: return new Real(a.arc() / (2 * Math.PI));

				default:
					return QuatVal.applyUnary(op, new Quat(a.re(), a.im(), 0, 0));
			}
		}


		@Override
		public Const apply(Op op, Const a1) {
			return applyBinary(op, this.value, ((CplxVal) a1).value);
		}

		@Override
		public Const apply(Op op) {
			return applyUnary(op, this.value);
		}

		/*@Override
		public CplxVal cplxConst() {
			return this;
		}

		@Override
		public QuatVal quatConst() {
			return new QuatVal(new Quat(value.re, value.im, 0, 0));
		}*/
	}

	public static class QuatVal extends Const /*implements QuatConst*/ {

		Quat value;

		public QuatVal(Quat value) {
			super(Type.quat);
			this.value = value;
		}

		public Value subitem(String id) throws CompileException {
			switch (id) {
				case "a":
					return new Real(value.s0());
				case "b":
					return new Real(value.s1());
				case "c":
					return new Real(value.s2());
				case "d":
					return new Real(value.s3());
			}

			return super.subitem(id);
		}

		@Override
		public int[] generateVMCode() {
			int[] s0s = Real.dtoi(value.s0());
			int[] s1s = Real.dtoi(value.s1());
			int[] s2s = Real.dtoi(value.s2());
			int[] s3s = Real.dtoi(value.s3());
			return new int[]{
					s0s[0], s0s[1], s1s[0], s1s[1],
					s2s[0], s2s[1], s3s[0], s3s[1] };
		}

		@Override
		public String toString() {
			return "quat[" + value + "]";
		}


		@Override
		public Const apply(Op op, Const a1) {
			return applyBinary(op, this.value, ((QuatVal) a1).value);
		}

		@Override
		public Const apply(Op op) {
			return applyUnary(op, this.value);
		}

		public static Const applyBinary(Op op, Quat a0, Quat a1) {
			switch(op) {
				case add: return new QuatVal(new Quat().add(a0, a1));
				case sub: return new QuatVal(new Quat().sub(a0, a1));
				case mul: return new QuatVal(new Quat().mul(a0, a1));
				case div: return new QuatVal(new Quat().div(a0, a1));
				case mod: return new QuatVal(new Quat().mod(a0, a1));
				case pow: return new QuatVal(new Quat().pow(a0, a1));
				case min: return new QuatVal(new Quat().min(a0, a1));
				case max: return new QuatVal(new Quat().max(a0, a1));
				default:
			}

			return null;
		}

		public static Const applyUnary(Op op, Quat a) {
			switch(op) {
				case neg: return new QuatVal(new Quat().neg(a));
				case recip: return new QuatVal(new Quat().rec(a));
				case abs: return new QuatVal(new Quat().abs(a));
				// not allowed case conj: return new QuatVal(new Quat().conj(value));
				case sqr: return new QuatVal(new Quat().sqr(a));
				default:
			}

			return null;
		}

		/*@Override
		public QuatVal quatConst() {
			return this;
		}*/
	}

	public static class Bool extends Const {
		public boolean value;

		public Bool(boolean value) {
			super(Type.bool);
			this.value = value;
		}

		/*@Override
		public Value linearizeExpr(Reg target, DataScope targetScope, DataScope currentScope, Program program) {
			throw new CompileException("a bool is not an expr");
		}*/

		@Override
		public void linearizeBool(Label trueLabel, Label falseLabel, DataScope currentScope, Program program) throws CompileException {
			Op.__jump.addToProgram(Collections.singletonList((Value) (value ? trueLabel : falseLabel)), currentScope, program);
		}

		@Override
		public Const apply(Op op, Const a1) {
			boolean a = ((Bool) a1).value;

			switch(op) {
				case and: return new Bool(value && a);
				case or: return new Bool(value || a);
			}

			return null;
		}

		@Override
		public Const apply(Op op) {
			if(op == Op.not) {
				return new Bool(!value);
			}

			return null;
		}

		@Override
		public int[] generateVMCode() {
			// fixme check
			throw new IllegalArgumentException("cannot convert a bool to byte code. This might be a bug or someone wrote true + 1.");
		}

		public String toString() {
			return "bool[" + value + "]";
		}
	}

	public static class StringVal extends Const {

		public final String value;

		public StringVal(String value) {
			super(Type.string);
			this.value = value;
		}

		@Override
		public Const apply(Op op, Const arg) {
			// FIXME well, I can allow some string operations...
			return null;
		}

		@Override
		public Const apply(Op op) {
			// FIXME and, unary operations, I don't know...
			return null;
		}

		@Override
		public int[] generateVMCode() {
			// FIXME which exception?
			throw new IllegalArgumentException("cannot convert a string to byte code. This might be a bug or someone wrote true + 1.");
		}

		public String toString() {
			return "string[" + value + "]";
		}
	}
}
