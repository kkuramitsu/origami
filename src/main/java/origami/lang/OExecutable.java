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

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import origami.OEnv;
import origami.type.OType;
import origami.type.OTypeSystem;

abstract class OExecutable<T extends Executable> extends OCommonMethodHandle {
	protected final OTypeSystem typeSystem;
	protected String localName;
	protected T method;
	protected OMethodDecl mdecl;

	OExecutable(OTypeSystem ts, String lname, T m, OMethodDecl decl) {
		this.typeSystem = ts;
		this.localName = lname;
		this.method = m;
		this.mdecl = decl;
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return typeSystem;
	}

	@Override
	public OMethodDecl getDecl() {
		return mdecl;
	}

	@Override
	public boolean isPublic() {
		if (method == null) {
			return mdecl.isPublic();
		}
		return Modifier.isPublic(method.getModifiers());
	}

	@Override
	public boolean isStatic() {
		if (method == null) {
			return mdecl.isStatic();
		}
		return Modifier.isStatic(this.method.getModifiers());
	}

	// @Override
	// public boolean getInvoation() {
	// if (method == null) {
	// return mdecl.getInvocation();
	// }
	// return this.method.getDeclaringClass().isInterface();
	// }

	@Override
	public OType getDeclaringClass() {
		if (method == null) {
			return mdecl.getDeclaringClass();
		}
		return typeSystem.newType(method.getDeclaringClass());
	}

	@Override
	public void setLocalName(String name) {
		localName = name;
	}

	@Override
	public String getLocalName() {
		if (localName == null) {
			return getName();
		}
		return localName;
	}

	@Override
	public String getName() {
		if (method == null) {
			return mdecl.getName();
		}
		return method.getName();
	}

	@Override
	public String[] getParamNames() {
		if (method == null) {
			return mdecl.getParamNames();
		}
		Parameter[] p = method.getParameters();
		String[] n = new String[p.length];
		for (int i = 0; i < p.length; i++) {
			n[i] = p[i].getName();
		}
		return n;
	}

	@Override
	public OType[] getParamTypes() {
		if (method == null) {
			return mdecl.getParamTypes();
		}
		if (!this.getDeclaringClass().isOrigami()) {
			return typeSystem.newTypes(method.getGenericParameterTypes());
		}
		return typeSystem.newTypes(method.getParameters());
	}

	@Override
	public OType[] getExceptionTypes() {
		if (method == null) {
			return mdecl.getExceptionTypes();
		}
		return typeSystem.newTypes(this.method.getExceptionTypes());
	}

	Class<?>[] params(OEnv env) {
		OType[] pt = this.getParamTypes();
		Class<?>[] p = new Class<?>[pt.length];
		for (int i = 0; i < pt.length; i++) {
			p[i] = pt[i].unwrap(env);
		}
		return p;
	}

	@Override
	public boolean isVarArgs() {
		if (method == null) {
			return mdecl.isVarArgs();
		}
		return method.isVarArgs();
	}

}
