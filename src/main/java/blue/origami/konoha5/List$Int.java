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
import blue.origami.util.OStrings;

public class List$Int implements OStrings, FuncIntInt {
	protected int[] arrays = null;
	protected int start = 0;
	protected int end = 0;
	protected List$Int next;
	protected int cost;

	List$Int(int[] arrays, int start, int end, List$Int next, int cost) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
		this.next = next;
		this.cost = cost;
	}

	List$Int(int[] arrays, int start, int end) {
		this(arrays, start, end, null, 0);
	}

	// private List$Int(int[] arrays, int size) {
	// this(arrays, 0, size, null, 0);
	// }

	public List$Int(int[] arrays) {
		this(arrays, 0, arrays.length, null, 0);
	}

	public static final List$Int newArray(boolean isMutable, int[] arrays) {
		return isMutable ? new ListM$Int(arrays) : new List$Int(arrays);
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

	public int size() {
		int len = 0;
		for (List$Int p = this; p != null; p = p.next) {
			len = p.end - p.start;
		}
		return len;
	}

	public int geti(int index) {
		if (this.next == null) {
			return this.arrays[this.start + index];
		}
		for (List$Int p = this; p != null; p = p.next) {
			int n = index + p.start;
			if (n < p.end) {
				return p.arrays[n];
			}
		}
		return this.arrays[index];
	}

	public void seti(int index, int value) {
		if (this.next == null) {
			this.arrays[this.start + index] = value;
			return;
		}
		for (List$Int p = this; p != null; p = p.next) {
			int n = index + p.start;
			if (n < p.end) {
				this.arrays[this.start + index] = value;
				return;
			}
		}
		this.arrays[index] = value;
	}

	public List$Int flat() {
		if (this.next != null) {
			int[] buf = new int[this.size()];
			int offset = 0;
			for (List$Int p = this; p != null; p = p.next) {
				System.arraycopy(p.arrays, p.start, buf, offset, p.end - p.start);
				offset += p.end - p.start;
			}
			return new List$Int(buf);
		}
		return this;
	}

	static int CopyCost = 8;

	public List$Int cons(int x, List$Int xs) {
		if (xs.cost < CopyCost) {
			int[] a = { x };
			return new List$Int(a, 0, 1, xs, xs.cost + 1);
		}
		int[] buf = new int[xs.size() + 1];
		buf[0] = x;
		int offset = 1;
		for (List$Int p = this; p != null; p = p.next) {
			System.arraycopy(p.arrays, p.start, buf, offset, p.end - p.start);
			offset += p.end - p.start;
		}
		return new List$Int(buf);
	}

	public List$Int ltrim(int shift) {
		if (this.next != null) {
			return this.flat().ltrim(shift);
		}
		return new List$Int(this.arrays, this.start + shift, this.end);
	}

	public List$Int rtrim(int shift) {
		if (this.next != null) {
			return this.flat().rtrim(shift);
		}
		return new List$Int(this.arrays, this.start, this.end - shift);
	}

	@Override
	public int applyI(int v) {
		return this.geti(v);
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append(this instanceof ListM$Int ? "{" : "[");
		for (List$Int p = this; p != null; p = p.next) {
			cnt = this.strOut(sb, p, cnt);
		}
		sb.append(this instanceof ListM$Int ? "}" : "]");
	}

	int strOut(StringBuilder sb, List$Int p, int cnt) {
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
		for (List$Int p = this; p != null; p = p.next) {
			for (int i = p.start; i < p.end; i++) {
				f.apply(p.arrays[i]);
			}
		}
	}

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

	public static final ListM$Int listM(IntStream s) {
		return new ListM$Int(s.toArray());
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

	public static final IntStream downCast(Object o) {
		if (o instanceof IntStream) {
			return (IntStream) o;
		}
		return ((List$Int) o).stream();
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
		super(arrays, start, end, null, 0);
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
