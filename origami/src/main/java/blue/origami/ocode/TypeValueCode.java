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

import java.util.ArrayList;
import java.util.List;

import blue.origami.asm.OCallSite;
import blue.origami.lang.OEnv;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.type.OType;

public class TypeValueCode extends ValueCode {
	public TypeValueCode(OType ty) {
		super(ty, ty.newType(Class.class));
	}

	// public TypeCode(OTypeSystem ts, Class<?> c) {
	// super(TS.unique(c), OType.Class);
	// }

	public OType getTypeValue() {
		return (OType) this.getHandled();
	}

	@Override
	public OCode newApplyCode(OEnv env, OCode... params) {
		OType c = this.getTypeValue();
		return c.newConstructorCode(env, params);
	}

	@Override
	public OCode newMethodCode(OEnv env, String name, OCode... params) {
		OType c = this.getTypeValue();
		List<OMethodHandle> l = new ArrayList<>(8);
		c.listMatchedMethods(name, l, (mh) -> mh.isPublic() && mh.isStatic() && mh.isThisParamSize(params.length));
		// ODebug.trace("found=%s", l);
		return OCallSite.matchParamCode(env, null, name, params, l);
	}

	@Override
	public OCode newGetterCode(OEnv env, String name) {
		OType c = this.getTypeValue();
		return c.newStaticGetterCode(env, name);
	}

}