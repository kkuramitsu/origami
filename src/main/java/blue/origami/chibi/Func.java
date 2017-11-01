package blue.origami.chibi;

import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

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
	public interface FuncBool extends BooleanSupplier {
		public boolean applyZ();

		@Override
		public default boolean getAsBoolean() {
			return applyZ();
		}
	}

	@FunctionalInterface
	public interface FuncInt extends IntSupplier {
		public int applyI();

		@Override
		public default int getAsInt() {
			return applyI();
		}
	}

	@FunctionalInterface
	public interface FuncFloat extends DoubleSupplier {
		public double applyD();

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

	// @FunctionalInterface
	// public interface FuncBoolVoid {
	// public void apply(boolean v);
	// }

	@FunctionalInterface
	public interface FuncIntVoid extends IntConsumer {
		public void apply(int v);

		@Override
		public default void accept(int v) {
			apply(v);
		}
	}

	@FunctionalInterface
	public interface FuncFloatVoid extends DoubleConsumer {
		public void apply(double v);

		@Override
		public default void accept(double v) {
			apply(v);
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
	public interface FuncObjBool extends Predicate<Object> {
		public boolean applyZ(Object v);

		@Override
		public default boolean test(Object v) {
			return applyZ(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjInt extends ToIntFunction<Object> {
		public int applyI(Object v);

		@Override
		public default int applyAsInt(Object v) {
			return applyI(v);
		}
	}

	@FunctionalInterface
	public interface FuncObjFloat extends ToDoubleFunction<Object> {
		public double applyD(Object v);

		@Override
		public default double applyAsDouble(Object v) {
			return applyD(v);
		}
	}

	public interface FuncBoolBool {
		public boolean applyZ(boolean v);
	}

	public interface FuncBoolInt extends FuncObjInt {
		public int applyI(boolean v);
	}

	@FunctionalInterface
	public interface FuncBoolFloat {
		public float applyD(boolean v);
	}

	@FunctionalInterface
	public interface FuncBoolObj {
		public Object apply(boolean v);
	}

	@FunctionalInterface
	public interface FuncIntBool extends IntPredicate {
		public boolean applyZ(int v);

		@Override
		default boolean test(int v) {
			return applyZ(v);
		}
	}

	@FunctionalInterface
	public interface FuncIntInt extends IntUnaryOperator {
		public int applyI(int v);

		@Override
		default int applyAsInt(int operand) {
			return applyI(operand);
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
	public interface FuncIntFloat extends IntToDoubleFunction {
		public double applyD(int v);

		@Override
		default double applyAsDouble(int v) {
			return applyD(v);
		}
	}

	@FunctionalInterface
	public interface FuncIntObj extends IntFunction<Object> {
		@Override
		public Object apply(int v);
	}

	@FunctionalInterface
	public interface FuncFloatBool extends DoublePredicate {
		public boolean applyZ(double v);

		@Override
		default boolean test(double value) {
			return applyZ(value);
		}

	}

	@FunctionalInterface
	public interface FuncFloatInt extends DoubleToIntFunction {
		public int applyI(double v);

		@Override
		default int applyAsInt(double value) {
			return applyI(value);
		}

	}

	@FunctionalInterface
	public interface FuncFloatFloat extends DoubleUnaryOperator {
		public double applyD(double v);

		@Override
		default double applyAsDouble(double v) {
			return applyD(v);
		}

	}

	@FunctionalInterface
	public interface FuncFloatFloatFloat extends DoubleBinaryOperator {
		public double applyD(double v, double v2);

		@Override
		default double applyAsDouble(double left, double right) {
			return applyD(left, right);
		}

	}

	@FunctionalInterface
	public interface FuncFloatObj extends DoubleFunction<Object> {
		@Override
		public Object apply(double v);
	}

	public interface FuncStrObjVoid extends BiConsumer<String, Object> {
		public void apply(String key, Object value);

		@Override
		default void accept(String key, Object value) {
			apply(key, value);
		}
	}
}
