package blue.origami.chibi;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import blue.origami.chibi.Func.FuncIntObj;
import blue.origami.chibi.Func.FuncObjBool;
import blue.origami.chibi.Func.FuncObjFloat;
import blue.origami.chibi.Func.FuncObjInt;
import blue.origami.chibi.Func.FuncObjObj;
import blue.origami.chibi.Func.FuncObjObjObj;
import blue.origami.chibi.Func.FuncObjVoid;
import blue.origami.common.OStrings;

public class List$ implements OStrings, FuncIntObj {
	protected Object[] arrays = null;
	protected int start = 0;
	protected int end = 0;
	protected List$ next;

	List$(Object[] arrays, int start, int end, List$ next) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
		this.next = next;
	}

	List$(Object[] arrays, int start, int end) {
		this(arrays, start, end, null);
	}

	public List$(Object[] arrays) {
		this(arrays, 0, arrays.length, null);
	}

	public static final List$ newArray(Object[] arrays) {
		return new List$(arrays);
	}

	public List$ bind() {
		return this;
	}

	public int size() {
		int len = 0;
		for (List$ p = this; p != null; p = p.next) {
			len += p.end - p.start;
		}
		return len;
	}

	private void flatten() {
		if (this.next != null) {
			Object[] buf = new Object[this.size()];
			int offset = 0;
			for (List$ p = this; p != null; p = p.next) {
				System.arraycopy(p.arrays, p.start, buf, offset, p.end - p.start);
				offset += p.end - p.start;
			}
			this.arrays = buf;
			this.start = 0;
			this.end = 0;
			this.next = null;
		}
	}

	public Object geti(int index) {
		this.flatten();
		return this.arrays[this.start + index];
	}

	public List$ getl(int left, int right) {
		this.flatten();
		return new List$(this.arrays, this.start + left, this.start + right);
	}

	public void seti(int index, Object value) {
		this.flatten();
		this.arrays[this.start + index] = value;
		return;
	}

	public static List$ cons(Object x, List$ xs) {
		Object[] a = { x };
		return new List$(a, 0, 1, xs);
	}

	public List$ tail(int shift) {
		this.flatten();
		return new List$(this.arrays, this.start + shift, this.end);
	}

	public List$ head(int shift) {
		this.flatten();
		return new List$(this.arrays, this.start, this.end - shift);
	}

	@Override
	public Object apply(int v) {
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
		for (List$ p = this; p != null; p = p.next) {
			cnt = this.strOut(sb, p, cnt);
		}
		sb.append("]");
	}

	private int strOut(StringBuilder sb, List$ p, int cnt) {
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
			this.arrays = new Object[Math.max(4, capacity)];
		} else if (this.arrays.length <= capacity) {
			Object[] na = new Object[Math.max(this.arrays.length * 2, capacity)];
			System.arraycopy(this.arrays, 0, na, 0, this.arrays.length);
			this.arrays = na;
		}
	}

	public void push(Object v) {
		this.ensure(this.end);
		this.arrays[this.end++] = v;
	}

	public Object pop() {
		this.end--;
		return this.arrays[this.end];
	}

	/* High-order functions */

	public Stream<Object> stream() {
		Stream<Object> s = Arrays.stream(this.arrays, this.start, this.end);
		if (this.next != null) {
			return Stream.concat(s, this.next.stream());
		}
		return s;
	}

	public static final List$ list(Stream<Object> s) {
		return new List$(s.toArray());
	}

	public void forEach(FuncObjVoid f) {
		forEach(this.stream(), f);
	}

	public static final void forEach(Stream<Object> s, FuncObjVoid f) {
		s.forEach(f);
	}

	public Stream<Object> filter(FuncObjBool f) {
		return filter(this.stream(), f);
	}

	public static final Stream<Object> filter(Stream<Object> s, FuncObjBool f) {
		return s.filter(f);
	}

	public Stream<Object> map(FuncObjObj f) {
		return map(this.stream(), f);
	}

	public static final Stream<Object> map(Stream<Object> s, FuncObjObj f) {
		return s.map(f);
	}

	public IntStream map(FuncObjInt f) {
		return map(this.stream(), f);
	}

	public static final IntStream map(Stream<Object> s, FuncObjInt f) {
		return s.mapToInt(f);
	}

	public DoubleStream map(FuncObjFloat f) {
		return map(this.stream(), f);
	}

	public static final DoubleStream map(Stream<Object> s, FuncObjFloat f) {
		return s.mapToDouble(f);
	}

	@SuppressWarnings("unchecked")
	public static final Stream<Object> downCast(Object o) {
		if (o instanceof Stream<?>) {
			return (Stream<Object>) o;
		}
		return ((List$) o).stream();
	}

	public final Stream<Object> flatMap(FuncObjObj f) {
		return flatMap(this.stream(), f);
	}

	public static final Stream<Object> flatMap(Stream<Object> s, FuncObjObj f) {
		return s.flatMap(x -> downCast(f.apply(x)));
	}

	public final Object reduce(Object acc, FuncObjObjObj f) {
		return reduce(this.stream(), acc, f);
	}

	public static final Object reduce(Stream<Object> s, Object acc, FuncObjObjObj f) {
		return s.reduce(acc, f);
	}

}
