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

import origami.asm.OCallSite;
import origami.code.OCode;
import origami.type.OType;
import origami.type.OTypeSystem;

public class OMethodWrapper implements OMethodHandle {
	protected final OMethodHandle mh;

	public OMethodWrapper(OMethodHandle mh) {
		this.mh = mh;
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return mh.getTypeSystem();
	}

	@Override
	public boolean isPublic() {
		return mh.isPublic();
	}

	@Override
	public boolean isStatic() {
		return mh.isStatic();
	}

	@Override
	public boolean isDynamic() {
		return mh.isDynamic();
	}

	@Override
	public int getInvocation() {
		return mh.getInvocation();
	}

	@Override
	public boolean isSpecial() {
		return mh.isSpecial();
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public OType getDeclaringClass() {
		return mh.getDeclaringClass();
	}

	@Override
	public OType getReturnType() {
		return mh.getReturnType();
	}

	@Override
	public String getName() {
		return mh.getName();
	}

	@Override
	public String[] getThisParamNames() {
		return mh.getThisParamNames();
	}

	@Override
	public OType[] getParamTypes() {
		return mh.getParamTypes();
	}

	@Override
	public OType[] getThisParamTypes() {
		return mh.getThisParamTypes();
	}

	@Override
	public OType[] getExceptionTypes() {
		return mh.getExceptionTypes();
	}

	@Override
	public OMethodDecl getDecl() {
		return mh.getDecl();
	}

	@Override
	public Object eval(OEnv env, Object... values) throws Throwable {
		return mh.eval(env, values);
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws Throwable {
		return mh.getMethodHandle(env, lookup);
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		return mh.newMatchedParamCode(env, site, ret, params, matchCost);
	}

}
