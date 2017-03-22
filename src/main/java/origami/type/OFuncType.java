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

package origami.type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.function.BiConsumer;

import origami.OEnv;
import origami.asm.OAnno;
import origami.asm.OClassLoader;
import origami.asm.code.LoadArgCode;
import origami.code.OCode;
import origami.code.OMultiCode;
import origami.code.OReturnCode;
import origami.ffi.OrigamiFunction;
import origami.lang.OClassDeclType;
import origami.lang.OMethodHandle;
import origami.util.OArrayUtils;
import origami.util.StringCombinator;

public class OFuncType extends OClassType {

	OFuncType(OTypeSystem ts, Class<?> c) {
		super(ts, c);
	}

	@Override
	public String getLocalName() {
		return StringCombinator.stringfy(this);
	}

	@Override
	public OType[] getParamTypes() {
		Method m = OFuncType.funcMethod(unwrap());
		OType[] p = new OType[m.getParameterCount() + 1];
		p[0] = newType(m.getReturnType());
		Class<?>[] pp = m.getParameterTypes();
		for (int i = 0; i < pp.length; i++) {
			p[i + 1] = newType(pp[i]);
		}
		return p;
	}

	@Override
	public boolean isImmutable() {
		return true;
	}

	@Override
	public void strOut(StringBuilder sb) {
		OType[] p = this.getParamTypes();
		if (p.length == 2) {
			StringCombinator.append(sb, p[1]);
			sb.append("->");
		} else {
			sb.append("(");
			for (int i = 1; i < p.length; i++) {
				if (i > 1) {
					sb.append(",");
				}
				StringCombinator.append(sb, p[i]);
			}
			sb.append(")");
			sb.append("->");
		}
		sb.append(p[0]);
	}

	static FuncType$ companion = new FuncType$();

	public static void addFunctionalInterface(Class<?> c) {
		FuncType$.addInterface(c);
	}

	public static OFuncType newType(OEnv env, OMethodHandle mh) {
		return companion.newFuncType(env, mh.getReturnType(), mh.getThisParamTypes());
	}

	public static OFuncType newType(OEnv env, OType returnType, OType... paramTypes) {
		return companion.newFuncType(env, returnType, paramTypes);
	}

	public static Method funcMethod(Class<?> c) {
		return companion.funcMethod(c);
	}

	public static String funcName(Class<?> c) {
		return companion.funcName(c);
	}

	public static OCode newFuncCode(OEnv env, OMethodHandle mh) {
		return companion.newFuncCode(env, mh).refineType(env, newType(env, mh));
	}

}

class FuncType$ implements OArrayUtils { /* Companions */

	public static OAnno A(String anno) {
		return new OAnno(anno);
	}

	public OFuncType newFuncType(OEnv env, OType returnType, OType... paramTypes) {
		OClassLoader cl = env.getClassLoader();
		String cname = cl.uniqueName("FuncType", append(returnType, paramTypes));
		Class<?> c = cl.getCompiledClass(cname);
		if (c == null) {
			OTypeSystem ts = returnType.getTypeSystem();
			OClassDeclType ct = cl.newType(env, A("public,abstract,interface"), cname, emptyTypes, env.t(Object.class),
					funcInterface(returnType, paramTypes));
			ct.addMethod(A("public,abstract"), returnType, "apply", emptyNames, paramTypes, emptyTypes, (OCode) null);
			c = ct.unwrap(env);
			return ts.define(new OFuncType(ts, c));
		}
		return (OFuncType) returnType.newType(c);
	}

	static HashMap<String, Class<?>> funcInterfaceMap = new HashMap<>();

	static void addInterface(Class<?> c) {
		assert (c.isInterface());
		Method[] ma = c.getMethods();
		// ODebug.trace("%s %s", c, ma.length);
		for (Method m : ma) {
			// ODebug.trace("%s", m);
		}
	}

	static {
		addInterface(BiConsumer.class);
	}

	private OType[] funcInterface(OType returnType, OType... paramTypes) {
		/* TODO */
		/* Here set proper interface in java.util.function */
		return returnType.newTypes(OrigamiFunction.class);
	}

	Method funcMethod(Class<?> c) {
		if (c.isInterface()) {
			Method[] m = c.getDeclaredMethods();
			if (m.length == 1) {
				return m[0];
			}
			return null;
		}
		for (Method m : c.getMethods()) {
			if (m.getName().equals("apply")) {
				return m;
			}
		}
		return null;
	}

	String funcName(Class<?> c) {
		Method m = funcMethod(c);
		if (m != null) {
			return m.getName();
		}
		return "apply";
	}

