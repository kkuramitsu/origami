/***********************************************************************
 * Copyright 2017 Kimio Kuramitsu and ORIGAMI project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ***********************************************************************/

package origami.rule.iroha;

import java.util.Arrays;

import origami.ffi.OMutable;
import origami.ffi.ONullable;
import origami.ffi.OrigamiObject;
import origami.nez.ast.Symbol;
import origami.util.StringCombinator;

public class IObject implements StringCombinator, OrigamiObject, Cloneable {
	private final static int Unused = 0;
	private int[] ids;
	private Object[] values;

	private IObject(int capacitySize) {
		this.ids = new int[capacitySize];
		Arrays.fill(ids, Unused);
		this.values = new Object[capacitySize];
	}

	public IObject() {
		this(5);
	}

	public IObject(int[] keys, Object[] array) {
		this(array.length * 2 + 1);
		for (int i = 0; i < keys.length; i++) {
			setf(keys[i], array[i]);
		}
	}

	@Override
	public IObject clone() {
		IObject o = new IObject(ids.length);
		for (int i = 0; i < ids.length; i++) {
			o.ids[i] = this.ids[i];
			o.values[i] = this.ids[i];
		}
		return o;
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
		int[] nids = new int[values.length * 2 + 1];
		Arrays.fill(nids, Unused);
		Object[] nvalues = new Object[values.length * 2 + 1];
		for (int i = 0; i < values.length; i++) {
			set(nids, nvalues, ids[i], values[i]);
		}
		this.ids = nids;
		this.values = nvalues;
	}

	@OMutable
	public final void setf(int key, Object value) {
		if (!set(this.ids, this.values, key, value)) {
			rehash();
			set(this.ids, this.values, key, value);
		}
	}

	@ONullable
	public final Object getf(int key) {
		return getf(key, null);
	}

	public final Object getf(int key, Object def) {
		final int start = key % values.length;
		for (int i = start; i < values.length; i++) {
			if (ids[i] == key) {
				return values[i];
			}
		}
		for (int i = 0; i < start; i++) {
			if (ids[i] == key) {
				return values[i];
			}
		}
		return def;
	}

	@Override
	public void strOut(StringBuilder sb) {
		int cnt = 0;
		sb.append("{");
		cnt = StringCombinator.appendField(sb, this, Object.class, cnt);
		for (int i = 0; i < values.length; i++) {
			if (ids[i] != Unused) {
				if (cnt > 0) {
					sb.append(", ");
				}
				sb.append(Symbol.tag(ids[i]));
				sb.append(": ");
				StringCombinator.appendQuoted(sb, values[i]);
				cnt++;
			}
		}
		sb.append("}");
	}

	@Override
	public String toString() {
		return StringCombinator.stringfy(this);
	}

}
