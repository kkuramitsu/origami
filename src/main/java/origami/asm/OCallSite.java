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

package origami.asm;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.ArrayList;
import java.util.List;

import origami.code.OCastCode;
import origami.code.ODyCode;
import origami.code.OErrorCode;
import origami.code.OCode;
import origami.code.OValueCode;
import origami.ffi.OCast;
import origami.ffi.OrigamiException;
import origami.lang.ODynamicMethodHandle;
import origami.lang.OEnv;
import origami.lang.OMethodHandle;
import origami.lang.OEnv.OListMatcher;
import origami.lang.type.OType;
import origami.util.ODebug;
import origami.util.OTypeUtils;
import origami.util.StringCombinator;

public abstract class OCallSite extends MutableCallSite {

	public static final String Dynamic = "Dynamic";
	public static final String Virtual = "Virtual";

	private final OEnv env0;
	private final String name;

	protected static MethodType dummyMethodType = MethodType.methodType(Object.class);

	protected static OEnv loadEnv(Class<?> entry) {
		return (OEnv) OTypeUtils.loadFieldValue(entry, "entry");
	}

	protected OCallSite(OEnv env, String name, String sig, MethodType methodType) {
		super(methodType == null ? dummyMethodType : methodType);
		this.env0 = env;
		this.name = name;
		if (methodType != null) {
			init(methodType, sig);
		}
	}

	protected OEnv env() {
		return env0;
	}

	private MethodHandle fallBack;
	private MethodHandle guard;
	private Class<?>[] testTypes;

	private void init(MethodType methodType, String sig) {
		try {
			// ODebug.trace("0 target %s", methodType);
			MethodHandle fb = initLoad(Object.class, "fallback" + sig, methodType);
			// ODebug.trace("1 lookuped %s", fb.type(), fb);
			fb = fb.bindTo(this);
			// ODebug.trace("2 bind %s", fb.type(), fb);
			fb = fb.asCollector(Object[].class, methodType.parameterCount());
			// ODebug.trace("3 collect %s", fb.type());
			this.fallBack = fb.asType(methodType);
			// ODebug.trace("4 asType %s", this.fallBack.type());
			this.setTarget(this.fallBack);
			// MethodType guradMethodDtype =
			// MethodType.methodType(boolean.class, new Class<?>[] {
			// Object[].class });
			MethodHandle mh = initLoad(boolean.class, "guard" + sig, methodType);
			this.guard = mh.bindTo(this).asCollector(Object[].class, methodType.parameterCount());
			this.testTypes = new Class<?>[methodType.parameterCount()];
		} catch (Throwable e) {
			ODebug.traceException(e);
		}
	}

	private MethodHandle initLoad(Class<?> ret, String name, MethodType methodType)
			throws NoSuchMethodException, IllegalAccessException {
		MethodType mt = MethodType.methodType(ret, new Class[] { Object[].class });
		return MethodHandles.lookup().findVirtual(this.getClass(), name, mt);
	}

	public Object unfound(Object[] args) throws Throwable {
		StringBuilder sb = new StringBuilder();
		sb.append("unfound " + this.name);
		for (int i = 0; i < args.length; i++) {
			sb.append(" ");
			StringCombinator.append(sb, args[i] == null ? args[i] : env().t(args[i].getClass()));
		}
		throw new OrigamiException(sb.toString());
	}

	private MethodHandle loadUnfoundMethodHandle() throws Throwable {
		MethodType mt = MethodType.methodType(Object.class, new Class[] { Object[].class });
		MethodHandle mh = MethodHandles.lookup().findVirtual(this.getClass(), "unfound", mt);
		return mh.bindTo(this).asCollector(Object[].class, type().parameterCount());
	}

	public final Object apply(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookupDynamic(args);
		return targetHandle.invokeWithArguments(args);
	}

