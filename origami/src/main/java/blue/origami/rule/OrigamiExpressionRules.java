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

import blue.nez.ast.Tree;
import blue.origami.asm.OAnno;
import blue.origami.ffi.OImportable;
import blue.origami.lang.OClassDecl;
import blue.origami.lang.OEnv;
import blue.origami.lang.OGlobalVariable;
import blue.origami.lang.OLocalVariable;
import blue.origami.lang.ONameEntity;
import blue.origami.lang.OVariable;
import blue.origami.lang.type.OType;
import blue.origami.lang.type.OUntypedType;
import blue.origami.ocode.AndCode;
import blue.origami.ocode.ArrayCode;
import blue.origami.ocode.CastCode;
import blue.origami.ocode.OCode;
import blue.origami.ocode.DefaultValueCode;
import blue.origami.ocode.EmptyCode;
import blue.origami.ocode.ErrorCode;
import blue.origami.ocode.GetSizeCode;
import blue.origami.ocode.IfCode;
import blue.origami.ocode.InstanceOfCode;
import blue.origami.ocode.OrCode;
import blue.origami.ocode.TypeValueCode;
import blue.origami.ocode.ValueCode;
import blue.origami.ocode.WarningCode;
import blue.origami.rule.java.JavaPostOpCode;
import blue.origami.rule.java.JavaThisCode;
import blue.origami.rule.java.PreOpCode;
import blue.origami.util.OArrayUtils;
import blue.origami.util.OTypeRule;

public class OrigamiExpressionRules implements OImportable, SyntaxAnalysis, OArrayUtils {

