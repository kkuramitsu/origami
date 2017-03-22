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

package origami.lang.type;

import org.objectweb.asm.Type;

public class OClassType extends OTypeSystemType {
	private final Class<?> wrapped;

	protected OClassType(OTypeSystem ts, Class<?> t) {
		super(ts);
		this.wrapped = t;
	}

	@Override
	public Class<?> unwrap() {
		return wrapped;
	}

	@Override
	public String getName() {
		return wrapped.getName();
	}

	@Override
	public String getLocalName() {
		return wrapped.getSimpleName();
	}

	@Override
	public void typeDesc(StringBuilder sb, int levelGeneric) {
		sb.append(Type.getDescriptor(unwrap()));
	}

	// @Override
	// public boolean eq(OType t) {
	// return (this == t || this.wrapped == t.unwrap());
	// }

	@Override
	public OType getSupertype() {
		return newType(this.unwrap().getSuperclass());
	}

	@Override
	public OType[] getInterfaces() {
		return newTypes(this.unwrap().getInterfaces());
	}

	@Override
	public boolean isPrimitive() {
		return this.unwrap().isPrimitive();
	}

}
