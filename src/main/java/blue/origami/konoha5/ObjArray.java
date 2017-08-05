package blue.origami.konoha5;

import blue.origami.konoha5.Func.FuncIntObj;
import blue.origami.konoha5.Func.FuncObjBool;
import blue.origami.konoha5.Func.FuncObjInt;
import blue.origami.konoha5.Func.FuncObjObj;
import blue.origami.konoha5.Func.FuncObjVoid;

public class ObjArray extends Data implements FuncIntObj {
	private Object[] arrays = null;
	private int size = 0;

	public ObjArray(int capacity) {
		super(DataMap.Null);
		this.ensure(capacity);
	}

	@Override
	public Object apply(int index) {
		return this.arrays[index];
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
			this.arrays = new Object[Math.max(4, capacity)];
		} else if (this.arrays.length <= capacity) {
			Object[] na = new Object[Math.max(this.arrays.length * 2, capacity)];
			System.arraycopy(this.arrays, 0, na, 0, this.arrays.length);
			this.arrays = na;
		}
	}

	public ObjArray push(Object v) {
		this.ensure(this.size);
		this.arrays[this.size++] = v;
		return this;
	}

	public Object pop() {
		this.size--;
		return this.arrays[this.size];
	}

	public int size() {
		return this.size;
	}

	public Object geti(int index) {
		return this.arrays[index];
	}

	public void seti(int index, Object value) {
		this.arrays[index] = value;
	}

	public void forEach(FuncObjVoid f) {
		for (int i = 0; i < this.size; i++) {
			f.apply(this.arrays[i]);
		}
	}

	public ObjArray(Object[] arrays, int size) {
		super(DataMap.Null);
		this.arrays = arrays;
		this.size = size;
	}

	public IntArray map(FuncObjInt f) {
		int[] a = new int[this.size];
		for (int i = 0; i < this.size; i++) {
			a[i] = f.apply(this.arrays[i]);
		}
		return new IntArray(a, a.length);
	}

	public ObjArray map(FuncObjObj f) {
		Object[] a = new Object[this.size];
		for (int i = 0; i < this.size; i++) {
			a[i] = f.apply(this.arrays[i]);
		}
		return new ObjArray(a, a.length);
	}

	public ObjArray filter(FuncObjBool f) {
		Object[] a = new Object[this.size];
		int c = 0;
		for (int i = 0; i < this.size; i++) {
			if (f.apply(this.arrays[i])) {
				a[i] = this.arrays[i];
			}
		}
		return new ObjArray(a, c);
	}

}