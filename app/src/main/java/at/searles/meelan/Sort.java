package at.searles.meelan;

public class Sort {


	public static enum Permission { c, r, rw, w };

	public final Type type;
	public final Permission p;

	public Sort(Type type, Permission p) {
		this.type = type;
		this.p = p;
	}

	/**
	 * Counts how many cases we have for this sort (types of registers, constants...)
	 * @return
	 */
	public int countCases() {
		switch(p) {
			case c: return 1; // only const
			case r: return 2; // const + reg
			case rw: return 1; // only reg
			case w: return 1; // only reg
		}

		throw new IllegalArgumentException();
	}



	/**
	 * Each of the cases corresponds to an index. This method
	 * returns, which index we have when we convert value to this sort.
	 *
	 * @param value
	 * @return -1 if value does not match converted this sort
	 */
	public int caseIndex(Value value) {
		if(value.type() == type) {
			switch (p) {
				case c:
					if(value instanceof Value.Const) return 0; else break;// only const
				case r:
					if(value instanceof Value.Const) return 0; else if(value instanceof Value.Reg) return 1; else break;
				case rw:
					if(value instanceof Value.Reg) return 0; else break;// only reg
				case w:
					if(value instanceof Value.Reg) return 0; else break;// only reg
			}
		}

		throw new IllegalArgumentException("case index: " + this + "(" + value + ")");
	}

	/**
	 * This is a bit more general and it is closely connected to 'addConversionCode'
	 * @param v
	 * @return
	 */
	public boolean matches(Value v) {
		switch(p) {
			case c:
				return v instanceof Value.Const && v.type().canConvertTo(type);
			case r:
				return v.type() != null && v.type().canConvertTo(type);
			case rw:
				return v instanceof Value.Reg && v.type() == type;
			case w:
				return v instanceof Value.Reg && (v.type() == null || type.canConvertTo(v.type()));
			default:
				// this is close to impossible.
				throw new IllegalArgumentException();
		}
	}

	/**
	 * No conversion here.
	 * @param v
	 * @return
	 */
	public boolean matchesStrict(Value v) {
		switch(p) {
			case c:
				return v instanceof Value.Const && v.type().canConvertTo(type);
			case r:
				return v.type() != null && v.type().canConvertTo(type);
			case rw:
				return v instanceof Value.Reg && v.type() == type;
			case w:
				return v instanceof Value.Reg && (v.type() == null || v.type() == type);
			default:
				// this is close to impossible.
				throw new IllegalArgumentException();
		}
	}

	/**
	 * Creates a sample value for the given index. This is the inverse of caseIndex(value).
	 * This is needed to generate the VM (saves some trouble implementing value.size and other methods
	 * twice).
	 * @param caseIndex
	 * @return
	 */
	public Value caseValue(int caseIndex) throws CompileException {
		if((p == Permission.c || p == Permission.r) && caseIndex == 0) {
			return type.createConst();
		} else if((p == Permission.r && caseIndex == 1) || caseIndex == 0) {
			return type.createReg();
		}

		throw new IllegalArgumentException();
	}

	public String toString() {
		return type + ":" + p;
	}

