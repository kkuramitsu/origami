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

import origami.ODebug;
import origami.OEnv;
import origami.asm.OAnno;
import origami.asm.OCallSite;
import origami.code.OCode;
import origami.code.OUntypedCode;
import origami.type.OType;
import origami.type.OTypeSystem;
import origami.type.OUntypedType;

public class OMethodDecl extends OCommonMethodHandle {
	public final OType cbase; // class name
	public final String name;
	public final OAnno anno;
	public final String[] paramNames;
	public final OType[] paramTypes;
	public final OType[] exceptions;

	public OType returnType;
	public OCode body;

	public OMethodDecl(OType cbase, OAnno anno, OType returnType, String name, String[] paramNames, OType[] paramTypes,
			OType[] exceptions, OCode body) {
		this.cbase = cbase;
		this.anno = anno;
		this.returnType = returnType;
		this.name = name;
		this.paramNames = paramNames;
		this.paramTypes = paramTypes;
		this.exceptions = exceptions == null ? OType.emptyTypes : exceptions;
		this.body = body;
	}

	@Override
	public OTypeSystem getTypeSystem() {
		return this.cbase.getTypeSystem();
	}

	public OAnno getAnno() {
		return this.anno;
	}

	@Override
	public boolean isPublic() {
		return anno.isPublic();
	}

	@Override
	public boolean isStatic() {
		return anno.isStatic();
	}

	@Override
	public boolean isDynamic() {
		return !anno.isFinal();
	}

	@Override
	public boolean isSpecial() {
		return this.name.equals("<init>");
	}

	@Override
	public int getInvocation() {
		if (this.isSpecial()) {
			return OMethodHandle.SpecialInvocation;
		}
		if (this.isStatic()) {
			return OMethodHandle.StaticInvocation;
		}
		if (this.getDeclaringClass().isInterface()) {
			return OMethodHandle.InterfaceInvocation;
		}
		return OMethodHandle.VirtualInvocation;
	}

	@Override
	public boolean isMutable() {
		ODebug.TODO();
		return false;
	}

	@Override
	public OType getDeclaringClass() {
		return cbase;
	}

	@Override
	public OType getReturnType() {
		return returnType;
	}

	// @Override
	// public void setReturnType(OType t) {
	// this.returnType = t;
	// }

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String[] getParamNames() {
		return this.paramNames;
	}

	@Override
	public OType[] getParamTypes() {
		return this.paramTypes;
	}

	public boolean hasUntypedParams() {
		for (OType t : this.getParamTypes()) {
			if (t instanceof OUntypedType) {
				return true;
			}
		}
		return false;
	}

	@Override
	public OType[] getExceptionTypes() {
		return this.exceptions;
	}

	@Override
	public OCode newMatchedParamCode(OEnv env, OCallSite site, OType ret, OCode[] params, int matchCost) {
		ODebug.NotAvailable();
		return null;
	}

	public String getSignature() {
		OType[] p = this.getParamTypes();
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		for (int i = 0; i < p.length; i++) {
			p[i].typeDesc(sb, 2);
		}
		sb.append(")");
		this.getReturnType().typeDesc(sb, 2);
		return sb.toString();
	}

	public OMethodHandle revisedMethod(OEnv env, OType[] p) {
		OClassDeclType ct = OClassDeclType.currentType(env);
		OMethodDecl mdecl = new OMethodDecl(ct, anno, this.returnType, name, paramNames, p, exceptions, body);
		OMethodHandle mh = new OMethod(env, mdecl);
		ct.getDecl().add(mh);
		return mh;
	}

	private boolean isTypeChecking = false;

	public void typeCheck(OEnv env0) {
		if (!this.isTypeChecking && this.body instanceof OUntypedCode) {
			this.isTypeChecking = true;
			this.body = ((OUntypedCode) this.body).typeCheck(env0, this);
			this.isTypeChecking = false;
		}
	}

}