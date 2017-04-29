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

import blue.origami.lang.type.OType;
import blue.origami.lang.type.OTypeSystem;

abstract class OFieldHandle extends OCommonMethodHandle {
	OField field;

	OFieldHandle(OField field) {
		this.field = field;
	}

	@Override
	public boolean isPublic() {
		return field.isPublic();
	}

	@Override
	public boolean isStatic() {
		return field.isStatic();
	}

	@Override
	public boolean isDynamic() {
		return false;
	}

	@Override
	public boolean isSpecial() {
		return false;
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.getDeclaringClass().getTypeSystem();
	}

	@Override
	public OType getDeclaringClass() {
		return this.field.getDeclaringClass();
	}

	@Override
	public String getName() {
		return this.field.getName();
	}

	@Override
	public OType[] getExceptionTypes() {
		return OType.emptyTypes;
	}
}