	private String funcNameId(OMethodHandle m) {
		StringBuilder sb = new StringBuilder();
		m.getDeclaringClass().typeDesc(sb, 1);
		sb.append(m.getName());
		sb.append("(");
		for (OType t : m.getParamTypes()) {
			t.typeDesc(sb, 1);
		}
		sb.append(")");
		m.getReturnType().typeDesc(sb, 1);
		return sb.toString();
	}

	private HashMap<String, OType> funcNameMap = new HashMap<>();

	public OType loadSimpleFunctionType(OEnv env, OMethodHandle mh) {
		String id = funcNameId(mh);
		OType ft = funcNameMap.get(id);
		if (ft != null) {
			return ft;
		}
		String cname = "$$Func$" + mh.getName() + funcNameMap.size();
		OClassLoader cl = env.getClassLoader();
		OType[] paramTypes = mh.isStatic() ? mh.getParamTypes() : mh.getThisParamTypes();
		OType funcType = newFuncType(env, mh.getReturnType(), paramTypes);
		OClassDeclType ct = cl.newType(env, A("public"), cname, emptyTypes, env.t(Object.class), funcType);
		OCode[] codes = new OCode[paramTypes.length];
		for (int i = 0; i < paramTypes.length; i++) {
			codes[i] = new LoadArgCode(i, paramTypes[i]);
		}
		OCode code = new OMultiCode(new OReturnCode(env, mh.newMethodCode(env, codes)));
		ct.addMethod(A("public,final"), mh.getReturnType(), "apply", mh.getParamNames(), mh.getParamTypes(), emptyTypes,
				code);
		// ct.getDecl().addDefaultConstructors();
		Class<?> c = ct.unwrap(env);
		ft = env.getTypeSystem().newType(c);
		funcNameMap.put(id, ft);
		return ft;
	}

	public OCode newFuncCode(OEnv env, OMethodHandle mh) {
		OType c = loadSimpleFunctionType(env, mh);
		return c.newConstructorCode(env);
	}

	// /* PolyFuncType */
	//
	// final static Comparator<Class<?>> classCompr = new Comparator<Class<?>>()
	// {
	// @Override
	// public int compare(Class<?> o1, Class<?> o2) {
	// return o1.getName().compareTo(o2.getName());
	// }
	// };
	//
	// public final static Class<?> newPolyType(OEnv env, Class<?>... funcTypes)
	// {
	// IClassLoader cl = env.getClassLoader();
	// Arrays.sort(funcTypes, classCompr);
	// String cname = cl.uniqueClassName("PolyFuncType",
	// cl.uniqueId(funcTypes));
	// Class<?> c = cl.getGeneratedClass(cname);
	// if (c == null) {
	// if (!cl.isDefined(IPolyFuncTypeHandle.class)) {
	// cl.addTypeHandle(new IPolyFuncTypeHandle());
	// }
	// IAsm asm = new IAsm(cl);
	// asm.openClass(ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE, cname, null,
	// Object.class, IRule.append(IPolyFuncType.class, funcTypes));
	// for (Class<?> f : funcTypes) {
	// Method m = funcMethod(f);
	// asm.addAbstractMethod(ACC_PUBLIC, m.getReturnType(),
	// IOption.ApplyMethodName, m.getParameterTypes());
	// }
	// c = asm.closeClass();
	// cl.setGeneratedClass(cname, c);
	// }
	// return c;
	// }
	//
	// public static interface IPolyFuncType {
	//
	// }
	//
	// public static class IPolyFuncTypeHandle extends ITypeHandle {
	//
	// @Override
	// public boolean matchHandle(Class<?> c) {
	// return IPolyFuncType.class.isAssignableFrom(c);
	// }
	//
	// @Override
	// public Class<?> baseType(Class<?> c) {
	// return IFuncType.class;
	// }
	//
	// @Override
	// public Class<?> superType(Class<?> c) {
	// return c.getSuperclass();
	// }
	//
	// @Override
	// public Class<?>[] paramTypes(Class<?> c) {
	// return IHandle.emptyTypes;
	// }
	//
	// @Override
	// public Class<?> newType(OEnv env, Class<?> c, Class<?>[] paramTypes) {
	// return c;
	// }
	//
	// @Override
	// public Class<?> valueType(Class<?> t) {
	// for (Class<?> i : t.getInterfaces()) {
	// String n = i.getSimpleName();
	// if (n.startsWith("PolyFuncType$")) {
	// return i;
	// }
	// }
	// return t;
	// }
	//
	// @Override
	// public void format(OEnv env, Class<?> c, StringBuilder sb) {
	// int cnt = 0;
	// for (Class<?> i : c.getInterfaces()) {
	// String n = i.getSimpleName();
	// if (n.startsWith("FuncType$")) {
	// if (cnt > 0) {
	// sb.append("|");
	// }
	// IFormat.formatClass(env, i, sb);
	// cnt++;
	// }
	// }
	// }
	// }

}