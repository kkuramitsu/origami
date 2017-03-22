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

import origami.OEnv;
import origami.asm.OAnno;
import origami.asm.OCallSite;
import origami.code.OCode;
import origami.code.OWarningCode;
import origami.rule.OFmt;
import origami.type.OType;
import origami.type.OUntypedType;
import origami.util.OLog;

public class OUntypedMethod extends OMethod {
	private OEnv definedEnv;

	OUntypedMethod(OEnv env, OMethodDecl decl) {
		super(env.getTypeSystem(), null, null, decl);
		this.definedEnv = env;
	}

	public static OMethodHandle newFunc(OEnv env, OAnno anno, OType ret, String name, String[] paramNames,
			OType[] paramTypes, OType[] exceptions, OCode body) {
		if (hasUntypedParams(paramTypes)) {
			OMethodDecl mdecl = new OMethodDecl(env.t(OUntypedType.class), anno, ret, name, paramNames, paramTypes,
					exceptions, body);
			return new OUntypedMethod(env, mdecl);
		} else {
			OClassDeclType ct = OClassDeclType.currentType(env);
			OMethodDecl mdecl = new OMethodDecl(ct, anno, ret, name, paramNames, paramTypes, exceptions, body);
			OMethodHandle mh = new OMethod(env, mdecl);
			ct.getDecl().add(mh);
			return mh;
		}
	}

	public static boolean hasUntypedParams(OType[] paramTypes) {
		for (OType t : paramTypes) {
			if (t instanceof OUntypedType) {
				return true;
			}
		}
		return false;
	}

	@Override
	public OCode matchParamCode(OEnv env, OCallSite site, OCode... nodes) {
		int paramSize = this.getThisParamSize();
		// if (nodes.length != paramSize) {
		// if (!this.isVarArgs() || !(paramSize - 1 < nodes.length)) {
		// return new MismatchedCode(env);
		// }
		// }
		OType[] p = this.getThisParamTypes().clone();
		for (int i = 0; i < p.length; i++) {
			if (p[i] instanceof OUntypedType) {
				p[i] = nodes[i].getType().valueType();
			}
		}
		OMethodHandle newmh = this.getDecl().revisedMethod(this.definedEnv, p);
		if (newmh.getReturnType() instanceof OUntypedType) {
			newmh.getDecl().typeCheck(this.definedEnv);
		}
		this.definedEnv.add(this.getName(), newmh);
		OCode node = newmh.matchParamCode(env, site, nodes);
		return new OWarningCode(node, OLog.TypeInfo, OFmt.type_inferred__YY0, newmh);
	}

}
