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

package origami.rule;

import static origami.rule.OFmt.quote;

import java.util.HashSet;
import java.util.Set;
import java.util.StringJoiner;

import origami.asm.OAnno;
import origami.code.OBreakCode;
import origami.code.OCode;
import origami.code.OContinueCode;
import origami.code.ODefaultValueCode;
import origami.code.OEmptyCode;
import origami.code.OErrorCode;
import origami.code.OIfCode;
import origami.code.OLabelBlockCode;
import origami.code.OMethodCode;
import origami.code.OMultiCode;
import origami.code.OReturnCode;
import origami.code.OTryCode;
import origami.code.OTryCode.CatchCode;
import origami.ffi.OImportable;
import origami.lang.OEnv;
import origami.lang.OLocalVariable;
import origami.lang.OMethodDecl;
import origami.lang.OMethodHandle;
import origami.lang.OUntypedMethod;
import origami.lang.type.OType;
import origami.lang.type.OUntypedType;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.rule.java.JavaForCode;
import origami.rule.java.JavaSwitchCode;
import origami.rule.java.JavaSwitchCode.CaseCode;
import origami.util.OArrayUtils;
import origami.util.ODebug;
import origami.util.OScriptUtils;
import origami.util.OTypeRule;
import origami.util.OTypeUtils;

public class StatementRules implements OImportable, OScriptUtils, SyntaxAnalysis, OArrayUtils {

