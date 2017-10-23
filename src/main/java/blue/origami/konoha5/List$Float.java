package blue.origami.konoha5;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import blue.origami.common.OStrings;
import blue.origami.konoha5.Func.FuncFloatBool;
import blue.origami.konoha5.Func.FuncFloatFloat;
import blue.origami.konoha5.Func.FuncFloatFloatFloat;
import blue.origami.konoha5.Func.FuncFloatInt;
import blue.origami.konoha5.Func.FuncFloatObj;
import blue.origami.konoha5.Func.FuncFloatVoid;
import blue.origami.konoha5.Func.FuncIntFloat;

public class List$Float implements OStrings, FuncIntFloat {
	private double[] arrays = null;
	private int start = 0;
	private int end = 0;

	public List$Float(double[] arrays, int size) {
		this.arrays = arrays;
		this.end = size;
	}

	public List$Float(double[] arrays) {
		this.arrays = arrays;
		this.start = 0;
		this.end = arrays.length;
	}

	public List$Float(double[] arrays, int start, int end) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
	}

	public List$Float(int capacity) {
		this.ensure(capacity);
	}

	@Override
	public double applyD(int v) {
		return this.arrays[v + this.start];
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append("[");
		for (int i = this.start; i < this.end; i++) {
			if (cnt > 0) {
				sb.append(", ");
			}
			sb.append(this.arrays[i]);
			cnt++;
		}
		sb.append("]");
	}

	private void ensure(int capacity) {
		if (this.arrays == null) {
			this.arrays = new double[Math.max(4, capacity)];
		} else if (this.arrays.length <= capacity) {
			double[] na = new double[Math.max(this.arrays.length * 2, capacity)];
			System.arraycopy(this.arrays, 0, na, 0, this.arrays.length);
			this.arrays = na;
		}
	}

	public List$Float push(double v) {
		this.ensure(this.end);
		this.arrays[this.end++] = v;
		return this;
	}

	public double pop() {
		this.end--;
		return this.arrays[this.end];
	}

	public int size() {
		return this.end - this.start;
	}

	public double geti(int index) {
		return this.arrays[this.start + index];
	}

	public void seti(int index, double value) {
		this.arrays[this.start + index] = value;
	}

	public void forEach(FuncFloatVoid f) {
		// Arrays.stream(this.arrays, this.start, this.end).forEach(f);
		for (int i = this.start; i < this.end; i++) {
			f.apply(this.arrays[i]);
		}
	}

	public DoubleStream stream() {
		return Arrays.stream(this.arrays, this.start, this.end);
	}

	public static final DoubleStream downCast(Object o) {
		if (o instanceof DoubleStream) {
			return (DoubleStream) o;
		}
		return ((List$Float) o).stream();
	}

	public static final List$Float list(DoubleStream s) {
		return new List$Float(s.toArray());
	}

	public static final void forEach(DoubleStream s, FuncFloatVoid f) {
		s.forEach(f);
	}

	public static final DoubleStream filter(DoubleStream s, FuncFloatBool f) {
		return s.filter(f);
	}

	public static final DoubleStream map(DoubleStream s, FuncFloatFloat f) {
		return s.map(f);
	}

	public static final Stream<Object> map(DoubleStream s, FuncFloatObj f) {
		return s.mapToObj(f);
	}

	public static final IntStream map(DoubleStream s, FuncFloatInt f) {
		return s.mapToInt(f);
	}

	public static final DoubleStream flatMap(DoubleStream s, FuncFloatObj f) {
		return s.flatMap(x -> downCast(f.apply(x)));
	}

	public static final double reduce(DoubleStream s, double acc, FuncFloatFloatFloat f) {
		return s.reduce(acc, f);
	}

}