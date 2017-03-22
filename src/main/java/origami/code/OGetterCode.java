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

package origami.code;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

import origami.OEnv;
import origami.lang.OField;
import origami.lang.OGetter;
import origami.rule.OFmt;
import origami.type.OType;
import origami.util.OTypeUtils;

public class OGetterCode extends OParamCode<OField> implements ODyCode {

	public OGetterCode(OType ret, OField field, OCode recv) {
		super(field, ret, recv);
	}

	public OGetterCode(OType ret, OField field) {
		super(field, ret);
	}

	public OGetterCode(OField field, OCode recv) {
		super(field, field.getType(), recv);
	}

	public OGetterCode(OField field) {
		super(field, field.getType());
	}

	public final boolean isStatic() {
		return this.getParams().length == 0;
	}

	public final boolean isReadOnly() {
		return this.getHandled().isReadOnly;
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		Field field = this.getHandled().unwrap(env);
		if (OTypeUtils.isStatic(field)) {
			return field.get(null);
		} else {
			Object[] values = this.evalParams(env, this.nodes);
			return field.get(values[0]);
		}
	}

	@Override
	public MethodHandle getMethodHandle(OEnv env, MethodHandles.Lookup lookup) throws Throwable {
		return new OGetter(this.getHandled()).getMethodHandle(env, lookup);
	}

	@Override
	public void generate(OGenerator gen) {
		gen.pushGetter(this);
	}

	@Override
	public OCode newAssignCode(OEnv env, OCode right) {
		OField field = this.getHandled();
		if (field.isReadOnly) {
			throw new OErrorCode(env, OFmt.read_only__YY0, field.getName());
		}
		if (field.isStatic()) {
			return new OSetterCode(field, this.getType(), right);
		} else {
			return new OSetterCode(field, this.getType(), this.getParams()[0], right);
		}
	}

}