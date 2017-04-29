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
import blue.origami.lang.type.OTypeSystem;
import blue.origami.ocode.OCode;
import blue.origami.ocode.SetterCode;
import blue.origami.ocode.UntypedCode;
import blue.origami.ocode.ValueCode;
import blue.origami.rule.java.JavaThisCode;

public class OFieldDecl {
	public final OField field;
	public final OType cbase; // class name
	public final String name;
	public final OAnno anno;
	public final OType type;
	public OCode initValue;

	OFieldDecl(OField field, OType cbase, OAnno anno, OType returnType, String name, OCode value) {
		this.field = field;
		this.cbase = cbase;
		this.anno = anno;
		this.type = returnType;
		this.name = name;
		this.initValue = value;
	}

	public OTypeSystem getTypeSystem() {
		return this.cbase.getTypeSystem();
	}

	public OAnno getAnno() {
		return this.anno;
	}

	public boolean isPublic() {
		return anno.isPublic();
	}

	public boolean isStatic() {
		return anno.isStatic();
	}

	public OType getDeclaringClass() {
		return cbase;
	}

	public OType getType() {
		return type;
	}

	public String getName() {
		return this.name;
	}

	public void typeCheck(OEnv env) {
		if (this.initValue instanceof UntypedCode) {
			this.initValue = ((UntypedCode) initValue).typeCheck(env, this.getType());
		}
	}

	public OCode getInitCode(OEnv env) {
		if (initValue instanceof ValueCode) {
			OType t = initValue.getType();
			// Integer, Long, Float, Double, String
			if (t.isPrimitive() || t.is(String.class)) {
				return null;
			}
		}
		if (this.initValue != null) {
			OCode initCode = null;
			if (this.isStatic()) {
				initCode = new SetterCode(this.field, env.t(void.class), initValue);
			} else {
				initCode = new SetterCode(this.field, env.t(void.class), new JavaThisCode(this.getDeclaringClass()),
						initValue);
			}
			this.initValue = null;
			return initCode;
		}
		return null;
	}

	public Object getInitValue() {
		if (initValue instanceof ValueCode) {
			OType t = initValue.getType();
			// Integer, Long, Float, Double, String
			if (t.isPrimitive() || t.is(String.class)) {
				return ((ValueCode) initValue).getValue();
			}
		}
		return null;
	}

	public Object getValue() {
		if (initValue instanceof ValueCode) {
			return ((ValueCode) initValue).getValue();
		}
		return null;
	}

}
