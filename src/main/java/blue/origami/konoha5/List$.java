package blue.origami.konoha5;

import java.util.Arrays;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import blue.origami.konoha5.Func.FuncIntObj;
import blue.origami.konoha5.Func.FuncObjBool;
import blue.origami.konoha5.Func.FuncObjFloat;
import blue.origami.konoha5.Func.FuncObjInt;
import blue.origami.konoha5.Func.FuncObjObj;
import blue.origami.konoha5.Func.FuncObjObjObj;
import blue.origami.konoha5.Func.FuncObjVoid;
import blue.origami.util.OStrings;

public class List$ implements OStrings, FuncIntObj {
	private Object[] arrays = null;
	private int start = 0;
	private int end = 0;

	public List$(Object[] arrays, int size) {
		this.arrays = arrays;
		this.end = size;
	}

	public List$(Object[] arrays) {
		this.arrays = arrays;
		this.start = 0;
		this.end = arrays.length;
	}

	public List$(Object[] arrays, int start, int end) {
		this.arrays = arrays;
		this.start = start;
		this.end = end;
	}

	public List$(int capacity) {
		this.ensure(capacity);
	}

	@Override
	public Object apply(int v) {
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
			OStrings.append(sb, this.arrays[i]);
			cnt++;
		}
		sb.append("]");
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

	public List$ push(int v) {
		this.ensure(this.end);
		this.arrays[this.end++] = v;
		return this;
	}

	public Object pop() {
		this.end--;
		return this.arrays[this.end];
	}

	public int size() {
		return this.end - this.start;
	}

	public Object geti(int index) {
		return this.arrays[this.start + index];
	}

	public void seti(int index, Object value) {
		this.arrays[this.start + index] = value;
	}

	public void forEach(FuncObjVoid f) {
		// this.stream().forEach(f);
		for (int i = this.start; i < this.end; i++) {
			f.apply(this.arrays[i]);
		}
	}

	public Stream<Object> stream() {
		return Arrays.stream(this.arrays, this.start, this.end);
	}

	public static final Stream<Object> filter(Stream<Object> s, FuncObjBool f) {
		return s.filter(f);
	}

	public static final Stream<Object> map(Stream<Object> s, FuncObjObj f) {
		return s.map(f);
	}

	public static final IntStream map(Stream<Object> s, FuncObjInt f) {
		return s.mapToInt(f);
	}

	public static final DoubleStream map(Stream<Object> s, FuncObjFloat f) {
		return s.mapToDouble(f);
	}

	public static final Object reduce(Stream<Object> s, Object acc, FuncObjObjObj f) {
		return s.reduce(acc, f);
	}

}