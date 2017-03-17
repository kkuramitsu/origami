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
import java.lang.reflect.Field;

import origami.OEnv;
import origami.asm.OCallSite;
import origami.code.GetterCode;
import origami.code.OCode;
import origami.type.OType;

public class OGetter extends OFieldHandle {

	public OGetter(OField field) {
		super(field);
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public int getInvocation() {
		return this.isStatic() ? OMethodHandle.StaticGetter : OMethodHandle.VirtualGetter;
	}

	@Override
	public OType getReturnType() {
		return this.field.getType();
	}

	@Override
	public int getParamSize() {
		return 0;
	}

	@Override
	public OType[] getParamTypes() {
		return OType.emptyTypes;
	}

	@Override
	public OType[] getThisParamTypes() {
		if (this.isStatic()) {
			return this.getParamTypes();
		}
		return new OType[] { this.getDeclaringClass() };
	}

	@Override
	public Object eval(OEnv env, Object... values) throws Throwable {
		Field f = this.field.unwrap(env);
		if (this.isStatic()) {
			return f.get(null);
		} else {
			return f.get(values[0]);
		}
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws Throwable {
		return lookup.unreflectGetter(this.field.unwrap(env));
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		if (this.isStatic()) {
			return new GetterCode(ret, this.field);
		} else {
			return new GetterCode(ret, this.field, params[0]);
		}
	}

}