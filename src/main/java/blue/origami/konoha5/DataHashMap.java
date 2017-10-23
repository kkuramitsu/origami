package blue.origami.konoha5;

import java.util.Arrays;

import blue.origami.common.OStrings;

public class DataHashMap implements DataMap {
	private final static int Unused = 0;
	private int[] keys;
	private Object[] values;

	private DataHashMap(int capacitySize) {
		this.keys = new int[capacitySize];
		this.values = new Object[capacitySize];
		Arrays.fill(this.keys, Unused);
	}

	public DataHashMap() {
		this(5);
	}

	public DataHashMap(int[] keys, Object[] array) {
		this(array.length * 2 + 1);
		for (int i = 0; i < keys.length; i++) {
			this.setf(keys[i], array[i]);
		}
	}

	@Override
	public DataHashMap clone() {
		DataHashMap o = new DataHashMap(this.keys.length);
		for (int i = 0; i < this.keys.length; i++) {
			o.keys[i] = this.keys[i];
			o.values[i] = this.values[i];
		}
		return o;
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append("{");
		// cnt = StringCombinator.appendField(sb, this, Object.class, cnt);
		for (int i = 0; i < this.values.length; i++) {
			if (this.keys[i] != Unused) {
				if (cnt > 0) {
					sb.append(", ");
				}
				sb.append(DSymbol.symbol(this.keys[i]));
				sb.append(": ");
				OStrings.appendQuoted(sb, this.values[i]);
				cnt++;
			}
		}
		sb.append("}");
	}

	@Override
	public String toString() {
		return OStrings.stringfy(this);
	}

	@Override
	public final void setf(int key, Object value) {
		if (!this.set(this.keys, this.values, key, value)) {
			this.rehash();
			this.set(this.keys, this.values, key, value);
		}
	}

	public final Object getf(int key) {
		return this.getf(key, null);
	}

	@Override
	public final Object getf(int key, Object def) {
		final int start = key % this.values.length;
		for (int i = start; i < this.values.length; i++) {
			if (this.keys[i] == key) {
				return this.values[i];
			}
		}
		for (int i = 0; i < start; i++) {
			if (this.keys[i] == key) {
				return this.values[i];
			}
		}
		return def;
	}

	private boolean set(final int[] ids, final Object[] values, final int key, final Object value) {
		final int start = key % values.length;
		for (int i = start; i < values.length; i++) {
			if (ids[i] == key) {
				values[i] = value;
				return true;
			}
			if (ids[i] == Unused) {
				ids[i] = key;
				values[i] = value;
				return true;
			}
		}
		for (int i = 0; i < start; i++) {
			if (ids[i] == key) {
				values[i] = value;
				return true;
			}
			if (ids[i] == Unused) {
				ids[i] = key;
				values[i] = value;
				return true;
			}
		}
		return false;
	}

	private void rehash() {
		int[] nids = new int[this.values.length * 2 + 1];
		Arrays.fill(nids, Unused);
		Object[] nvalues = new Object[this.values.length * 2 + 1];
		for (int i = 0; i < this.values.length; i++) {
			this.set(nids, nvalues, this.keys[i], this.values[i]);
		}
		this.keys = nids;
		this.values = nvalues;
	}

}
