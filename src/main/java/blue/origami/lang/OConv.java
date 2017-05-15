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

package blue.origami.lang;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.List;

import blue.origami.asm.OCallSite;
import blue.origami.ffi.OCast;
import blue.origami.lang.OEnv.OListMatcher;
import blue.origami.lang.type.OType;
import blue.origami.nez.ast.SourcePosition;
import blue.origami.ocode.CastCode;
import blue.origami.ocode.OCode;

public class OConv extends OMethodWrapper {
	int matchCost;

	public OConv(OMethodHandle mh, int matchCost) {
		super(mh);
		this.matchCost = matchCost;
	}

	public int getMatchCost() {
		return this.matchCost;
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		return mh.newMatchedParamCode(env, site, ret, params, this.matchCost);
	}

	@Override
	public OCode newMethodCode(OEnv env, OCode... params) {
		return new CastCode(this.getReturnType(), this, params);
	}

	// Utils

	public static void addConv(OEnv env, SourcePosition s, int convCost, Method m) {
		OMethodHandle mh = new OMethod(env, m);
		if (convCost == 0) {
			OCast c = m.getAnnotation(OCast.class);
			if (c != null) {
				convCost = c.cost();
			}
		}
		addConv(env, s, convCost, mh);
	}

	public static void addConv(OEnv env, SourcePosition s, int convCost, OMethodHandle mh) {
		OType[] p = mh.isSpecial() ? mh.getParamTypes() : mh.getThisParamTypes();
		assert (p.length == 1);
		addConv(env, s, p[0], mh.getReturnType(), convCost, mh);
	}

	public static void addConv(OEnv env, SourcePosition s, OType f, OType t, int cost, OMethodHandle m) {
		String key = f.typeDesc(0) + t.typeDesc(0);
		OConv conv = m instanceof OConv ? (OConv) m : new OConv(m, cost);
		env.add(s, key, conv);
		env.add(s, "->" + t.typeDesc(0), conv);
	}

	public static OConv getConv(OEnv env, OType f, OType t) {
		String tkey = t.typeDesc(0);
		for (OType c = f; c != null; c = c.getSupertype()) {
			String key = c.typeDesc(0) + tkey;
			OConv conv = env.get(key, OConv.class);
			// ODebug.trace("%s %s", key, conv);
			if (conv != null) {
				return conv;
			}
		}
		// if (t.isArray()) {
		// OType ctype = t.getParamTypes()[0];
		// if (ctype.eq(f)) {
		// Method m = TypeUtils.loadMethod(OConv.class, "toArray", Object.class,
		// Class.class);
		// return new OExtraConv(env, t, m, OCast.CONV, ctype.unwrap(env));
		// }
		// }
		// if (f.isArray()) {
		// OType ctype = f.getParamTypes()[0];
		// if (ctype.eq(t)) {
		// Method m = TypeUtils.loadMethod(OConv.class, "toArray", Object.class,
		// Class.class);
		// return new OExtraConv(env, t, m, OCast.CONV, ctype.unwrap(env));
		// }
		// }
		return null;
	}

	public static class OExtraConv extends OConv {
		private Object[] values;
		private OType ret;

		public OExtraConv(OEnv env, OType ret, Method m, int matchCost, Object... values) {
			super(new OMethod(env, m), matchCost);
			this.ret = ret;
			this.values = values;
		}

		public OExtraConv(OMethodHandle mh, int matchCost, Object... values) {
			super(mh, matchCost);
			this.values = values;
		}

		@Override
		public OCode newMethodCode(OEnv env, OCode... params) {
			assert (params.length == 1);
			OCode[] a = new OCode[params.length + values.length];
			a[0] = params[0];
			for (int i = 0; i < values.length; i++) {
				a[i + 1] = env.v(values[i]);
			}
			return new CastCode(ret, this, a);
		}

		// @Override
		// public MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws
		// Throwable {
		// MethodHandle target = super.getMethodHandle(env, lookup);
		// MethodHandle filters[] = new MethodHandle[values.length];
		// for (int i = 0; i < values.length; i++) {
		// filters[i] = MethodHandles.constant(values[i].getClass(), values[i]);
		// }
		// return MethodHandles.filterArguments(target, 1, filters);
		// }

	}

	@SuppressWarnings("unchecked")
	public static <T> T[] toArray(T x, Class<T> c) {
		if (x != null) {
			T[] a = (T[]) Array.newInstance(c, 1);
			a[0] = x;
			return a;
		}
		return (T[]) Array.newInstance(c, 0);
	}

	public static <T> T toValue(T[] a) {
		if (a.length > 0) {
			return a[0];
		}
		return null;
	}

	public static class OConvCallSite extends OCallSite {
		public OConvCallSite() {
			super(null, null, null, null);
		}

		private OConvCallSite(OEnv env, String name, String sig, MethodType methodType) {
			super(env, name, sig, methodType);
		}

		public static CallSite bootstrap(MethodHandles.Lookup lookup, String name, MethodType type, Class<?> entry,
				String sig) throws Throwable {
			return new OConvCallSite(loadEnv(entry), name, sig, type);
		}

		@Override
		public void listMatchedMethods(OEnv env, OType f, String name, List<OMethodHandle> l,
				OListMatcher<OMethodHandle> mat) {
			OType t = env().t(type().returnType());
			String key = f.typeDesc(0) + t.typeDesc(0);
			OConv conv = env.get(key, OConv.class);
			if (conv != null) {
				l.add(conv);
			}
		}

	}
}
