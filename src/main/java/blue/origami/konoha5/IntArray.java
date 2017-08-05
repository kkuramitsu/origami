package blue.origami.konoha5;

import blue.origami.konoha5.Func.FuncIntBool;
import blue.origami.konoha5.Func.FuncIntInt;
import blue.origami.konoha5.Func.FuncIntObj;
import blue.origami.konoha5.Func.FuncIntVoid;

public class IntArray extends Data implements FuncIntInt {
	private int[] arrays = null;
	private int size = 0;

	public IntArray(int capacity) {
		super(DataMap.Null);
		this.ensure(capacity);
	}

	@Override
	public int apply(int v) {
		return this.arrays[v];
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append("[");
		for (int i = 0; i < this.arrays.length; i++) {
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
			this.arrays = new int[Math.max(4, capacity)];
		} else if (this.arrays.length <= capacity) {
			int[] na = new int[Math.max(this.arrays.length * 2, capacity)];
			System.arraycopy(this.arrays, 0, na, 0, this.arrays.length);
			this.arrays = na;
		}
	}

	public IntArray push(int v) {
		this.ensure(this.size);
		this.arrays[this.size++] = v;
		return this;
	}

	public int pop() {
		this.size--;
		return this.arrays[this.size];
	}

	public int size() {
		return this.size;
	}

	public int geti(int index) {
		return this.arrays[index];
	}

	public void seti(int index, int value) {
		this.arrays[index] = value;
	}

	public void forEach(FuncIntVoid f) {
		for (int i = 0; i < this.size; i++) {
			f.apply(this.arrays[i]);
		}
	}

	public IntArray(int[] arrays, int size) {
		super(DataMap.Null);
		this.arrays = arrays;
		this.size = size;
	}

	public IntArray map(FuncIntInt f) {
		int[] a = new int[this.size];
		for (int i = 0; i < this.size; i++) {
			a[i] = f.apply(this.arrays[i]);
		}
		return new IntArray(a, a.length);
	}

	public ObjArray map(FuncIntObj f) {
		Object[] a = new Object[this.size()];
		for (int i = 0; i < this.size; i++) {
			a[i] = f.apply(i);
		}
		return new ObjArray(a, a.length);
	}

	public IntArray filter(FuncIntBool f) {
		int[] a = new int[this.size];
		int c = 0;
		for (int i = 0; i < this.size; i++) {
			if (f.apply(this.arrays[i])) {
				a[i] = this.arrays[i];
			}
		}
		return new IntArray(a, c);
	}

}
