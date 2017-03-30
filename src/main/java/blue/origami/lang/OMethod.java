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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import blue.origami.asm.OCallSite;
import blue.origami.asm.OClassLoader;
import blue.origami.code.OCode;
import blue.origami.code.OFuncNameCode;
import blue.origami.code.OMethodCode;
import blue.origami.ffi.OMutable;
import blue.origami.ffi.ONullable;
import blue.origami.lang.type.OFuncType;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;

public class OMethod extends OExecutable<Method> implements ONameEntity, OTypeName {

	OMethod(OTypeSystem ts, String lname, Method m, OMethodDecl decl) {
		super(ts, lname, m, decl);
	}

	public OMethod(OEnv env, Method m) {
		this(env.getTypeSystem(), null, m, null);
	}

	public OMethod(OTypeSystem ts, Method m) {
		this(ts, null, m, null);
	}

	public OMethod(OEnv env, OMethodDecl mdecl) {
		this(env.getTypeSystem(), mdecl);
	}

	public OMethod(OTypeSystem ts, OMethodDecl mdecl) {
		this(ts, null, null, mdecl);
	}

	protected Method unwrap(OEnv env) throws Throwable {
		if (this.method == null) {
			// ODebug.trace("loading %s %s", mdecl.getDeclaringClass(), this);
			OClassLoader cl = env.getClassLoader();
			Class<?> c = cl.getCompiledClass(mdecl.getDeclaringClass().getName());
			this.method = c.getMethod(this.getName(), params(env));
			mdecl = null;
		}
		return method;
	}

	@Override
	public boolean isSpecial() {
		return false;
	}

	@Override
	public boolean isDynamic() {
		if (this.method == null) {
			return mdecl.isDynamic();
		}
		return !Modifier.isFinal(this.method.getModifiers());
	}

	@Override
	public int getInvocation() {
		if (this.isStatic()) {
			return OMethodHandle.StaticInvocation;
		}
		if (this.getDeclaringClass().isInterface()) {
			return OMethodHandle.InterfaceInvocation;
		}
		return OMethodHandle.VirtualInvocation;
	}

	@Override
	public boolean isMutable() {
		if (this.getDeclaringClass().isOrigami()) {
			if (this.method == null) {
				return this.mdecl.isMutable();
			}
			return this.method.getAnnotation(OMutable.class) != null;
		}
		return false;
	}

	@Override
	public OType getReturnType() {
		if (this.method == null) {
			return mdecl.getReturnType();
		}
		OType t = typeSystem.newType(method.getGenericReturnType());
		if (this.getDeclaringClass().isOrigami()) {
			if (this.method.getAnnotation(ONullable.class) != null) {
				t = this.getTypeSystem().newNullableType(t);
			}
		}
		return t;
	}

	@Override
	public final Object eval(OEnv env, Object... values) throws Throwable {
		if (this.isStatic()) {
			return unwrap(env).invoke(null, values);
		} else {
			return unwrap(env).invoke(values[0], ltrim(values));
		}
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws Throwable {
		return lookup.unreflect(unwrap(env));
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		return new OMethodCode(this, ret, params, matchCost);
	}

	/* OName */

	@Override
	public boolean isName(OEnv env) {
		return this.isStatic();
	}

	@Override
	public OCode nameCode(OEnv env, String name) {
		return new OFuncNameCode(env, name, this);
	}

	/* OTypeName */

	@Override
	public boolean isTypeName(OEnv env) {
		return this.isStatic();
	}

	@Override
	public OType inferTypeByName(OEnv env) {
		return OFuncType.newType(env, this.getReturnType(), this.getThisParamTypes());
	}

}