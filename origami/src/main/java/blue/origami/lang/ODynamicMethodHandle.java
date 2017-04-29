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

import java.lang.invoke.MethodType;
import java.util.Arrays;

import org.objectweb.asm.Type;

import blue.origami.asm.OCallSite;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.lang.type.OUntypedType;
import blue.origami.ocode.ApplyCode;
import blue.origami.ocode.OCode;

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
		return this.env;
	}

	@Override
	public OCallSite getCallSite() {
		return this.site;
	}

	@Override
	public Object[] getCallSiteParams() {
		return new Object[] { Type.getType(this.env.findExportableEnv().getSingletonClass()), OCallSite.Dynamic };
	}

	@Override
	public MethodType methodType() {
		Class<?>[] p = new Class<?>[this.paramSize];
		Arrays.fill(p, Object.class);
		return MethodType.methodType(this.ret.unwrapOrNull(Object.class), p);
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
		return this.env.t(this.site.getClass());
	}

	@Override
	public OType getReturnType() {
		return this.ret;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OType[] getParamTypes() {
		OType[] p = new OType[this.paramSize];
		Arrays.fill(p, this.env.t(Object.class));
		return p;
	}

	@Override
	public OType[] getExceptionTypes() {
		return OType.emptyTypes;
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		assert (site == null);
		return new ApplyCode(this, env.t(OUntypedType.class), params, matchCost);
	}

	public OCode retype(ApplyCode orig, OCode[] params) {
		for (int i = 0; i < params.length; i++) {
			if (params[i].isUntyped()) {
				return orig;
			}
		}
		return this.site.findParamCode(this.env, this.name, params);
	}

}
