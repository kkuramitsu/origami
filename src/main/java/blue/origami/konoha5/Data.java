package blue.origami.konoha5;

import java.util.Arrays;

import blue.origami.nez.ast.Symbol;
import blue.origami.util.StringCombinator;

class IRefData extends Number implements Cloneable {
	int value;

	public IRefData(int value) {
		this.value = value;
	}

	@Override
	public int intValue() {
		return this.value;
	}

	@Override
	public long longValue() {
		return this.value;
	}

	@Override
	public float floatValue() {
		return this.value;
	}

	@Override
	public double doubleValue() {
		return this.value;
	}

}

public class Data implements StringCombinator, Cloneable {
	private final static int Unused = 0;
	private int[] keys;
	private Object[] values;

	private Data(int capacitySize) {
		this.keys = new int[capacitySize];
		Arrays.fill(this.keys, Unused);
		this.values = new Object[capacitySize];
	}

	public Data() {
		this(5);
	}

	public Data(int[] keys, Object[] array) {
		this(array.length * 2 + 1);
		for (int i = 0; i < keys.length; i++) {
			this.setf(keys[i], array[i]);
		}
	}

	@Override
	public Data clone() {
		Data o = new Data(this.keys.length);
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
		cnt = StringCombinator.appendField(sb, this, Object.class, cnt);
		for (int i = 0; i < this.values.length; i++) {
			if (this.keys[i] != Unused) {
				if (cnt > 0) {
					sb.append(", ");
				}
				sb.append(Symbol.tag(this.keys[i]));
				sb.append(": ");
				StringCombinator.appendQuoted(sb, this.values[i]);
				cnt++;
			}
		}
		sb.append("}");
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

	public final void setf(int key, Object value) {
		if (!this.set(this.keys, this.values, key, value)) {
			this.rehash();
			this.set(this.keys, this.values, key, value);
		}
	}

	public final Object getf(int key) {
		return this.getf(key, null);
	}

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

	public final int getInt(int key, int def) {
		Object ref = this.getf(key, null);
		if (ref instanceof IRefData) {
			return ((IRefData) ref).value;
		}
		return def;
	}

	public final void setInt(int key, int value) {
		Object ref = this.getf(key, null);
		if (ref instanceof IRefData) {
			((IRefData) ref).value = value;
		} else {
			this.setf(key, new IRefData(value));
		}
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
