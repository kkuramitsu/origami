package blue.origami.chibi;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import blue.origami.chibi.Func.FuncIntBool;
import blue.origami.chibi.Func.FuncIntFloat;
import blue.origami.chibi.Func.FuncIntInt;
import blue.origami.chibi.Func.FuncIntIntInt;
import blue.origami.chibi.Func.FuncIntObj;
import blue.origami.chibi.Func.FuncIntVoid;
import blue.origami.common.OStrings;

public class List$Int implements OStrings, FuncIntInt {
	protected int[] arrays = null;
	protected int start = 0;
	protected int end = 0;
	protected List$Int next;

	List$Int(int[] arrays, int start, int end, List$Int next) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
		this.next = next;
	}

	List$Int(int[] arrays, int start, int end) {
		this(arrays, start, end, null);
	}

	public List$Int(int[] arrays) {
		this(arrays, 0, arrays.length, null);
	}

	public static final List$Int newArray(boolean isMutable, int[] arrays) {
		return isMutable ? new ListM$Int(arrays) : new List$Int(arrays);
	}

	public List$Int bind() {
		return this;
	}

	public int size() {
		int len = 0;
		for (List$Int p = this; p != null; p = p.next) {
			len += p.end - p.start;
		}
		return len;
	}

	public List$Int connect(List$Int last) {
		List$Int p = this;
		while (p.next != null) {
			p = p.next;
		}
		p.next = last;
		return this;
	}

	private void flatten() {
		if (this.next != null) {
			int[] buf = new int[this.size()];
			int offset = 0;
			for (List$Int p = this; p != null; p = p.next) {
				System.arraycopy(p.arrays, p.start, buf, offset, p.end - p.start);
				offset += p.end - p.start;
			}
			this.arrays = buf;
			this.start = 0;
			this.end = offset;
			this.next = null;
		}
	}

	public int geti(int index) {
		this.flatten();
		return this.arrays[this.start + index];
	}

	public void seti(int index, int value) {
		this.flatten();
		this.arrays[this.start + index] = value;
		return;
	}

	public static List$Int cons(int x, List$Int xs) {
		int[] a = { x };
		return new List$Int(a, 0, 1, xs);
	}

	public List$Int tail(int shift) {
		this.flatten();
		return new List$Int(this.arrays, this.start + shift, this.end);
	}

	public List$Int head(int shift) {
		this.flatten();
		return new List$Int(this.arrays, this.start, this.end - shift);
	}

	@Override
	public int applyI(int v) {
		return this.geti(v);
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		// if (this instanceof ListM$Int) {
		// sb.append("$");
		// }
		sb.append("[");
		for (List$Int p = this; p != null; p = p.next) {
			cnt = this.strOut(sb, p, cnt);
		}
		sb.append("]");
	}

	private int strOut(StringBuilder sb, List$Int p, int cnt) {
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
			this.arrays = new int[Math.max(4, capacity)];
		} else if (this.arrays.length <= capacity) {
			int[] na = new int[Math.max(this.arrays.length * 2, capacity)];
			System.arraycopy(this.arrays, 0, na, 0, this.arrays.length);
			this.arrays = na;
		}
	}

	public void push(int v) {
		this.ensure(this.end);
		this.arrays[this.end++] = v;
	}

	public int pop() {
		this.end--;
		return this.arrays[this.end];
	}

	/* High-order functions */

	public IntStream stream() {
		IntStream s = Arrays.stream(this.arrays, this.start, this.end);
		if (this.next != null) {
			return IntStream.concat(s, this.next.stream());
		}
		return s;
	}

	public static final List$Int list(IntStream s) {
		return new List$Int(s.toArray());
	}

	public static final List$Int listM(IntStream s) {
		return new ListM$Int(s.toArray());
	}

	public final void forEach(FuncIntVoid f) {
		forEach(this.stream(), f);
	}

	public static final void forEach(IntStream s, FuncIntVoid f) {
		s.forEach(f);
	}

	public final void filter(FuncIntBool f) {
		filter(this.stream(), f);
	}

	public static final IntStream filter(IntStream s, FuncIntBool f) {
		return s.filter(f);
	}

	public final IntStream map(FuncIntInt f) {
		return map(this.stream(), f);
	}

	public static final IntStream map(IntStream s, FuncIntInt f) {
		return s.map(f);
	}

	public final Stream<Object> map(FuncIntObj f) {
		return map(this.stream(), f);
	}

	public static final Stream<Object> map(IntStream s, FuncIntObj f) {
		return s.mapToObj(f);
	}

	public final DoubleStream map(FuncIntFloat f) {
		return map(this.stream(), f);
	}

	public static final DoubleStream map(IntStream s, FuncIntFloat f) {
		return s.mapToDouble(f);
	}

	public static final IntStream downCast(Object o) {
		if (o instanceof IntStream) {
			return (IntStream) o;
		}
		return ((List$Int) o).stream();
	}

	public final IntStream flatMap(FuncIntObj f) {
		return flatMap(this.stream(), f);
	}

	public static final IntStream flatMap(IntStream s, FuncIntObj f) {
		return s.flatMap(x -> downCast(f.apply(x)));
	}

	public final int reduce(int acc, FuncIntIntInt f) {
		return reduce(this.stream(), acc, f);
	}

	public static final int reduce(IntStream s, int acc, FuncIntIntInt f) {
		return s.reduce(acc, f);
	}

}

class ListM$Int extends List$Int {

	private List$Int imm = null;

	public ListM$Int(int[] arrays, int start, int end) {
		super(arrays, start, end, null);
	}

	public ListM$Int(int[] arrays) {
		super(arrays);
	}

	@Override
	public List$Int bind() {
		if (this.imm == null) {
			this.imm = new List$Int(this.arrays, this.start, this.end);
		}
		return this.imm;
	}

	@Override
	public void seti(int index, int value) {
		this.imm = null;
		super.seti(index, value);
	}

	@Override
	public void push(int v) {
		this.imm = null;
		super.push(v);
	}

	@Override
	public int pop() {
		this.imm = null;
		return super.pop();
	}

}
