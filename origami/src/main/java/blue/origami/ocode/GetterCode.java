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
import java.lang.reflect.Field;

import blue.origami.lang.OEnv;
import blue.origami.lang.OField;
import blue.origami.lang.OGetter;
import blue.origami.lang.type.OType;
import blue.origami.rule.OFmt;
import blue.origami.util.OTypeUtils;

public class GetterCode extends OParamCode<OField> implements ODyCode {

	public GetterCode(OType ret, OField field, OCode recv) {
		super(field, ret, recv);
	}

	public GetterCode(OType ret, OField field) {
		super(field, ret);
	}

	public GetterCode(OField field, OCode recv) {
		super(field, field.getType(), recv);
	}

	public GetterCode(OField field) {
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
			throw new ErrorCode(env, OFmt.read_only__YY0, field.getName());
		}
		if (field.isStatic()) {
			return new SetterCode(field, this.getType(), right);
		} else {
			return new SetterCode(field, this.getType(), this.getParams()[0], right);
		}
	}

}