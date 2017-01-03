package at.searles.meelan;

import at.searles.math.Cplx;

public enum Constant {
	PI {
		@Override
		public Tree get() {
			return new Value.Real(Math.PI);
		}
	},
	TAU {
		@Override
		public Tree get() {
			return new Value.Real(2 * Math.PI);
		}
	},
	E {
		@Override
		public Tree get() {
			return new Value.Real(Math.E);
		}
	},
	I {
		@Override
		public Tree get() {
			return new Value.CplxVal(new Cplx(0, 1));
		}
	};

	public abstract Tree get();

}
