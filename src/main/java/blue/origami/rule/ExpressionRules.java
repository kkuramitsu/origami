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

import blue.origami.asm.OAnno;
import blue.origami.code.OAndCode;
import blue.origami.code.OArrayCode;
import blue.origami.code.OCastCode;
import blue.origami.code.OCode;
import blue.origami.code.ODefaultValueCode;
import blue.origami.code.OEmptyCode;
import blue.origami.code.OErrorCode;
import blue.origami.code.OGetSizeCode;
import blue.origami.code.OIfCode;
import blue.origami.code.OInstanceOfCode;
import blue.origami.code.OOrCode;
import blue.origami.code.OTypeCode;
import blue.origami.code.OValueCode;
import blue.origami.code.OWarningCode;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OClassDecl;
import blue.origami.lang.OEnv;
import blue.origami.lang.OGlobalVariable;
import blue.origami.lang.OLocalVariable;
import blue.origami.lang.ONameEntity;
import blue.origami.lang.OVariable;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.nez.ast.Tree;
import blue.origami.rule.java.JavaPostOpCode;
import blue.origami.rule.java.JavaThisCode;
import blue.origami.rule.java.PreOpCode;
import blue.origami.util.OArrayUtils;
import blue.origami.util.OTypeRule;

public class ExpressionRules implements OImportable, SyntaxAnalysis, OArrayUtils {

