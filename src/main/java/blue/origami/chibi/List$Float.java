package blue.origami.chibi;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import blue.origami.chibi.Func.FuncFloatBool;
import blue.origami.chibi.Func.FuncFloatFloat;
import blue.origami.chibi.Func.FuncFloatFloatFloat;
import blue.origami.chibi.Func.FuncFloatInt;
import blue.origami.chibi.Func.FuncFloatObj;
import blue.origami.chibi.Func.FuncFloatVoid;
import blue.origami.chibi.Func.FuncIntFloat;
import blue.origami.chibi.Func.FuncObjVoid;
import origami.nez2.OStrings;

public class List$Float implements OStrings, FuncIntFloat {
	protected double[] arrays = null;
	protected int start = 0;
	protected int end = 0;
	protected List$Float next;

	List$Float(double[] arrays, int start, int end, List$Float next) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
		this.next = next;
	}

	List$Float(double[] arrays, int start, int end) {
		this(arrays, start, end, null);
	}

	public List$Float(double[] arrays) {
		this(arrays, 0, arrays.length, null);
	}

	public static final List$Float newArray(double[] arrays) {
		return new List$Float(arrays);
	}

	public List$Float bind() {
		return this;
	}

	public int size() {
		int len = 0;
		for (List$Float p = this; p != null; p = p.next) {
			len += p.end - p.start;
		}
		return len;
	}

	private void flatten() {
		if (this.next != null) {
			double[] buf = new double[this.size()];
			int offset = 0;
			for (List$Float p = this; p != null; p = p.next) {
				System.arraycopy(p.arrays, p.start, buf, offset, p.end - p.start);
				offset += p.end - p.start;
			}
			this.arrays = buf;
			this.start = 0;
			this.end = 0;
			this.next = null;
		}
	}

	public double geti(int index) {
		this.flatten();
		return this.arrays[this.start + index];
	}

	public void seti(int index, double value) {
		this.flatten();
		this.arrays[this.start + index] = value;
		return;
	}

	public static List$Float cons(double x, List$Float xs) {
		double[] a = { x };
		return new List$Float(a, 0, 1, xs);
	}

	public List$Float tail(int shift) {
		this.flatten();
		return new List$Float(this.arrays, this.start + shift, this.end);
	}

	public List$Float head(int shift) {
		this.flatten();
		return new List$Float(this.arrays, this.start, this.end - shift);
	}

	@Override
	public double applyD(int v) {
		return this.geti(v);
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		// if (this instanceof ListM$) {
		// sb.append("$");
		// }
		sb.append("[");
		for (List$Float p = this; p != null; p = p.next) {
			cnt = this.strOut(sb, p, cnt);
		}
		sb.append("]");
	}

	private int strOut(StringBuilder sb, List$Float p, int cnt) {
		for (int i = p.start; i < p.end; i++) {
			if (cnt > 0) {
				sb.append(",");
			}
			sb.append(p.arrays[i]);
			cnt++;
		}
		return cnt;
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

	public void push(double v) {
		this.ensure(this.end);
		this.arrays[this.end++] = v;
	}

	public double pop() {
		this.end--;
		return this.arrays[this.end];
	}

	/* High-order functions */

	public void forEach(FuncObjVoid f) {
		for (List$Float p = this; p != null; p = p.next) {
			for (int i = p.start; i < p.end; i++) {
				f.apply(p.arrays[i]);
			}
		}
	}

	public DoubleStream stream() {
		DoubleStream s = Arrays.stream(this.arrays, this.start, this.end);
		if (this.next != null) {
			return DoubleStream.concat(s, this.next.stream());
		}
		return s;
	}

	public static final List$Float list(DoubleStream s) {
		return new List$Float(s.toArray());
	}

	public final void forEach(FuncFloatVoid f) {
		forEach(this.stream(), f);
	}

	public static final void forEach(DoubleStream s, FuncFloatVoid f) {
		s.forEach(f);
	}

	public final DoubleStream filter(FuncFloatBool f) {
		return filter(this.stream(), f);
	}

	public static final DoubleStream filter(DoubleStream s, FuncFloatBool f) {
		return s.filter(f);
	}

	public final DoubleStream map(FuncFloatFloat f) {
		return map(this.stream(), f);
	}

	public static final DoubleStream map(DoubleStream s, FuncFloatFloat f) {
		return s.map(f);
	}

	public final Stream<Object> map(FuncFloatObj f) {
		return map(this.stream(), f);
	}

	public static final Stream<Object> map(DoubleStream s, FuncFloatObj f) {
		return s.mapToObj(f);
	}

	public final IntStream map(FuncFloatInt f) {
		return map(this.stream(), f);
	}

	public static final IntStream map(DoubleStream s, FuncFloatInt f) {
		return s.mapToInt(f);
	}

	public final DoubleStream flatMap(FuncFloatObj f) {
		return flatMap(this.stream(), f);
	}

	public static final DoubleStream flatMap(DoubleStream s, FuncFloatObj f) {
		return s.flatMap(x -> downCast(f.apply(x)));
	}

	public final double reduce(double acc, FuncFloatFloatFloat f) {
		return reduce(this.stream(), acc, f);
	}

	public static final double reduce(DoubleStream s, double acc, FuncFloatFloatFloat f) {
		return s.reduce(acc, f);
	}

	public static final DoubleStream downCast(Object o) {
		if (o instanceof DoubleStream) {
			return (DoubleStream) o;
		}
		return ((List$Float) o).stream();
	}
}
