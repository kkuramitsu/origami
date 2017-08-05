package blue.origami.konoha5;

import blue.origami.util.StringCombinator;

public class Data implements StringCombinator, Cloneable {

	public DataMap map;

	protected Data(DataMap map) {
		this.map = map;
	}

	public Data(int[] keys, Object[] values) {
		this.map = new DataHashMap(keys, values);
	}

	@Override
	public Data clone() {
		return new Data(this.map.clone());
	}

	@Override
	public void strOut(StringBuilder sb) {
		this.map.strOut(sb);
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

	public boolean hasf(int key) {
		return this.map.getf(key, null) != null;
	}

	public Object getf(int key, Object value) {
		return this.map.getf(key, value);
	}

	public void setf(int key, Object value) {
		this.map.setf(key, value);
	}

	// private Object[] arrays = null;
	// private int size = 0;
	//
	// public Data(int capacity) {
	// this.ensure(capacity);
	// }
	//
	// private void ensure(int capacity) {
	// if (this.arrays == null) {
	// this.arrays = new Object[Math.max(4, capacity)];
	// } else if (this.arrays.length <= capacity) {
	// Object[] na = new Object[Math.max(this.arrays.length * 2, capacity)];
	// System.arraycopy(this.arrays, 0, na, 0, this.arrays.length);
	// this.arrays = na;
	// }
	// }
	//
	// public Data push(Object v) {
	// this.ensure(this.size);
	// this.arrays[this.size++] = v;
	// return this;
	// }
	//
	// public Object pop() {
	// this.size--;
	// return this.arrays[this.size];
	// }
	//
	// public int size() {
	// return this.size;
	// }
	//
	// public Object geti(int index) {
	// return this.arrays[index];
	// }
	//
	// public void forEach(FuncIntVoid f) {
	// for (int i = 0; i < this.size; i++) {
	// Object v = this.arrays[i];
	// if (v instanceof Number) {
	// f.apply(((Number) v).intValue());
	// }
	// }
	// }
	//
	// private Data(Object[] arrays, int size) {
	// this.arrays = arrays;
	// this.size = size;
	// }
	//
	// public Data map(FuncIntInt f) {
	// ArrayList<Object> a = new ArrayList<>(this.size);
	// for (int i = 0; i < this.size; i++) {
	// Object v = this.arrays[i];
	// if (v instanceof Number) {
	// a.add(f.apply(((Number) v).intValue()));
	// }
	// }
	// return new Data(a.toArray(new Object[a.size()]), a.size());
	// }
	//
	// public Data filter(FuncIntBool f) {
	// ArrayList<Object> a = new ArrayList<>(this.size);
	// for (int i = 0; i < this.size; i++) {
	// Object v = this.arrays[i];
	// if (v instanceof Number && f.apply(((Number) v).intValue())) {
	// a.add(v);
	// }
	// }
	// return new Data(a.toArray(new Object[a.size()]), a.size());
	// }

}
