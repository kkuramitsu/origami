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

import blue.origami.asm.OAnno;
import blue.origami.lang.type.OType;
import blue.origami.ocode.OCode;
import blue.origami.ocode.GetterCode;
import blue.origami.ocode.SetterCode;

public class OGlobalVariable extends OVariable {
	private final OField field;

	public OGlobalVariable(OEnv env, OAnno anno, String name, OType type, OCode expr) {
		super(anno.isReadOnly(), name, type);
		OClassDeclType ct = OClassDeclType.currentType(env);
		name = ct.getDecl().uniqueFieldName(name);
		this.field = ct.addField(anno, type, name, expr);
	}

	public OGlobalVariable(OField field) {
		super(field.isReadOnly(), field.getName(), field.getType());
		this.field = field;
	}

	public OField getField() {
		return this.field;
	}

	@Override
	public OCode nameCode(OEnv env, String name) {
		return new GetterCode(this.field);
	}

	@Override
	public OCode defineCode(OEnv env, OCode right) {
		return new SetterCode(this.field, this.getType(), right);
	}

}