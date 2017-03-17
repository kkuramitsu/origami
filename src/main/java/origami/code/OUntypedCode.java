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

import origami.ODebug;
import origami.OEnv;
import origami.asm.OAsm;
import origami.lang.OLocalVariable;
import origami.lang.OMethodDecl;
import origami.nez.ast.Tree;
import origami.rule.SyntaxAnalysis;
import origami.rule.TypeAnalysis;
import origami.type.OType;
import origami.type.OUntypedType;
import origami.type.OVarType;

public class OUntypedCode extends OParamCode<Tree<?>> implements SyntaxAnalysis, TypeAnalysis {

	public OUntypedCode(OEnv env, Tree<?> t) {
		super(t, env.t(OUntypedType.class));
	}

	public Tree<?> getSyntaxTree() {
		return this.getHandled();
	}

	@Override
	public Object eval(OEnv env) throws Throwable {
		return this.getHandled();
	}

	@Override
	public void generate(OAsm gen) {
		ODebug.NotAvailable();
	}

	public OCode typeCheck(OEnv env, OType type) {
		return typeCheck(env, type, this.getSyntaxTree());
	}

	public OCode typeCheck(OEnv env0, OMethodDecl mdecl) {
		// ODebug.trace("typing body: %s", mdecl);
		OEnv env = env0.newEnv();
		setFunctionContext(env, mdecl);
		OType[] op = mdecl.getThisParamTypes();
		String[] names = mdecl.getThisParamNames();
		for (int i = 0; i < op.length; i++) {
			env.add0(names[i], new OLocalVariable(true, names[i], op[i]));
			// ODebug.trace("'%s' %s", names[i], env.get(names[i],
			// NameDecl.class));
		}
		OCode typedCode = null;
		Tree<?> t = this.getSyntaxTree();
		if (mdecl.returnType.isUntyped()) {
			mdecl.returnType = new OVarType("return", mdecl.returnType);
			typedCode = typeBlock(env, mdecl, t);
			ODebug.trace("varType=%s", mdecl.returnType);
			mdecl.returnType = ((OVarType) mdecl.returnType).thisType();
		} else {
			typedCode = typeBlock(env, mdecl, t);
		}
		// ODebug.trace("code %s : %s => %s", t, codeType, code);
		ODebug.trace("typed method: %s", this);
		return typedCode;
	}

	private OCode typeBlock(OEnv env, OMethodDecl mdecl, Tree<?> t) {
		OCode code = typeExprOrErrorCode(env, t);
		code = typeCheck(env, mdecl.getReturnType(), code);
		if (!code.hasReturnCode()) {
			code = new OReturnCode(env, code);
		}
		return code;
	}

}