	// public final Object fallback(Object[] args) throws Throwable {
	// MethodHandle targetHandle = lookup(args);
	// return setGuardTest(targetHandle, args);
	// }
	//
	// public boolean guard(Object[] a) {
	// if (testTypes.length != a.length) {
	// return false;
	// }
	// if (testTypes.length > 0) {
	// if (testTypes[0] == null || !testTypes[0].isInstance(a[0])) {
	// return false;
	// }
	// for (int i = 1; i < testTypes.length; i++) {
	// Class<?> paramClass = testTypes[i];
	// Object param = a[i];
	// if (!paramClass.isInstance(param)) {
	// return false;
	// }
	// }
	// }
	// return true;
	// }

	private Object setGuardTest(MethodHandle targetHandle, Object... args) throws Throwable {
		MethodHandle guard = MethodHandles.guardWithTest(this.guard, targetHandle, this.fallBack);
		this.setTarget(guard);
		return targetHandle.invokeWithArguments(args);
	}

	// -----------------------------------------------------------------------------------------------------
	/* Dynamic */

	public final Object fallbackDynamic(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookupDynamic(args);
		return setGuardTest(targetHandle, args);
	}

	public boolean guardDynamic(Object[] a) {
		if (testTypes.length != a.length) {
			return false;
		}
		if (testTypes.length > 0) {
			if (testTypes[0] == null || !testTypes[0].isInstance(a[0])) {
				return false;
			}
			for (int i = 1; i < testTypes.length; i++) {
				Class<?> paramClass = testTypes[i];
				Object param = a[i];
				if (!paramClass.isInstance(param)) {
					return false;
				}
			}
		}
		return true;
	}

	private MethodHandle lookupDynamic(Object[] args) throws Throwable {
		OCode[] params = new OCode[args.length];
		for (int i = 0; i < args.length; i++) {
			this.testTypes[i] = (args[i] == null) ? Object.class : args[i].getClass();
			params[i] = new OValueCode(args[i], env().t(OTypeUtils.unboxType(this.testTypes[i])));
		}
		OCode node = findParamCode2(env(), true, this.name, params);
		if (node instanceof ODyCode) {
			MethodHandles.Lookup lookup = MethodHandles.lookup();
			MethodHandle mh = ((ODyCode) node).getMethodHandle(env(), lookup);
			OCode[] p = node.getParams();
			for (int i = 0; i < p.length; i++) {
				if (p[i] instanceof OCastCode) {
					MethodHandle conv = ((OCastCode) p[i]).getMethodHandle(env(), lookup);
					if (conv != null) {
						mh = MethodHandles.filterArguments(mh, i, conv);
					}
				}
			}
			mh = mh.asType(this.type());
			return mh;
		}
		return loadUnfoundMethodHandle();
	}

	// -----------------------------------------------------------------------------------------------------
	/* Virtual */

	public final Object fallbackVirutal(Object[] args) throws Throwable {
		MethodHandle targetHandle = lookupVirtual(args);
		return setGuardTest(targetHandle, args);
	}

	public boolean guardVirtual(Object[] a) {
		if (this.testTypes.length > 0 && a[0] != null) {
			Class<?> c = a[0].getClass();
			if (this.testTypes[0] != c) {
				this.testTypes[0] = c;
				return false;
			}
		}
		return checkLocalGuard();
	}

	public boolean checkLocalGuard() {
		return true;
	}

	public MethodHandle lookupVirtual(Object[] args) throws Throwable {
		MethodType mt = this.type();
		List<OMethodHandle> l = new ArrayList<>(8);
		if (mt.parameterCount() > 0) {
			Class<?> orig = mt.parameterType(0);
			for (Class<?> callee = args[0] == null ? orig : args[0].getClass(); callee != null; callee = callee
					.getSuperclass()) {
				mt.changeParameterType(0, callee);
				listMatchedMethods(env(), env().t(callee), name, l, (mh) -> mh.isMethodType(mt));
				if (l.size() > 0) {
					mt.changeParameterType(0, orig);
					return l.get(0).getMethodHandle(env(), MethodHandles.lookup()).asType(mt);
				}
				if (orig == callee) {
					break;
				}
			}
		} else {
			listMatchedMethods(env(), null, name, l, (mh) -> mh.isMethodType(mt));
			if (l.size() > 0) {
				return l.get(0).getMethodHandle(env(), MethodHandles.lookup()).asType(mt);
			}
		}
		return loadUnfoundMethodHandle();
	}

