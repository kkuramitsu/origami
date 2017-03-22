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

package origami.lang;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;

import origami.asm.OCallSite;
import origami.asm.OClassLoader;
import origami.code.OCode;
import origami.code.OConstructorCode;
import origami.code.ODefaultValueCode;
import origami.lang.type.OType;
import origami.lang.type.OTypeSystem;
import origami.util.ODebug;

public class OConstructor extends OExecutable<Constructor<?>> {

	OConstructor(OTypeSystem ts, String lname, Constructor<?> m, OMethodDecl decl) {
		super(ts, lname, m, decl);
	}

	public OConstructor(OEnv env, Constructor<?> c) {
		this(env.getTypeSystem(), null, c, null);
	}

	public OConstructor(OTypeSystem ts, Constructor<?> c) {
		super(ts, null, c, null);
	}

	protected Constructor<?> method(OEnv env) throws Throwable {
		if (this.method == null) {
			ODebug.trace("loading %s %s", mdecl.getDeclaringClass(), this);
			OClassLoader cl = env.getClassLoader();
			Class<?> c = cl.getCompiledClass(mdecl.getDeclaringClass().getName());
			this.method = c.getConstructor(params(env));
			mdecl = null;
		}
		return method;
	}

	@Override
	public int getInvocation() {
		return OMethodHandle.SpecialInvocation;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public OType getReturnType() {
		return this.getDeclaringClass().toGenericType();
	}

	@Override
	public String getName() {
		return "<init>";
	}

	@Override
	public final Object eval(OEnv env, Object... values) throws Throwable {
		return method(env).newInstance(values);
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws Throwable {
		return lookup.unreflectConstructor(method(env));
	}

	public static class DefaultThisCode extends ODefaultValueCode {
		public DefaultThisCode(OType ty) {
			super(ty.toGenericType());
		}
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		if (params.length > 0) {
			if (params[0] instanceof DefaultThisCode) {
				params = ltrim(params);
			}
		}
		return new OConstructorCode(this, ret, params, matchCost);
	}

}