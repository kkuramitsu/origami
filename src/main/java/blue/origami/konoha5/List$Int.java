package blue.origami.konoha5;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import blue.origami.konoha5.Func.FuncIntBool;
import blue.origami.konoha5.Func.FuncIntFloat;
import blue.origami.konoha5.Func.FuncIntInt;
import blue.origami.konoha5.Func.FuncIntIntInt;
import blue.origami.konoha5.Func.FuncIntObj;
import blue.origami.konoha5.Func.FuncIntVoid;
import blue.origami.util.StringCombinator;

public class List$Int implements StringCombinator, FuncIntInt {
	protected int[] arrays = null;
	protected int start = 0;
	protected int end = 0;

	public List$Int(int[] arrays, int start, int end) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
	}

	public List$Int(int[] arrays, int size) {
		this(arrays, 0, size);
	}

	public List$Int(int[] arrays) {
		this(arrays, 0, arrays.length);
	}

	public List$Int(int capacity) {
		this.ensure(capacity);
	}

	public List$Int bind() {
		return this;
	}

	public Object bindOption(Object o) {
		if (o instanceof List$Int) {
			return ((List$Int) o).bind();
		}
		return o;
	}

	public List$Int ltrim(int shift) {
		return new List$Int(this.arrays, this.start + shift, this.end);
	}

	public List$Int rtrim(int shift) {
		return new List$Int(this.arrays, this.start, this.end - shift);
	}

	@Override
	public int applyI(int v) {
		return this.arrays[v + this.start];
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append(this instanceof ListM$Int ? "{" : "[");
		for (int i = this.start; i < this.end; i++) {
			if (cnt > 0) {
				sb.append(", ");
			}
			sb.append(this.arrays[i]);
			cnt++;
		}
		sb.append(this instanceof ListM$Int ? "}" : "]");
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

	public int size() {
		return this.end - this.start;
	}

	public int geti(int index) {
		return this.arrays[this.start + index];
	}

	public void seti(int index, int value) {
		this.arrays[this.start + index] = value;
	}

	public List$Int push(int v) {
		this.ensure(this.end);
		this.arrays[this.end++] = v;
		return this;
	}

	public int pop() {
		this.end--;
		return this.arrays[this.end];
	}

	public void forEach(FuncIntVoid f) {
		for (int i = this.start; i < this.end; i++) {
			f.apply(this.arrays[i]);
		}
	}

	public IntStream stream() {
		return Arrays.stream(this.arrays, this.start, this.end);
	}

	public static final List$Int newArray(boolean isMutable, int[] arrays) {
		return isMutable ? new ListM$Int(arrays) : new List$Int(arrays);
	}

	public static final List$Int list(IntStream s) {
		return new List$Int(s.toArray());
	}

	public static final ListM$Int listM(IntStream s) {
		return new ListM$Int(s.toArray());
	}

	public static final IntStream downCast(Object o) {
		if (o instanceof IntStream) {
			return (IntStream) o;
		}
		return ((List$Int) o).stream();
	}

	public static final void forEach(IntStream s, FuncIntVoid f) {
		s.forEach(f);
	}

	public static final IntStream filter(IntStream s, FuncIntBool f) {
		return s.filter(f);
	}

	public static final IntStream map(IntStream s, FuncIntInt f) {
		return s.map(f);
	}

	public static final Stream<Object> map(IntStream s, FuncIntObj f) {
		return s.mapToObj(f);
	}

	public static final DoubleStream map(IntStream s, FuncIntFloat f) {
		return s.mapToDouble(f);
	}

	public static final IntStream flatMap(IntStream s, FuncIntObj f) {
		return s.flatMap(x -> downCast(f.apply(x)));
	}

	public static final int reduce(IntStream s, int acc, FuncIntIntInt f) {
		return s.reduce(acc, f);
	}

}

class ListM$Int extends List$Int {

	private List$Int imm = null;

	public ListM$Int(int[] arrays, int start, int end) {
		super(arrays, start, end);
	}

	public ListM$Int(int[] arrays, int end) {
		super(arrays, end);
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
	public List$Int push(int v) {
		this.imm = null;
		return super.push(v);
	}

	@Override
	public int pop() {
		this.imm = null;
		return super.pop();
	}

}
