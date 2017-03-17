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

package origami.rule;

import java.util.HashMap;
import java.util.Iterator;

import origami.OEnv;
import origami.code.OCode;
import origami.ffi.OrigamiPrimitiveGenerics;
import origami.trait.OStringOut;
import origami.type.OType;

public class OrigamiIterator {

	public static OCode newIteratorCode(OEnv env, OCode code) {
		OType targetType = code.getType();
		if (targetType.getBaseType().is(Iterator.class)) {
			return code;
		}
		if (targetType.isA(Iterable.class)) {
			if (targetType.isOrigami()) {
				return code.newMethodCode(env, "iter");
			}
			return code.newMethodCode(env, "iterator");
		}
		if (targetType.isArray()) {
			OType ctype = targetType.getParamTypes()[0];
			OType iterType = env.t(arrayIteratorMap.getOrDefault(ctype.typeDesc(0), defaultArrayIterator));
			return iterType.newConstructorCode(env, code);
		}
		return env.t(SingleIterator.class).newConstructorCode(env, code);
	}

	public static class SingleIterator<T> implements Iterator<T> {
		T value;

		public SingleIterator(T value) {
			this.value = value;
		}

		@Override
		public boolean hasNext() {
			return value != null;
		}

		@Override
		public T next() {
			T v = this.value;
			this.value = null;
			return v;
		}
	}

	private static HashMap<String, Class<?>> arrayIteratorMap = new HashMap<>();
	private static Class<?> defaultArrayIterator = OArrayIterator.class;
	static {
		arrayIteratorMap.put("I", IArrayIterator.class);
	}

	private static class OArrayIterator<T> extends ARangeIterator<T> {
		T[] a;

		public OArrayIterator(T[] a, int start, int until) {
			super(start, until);
			this.a = a;
		}

		@Override
		public T next() {
			T c = a[cur];
			cur++;
			return c;
		}

	}

	public static <T> Iterator<T> newArrayIterator(T[] a, int start, int end) {
		return new OArrayIterator<>(a, start, end);
	}

	public static Iterator<Integer> newArrayIterator(int[] a, int start, int end) {
		return new IArrayIterator(a, start, end);
	}

	private static class IArrayIterator extends IRangeIterator {
		int[] a;

		public IArrayIterator(int[] a, int start, int until) {
			super(start, until);
			this.a = a;
		}

		@Override
		public int nextp() {
			int c = a[cur];
			cur++;
			return c;
		}
	}

	public static abstract class ARangeIterator<T> implements Iterator<T>, OStringOut {
		int cur;
		int until;

		public ARangeIterator(int cur, int until) {
			this.cur = cur;
			this.until = until;
		}

		@Override
		public final boolean hasNext() {
			return this.cur < this.until;
		}

		@Override
		public void strOut(StringBuilder sb) {
			sb.append("[");
			sb.append(cur);
			sb.append(" .. <");
			sb.append(until);
			sb.append("]");
		}

		@Override
		public String toString() {
			return OStringOut.stringfy(this);
		}
	}

	public static class IRangeIterator extends ARangeIterator<Integer> implements IIterator {

		public IRangeIterator(int cur, int until) {
			super(cur, until);
		}

		@Override
		public int nextp() {
			int c = cur;
			this.cur++;
			return c;
		}

		@Override
		public final Integer next() {
			return nextp();
		}

	}

	public static interface IIterator extends OrigamiPrimitiveGenerics {
		public int nextp();

		public boolean hasNext();
	}

}