	public OTypeRule NameExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getString();
			ONameEntity nameDecl = env.get(name, ONameEntity.class, (e, c) -> e.isName(env) ? e : null);
			if (nameDecl == null) {
				throw new ErrorCode(env, t, OFmt.undefined_name__YY0, name);
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
			String name = t.getStringAt(_name, "");
			OAnno anno = OrigamiExpressionRules.this.parseAnno(env, "public,static", t.get(_anno, null));
			anno.setReadOnly(this.isReadOnly);
			OCode right = null;
			OType type = env.t(Object.class);
			if (t.has(_expr)) {
				right = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
				if (t.has(_type)) {
					type = OrigamiExpressionRules.this.parseType(env, t.get(_type, null), type);
				} else {
					type = right.valueType();
				}
				type = OrigamiExpressionRules.this.parseTypeArity(env, type, t);
				right = OrigamiExpressionRules.this.typeCheck(env, type, right);
				// ODebug.trace("right %s", right);
			} else {
				type = OrigamiExpressionRules.this.parseType(env, t.get(_type, null), type);
				type = OrigamiExpressionRules.this.parseTypeArity(env, type, t);
				right = new DefaultValueCode(type);
			}

			if (OrigamiExpressionRules.this.isTopLevel(env)) {
				OVariable var = new OGlobalVariable(env, anno, name, type, right);
				OrigamiExpressionRules.this.defineName(env, t, var);
				return new EmptyCode(env);
			}
			OVariable var = new OLocalVariable(this.isReadOnly, name, type);
			OrigamiExpressionRules.this.defineName(env, t, var);
			return var.defineCode(env, right);
		}
	}

	public OTypeRule VarDecl = new VarRule(false);
	public OTypeRule LetDecl = new VarRule(true);

	public OTypeRule ThisExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OClassDecl defined = OrigamiExpressionRules.this.getClassContext(env);
			if (defined != null) {
				return new JavaThisCode(defined.getType());
			}
			throw new ErrorCode(env, t, "can't use 'this' in global");
		}
	};

	public OTypeRule AssignExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = OrigamiExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = OrigamiExpressionRules.this.typeExpr(env, t.get(_right));
			return left.newAssignCode(env, OrigamiExpressionRules.this.typeCheck(env, left.getType(), right));
		}
	};

	public OTypeRule AssignStmt = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = OrigamiExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = OrigamiExpressionRules.this.typeExpr(env, t.get(_right));
			OCode code = left.newAssignCode(env, OrigamiExpressionRules.this.typeCheck(env, left.getType(), right));
			return code.asType(env, void.class);
		}
	};

	public OTypeRule CastExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
			if (t.has(_type)) {
				OType type = OrigamiExpressionRules.this.parseType(env, t.get(_type));
				expr = expr.asType(env, type);
				if (expr instanceof CastCode) {
					CastCode node = (CastCode) expr;
					if (node.isStupidCast()) {
						throw node.newErrorCode(env);
					}
					node.setMatchCost(0);
				}
				return expr;
			}
			return new CastCode(env.t(OUntypedType.class), 0, expr);
		}
	};

	public OTypeRule InstanceOfExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = OrigamiExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = OrigamiExpressionRules.this.typeExpr(env, t.get(_right));
			OType ty = (right instanceof TypeValueCode) ? ((TypeValueCode) right).getTypeValue() : right.getType();
			OType lty = left.getType();
			if (ty.isPrimitive()) {
				if (lty.isPrimitive()) {
					return env.v(ty.eq(lty));
				}
			}
			if (!lty.isUntyped()) {
				ty = ty.boxType();
				if (ty.isAssignableFrom(lty)) {
					return new WarningCode(env.v(true), OFmt.unnecessary_expression);
				}
				if (!lty.isAssignableFrom(ty)) {
					return new WarningCode(env.v(false), OFmt.stupid_expression);
				}
			}
			return new InstanceOfCode(left, ty);
		}
	};

	public OTypeRule ApplyExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] params = OrigamiExpressionRules.this.typeParams(env, t);
			OCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
			return recv.newApplyCode(env, params);
		}
	};

	public OTypeRule MethodExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode[] params = OrigamiExpressionRules.this.typeParams(env, t);
			String name = t.getStringAt(_name, "");
			OCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
			return recv.newMethodCode(env, name, params);
		}
	};

	public OTypeRule GetExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			String name = t.getStringAt(_name, "");
			OCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
			return recv.newGetterCode(env, name);
		}
	};

	public OTypeRule SizeOfExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode expr = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
			OType recvType = expr.getType();
			if (recvType.isArray()) {
				return new GetSizeCode(env, null, expr);
			}
			String name = recvType.rename("size");
			return expr.newMethodCode(env, name);
		}
	};

	public OTypeRule IndexExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
			OCode[] params = OrigamiExpressionRules.this.typeParams(env, t);
			return recv.newMethodCode(env, "get", params);
		}
	};

	public OTypeRule NewArrayExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OType type = OrigamiExpressionRules.this.parseType(env, t.get(_type), null);
			OCode[] expr = null;
			if (t.has(_expr)) {
				Tree<?> exprs = t.get(_expr);
				expr = new OCode[exprs.size()];
				for (int i = 0; i < expr.length; i++) {
					expr[i] = OrigamiExpressionRules.this.typeExpr(env, exprs.get(i));
				}
			}

			return new ArrayCode(type, expr);
		}
	};

	/* IfExpr */

	public OTypeRule IfExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode condCode = OrigamiExpressionRules.this.typeCondition(env, t.get(_cond));
			OCode thenCode = OrigamiExpressionRules.this.typeExprOrErrorCode(env, t.get(_then));
			OCode elseCode = t.has(_else) ? OrigamiExpressionRules.this.typeExprOrErrorCode(env, t.get(_else))
					: new EmptyCode(env);
			return new IfCode(env, condCode, thenCode, elseCode);
		}
	};

	/* AndExpr */

	public OTypeRule AndExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = OrigamiExpressionRules.this.typeCondition(env, t.get(_left));
			OCode right = OrigamiExpressionRules.this.typeCondition(env, t.get(_left));
			return new AndCode(env, left, right);
		}
	};

	public OTypeRule OrExpr = new TypeRule() {
		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left = OrigamiExpressionRules.this.typeCondition(env, t.get(_left));
			OCode right = OrigamiExpressionRules.this.typeCondition(env, t.get(_left));
			return new OrCode(env, left, right);
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
			OCode left = OrigamiExpressionRules.this.typeExpr(env, t.get(_left));
			OCode right = OrigamiExpressionRules.this.typeExpr(env, t.get(_right));
			OrigamiExpressionRules.this.syncType(env, left, right);
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
			OCode value = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
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
				left = OrigamiExpressionRules.this.typeExpr(env, l);
				OCode expr = OrigamiExpressionRules.this.typeExpr(env, t.get(_right));
				OCode op = left.newBinaryCode(env, this.name, expr);
				return left.newAssignCode(env, op);
			} else {
				Tree<?> tree = t.get(_expr);
				left = OrigamiExpressionRules.this.typeExpr(env, tree);
				OCode op = left.newBinaryCode(env, this.name, this.expr);
				OCode setter = left.newAssignCode(env, op);
				return setter;
			}

		}
	}

	class PreOpExpr extends SelfAssign {

		PreOpExpr(OEnv env, String name) {
			super(name, new ValueCode(1, env.t(int.class)));
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			OCode left;
			Tree<?> tree = t.get(_expr);
			left = OrigamiExpressionRules.this.typeExpr(env, tree);
			return new PreOpCode(this.name, left.getType(), left, this.expr, env);
		}
	}

	class PostOpExpr extends TypeRule {
		String name;
		OCode expr;

		PostOpExpr(OEnv env, String name) {
			this.name = name;
			this.expr = new ValueCode(1, env.t(int.class));
		}

		@Override
		public OCode typeRule(OEnv env, Tree<?> t) {
			Tree<?> tree = t.get(_expr);
			OCode expr = OrigamiExpressionRules.this.typeExpr(env, tree);
			OCode op = expr.newBinaryCode(env, this.name, this.expr);
			OCode setter = expr.newAssignCode(env, op);
			return new JavaPostOpCode(this.name, expr.getType(), expr, setter);
		}
	}

}
