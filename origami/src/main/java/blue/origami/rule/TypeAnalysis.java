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

import blue.nez.ast.Symbol;
import blue.nez.ast.Tree;
import blue.origami.lang.OEnv;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.ocode.OCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.TypeValueCode;
import blue.origami.util.OTypeRule;

public interface TypeAnalysis {

	public default OCode typeTree(OEnv env, Tree<?> t) {
		String name = t.getTag().getSymbol();
		OCode node = null;
		try {
			node = env.get(name, OTypeRule.class, (d, c) -> d.typeRule(env, t));
		} catch (ErrorCode e) {
			e.setSourcePosition(t);
			throw e;
		}
		if (node == null) {
			throw new ErrorCode(env, t, OFmt.undefined_syntax__YY0, name);
		}
		node.setSourcePosition(t);
		return node;
	}

	public default OCode typeExpr(OEnv env, Tree<?> t) {
		if (t == null) {
			return new EmptyCode(env);
		}
		return typeTree(env, t);
	}

	public default OCode typeExprOrErrorCode(OEnv env, Tree<?> t) {
		try {
			return typeExpr(env, t);
		} catch (ErrorCode e) {
			if (e.getType() == null) {
				e.refineType(env, env.t(OUntypedType.class));
			}
			return e;
		}
	}

	public default OCode typeStmt(OEnv env, Tree<?> t) {
		if (t == null) {
			return new EmptyCode(env);
		}
		OCode node = typeTree(env, t);
		return node.asType(env, env.t(void.class));
	}

	public default OCode ensureTypedExpr(OEnv env, Tree<?> t) {
		OCode node = typeExpr(env, t);
		if (node.isUntyped()) {
			throw new ErrorCode(env, t, OFmt.implicit_type);
		}
		return node;
	}

	public default void syncType(OEnv env, OCode left, OCode right) {
		if (left.getType() instanceof OUntypedType) {
			left.refineType(env, right.getType());
		}
		if (right.getType() instanceof OUntypedType) {
			right.refineType(env, left.getType());
		}
	}

	public default OCode typeCheck(OEnv env, OType req, Tree<?> t) {
		return typeCheck(env, req, typeExpr(env, t));
	}

	public default OCode typeCheck(OEnv env, OType req, OCode node) {
		return req.accept(env, node, OType.EmptyTypeChecker);
	}

	public default OCode typeCondition(OEnv env, Tree<?> t) {
		if (t == null) {
			return env.v(true);
		}
		// if (node.get(cond).is(_AssignExpr)) {
		// this.reportWarning(node.get(cond), Message.AssignInCondition);
		// node.setTag(_EqExpr);
		// }
		return typeCheck(env, env.t(boolean.class), typeExpr(env, t));
	}

	public default OType parseType(OEnv env, Tree<?> t) {
		OCode node = typeTree(env, t);
		if (node instanceof TypeValueCode) {
			return ((TypeValueCode) node).getTypeValue();
		}
		return node.getType();
	}

	public default OType parseType(OEnv env, Tree<?> t, OType defty) {
		if (t != null) {
			try {
				OCode node = typeTree(env, t);
				if (node instanceof TypeValueCode) {
					return ((TypeValueCode) node).getTypeValue();
				}
				return node.getType();
			} catch (ErrorCode e) {
			}
		}
		return defty;
	}

	public default int treeSize(Tree<?> node) {
		return node == null ? 0 : node.size();
	}

	public default OCode[] typeParams(OEnv env, Tree<?> t) {
		return typeParams(env, t, OSymbols._param);
	}

	public default OCode[] typeParams(OEnv env, Tree<?> t, Symbol param) {
		Tree<?> p = t.get(param, null);
		OCode[] params = new OCode[p.size()];
		for (int i = 0; i < p.size(); i++) {
			params[i] = typeExpr(env, p.get(i));
		}
		return params;
	}

}