	// lookup

	public final static OCode findParamCode(OEnv env, Class<? extends OCallSite> c, String name, OCode... params) {
		OCallSite factory = env.get(c);
		for (OCode p : params) {
			if (p.isUntyped() || p.getType().isDynamic()) {
				OMethodHandle m = new ODynamicMethodHandle(env, factory, name, params.length);
				return m.newMethodCode(env, params);
			}
		}
		return factory.findParamCode2(env, false, name, params);
	}

	public OListMatcher<OMethodHandle> newParamSizeMatcher(int psize) {
		return (mh) -> mh.isThisParamSize(psize);
	}

	// Method

	public final OCode findParamCode(OEnv env, String name, OCode[] params) {
		return this.findParamCode2(env, false, name, params);
	}

	public final OCode findParamCode2(OEnv env, boolean isRuntime, String name, OCode[] params) {
		List<OMethodHandle> l = new ArrayList<>(8);
		listMatchedMethods(env, params.length == 0 ? null : params[0].getType(), name, l,
				(mh) -> mh.isPublic() && mh.isThisParamSize(params.length));
		// ODebug.trace("l = %s", l);
		return matchParamCode(env, isRuntime ? null : this, name, params, l);
	}

	public final static OCode matchParamCode(OEnv env, OCallSite site, String name, OCode[] params,
			List<OMethodHandle> l) {
		if (l.size() == 0) {
			throw new OErrorCode(env, "undefined %s(%s)", name, params);
		}
		OCode start = l.get(0).matchParamCode(env, site, params);
		for (int i = 1; i < l.size(); i++) {
			if (start.getMatchCost() <= 0) {
				return start;
			}
			OCode next = l.get(i).matchParamCode(env, site, params);
			start = (next.getMatchCost() < start.getMatchCost()) ? next : start;
		}
		if (start.getMatchCost() >= OCast.STUPID) {
			// ODebug.trace("miss cost=%d %s", start.getMatchCost(), start);
			throw new OErrorCode(env, "mismatched %s(%s)", name, params);
		}
		return site == null ? start : site.staticCheck(env, start);
	}

	protected OCode staticCheck(OEnv env, OCode matched) {
		return matched;
	}

	public abstract void listMatchedMethods(OEnv env, OType base, String name, List<OMethodHandle> l,
			OListMatcher<OMethodHandle> mat);

	// public void listMatchedMethods(OEnv env, String name, params,
	// List<OMethodHandle> l, OListMatcher<OMethodHandle> mat) {
	// env.findList(name, OMethodHandle.class, l, mat);
	// }

	// public final OType[] findParamTypes(OEnv env, String name, int paramSize)
	// {
	// List<OMethodHandle> l = listMethods(env, name, paramSize);
	// if (l.size() == 0) {
	// return null;
	// }
	// if (paramSize == 0) {
	// OType[] p = new OType[paramSize + 1];
	// OType[] pp = l.get(0).getThisParamTypes();
	// for (int j = 0; j < pp.length; j++) {
	// p[j] = pp[j];
	// }
	// p[paramSize] = l.get(0).getReturnType();
	// for (int i = 1; i < l.size(); i++) {
	// pp = l.get(i).getThisParamTypes();
	// for (int j = 0; j < pp.length; j++) {
	// p[j] = merge(p[j], pp[j]);
	// }
	// p[paramSize] = merge(p[paramSize], l.get(i).getReturnType());
	// if (isBreak(p)) {
	// break;
	// }
	// }
	// return p;
	// }
	// return OType.emptyTypes;
	// }
	//
	// private OType merge(OType t, OType t2) {
	// if (t == null) {
	// return null;
	// }
	// if (t.eq(t2)) {
	// return t;
	// }
	// return null;
	// }
	//
	// private boolean isBreak(OType[] p) {
	// for (int i = 0; i < p.length; i++) {
	// if (p[i] != null) {
	// return false;
	// }
	// }
	// return true;
	// }

}
