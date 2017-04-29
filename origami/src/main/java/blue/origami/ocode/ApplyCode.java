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

package blue.origami.ocode;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import blue.origami.ffi.OCast;
import blue.origami.lang.ODynamicMethodHandle;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethod;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.type.OType;
import blue.origami.util.OArrayUtils;

public class ApplyCode extends OMatchedCode<OMethodHandle> implements ODyCode, OArrayUtils {

	public ApplyCode(OMethodHandle method, OType ty, OCode[] nodes, int cost) {
		super(method, ty, nodes, cost);
	}

	public ApplyCode(OMethodHandle method, OCode[] nodes, int cost) {
		super(method, method.getReturnType(), nodes, cost);
	}

	public ApplyCode(OMethodHandle method, OCode... nodes) {
		super(method, method.getReturnType(), nodes, OCast.SAME);
	}

	public ApplyCode(OEnv env, Method method, OCode... nodes) {
		this(new OMethod(env, method), nodes, OCast.SAME);
	}

	public OMethodHandle getMethod() {
		return this.getHandled();
	}

	@Override
	public OCode refineType(OEnv env, OType t) {
		if (t.is(void.class)) {
			this.setType(t);
		}
		return this;
	}

	@Override
	public OCode retypeLocal() {
		OMethodHandle m = this.getMethod();
		if (m instanceof ODynamicMethodHandle) {
			return ((ODynamicMethodHandle) m).retype(this, this.getParams());
		}
		return this;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Object[] values = this.evalParams(env, this.nodes);
		return this.getHandled().eval(env, values);
	}

	/* Asm interface */

	@Override
	public void generate(OGenerator gen) {
		gen.pushMethod(this);
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, MethodHandles.Lookup lookup) throws Throwable {
		if (this.getMethod() == null) {
			return null;
		}
		return this.getMethod().getMethodHandle(env, lookup);
	}

}