	public OTypeRule MultiExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			if (t.size() == 0) {
				return new ODefaultValueCode(env);
			}
			OCode[] nodes = new OCode[t.size()];
			int last = t.size() - 1;
			for (int i = 0; i < last; i++) {
				nodes[i] = StatementRules.this.typeStmt(env, t.get(i));
			}
			if (last >= 0) {
				nodes[last] = StatementRules.this.typeExpr(env, t.get(last));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule BlockExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] nodes = new OCode[t.size()];
			OEnv lenv = env.newEnv();
			int last = t.size() - 1;
			for (int i = 0; i < last; i++) {
				nodes[i] = StatementRules.this.typeStmt(lenv, t.get(i));
			}
			if (last >= 0) {
				nodes[last] = StatementRules.this.typeExpr(lenv, t.get(last));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule MultiStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			if (t.size() == 0) {
				return new OEmptyCode(env);
			}
			OCode[] nodes = new OCode[t.size()];
			int last = t.size();
			for (int i = 0; i < last; i++) {
				nodes[i] = StatementRules.this.typeStmt(env, t.get(i));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule BlockStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] nodes = new OCode[t.size()];
			int last = t.size();
			OEnv lenv = env.newEnv();
			for (int i = 0; i < last; i++) {
				nodes[i] = StatementRules.this.typeStmt(lenv, t.get(i));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule Source = this.MultiExpr;

	private String toPath(Tree<?> t) {
		StringJoiner sj = new StringJoiner(".");
		for (Tree<?> tree : t) {
			sj.add(tree.is(Symbol.unique("NameExpr")) ? tree.toText() : this.toPath(tree));
		}
		return sj.toString();
	}

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
				StatementRules.this.importClass(env, t, c, alias, option);
			} catch (ClassNotFoundException e) {
				throw new OErrorCode(env, t.get(_path), "undefined class: %s by %s", path, e);
			}
			return new OEmptyCode(env);
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
			OAnno anno = StatementRules.this.parseAnno(env, "public,static,final", t.get(_anno, null));
			String name = t.getText(_name, null);
			String[] paramNames = StatementRules.this.parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = StatementRules.this.parseParamTypes(env, paramNames, t.get(_param, null),
					StatementRules.this.getDefaultParamType(env));
			OType returnType = StatementRules.this.parseType(env, t.get(_type, null), env.t(OUntypedType.class));
			OType[] exceptions = StatementRules.this.parseExceptionTypes(env, t.get(_throws, null));

			OCode body = StatementRules.this.parseUntypedCode(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions,
					body);
			StatementRules.this.defineName(env, t, mh);
			return new OEmptyCode(env);
		}
	};

	public OTypeRule DyFuncDecl = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OAnno anno = StatementRules.this.parseAnno(env, "public,static", t.get(_anno, null));

			String name = t.getText(_name, null);
			String[] paramNames = StatementRules.this.parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = StatementRules.this.parseParamTypes(env, paramNames, t.get(_param, null),
					StatementRules.this.getDefaultParamType(env));
			OType returnType = StatementRules.this.parseType(env, t.get(_type, null), env.t(OUntypedType.class));
			OType[] exceptions = StatementRules.this.parseExceptionTypes(env, t.get(_throws, null));

			OCode body = StatementRules.this.parseUntypedCode(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions,
					body);
			StatementRules.this.defineName(env, t, mh);
			return new OEmptyCode(env);
		}
	};

	public OTypeRule EmptyStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return new OEmptyCode(env);
		}
	};

	public OTypeRule ReturnStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = StatementRules.this.getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("return"));
			}
			OCode expr = t.has(_expr) ? StatementRules.this.typeExpr(env, t.get(_expr)) : new OEmptyCode(env);
			expr = StatementRules.this.typeCheck(env, mdecl.getReturnType(), expr);
			return new OReturnCode(env, expr);
		}
	};

	public OTypeRule ThrowStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = StatementRules.this.getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("throw"));
			}
			OCode expr = StatementRules.this.typeCheck(env, env.t(Throwable.class), t.get(_expr));
			return new OReturnCode(env, expr);
		}
	};

	public OTypeRule BreakStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = StatementRules.this.getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("break"));
			}
			if (t.has(_expr)) {
				OCode expr = StatementRules.this.typeExpr(env, t.get(_expr));
				return new OBreakCode(env, t.getText(_label, null), expr);
			}
			return new OBreakCode(env, t.getText(_label, null));
		}
	};

	public OTypeRule ContinueStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = StatementRules.this.getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("continue"));
			}
			if (t.has(_expr)) {
				OCode expr = StatementRules.this.typeExpr(env, t.get(_expr));
				return new OContinueCode(env, t.getText(_label, null), expr);
			}
			return new OContinueCode(env, t.getText(_label, null));

		}
	};

	public OTypeRule LabelStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode body = StatementRules.this.typeStmt(env, t.get(_body));
			return new OLabelBlockCode(t.getText(_label, null), new OEmptyCode(env), body, new OEmptyCode(env));
		}
	};

	public OTypeRule IfStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = StatementRules.this.typeCondition(env, t.get(_cond, null));
			OCode thenCode = StatementRules.this.typeStmt(env, t.get(_then));
			OCode elseCode = StatementRules.this.typeStmt(env, t.get(_else, null));
			return new OIfCode(env, condCode, thenCode, elseCode);
		}
	};

	public OTypeRule ForStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode initCode = StatementRules.this.typeStmt(env, t.get(_init, null));
			OCode condCode = StatementRules.this.typeCondition(env, t.get(_cond, null));
			OCode iterCode = StatementRules.this.typeStmt(env, t.get(_iter, null));
			OCode bodyCode = StatementRules.this.typeStmt(env, t.get(_body, null));
			return new JavaForCode(env, initCode, condCode, iterCode, bodyCode);
		}
	};

	public OTypeRule WhileStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode initCode = new OEmptyCode(env);
			OCode condCode = StatementRules.this.typeCondition(env, t.get(_cond, null));
			OCode iterCode = new OEmptyCode(env);
			OCode bodyCode = StatementRules.this.typeStmt(env, t.get(_body, null));
			return new JavaForCode(env, initCode, condCode, iterCode, bodyCode);
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
			OCode condCode = StatementRules.this.typeExpr(env, t.get(_cond));
			Tree<?> caseNode = t.get(_body);
			OCode[] caseCodes = new OCode[caseNode.size()];
			int i = 0;
			for (Tree<?> sub : caseNode) {
				OCode cond = StatementRules.this.typeExpr(env, sub.get(_cond, null));
				OCode clause = StatementRules.this.typeStmt(env, sub.get(_body, null));
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
				caseCode = new OMultiCode(caseCodes);
			}
			return new JavaSwitchCode(env, condCode, caseCode);
		}
	};

	public OTypeRule TryStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return StatementRules.this.typeTry(env, t);
		}
	};

	public OTypeRule JavaTryWithResource = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return StatementRules.this.typeTry(env, t);
		}
	};

	private OCode typeTry(OEnv env, Tree<?> t) {
		/* With Resources */
		OCode withClause = null;
		// if (t.has(_with)) {
		// Tree<?> resourceNode = t.get(_with, null);
		// OCode[] resourceCodes = null;
		// if (resourceNode != null) {
		// resourceCodes = new OCode[resourceNode.size()];
		// int i = 0;
		// for (Tree<?> sub : resourceNode) {
		// OCode vardecl = typeStmt(env, sub);
		// OType type = vardecl.getType();
		// OCode thisCode = new ThisCode(type);
		// OCode[] params = { thisCode };
		// OCode closeCode = MethodCallSite.lookup(env, t, "close", thisCode);
		// resourceCodes[i] = new WithResourceCode(env, vardecl, closeCode);
		// i++;
		// }
		// nodes[0] = new MultiCode(resourceCodes);
		// }
		// }

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

		return new OTryCode(env, tryCode, catchCodes, finallyCode);
	}

	public OTypeRule AssertStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode cond = StatementRules.this.typeCondition(env, t.get(_cond));
			OCode msg = null;
			if (t.has(_value)) {
				msg = StatementRules.this.typeCheck(env, env.t(String.class), t.get(_value));
			} else {
				msg = env.v(ODebug.assertMessage(env, t.get(_cond)));
			}
			return new OMethodCode(env,
					OTypeUtils.loadMethod(StatementRules.class, "assertTest", boolean.class, String.class), cond, msg);
		}
	};

	// APIs
	// -----------------------------------------------------------------------

	private static int tested = 0;
	private static int succ = 0;

	public static final void assertTest(boolean cond, String msg) {
		tested++;
		assert (cond) : msg;
		succ++;
	}

	public static final void exitTest() {
		if (tested == 0 || tested > succ) {
			System.exit(1);
		}
		System.exit(0);
	}

	public static void initTest() {
		tested = 0;
		succ = 0;
	}

	public static void checkTest() {
		assert (tested > 0) : "untested";
	}

}
