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

import origami.ODebug;
import origami.OEnv;
import origami.asm.OAnno;
import origami.code.ForCode;
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
import origami.code.SwitchCode;
import origami.code.SwitchCode.CaseCode;
import origami.code.TryCatchCode;
import origami.code.TryCatchCode.CatchCode;
import origami.lang.OLocalVariable;
import origami.lang.OMethodDecl;
import origami.lang.OMethodHandle;
import origami.lang.OUntypedMethod;
import origami.nez.ast.Symbol;
import origami.nez.ast.Tree;
import origami.trait.OArrayUtils;
import origami.trait.OImportable;
import origami.trait.OScriptUtils;
import origami.trait.OTypeRule;
import origami.trait.OTypeUtils;
import origami.type.OType;
import origami.type.OUntypedType;

public class StatementRules implements OImportable, OScriptUtils, SyntaxAnalysis, OArrayUtils {

	public OTypeRule MultiExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			if (t.size() == 0) {
				return new ODefaultValueCode(env);
			}
			OCode[] nodes = new OCode[t.size()];
			int last = t.size() - 1;
			for (int i = 0; i < last; i++) {
				nodes[i] = typeStmt(env, t.get(i));
			}
			if (last >= 0) {
				nodes[last] = typeExpr(env, t.get(last));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule BlockExpr = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] nodes = new OCode[t.size()];
			OEnv lenv = env.newEnv();
			int last = t.size() - 1;
			for (int i = 0; i < last; i++) {
				nodes[i] = typeStmt(lenv, t.get(i));
			}
			if (last >= 0) {
				nodes[last] = typeExpr(lenv, t.get(last));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule MultiStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			if (t.size() == 0) {
				return new OEmptyCode(env);
			}
			OCode[] nodes = new OCode[t.size()];
			int last = t.size();
			for (int i = 0; i < last; i++) {
				nodes[i] = typeStmt(env, t.get(i));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule BlockStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] nodes = new OCode[t.size()];
			int last = t.size();
			OEnv lenv = env.newEnv();
			for (int i = 0; i < last; i++) {
				nodes[i] = typeStmt(lenv, t.get(i));
			}
			return new OMultiCode(nodes);
		}
	};

	public OTypeRule Source = MultiExpr;

	private String toPath(Tree<?> t) {
		StringJoiner sj = new StringJoiner(".");
		for (Tree<?> tree : t) {
			sj.add(tree.is(Symbol.unique("NameExpr")) ? tree.toText() : toPath(tree));
		}
		return sj.toString();
	}

	public OTypeRule ImportDecl = new AbstractTypeRule() {
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
				option = parseSubSymbols(t.get(_param, null), option);
			}
			try {
				Class<?> c = Class.forName(path);
				importClass(env, t, c, alias, option);
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

	public OTypeRule FuncDecl = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OConfig2 conf = env.get(OConfig2.class);
			OAnno anno = parseAnno(env, "public,static,final", t.get(_anno, null));

			String name = t.getText(_name, null);
			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), conf.DefaultParamType);
			OType returnType = parseType(env, t.get(_type, null), env.t(OUntypedType.class));
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));

			OCode body = parseUntypedCode(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions, body);
			defineName(env, t, mh);
			return new OEmptyCode(env);
		}
	};

	public OTypeRule DyFuncDecl = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OConfig2 conf = env.get(OConfig2.class);
			OAnno anno = parseAnno(env, "public,static", t.get(_anno, null));

			String name = t.getText(_name, null);
			String[] paramNames = parseParamNames(env, t.get(_param, null));
			OType[] paramTypes = parseParamTypes(env, paramNames, t.get(_param, null), conf.DefaultParamType);
			OType returnType = parseType(env, t.get(_type, null), env.t(OUntypedType.class));
			OType[] exceptions = parseExceptionTypes(env, t.get(_throws, null));

			OCode body = parseUntypedCode(env, t.get(_body, null));
			OMethodHandle mh = OUntypedMethod.newFunc(env, anno, returnType, name, paramNames, paramTypes, exceptions, body);
			defineName(env, t, mh);
			return new OEmptyCode(env);
		}
	};

	public OTypeRule EmptyStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return new OEmptyCode(env);
		}
	};

	public OTypeRule ReturnStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("return"));
			}
			OCode expr = t.has(_expr) ? typeExpr(env, t.get(_expr)) : new OEmptyCode(env);
			expr = typeCheck(env, mdecl.getReturnType(), expr);
			return new OReturnCode(env, expr);
		}
	};

	public OTypeRule ThrowStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("throw"));
			}
			OCode expr = typeCheck(env, env.t(Throwable.class), t.get(_expr));
			return new OReturnCode(env, expr);
		}
	};

	public OTypeRule BreakStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("break"));
			}
			if (t.has(_expr)) {
				OCode expr = typeExpr(env, t.get(_expr));
				return new OBreakCode(env, t.getText(_label, null), expr);
			}
			return new OBreakCode(env, t.getText(_label, null));
		}
	};

	public OTypeRule ContinueStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OMethodDecl mdecl = getFunctionContext(env);
			if (mdecl == null) {
				throw new OErrorCode(env, t, OFmt.YY0_is_not_here, quote("continue"));
			}
			if (t.has(_expr)) {
				OCode expr = typeExpr(env, t.get(_expr));
				return new OContinueCode(env, t.getText(_label, null), expr);
			}
			return new OContinueCode(env, t.getText(_label, null));

		}
	};

	public OTypeRule LabelStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode body = typeStmt(env, t.get(_body));
			return new OLabelBlockCode(t.getText(_label, null), new OEmptyCode(env), body, new OEmptyCode(env));
		}
	};

	public OTypeRule IfStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = typeCondition(env, t.get(_cond, null));
			OCode thenCode = typeStmt(env, t.get(_then));
			OCode elseCode = typeStmt(env, t.get(_else, null));
			return new OIfCode(env, condCode, thenCode, elseCode);
		}
	};

	public OTypeRule ForStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode initCode = typeStmt(env, t.get(_init, null));
			OCode condCode = typeCondition(env, t.get(_cond, null));
			OCode iterCode = typeStmt(env, t.get(_iter, null));
			OCode bodyCode = typeStmt(env, t.get(_body, null));
			return new ForCode(env, initCode, condCode, iterCode, bodyCode);
		}
	};

	public OTypeRule WhileStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode initCode = new OEmptyCode(env);
			OCode condCode = typeCondition(env, t.get(_cond, null));
			OCode iterCode = new OEmptyCode(env);
			OCode bodyCode = typeStmt(env, t.get(_body, null));
			return new ForCode(env, initCode, condCode, iterCode, bodyCode);
		}
	};

	public OTypeRule DoWhileStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = typeCondition(env, t.get(_cond, null));
			// if (t.has(_cond)) {
			// condCode = typeExpr(env, t.get(_cond));
			// condCode = condCode.newUnaryCode(env, "!");
			// condCode = new IfCode(env, condCode, new BreakCode(env));
			// }
			OCode bodyCode = typeStmt(env, t.get(_body));
			bodyCode = new OMultiCode(bodyCode, condCode);
			return new ForCode(null, env.v(true), null, bodyCode);
		}
	};

	public OTypeRule SwitchStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = typeExpr(env, t.get(_cond));
			Tree<?> caseNode = t.get(_body);
			OCode[] caseCodes = new OCode[caseNode.size()];
			int i = 0;
			for (Tree<?> sub : caseNode) {
				OCode cond = typeExpr(env, sub.get(_cond, null));
				OCode clause = typeStmt(env, sub.get(_body, null));
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
			return new SwitchCode(env, condCode, caseCode);
		}
	};

	public OTypeRule TryStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return typeTry(env, t);
		}
	};

	public OTypeRule JavaTryWithResource = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			return typeTry(env, t);
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
		OCode tryCode = typeStmt(env, t.get(_try));

		/* Catch Clause */
		CatchCode[] catchCodes = new CatchCode[t.size(_catch, 0)];
		if (catchCodes.length > 0) {
			Tree<?> catchNode = t.get(_catch);
			int i = 0;
			for (Tree<?> sub : catchNode) {
				String name = sub.getText(_name, "");
				OType type = parseType(env, sub.get(_type, null), env.t(Exception.class));
				OEnv lenv = env.newEnv();
				lenv.add0(sub.get(_name), name, new OLocalVariable(name, type));
				OCode clause = typeStmt(lenv, sub.get(_body, null));
				catchCodes[i] = new CatchCode(type, name, clause);
				i++;
			}
		}
		/* Finally Clause */
		OCode finallyCode = typeStmt(env, t.get(_finally, null));

		return new TryCatchCode(env, tryCode, catchCodes, finallyCode);
	}

	public OTypeRule AssertStmt = new AbstractTypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode cond = typeCondition(env, t.get(_cond));
			OCode msg = null;
			if (t.has(_value)) {
				msg = typeCheck(env, env.t(String.class), t.get(_value));
			} else {
				msg = env.v(ODebug.assertMessage(env, t.get(_cond)));
			}
			return new OMethodCode(env, OTypeUtils.loadMethod(StatementRules.class, "assertTest", boolean.class, String.class), cond, msg);
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
