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

import origami.asm.OCallSite;
import origami.code.OCode;
import origami.code.OSetterCode;
import origami.lang.type.OType;

public class OSetter extends OFieldHandle {

	public OSetter(OField field) {
		super(field);
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public int getInvocation() {
		return this.isStatic() ? OMethodHandle.StaticSetter : OMethodHandle.VirtualSetter;
	}

	@Override
	public OType getReturnType() {
		return this.getTypeSystem().newType(void.class);
	}

	@Override
	public int getParamSize() {
		return 1;
	}

	@Override
	public OType[] getParamTypes() {
		return new OType[] { this.field.getType() };
	}

	@Override
	public OType[] getThisParamTypes() {
		if (this.isStatic()) {
			return this.getParamTypes();
		}
		return new OType[] { this.getDeclaringClass().toGenericType(), this.field.getType() };
	}

	@Override
	public Object eval(OEnv env, Object... values) throws Throwable {
		Field f = this.field.unwrap(env);
		if (this.isStatic()) {
			f.set(null, values[0]);
		} else {
			f.set(values[0], values[1]);
		}
		return null;
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, Lookup lookup) throws Throwable {
		return lookup.unreflectSetter(this.field.unwrap(env));
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		if (this.isStatic()) {
			return new OSetterCode(this.field, ret, params[1]);
		} else {
			return new OSetterCode(this.field, ret, params[0], params[1]);
		}
	}
}