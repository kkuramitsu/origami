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

package blue.origami.rule;

import static blue.origami.rule.OFmt.quote;

import java.util.HashSet;
import java.util.Set;

import blue.nez.ast.Tree;
import blue.origami.asm.OAnno;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OEnv;
import blue.origami.lang.OLocalVariable;
import blue.origami.lang.OMethodDecl;
import blue.origami.lang.OMethodHandle;
import blue.origami.lang.OUntypedMethod;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.ocode.BreakCode;
import blue.origami.ocode.ContinueCode;
import blue.origami.ocode.DefaultValueCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.IfCode;
import blue.origami.ocode.MultiCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.ReturnCode;
import blue.origami.ocode.TryCode;
import blue.origami.ocode.TryCode.CatchCode;
import blue.origami.ocode.WhileCode;
import blue.origami.rule.java.JavaForCode;
import blue.origami.rule.java.JavaSwitchCode;
import blue.origami.rule.java.JavaSwitchCode.CaseCode;
import blue.origami.util.OTypeRule;

public class OrigamiStatementRules implements OImportable {

	public OTypeRule MultiExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			if (t.size() == 0) {
				return new DefaultValueCode(env);
			}
			OCode[] nodes = new OCode[t.size()];
			int last = t.size() - 1;
			for (int i = 0; i < last; i++) {
				nodes[i] = this.typeStmt(env, t.get(i));
			}
			if (last >= 0) {
				nodes[last] = this.typeExpr(env, t.get(last));
			}
			return new MultiCode(nodes);
		}
	};

	public OTypeRule CompilationUnit = this.MultiExpr;
	public OTypeRule Source = this.MultiExpr;

	public OTypeRule BlockExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] nodes = new OCode[t.size()];
			OEnv lenv = env.newEnv();
			int last = t.size() - 1;
			for (int i = 0; i < last; i++) {
				nodes[i] = this.typeStmt(lenv, t.get(i));
			}
			if (last >= 0) {
				nodes[last] = this.typeExpr(lenv, t.get(last));
			}
			return new MultiCode(nodes);
		}
	};

	public OTypeRule MultiStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			if (t.size() == 0) {
				return new EmptyCode(env);
			}
			OCode[] nodes = new OCode[t.size()];
			int last = t.size();
			for (int i = 0; i < last; i++) {
				nodes[i] = this.typeStmt(env, t.get(i));
			}
			return new MultiCode(nodes);
		}
	};

	public OTypeRule BlockStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] nodes = new OCode[t.size()];
			int last = t.size();
			OEnv lenv = env.newEnv();
			for (int i = 0; i < last; i++) {
				nodes[i] = this.typeStmt(lenv, t.get(i));
			}
			return new MultiCode(nodes);
		}
	};

	// public OTypeRule ExportDecl = new TypeRule() {
	// @Override
	// public OCode typeRule(OEnv env, Tree<?> t) {
	// env = findExportableEnv();
	// return new EmptyCode(env);
	// }
	// };

	// private String toPath(Tree<?> t) {
	// StringJoiner sj = new StringJoiner(".");
	// for (Tree<?> tree : t) {
	// sj.add(tree.is(Symbol.unique("NameExpr")) ? tree.toText() :
	// this.toPath(tree));
	// }
	// return sj.toString();
	// }

	public OTypeRule ImportDecl = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			Set<String> option = NoSubSymbols;
			String alias = t.getText(_name, null);
			String path = t.get(_path).toText();
			if (path.endsWith(".*")) {
				option = AllSubSymbols;
				path = path.substring(0, path.length() - 2);
			}
			if (path.endsWith(".")) {
				path = path.substring(0, path.length() - 1);
				option = this.parseSubSymbols(t.get(_param, null), option);
			}
			try {
				Class<?> c = Class.forName(path);
				this.importClass(env, t, c, alias, option);
			} catch (ClassNotFoundException e) {
				throw new ErrorCode(env, t.get(_path), "undefined class: %s by %s", path, e);
			}
			return new EmptyCode(env);
		}

		private Set<String> parseSubSymbols(Tree<?> t, Set<String> option) {
			if (t != null) {
				Set<String> ops = new HashSet<>();
				for (Tree<?> sub : t) {
					ops.add(sub.toText());
				}
				return ops;
			}
			return option;
		}
	};

	public OTypeRule FuncDecl = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OAnno anno = this.parseAnno(env, "public,static,final", t.get(_anno, null));
			String name = t.getText(_name, null);
			String[] paramNames = this.parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null),
					this.getDefaultParamType(env));
			OType returnType = this.parseType(env, t.get(_type, null), env.t(OUntypedType.class));
			OType[] exceptions = this.parseExceptionTypes(env, t.get(_throws, null));
			OCode body = this.parseFuncBody(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions,
					body);
			this.defineName(env, t, mh);
			return new EmptyCode(env);
		}
	};

	public OTypeRule EmptyStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return new EmptyCode(env);
		}
	};

	public OTypeRule ReturnStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = this.getFunctionContext(env);
			if (mdecl == null) {
				throw new ErrorCode(env, t, OFmt.YY0_is_not_here, quote("return"));
			}
			OCode expr = t.has(_expr) ? this.typeExpr(env, t.get(_expr)) : new EmptyCode(env);
			expr = this.typeCheck(env, mdecl.getReturnType(), expr);
			return new ReturnCode(env, expr);
		}
	};

	public OTypeRule ThrowStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = this.getFunctionContext(env);
			if (mdecl == null) {
				throw new ErrorCode(env, t, OFmt.YY0_is_not_here, quote("throw"));
			}
			OCode expr = this.typeCheck(env, env.t(Throwable.class), t.get(_expr));
			return new ReturnCode(env, expr);
		}
	};

	public OTypeRule BreakStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = this.getFunctionContext(env);
			if (mdecl == null) {
				throw new ErrorCode(env, t, OFmt.YY0_is_not_here, quote("break"));
			}
			if (t.has(_expr)) {
				OCode expr = this.typeExpr(env, t.get(_expr));
				return new BreakCode(env, t.getText(_label, null), expr);
			}
			return new BreakCode(env, t.getText(_label, null));
		}
	};

	public OTypeRule ContinueStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = this.getFunctionContext(env);
			if (mdecl == null) {
				throw new ErrorCode(env, t, OFmt.YY0_is_not_here, quote("continue"));
			}
			if (t.has(_expr)) {
				OCode expr = this.typeExpr(env, t.get(_expr));
				return new ContinueCode(env, t.getText(_label, null), expr);
			}
			return new ContinueCode(env, t.getText(_label, null));

		}
	};

	public OTypeRule IfStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = this.typeCondition(env, t.get(_cond, null));
			OCode thenCode = this.typeStmt(env, t.get(_then));
			OCode elseCode = this.typeStmt(env, t.get(_else, null));
			return new IfCode(env, condCode, thenCode, elseCode);
		}
	};

	public OTypeRule WhileStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = this.typeCondition(env, t.get(_cond, null));
			OCode bodyCode = this.typeStmt(env, t.get(_body, null));
			return new WhileCode(env, condCode, bodyCode);
		}
	};

	public OTypeRule TryStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			/* Try Clause */
			OCode tryCode = this.typeStmt(env, t.get(_try));

			/* Catch Clause */
			CatchCode[] catchCodes = new CatchCode[t.size(_catch, 0)];
			if (catchCodes.length > 0) {
				Tree<?> catchNode = t.get(_catch);
				int i = 0;
				for (Tree<?> sub : catchNode) {
					String name = sub.getText(_name, "");
					OType type = this.parseType(env, sub.get(_type, null), env.t(Exception.class));
					OEnv lenv = env.newEnv();
					lenv.add(sub.get(_name), name, new OLocalVariable(name, type));
					OCode clause = this.typeStmt(lenv, sub.get(_body, null));
					catchCodes[i] = new CatchCode(type, name, clause);
					i++;
				}
			}
			/* Finally Clause */
			OCode finallyCode = this.typeStmt(env, t.get(_finally, null));
			return new TryCode(env, tryCode, finallyCode, catchCodes);
		}

	};

	/* java specific syntax */

	// public OTypeRule LabelStmt = new TypeRule() {
	// @Override
	// public OCode typeRule(OEnv env, Tree<?> t) {
	// OCode body = this.typeStmt(env, t.get(_body));
	// return new OLabelBlockCode(t.getText(_label, null), new OEmptyCode(env),
	// body, new OEmptyCode(env));
	// }
	// };

	public OTypeRule ForStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode initCode = this.typeStmt(env, t.get(_init, null));
			OCode condCode = this.typeCondition(env, t.get(_cond, null));
			OCode nextCode = this.typeStmt(env, t.get(_iter, null));
			OCode bodyCode = this.typeStmt(env, t.get(_body, null));
			return new JavaForCode(env, initCode, condCode, nextCode, bodyCode);
		}
	};

	// public OTypeRule DoWhileStmt = new AbstractTypeRule() {
	// @Override
	// public OCode typeRule(OEnv env, Tree<?> t) {
	// OCode condCode = typeCondition(env, t.get(_cond, null));
	// // if (t.has(_cond)) {
	// // condCode = typeExpr(env, t.get(_cond));
	// // condCode = condCode.newUnaryCode(env, "!");
	// // condCode = new IfCode(env, condCode, new BreakCode(env));
	// // }
	// OCode bodyCode = typeStmt(env, t.get(_body));
	// bodyCode = new OMultiCode(bodyCode, condCode);
	// return new CStyleForCode(null, env.v(true), null, bodyCode);
	// }
	// };

	public OTypeRule SwitchStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = this.typeExpr(env, t.get(_cond));
			Tree<?> caseNode = t.get(_body);
			OCode[] caseCodes = new OCode[caseNode.size()];
			int i = 0;
			for (Tree<?> sub : caseNode) {
				OCode cond = this.typeExpr(env, sub.get(_cond, null));
				OCode clause = this.typeStmt(env, sub.get(_body, null));
				Object value = null;
				try {
					value = cond.eval(env);
				} catch (Throwable e) {
				}
				caseCodes[i] = new CaseCode(env, value, cond, clause);
				i++;
			}
			OCode caseCode = null;
			if (caseNode.size() > 0) {
				caseCode = new MultiCode(caseCodes);
			}
			return new JavaSwitchCode(env, condCode, caseCode);
		}
	};

}
