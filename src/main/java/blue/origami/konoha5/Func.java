package blue.origami.konoha5;

import blue.origami.transpiler.asm.APIs;

public class Func {
	@FunctionalInterface
	public interface FuncObj {
		public Object apply();
	}

	@FunctionalInterface
	public interface FuncBool extends FuncObj {
		public boolean applyZ();

		@Override
		public default Object apply() {
			return applyZ();
		}
	}

	@FunctionalInterface
	public interface FuncInt extends FuncObj {
		public int applyI();

		@Override
		public default Object apply() {
			return applyI();
		}
	}

	@FunctionalInterface
	public interface FuncFloat extends FuncObj {
		public double applyD();

		@Override
		public default Object apply() {
			return applyD();
		}
	}

	@FunctionalInterface
	public interface FuncStr extends FuncObj {
		public String applyS();

		@Override
		public default Object apply() {
			return applyS();
		}
	}

	@FunctionalInterface
	public interface FuncObjVoid {
		public void apply(Object v);
	}

	@FunctionalInterface
	public interface FuncBoolVoid extends FuncObjVoid {
		public void apply(boolean v);

		@Override
		public default void apply(Object v) {
			apply(APIs.unboxZ(v));
		}
	}

	@FunctionalInterface
	public interface FuncIntVoid extends FuncObjVoid {
		public void apply(int v);

		@Override
		public default void apply(Object v) {
			apply(APIs.unboxI(v));
		}
	}

	@FunctionalInterface
	public interface FuncFloatVoid extends FuncObjVoid {
		public void apply(double v);

		@Override
		public default void apply(Object v) {
			apply(APIs.unboxD(v));
		}
	}

	@FunctionalInterface
	public interface FuncStrVoid extends FuncObjVoid {
		public void apply(String v);

		@Override
		public default void apply(Object v) {
			apply(APIs.unboxS(v));
		}
	}

	@FunctionalInterface
	public interface FuncObjObj {
		public Object apply(Object v);
	}

	@FunctionalInterface
	public interface FuncObjBool extends FuncObjObj {
		public boolean applyZ(Object v);

		@Override
		public default Object apply(Object v) {
			return applyZ(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjInt extends FuncObjObj {
		public int applyI(Object v);

		@Override
		public default Object apply(Object v) {
			return applyI(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjFloat extends FuncObjObj {
		public float applyD(Object v);

		@Override
		public default Object apply(Object v) {
			return applyD(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjStr extends FuncObjObj {
		public String applyS(Object v);

		@Override
		public default Object apply(Object v) {
			return applyS(v);
		}
	}

	public interface FuncBoolBool extends FuncObjBool {
		public boolean applyZ(boolean v);

		public default Object apply(boolean v) {
			return applyZ(v);
		}
	}

	public interface FuncBoolInt extends FuncObjInt {
		public int applyI(boolean v);

		public default Object apply(boolean v) {
			return applyI(v);
		}
	}

	@FunctionalInterface
	public interface FuncBoolFloat {
		public float applyD(boolean v);
	}

	@FunctionalInterface
	public interface FuncBoolStr {
		public String applyS(boolean v);
	}

	@FunctionalInterface
	public interface FuncBoolObj {
		public Object apply(boolean v);
	}

	@FunctionalInterface
	public interface FuncIntBool {
		public boolean apply(int v);
	}

	@FunctionalInterface
	public interface FuncIntInt {
		public int apply(int v);
	}

	@FunctionalInterface
	public interface FuncIntFloat {
		public float apply(int v);
	}

	@FunctionalInterface
	public interface FuncIntStr {
		public String apply(int v);
	}

	@FunctionalInterface
	public interface FuncIntObj {
		public Object apply(int v);
	}

	@FunctionalInterface
	public interface FuncFloatBool {
		public boolean apply(double v);
	}

	@FunctionalInterface
	public interface FuncFloatInt {
		public int apply(double v);
	}

	@FunctionalInterface
	public interface FuncFloatFloat {
		public float apply(double v);
	}

	@FunctionalInterface
	public interface FuncFloatStr {
		public String apply(double v);
	}

	@FunctionalInterface
	public interface FuncFloatObj {
		public Object apply(double v);
	}

	@FunctionalInterface
	public interface FuncStrBool {
		public boolean apply(String v);
	}

	@FunctionalInterface
	public interface FuncStrInt {
		public int apply(String v);
	}

	@FunctionalInterface
	public interface FuncStrFloat {
		public float apply(String v);
	}

	@FunctionalInterface
	public interface FuncStrStr {
		public String apply(String v);
	}

	@FunctionalInterface
	public interface FuncStrObj {
		public Object apply(String v);
	}

}