	/*public static class TypedSort implements Sort {
		Type t;
		Permissions p;

		TypedSort(Type t, Permissions p) {
			this.t = t;
			this.p = p;
		}

		@Override
		public int caseIndex(Value v) {
			Value.Typed value = (Value.Typed) v;

			// types were checked before.
			switch(p) {
				case r:
				{
					if(value instanceof Value.Const) return 0;
					else if(value.type() == t) return 1; // actually, same type.
					else return 2 + Arrays.asList(t.supertypes()).indexOf(value.type());
				}
				case rs: {
					if (value instanceof Value.Const) return 0;
					else return 1; // actually, same type.
				}
				case ws: return 0; // there is only one possibility anyways
				case w:
				{
					if(value.type() == t) return 0; // actually, same type.
					else return 1 + Arrays.asList(t.subtypes()).indexOf(value.type());
				}
			}

			throw new IllegalArgumentException("p is neither r, ws or w but " + p + ". This is a bug.");
		}

		@Override
		public Value caseValue(int caseIndex) throws CompileException {
			// location of the argument
			// types were checked before.
			switch(p) {
				case r:
				{
					if(caseIndex == 0) {
						// it is a const. In this case the argument is encoded right in 'is'.
						// convert pc + argumentIndex to t.
						return t.createConst();
					}
					else if(caseIndex == 1) return t.createReg();
					else {
						for (Type u : t.supertypes()) {
							if(caseIndex == 2) return u.createReg();
							caseIndex--;
						}
					}
				}
				case rs: {
					if (caseIndex == 0) {
						return t.createConst();
					} else return t.createReg();
				}
				case ws: return t.createReg();
				case w:
				{
					if(caseIndex == 0) return t.createReg(); // actually, same type.
					else {
						for (Type u : t.subtypes()) {
							if(caseIndex == 1) return u.createReg();
							caseIndex--;
						}
					}
				}
			}

			throw new IllegalArgumentException("p is neither r, ws or w but " + p + ". This is a bug.");
		}

		@Override
		public String cConvertTo(Value value, String accessCode) {
			// from this to value
			assert value instanceof Value.Typed;

			Value.Typed vt = (Value.Typed) value;

			return t.convertToInVM(vt.type(), accessCode);
		}



		/**
		 * Returns code to convert the value that is fetched with "accessCode" in C and has
		 * type 'type' (hence, not for labels) to this sort. This is only possible for typed sorts.
		 * @param type
		 * @param accessCode
		 * @return
		 *
		public String cConvertFrom(Type type, String accessCode) {
			return type.convertToInVM(this.t, accessCode);
		}


		/*@Override
		public String accessCode(int argumentIndex, int sortIndex) {
			// location of the argument
			// types were checked before.
			switch(p) {
				case r:
				{
					if(sortIndex == 0) {
						// it is a const. In this case the argument is encoded right in 'is'.
						// convert pc + argumentIndex to t.
						return t.cConst(argumentIndex);
					}
					else if(sortIndex == 1) return "register of type " + t;
					else {
						for (Type u : Type.values()) {
							if (t.rank() > u.rank()) {
								if(sortIndex == 2) {
									return u.cReg(argumentIndex);
								}
								sortIndex --;
							}
						}
					}
				}
				case ws: return t.cReg(argumentIndex);
				case w:
				{
					if(sortIndex == 0) return "register of type " + t; // actually, same type.
					else {
						for (Type u : Type.values()) {
							if (t.rank() > u.rank()) {
								if(sortIndex == 1) return u.cReg(argumentIndex);
								sortIndex--;
							}
						}
					}
				}
			}

			throw new IllegalArgumentException("p is neither r, ws or w but " + p + ". This is a bug.");
		}*

		public int countCases() {
			switch(p) {
				case r: return 2 + t.supertypes().length; // one for const, one for identical
				case rs: return 2;
				case ws: return 1;
				case w: return 1 + t.subtypes().length;
			}

			throw new IllegalArgumentException("p is neither r, ws or w but " + p + ". This is a bug.");
		}

		@Override
		public String toString() {
			return t.name() + "_" + p.name();
		}

		@Override
		public boolean canConvert(Value value) {
			if(!(value instanceof Value.Typed)) return false;

			Value.Typed v = (Value.Typed) value;

			switch(p) {
				case r: {
					return v.type() != null && (v.type() == t || Arrays.asList(t.supertypes()).contains(v.type()));
				}
				case rs: {
					return v.type() != null && ((v instanceof Value.Const && Arrays.asList(t.supertypes()).contains(v.type())) || v.type() == t);
				}
				case ws: {
					// I'll allow it. if(v.type() == null) throw new CompileException("cannot use uninitialized register for read/write");
					return (v instanceof Value.Reg) && (v.type() == null || v.type() == t);
				}
				case w: {
					return (v instanceof Value.Reg) && (v.type() == null ||
							v.type() == t ||
							Arrays.asList(t.subtypes()).contains(v.type()) );
				}
			}
			return false;
		}

		@Override
		public Value convert(Value v) throws CompileException {
			if(v instanceof Value.Typed)
				return ((Value.Typed) v).convertTo(t);

			throw new IllegalArgumentException("can't convert " + v);
		}

		public Type type() {
			return t;
		}
	}*/
}
