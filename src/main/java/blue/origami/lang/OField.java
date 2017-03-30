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

import java.lang.reflect.Field;

import blue.origami.asm.OAnno;
import blue.origami.ffi.OMutable;
import blue.origami.ffi.ONullable;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;
import blue.origami.ocode.OCode;
import blue.origami.ocode.GetterCode;
import blue.origami.util.OTypeUtils;

public class OField {
	public final OTypeSystem typeSystem;
	public final boolean isReadOnly;
	public Field field; // compiled
	public OFieldDecl fdecl;

	public OField(OType cbase, OAnno anno, OType type, String name, OCode value) {
		this.typeSystem = cbase.getTypeSystem();
		this.field = null;
		this.fdecl = new OFieldDecl(this, cbase, anno, type, name, value);
		this.isReadOnly = anno.isReadOnly();
	}

	public OField(OTypeSystem ts, Field field) {
		this.typeSystem = ts;
		this.field = field;
		this.isReadOnly = OTypeUtils.isFinal(field);

	}

	public OField(OEnv env, Field field) {
		this(env.getTypeSystem(), field);
	}

	public boolean isReadOnly() {
		return this.isReadOnly;
	}

	public final OTypeSystem getTypeSystem() {
		return this.typeSystem;
	}

	public final OFieldDecl getDecl() {
		return fdecl;
	}

	public boolean isPublic() {
		if (this.field != null) {
			return OTypeUtils.isPublic(this.field);
		}
		return fdecl.isPublic();
	}

	public boolean isStatic() {
		if (this.field != null) {
			return OTypeUtils.isStatic(this.field);
		}
		return fdecl.isStatic();
	}

	public Field unwrap(OEnv env) {
		if (this.field == null) {
			// ODebug.trace("class %s", fdecl.getDeclaringClass());
			this.field = OTypeUtils.loadField(fdecl.getDeclaringClass().unwrap(env), fdecl.getName());
			assert (this.field != null);
		}
		return this.field;
	}

	public OType getDeclaringClass() {
		if (this.field != null) {
			return typeSystem.newType(field.getDeclaringClass());
		}
		return fdecl.getDeclaringClass();
	}

	public String getName() {
		if (this.field != null) {
			return field.getName();
		}
		return fdecl.getName();
	}

	public boolean matchName(String name) {
		return name.equals(this.getName());
	}

	public OType getType() {
		if (this.field != null) {
			OType ty = typeSystem.newType(field.getGenericType());
			if (this.field.getAnnotation(OMutable.class) != null) {
				ty = this.getTypeSystem().newMutableType(ty);
			}
			if (this.field.getAnnotation(ONullable.class) != null) {
				ty = this.getTypeSystem().newNullableType(ty);
			}
			return ty;
		}
		return fdecl.getType();
	}

	public OCode nameCode(OEnv env) {
		assert (this.isStatic());
		return new GetterCode(this);
	}

	public OCode matchParamCode(OEnv env, OCode[] params) {
		// if (params.length == 1) {
		if (this.isStatic()) {
			return new GetterCode(this);
		}
		return new GetterCode(this, params[0]);
		// }
		// return new MismatchedCode(env, new OGetter(this));
	}

	public final static OField[] emptyFields = new OField[0];

}