	public OTypeRule NameExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.toText();
			ONameEntity nameDecl = env.get(name, ONameEntity.class, (e, c) -> e.isName(env) ? e : null);
			if (nameDecl == null) {
				throw new OErrorCode(env, t, OFmt.undefined_name__YY0, name);
			}
			return nameDecl.nameCode(env, name);
		}
	};

	class VarRule extends TypeRule {
		final boolean isReadOnly;

		VarRule(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getText(_name, "");
			OAnno anno = ExpressionRules.this.parseAnno(env, "public,static", t.get(_anno, null));
			anno.setReadOnly(this.isReadOnly);
			OCode right = null;
			OType type = env.t(Object.class);
			if (t.has(_expr)) {
				right = ExpressionRules.this.typeExpr(env, t.get(_expr));
				if (t.has(_type)) {
					type = ExpressionRules.this.parseType(env, t.get(_type, null), type);
				} else {
					type = right.valueType();
				}
				type = ExpressionRules.this.parseTypeArity(env, type, t);
				right = ExpressionRules.this.typeCheck(env, type, right);
				// ODebug.trace("right %s", right);
			} else {
				type = ExpressionRules.this.parseType(env, t.get(_type, null), type);
				type = ExpressionRules.this.parseTypeArity(env, type, t);
				right = new ODefaultValueCode(type);
			}

			if (ExpressionRules.this.isTopLevel(env)) {
				OVariable var = new OGlobalVariable(env, anno, name, type, right);
				ExpressionRules.this.defineName(env, t, var);
				return new OEmptyCode(env);
			}
			OVariable var = new OLocalVariable(this.isReadOnly, name, type);
			ExpressionRules.this.defineName(env, t, var);
			return var.defineCode(env, right);
		}
	}

	public OTypeRule VarDecl = new VarRule(false);
	public OTypeRule LetDecl = new VarRule(true);

	public OTypeRule ThisExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl defined = ExpressionRules.this.getClassContext(env);
			if (defined != null) {
				return new JavaThisCode(defined.getType());
			}
			throw new OErrorCode(env, t, "can't use 'this' in global");
		}
	};

	public OTypeRule AssignExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = ExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = ExpressionRules.this.typeExpr(env, t.get(_right));
			return left.newAssignCode(env, ExpressionRules.this.typeCheck(env, left.getType(), right));
		}
	};

	public OTypeRule AssignStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = ExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = ExpressionRules.this.typeExpr(env, t.get(_right));
			OCode code = left.newAssignCode(env, ExpressionRules.this.typeCheck(env, left.getType(), right));
			return code.asType(env, void.class);
		}
	};

	public OTypeRule CastExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = ExpressionRules.this.typeExpr(env, t.get(_expr));
			if (t.has(_type)) {
				OType type = ExpressionRules.this.parseType(env, t.get(_type));
				expr = expr.asType(env, type);
				if (expr instanceof OCastCode) {
					OCastCode node = (OCastCode) expr;
					if (node.isStupidCast()) {
						throw node.newErrorCode(env);
					}
					node.setMatchCost(0);
				}
				return expr;
			}
			return new OCastCode(env.t(OUntypedType.class), 0, expr);
		}
	};

	public OTypeRule InstanceOfExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = ExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = ExpressionRules.this.typeExpr(env, t.get(_right));
			OType ty = (right instanceof OTypeCode) ? ((OTypeCode) right).getTypeValue() : right.getType();
			OType lty = left.getType();
			if (ty.isPrimitive()) {
				if (lty.isPrimitive()) {
					return env.v(ty.eq(lty));
				}
			}
			if (!lty.isUntyped()) {
				ty = ty.boxType();
				if (ty.isAssignableFrom(lty)) {
					return new OWarningCode(env.v(true), OFmt.unnecessary_expression);
				}
				if (!lty.isAssignableFrom(ty)) {
					return new OWarningCode(env.v(false), OFmt.stupid_expression);
				}
			}
			return new OInstanceOfCode(left, ty);
		}
	};

	public OTypeRule ApplyExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] params = ExpressionRules.this.typeParams(env, t);
			OCode recv = ExpressionRules.this.typeExpr(env, t.get(_recv));
			return recv.newApplyCode(env, params);
		}
	};

	public OTypeRule MethodExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] params = ExpressionRules.this.typeParams(env, t);
			String name = t.getText(_name, "");
			OCode recv = ExpressionRules.this.typeExpr(env, t.get(_recv));
			return recv.newMethodCode(env, name, params);
		}
	};

	public OTypeRule GetExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getText(_name, "");
			OCode recv = ExpressionRules.this.typeExpr(env, t.get(_recv));
			return recv.newGetterCode(env, name);
		}
	};

	public OTypeRule SizeOfExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = ExpressionRules.this.typeExpr(env, t.get(_expr));
			OType recvType = expr.getType();
			if (recvType.isArray()) {
				return new OGetSizeCode(env, null, expr);
			}
			String name = recvType.rename("size");
			return expr.newMethodCode(env, name);
		}
	};

	public OTypeRule IndexExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode recv = ExpressionRules.this.typeExpr(env, t.get(_recv));
			OCode[] params = ExpressionRules.this.typeParams(env, t);
			return recv.newMethodCode(env, "get", params);
		}
	};

	public OTypeRule NewArrayExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType type = ExpressionRules.this.parseType(env, t.get(_type), null);
			OCode[] expr = null;
			if (t.has(_expr)) {
				Tree<?> exprs = t.get(_expr);
				expr = new OCode[exprs.size()];
				for (int i = 0; i < expr.length; i++) {
					expr[i] = ExpressionRules.this.typeExpr(env, exprs.get(i));
				}
			}

			return new OArrayCode(type, expr);
		}
	};

	/* IfExpr */

	public OTypeRule IfExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = ExpressionRules.this.typeCondition(env, t.get(_cond));
			OCode thenCode = ExpressionRules.this.typeExprOrErrorCode(env, t.get(_then));
			OCode elseCode = t.has(_else) ? ExpressionRules.this.typeExprOrErrorCode(env, t.get(_else))
					: new OEmptyCode(env);
			return new OIfCode(env, condCode, thenCode, elseCode);
		}
	};

	/* AndExpr */

	public OTypeRule AndExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = ExpressionRules.this.typeCondition(env, t.get(_left));
			OCode right = ExpressionRules.this.typeCondition(env, t.get(_left));
			return new OAndCode(env, left, right);
		}
	};

	public OTypeRule OrExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = ExpressionRules.this.typeCondition(env, t.get(_left));
			OCode right = ExpressionRules.this.typeCondition(env, t.get(_left));
			return new OOrCode(env, left, right);
		}
	};

	/* Operator */

	public OTypeRule AddExpr = new Binary("+");
	public OTypeRule SubExpr = new Binary("-");
	public OTypeRule MulExpr = new Binary("*");
	public OTypeRule DivExpr = new Binary("/");
	public OTypeRule ModExpr = new Binary("%");
	public OTypeRule BitwiseOrExpr = new Binary("|");
	public OTypeRule BitwiseXorExpr = new Binary("~");
	public OTypeRule BitwiseAndExpr = new Binary("&");
	public OTypeRule EqExpr = new Binary("==");
	public OTypeRule NeExpr = new Binary("!=");
	public OTypeRule LteExpr = new Binary("<=");
	public OTypeRule GteExpr = new Binary(">=");
	public OTypeRule LtExpr = new Binary("<");
	public OTypeRule GtExpr = new Binary(">");
	public OTypeRule LShiftExpr = new Binary("<<");
	public OTypeRule RShiftExpr = new Binary(">>");
	public OTypeRule LRShiftExpr = new Binary(">>>");

	public OTypeRule PlusExpr = new Unary("+");
	public OTypeRule MinusExpr = new Unary("-");
	public OTypeRule ComplExpr = new Unary("~");
	public OTypeRule NotExpr = new Unary("!");

	// TODO add unary operator TypeRules

	class Binary extends TypeRule {

		final String op;
		final String name;

		public Binary(String name) {
			this.op = name;
			this.name = name;
		}

		public Binary(String op, String name) {
			this.op = op;
			this.name = name;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = ExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = ExpressionRules.this.typeExpr(env, t.get(_right));
			ExpressionRules.this.syncType(env, left, right);
			return left.newBinaryCode(env, this.name, right);
		}
	}

	class Unary extends TypeRule {

		final String op;
		final String name;

		public Unary(String name) {
			this.op = name;
			this.name = name;
		}

		public Unary(String op, String name) {
			this.op = op;
			this.name = name;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode value = ExpressionRules.this.typeExpr(env, t.get(_expr));
			return value.newUnaryCode(env, this.name);
		}
	}

	public OTypeRule AddAssign = new SelfAssign("+");
	public OTypeRule SubAssign = new SelfAssign("-");
	public OTypeRule MulAssign = new SelfAssign("*");
	public OTypeRule DivAssign = new SelfAssign("/");
	public OTypeRule ModAssign = new SelfAssign("%");
	public OTypeRule LShiftAssign = new SelfAssign("<<");
	public OTypeRule RShiftAssign = new SelfAssign(">>");
	public OTypeRule LRShiftAssign = new SelfAssign(">>>");
	public OTypeRule BitwiseAndAssign = new SelfAssign("&");
	public OTypeRule BitwiseOrAssign = new SelfAssign("|");
	public OTypeRule BitwiseXorAssign = new SelfAssign("^");
	// public OTypeRule IncExpr = new PreOpExpr("+");
	// public OTypeRule DecExpr = new PreOpExpr("-");
	// public OTypeRule PostIncExpr = new PostOpExpr("+");
	// public OTypeRule PostDecExpr = new PostOpExpr("-");

	class SelfAssign extends TypeRule {
		String name;
		OCode expr;

		SelfAssign(String name) {
			this.name = name;
		}

		SelfAssign(String name, OCode expr) {
			this.name = name;
			this.expr = expr;
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left;
			if (this.expr == null) {
				Tree<?> l = t.get(_left);
				left = ExpressionRules.this.typeExpr(env, l);
				OCode expr = ExpressionRules.this.typeExpr(env, t.get(_right));
				OCode op = left.newBinaryCode(env, this.name, expr);
				return left.newAssignCode(env, op);
			} else {
				Tree<?> tree = t.get(_expr);
				left = ExpressionRules.this.typeExpr(env, tree);
				OCode op = left.newBinaryCode(env, this.name, this.expr);
				OCode setter = left.newAssignCode(env, op);
				return setter;
			}

		}
	}

	class PreOpExpr extends SelfAssign {

		PreOpExpr(OEnv env, String name) {
			super(name, new OValueCode(1, env.t(int.class)));
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left;
			Tree<?> tree = t.get(_expr);
			left = ExpressionRules.this.typeExpr(env, tree);
			return new PreOpCode(this.name, left.getType(), left, this.expr, env);
		}
	}

	class PostOpExpr extends TypeRule {
		String name;
		OCode expr;

		PostOpExpr(OEnv env, String name) {
			this.name = name;
			this.expr = new OValueCode(1, env.t(int.class));
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			Tree<?> tree = t.get(_expr);
			OCode expr = ExpressionRules.this.typeExpr(env, tree);
			OCode op = expr.newBinaryCode(env, this.name, this.expr);
			OCode setter = expr.newAssignCode(env, op);
			return new JavaPostOpCode(this.name, expr.getType(), expr, setter);
		}
	}

}
