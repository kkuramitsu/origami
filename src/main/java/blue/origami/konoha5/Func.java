package blue.origami.konoha5;

import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

import blue.origami.transpiler.asm.APIs;

public class Func {
	@FunctionalInterface
	public interface FuncObj extends Supplier<Object> {
		public Object apply();

		@Override
		public default Object get() {
			return apply();
		}
	}

	@FunctionalInterface
	public interface FuncBool extends Supplier<Object>, BooleanSupplier {
		public boolean applyZ();

		@Override
		public default Object get() {
			return applyZ();
		}

		@Override
		public default boolean getAsBoolean() {
			return applyZ();
		}
	}

	@FunctionalInterface
	public interface FuncInt extends Supplier<Object>, IntSupplier {
		public int applyI();

		@Override
		public default Object get() {
			return applyI();
		}

		@Override
		public default int getAsInt() {
			return applyI();
		}
	}

	@FunctionalInterface
	public interface FuncFloat extends Supplier<Object>, DoubleSupplier {
		public double applyD();

		@Override
		public default Object get() {
			return applyD();
		}

		@Override
		public default double getAsDouble() {
			return applyD();
		}
	}

	@FunctionalInterface
	public interface FuncObjVoid extends Consumer<Object> {
		public void apply(Object v);

		@Override
		public default void accept(Object v) {
			apply(v);
		}
	}

	@FunctionalInterface
	public interface FuncBoolVoid extends Consumer<Object> {
		public void apply(boolean v);

		@Override
		public default void accept(Object v) {
			apply(APIs.unboxZ(v));
		}
	}

	@FunctionalInterface
	public interface FuncIntVoid extends Consumer<Object>, IntConsumer {
		public void apply(int v);

		@Override
		public default void accept(Object v) {
			apply(APIs.unboxI(v));
		}

		@Override
		public default void accept(int v) {
			apply(v);
		}
	}

	@FunctionalInterface
	public interface FuncFloatVoid extends Consumer<Object>, DoubleConsumer {
		public void apply(double v);

		@Override
		public default void accept(double v) {
			apply(v);
		}

		@Override
		public default void accept(Object v) {
			apply(APIs.unboxD(v));
		}
	}

	@FunctionalInterface
	public interface FuncObjObj extends Function<Object, Object> {
		@Override
		public Object apply(Object v);
	}

	@FunctionalInterface
	public interface FuncObjObjObj extends BinaryOperator<Object> {
		@Override
		public Object apply(Object v, Object v2);
	}

	@FunctionalInterface
	public interface FuncObjBool extends Function<Object, Object>, Predicate<Object> {
		public boolean applyZ(Object v);

		@Override
		public default boolean test(Object v) {
			return applyZ(v);
		}

		@Override
		public default Object apply(Object v) {
			return applyZ(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjInt extends Function<Object, Object>, ToIntFunction<Object> {
		public int applyI(Object v);

		@Override
		public default int applyAsInt(Object v) {
			return applyI(v);
		}

		@Override
		public default Object apply(Object v) {
			return applyI(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjFloat extends Function<Object, Object>, ToDoubleFunction<Object> {
		public double applyD(Object v);

		@Override
		public default double applyAsDouble(Object v) {
			return applyD(v);
		}

		@Override
		public default Object apply(Object v) {
			return applyD(v);
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
	public interface FuncIntBool extends Predicate<Object>/* , IntPredicate */ {
		public boolean applyZ(int v);

		// @Override
		// default boolean test(int v) {
		// return applyZ(v);
		// }

		@Override
		default boolean test(Object v) {
			return applyZ(APIs.unboxI(v));
		}

	}

	@FunctionalInterface
	public interface FuncIntInt
			extends IntUnaryOperator, Function<Object, Object>, IntFunction<Object>, ToIntFunction<Object> {
		public int applyI(int v);

		@Override
		default int applyAsInt(int operand) {
			return applyI(operand);
		}

		@Override
		default int applyAsInt(Object v) {
			return applyI(APIs.unboxI(v));
		}

		@Override
		default Object apply(int v) {
			return applyI(v);
		}

		@Override
		default Object apply(Object v) {
			return applyI(APIs.unboxI(v));
		}

	}

	@FunctionalInterface
	public interface FuncIntIntInt extends IntBinaryOperator {
		public int applyI(int v, int v2);

		@Override
		default int applyAsInt(int left, int right) {
			return applyI(left, right);
		}

	}

	@FunctionalInterface
	public interface FuncIntFloat
			extends IntToDoubleFunction, Function<Object, Object>, IntFunction<Object>, ToDoubleFunction<Object> {
		public double applyD(int v);

		@Override
		default double applyAsDouble(int v) {
			return applyD(v);
		}

		@Override
		default double applyAsDouble(Object v) {
			return applyD(APIs.unboxI(v));
		}

		@Override
		default Object apply(int v) {
			return applyD(v);
		}

		@Override
		default Object apply(Object v) {
			return applyD(APIs.unboxI(v));
		}
	}

	@FunctionalInterface
	public interface FuncIntObj extends IntFunction<Object> {
		@Override
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
