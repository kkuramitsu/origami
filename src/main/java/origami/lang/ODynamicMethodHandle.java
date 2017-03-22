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

import java.lang.invoke.MethodType;
import java.util.Arrays;

import org.objectweb.asm.Type;

import origami.asm.OCallSite;
import origami.code.OCode;
import origami.code.OMethodCode;
import origami.lang.type.OType;
import origami.lang.type.OTypeSystem;
import origami.lang.type.OUntypedType;

public class ODynamicMethodHandle extends OCommonMethodHandle {

	private final OEnv env;
	private final OCallSite site;
	private final OType ret;
	private final String name;
	private final int paramSize;

	public ODynamicMethodHandle(OEnv env, OCallSite site, String name, int paramSize) {
		this(env, site, name, env.t(Object.class), paramSize);
	}

	public ODynamicMethodHandle(OEnv env, OCallSite site, String name, OType ret, int paramSize) {
		this.env = env;
		this.site = site;
		this.ret = ret;
		this.name = name;
		this.paramSize = paramSize;
	}

	public OEnv getEnv() {
		return env;
	}

	@Override
	public OCallSite getCallSite() {
		return this.site;
	}

	@Override
	public Object[] getCallSiteParams() {
		return new Object[] { Type.getType(env.findEntryPoint()), OCallSite.Dynamic };
	}

	@Override
	public MethodType methodType() {
		Class<?>[] p = new Class<?>[paramSize];
		Arrays.fill(p, Object.class);
		return MethodType.methodType(ret.unwrapOrNull(Object.class), p);
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.env.getTypeSystem();
	}

	@Override
	public boolean isPublic() {
		return true;
	}

	@Override
	public boolean isStatic() {
		return false;
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public int getInvocation() {
		return OMethodHandle.DynamicInvocation;
	}

	@Override
	public boolean isSpecial() {
		return false;
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public OType getDeclaringClass() {
		return env.t(site.getClass());
	}

	@Override
	public OType getReturnType() {
		return ret;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OType[] getParamTypes() {
		OType[] p = new OType[this.paramSize];
		Arrays.fill(p, env.t(Object.class));
		return p;
	}

	@Override
	public OType[] getExceptionTypes() {
		return OType.emptyTypes;
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		assert (site == null);
		return new OMethodCode(this, env.t(OUntypedType.class), params, matchCost);
	}

	public OCode retype(OMethodCode orig, OCode[] params) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].isUntyped()) {
				return orig;
			}
		}
		return site.findParamCode(env, this.name, params);
	}

}
