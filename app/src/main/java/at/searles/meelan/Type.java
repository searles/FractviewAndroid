package at.searles.meelan;

import at.searles.math.Cplx;
import at.searles.math.Quat;

/*
Some thoughts of type-checks in this app:

I have statements, bools and exprs. Putting type-check into inline is difficult
because I allow tree transformations that are not type compatible (add with 3 arguments for instance).
Furthermore, what is the type of ifOp? I would need genericity.

Putting eval into linearizeExpr is also not a good idea because I could not do tree transformations in another way.
Thus the type checks are done only in linearizeExpr.
 */

public enum Type {
	bool {
		@Override
		public int size() {
			throw new IllegalArgumentException();
		}

		@Override
		public String typeNameInVM() {
			throw new IllegalArgumentException();
		}

		@Override
		public Value createConst() {
			throw new IllegalArgumentException();
		}

		@Override
		public boolean canConvertTo(Type type) {
			return false;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			throw new IllegalArgumentException();
		}
	},
	string {
		@Override
		public int size() {
			throw new IllegalArgumentException();
		}

		@Override
		public String typeNameInVM() {
			throw new IllegalArgumentException();
		}

		@Override
		public Value createConst() {
			throw new IllegalArgumentException();
		}

		@Override
		public boolean canConvertTo(Type type) {
			return false;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			throw new IllegalArgumentException();
		}
	},
	label {
		@Override
		public int size() {
			return 1;
		}

		@Override
		public String typeNameInVM() {
			return "int";
		}

		@Override
		public Value createConst() {
			return new Value.Label();
		}

		/*@Override
		public String convertToInVM(Type t, String accessCode) {
			return null;
		}*/

		@Override
		public boolean canConvertTo(Type type) {
			return type == label;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			assert dstType == this;
			return src;
		}
	},
	integer {
		@Override
		public int size() {
			return 1;
		}

		@Override
		public String typeNameInVM() {
			return "int";
		}

		@Override
		public Value createConst() {
			return new Value.Const.Int(0);
		}

		/*@Override
		public String convertToInVM(Type t, String accessCode) {
			switch(t) {
				case integer: return accessCode;
				case real: return "(double)" + accessCode;
				case cplx: return "(double2) {" + accessCode + ", 0.}";
				case quat: return "(double4) {" + accessCode + ", 0., 0., 0.}";
			}
			throw new IllegalArgumentException("bug in type checking");
		}*/

		@Override
		public boolean canConvertTo(Type type) {
			return type == integer || type == real || type == cplx || type == quat;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			Value.Int v = (Value.Int) src;
			switch(dstType) {
				case integer: return v;
				case real: return new Value.Real(v.value);
				case cplx: return new Value.CplxVal(new Cplx(v.value, 0));
				case quat: return new Value.QuatVal(new Quat(v.value, 0, 0, 0));
			}

			throw new IllegalArgumentException();
		}
	}, // integer
	real {
		@Override
		public int size() {
			return 2;
		}

		@Override
		public String typeNameInVM() {
			return "double";
		}

		@Override
		public Value createConst() {
			return new Value.Const.Real(0);
		}

		/*@Override
		public String convertToInVM(Type t, String accessCode) {
			switch(t) {
				case real: return accessCode;
				case cplx: return "(double2) {" + accessCode + ", 0.}";
				case quat: return "(double4) {" + accessCode + ", 0., 0., 0.}";
			}
			throw new IllegalArgumentException("bug in type checking: " + this + " and " + t);
		}*/

		@Override
		public boolean canConvertTo(Type type) {
			return type == real || type == cplx || type == quat;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			Value.Real v = (Value.Real) src;
			switch(dstType) {
				case real: return v;
				case cplx: return new Value.CplxVal(new Cplx(v.value, 0));
				case quat: return new Value.QuatVal(new Quat(v.value, 0, 0, 0));
			}

			throw new IllegalArgumentException();
		}
	},
	cplx {
		@Override
		public int size() {
			return 4;
		}

		@Override
		public String typeNameInVM() {
			return "double2";
		}

		@Override
		public Value createConst() {
			return new Value.Const.CplxVal(new Cplx(0));
		}

		/*@Override
		public String convertToInVM(Type t, String accessCode) {
			switch(t) {
				case cplx: return accessCode;
			}
			throw new IllegalArgumentException("bug in type checking");
		}*/

		@Override
		public boolean canConvertTo(Type type) {
			return type == cplx || type == quat;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			Value.CplxVal v = (Value.CplxVal) src;
			switch(dstType) {
				case cplx: return v;
				case quat: return new Value.QuatVal(new Quat(v.value.re(), v.value.im(), 0, 0));
			}

			throw new IllegalArgumentException();
		}
	},
	quat {
		@Override
		public int size() {
			return 8;
		}

		@Override
		public String typeNameInVM() {
			return "double4";
		}

		@Override
		public Value createConst() {
			return new Value.QuatVal(new Quat(0, 0, 0, 0));
		}

		/*@Override
		public String convertToInVM(Type t, String accessCode) {
			switch(t) {
				case quat: return accessCode;
			}
			throw new IllegalArgumentException("bug in type checking");
		}*/

		@Override
		public boolean canConvertTo(Type type) {
			return type == quat;
		}

		@Override
		public Value.Const convertTo(Value.Const src, Type dstType) {
			Value.QuatVal v = (Value.QuatVal) src;
			switch(dstType) {
				case quat: return v;
			}

			throw new IllegalArgumentException();
		}
	};

	public static Type get(String t) {
		switch (t) {
			case "int":
				return Type.integer;
			case "realf":
				return Type.real; // fixme depr
			case "real":
				return Type.real;
			case "cplxf":
				return Type.cplx; // fixme depr
			case "cplx":
				return Type.cplx;
			case "quadf":
				return Type.quat; // fixme depr
			case "quad":
				return Type.quat; // fixme depr
			case "quat":
				return Type.quat;
			default:
				return null;
		}
	}

	/**
	 * Size of type in integers
	 * @return
	 */
	public abstract int size();

	/**
	 * Returns the name of this type in the VM (optional)
	 * @return
	 */
	public abstract String typeNameInVM();

	public Value createReg() throws CompileException {
		Value.Reg reg = new Value.Reg(null, "$$");
		reg.initType(this);
		return reg;
	}

	public abstract Value createConst();

	//public abstract String convertToInVM(Type t, String accessCode);

	public abstract boolean canConvertTo(Type type);

	/**
	 * @param src some const of type 'this'.
	 * @param dstType dstType.
	 * @return
	 */
	public abstract Value.Const convertTo(Value.Const src, Type dstType);
}
