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

package blue.origami.rule.iroha;

import java.util.Iterator;
import java.util.Objects;

import blue.origami.ffi.Immutable;
import blue.origami.ffi.OrigamiObject;
import blue.origami.rule.OrigamiIterator.IRangeIterator;
import blue.origami.util.StringCombinator;

public class IRange<T extends Number> implements Iterable<Integer>, StringCombinator, OrigamiObject, Immutable {
	final T start;
	final T until;
	final boolean inclusive;

	public IRange(T start, T until, boolean inclusive) {
		this.start = start;
		this.until = until;
		this.inclusive = inclusive;
	}

	public IRange(T start, T until) {
		this(start, until, false);
	}

	public final boolean matchValue(double value) {
		if (this.start != null) {
			if (this.start.doubleValue() > value) {
				return false;
			}
		}
		if (this.until != null) {
			if (this.until.doubleValue() < value || inclusive && this.until.doubleValue() == value) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof IRange<?>) {
			IRange<?> r = (IRange<?>) o;
			return Objects.equals(start, r.start) && Objects.equals(until, r.until) && inclusive == r.inclusive;
		}
		if (o != null) {
			Class<?> c = start == null ? until.getClass() : start.getClass();
			return (c.isInstance(o) && this.matchValue(((Number) o).doubleValue()));
		}
		return false;
	}

	public final IRangeIterator iter() {
		int s = start.intValue();
		int e = until.intValue();
		if (this.inclusive) {
			e++;
		}
		return new IRangeIterator(s, e);
	}

	@Override
	public final Iterator<Integer> iterator() {
		return iter();
	}

	@Override
	public void strOut(StringBuilder sb) {
		sb.append("(");
		StringCombinator.append(sb, this.start);
		sb.append(" to ");
		if (!inclusive) {
			sb.append("<");
		}
		StringCombinator.append(sb, this.until);
		sb.append(")");
	}

	@Override
	public final String toString() {
		return StringCombinator.stringfy(this);
	}

}
