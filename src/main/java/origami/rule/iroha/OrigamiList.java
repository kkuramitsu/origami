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

import java.lang.reflect.Array;
import java.util.Iterator;

import origami.ffi.OCast;
import origami.ffi.OMutable;
import origami.ffi.OrigamiObject;
import origami.ffi.OrigamiPrimitiveGenerics;
import origami.ffi.SequenceExtractable;
import origami.lang.type.OParamType;
import origami.lang.type.OType;
import origami.rule.OrigamiIterator;
import origami.util.StringCombinator;

public class OrigamiList implements OrigamiObject {

	public static OType newListType(OType ctype) {
		if (ctype.is(int.class)) {
			return ctype.newType(IList.class);
		}
		return OParamType.of(OList.class, ctype);
	}

	static abstract class AList<T> implements StringCombinator, OrigamiObject, SequenceExtractable<T> {
		protected int start;
		protected int length;

		public AList(int start, int length) {
			this.start = start;
			this.length = length;
		}

		@Override
		public final int size() {
			return length - start;
		}

		protected final int index(int n) {
			return n - this.start;
		}

		@SuppressWarnings("unchecked")
		final <A> A newArray(A a, int newsize, int start, int length) {
			Object na = Array.newInstance(a.getClass().getComponentType(), newsize);
			System.arraycopy(a, start, na, 0, length);
			return (A) na;
		}

		final <A> A extendedArray(A a) {
			int newsize = length * 2;
			if (newsize == 0) {
				newsize = 4;
			}
			if (newsize > 10000) {
				newsize = length + 10000;
			}
			return newArray(a, newsize, start, length);
		}

		@Override
		public abstract T get(int index);

		@OMutable
		public abstract void set(int index, T value);

		@OMutable
		public abstract void add(T value);

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("[");
			for (int i = 0; i < this.size(); i++) {
				if (i > 0) {
					sb.append(",");
				}
				StringCombinator.appendQuoted(sb, this.get(i));
			}
			sb.append("]");
		}

		@Override
		public String toString() {
			return StringCombinator.stringfy(this);
		}

	}

	public static class OList<T> extends AList<T> {
		T[] a;

		public OList(T[] a) {
			super(0, a.length);
			this.a = a;
		}

		@Override
		public final T get(int n) {
			return a[index(n)];
		}

		@Override
		@OMutable
		public final void set(int n, T value) {
			a[index(n)] = value;
		}

		@Override
		@OMutable
		public final void add(T value) {
			if (!(this.length < a.length)) {
				a = this.extendedArray(a);
			}
			a[this.length] = value;
			this.length++;
		}

		@OCast(cost = OCast.LESSSAME)
		public final Iterator<T> toIterator() {
			return OrigamiIterator.newArrayIterator(a, this.start, a.length);
		}

		@OCast(cost = OCast.LESSSAME)
		public final T[] toArray() {
			return this.newArray(a, this.size(), start, this.size());
		}

	}

	@OCast(cost = OCast.SAME)
	public final static <T> OList<T> conv(T[] a) {
		return new OList<>(a);
	}

	public static interface FuncInt extends origami.ffi.OrigamiFunction {
		@Override
		public default Object invoke(Object... a) {
			return apply((Integer) a[0]);
		}

		public int apply(int x);

	}

	public static class IList extends AList<Integer> implements OrigamiPrimitiveGenerics {
		int[] a;

		public IList(int[] a) {
			super(0, a.length);
			this.a = a;
		}

		@Override
		public final Integer get(int n) {
			return a[index(n)];
		}

		@Override
		@OMutable
		public final void set(int n, Integer v) {
			a[index(n)] = v;
		}

		@Override
		@OMutable
		public final void add(Integer v) {
			addp(v);
		}

		public final int getp(int n) {
			return a[index(n)];
		}

		@OMutable
		public final int setp(int n, int value) {
			return a[index(n)] = value;
		}

		@OMutable
		public final void addp(int value) {
			if (!(this.length < a.length)) {
				a = this.extendedArray(a);
			}
			a[this.length] = value;
			this.length++;
		}

		@OCast(cost = OCast.LESSSAME)
		public final Iterator<Integer> toIterator() {
			return OrigamiIterator.newArrayIterator(a, this.start, a.length);
		}

		@OCast(cost = OCast.LESSSAME)
		public final int[] toArray() {
			int[] newa = new int[this.size()];
			System.arraycopy(this.a, this.start, newa, 0, newa.length);
			return newa;
		}
	}

	@OCast(cost = OCast.SAME)
	public final static IList conv(int[] a) {
		return new IList(a);
	}

}
