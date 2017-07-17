package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;

public class ApplyExpr implements TTypeRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		TCode[] params = env.typeParams(env, t, _param);
		TCode recv = env.typeExpr(env, t.get(_recv));
		return recv.applyCode(env, params);
	}

	// public TTypeRule ApplyExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// }
	// };
	//
	// public TTypeRule MethodExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode[] params = OrigamiExpressionRules.this.typeParams(env, t);
	// String name = t.getStringAt(_name, "");
	// TCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
	// return recv.newMethodCode(env, name, params);
	// }
	// };
	//
	// public TTypeRule GetExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// String name = t.getStringAt(_name, "");
	// TCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
	// return recv.newGetterCode(env, name);
	// }
	// };
	//
	// public TTypeRule SizeOfExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode expr = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
	// TType recvType = expr.getType();
	// if (recvType.isArray()) {
	// return new GetSizeCode(env, null, expr);
	// }
	// String name = recvType.rename("size");
	// return expr.newMethodCode(env, name);
	// }
	// };
	//
	// public TTypeRule IndexExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode recv = OrigamiExpressionRules.this.typeExpr(env, t.get(_recv));
	// TCode[] params = OrigamiExpressionRules.this.typeParams(env, t);
	// return recv.newMethodCode(env, "get", params);
	// }
	// };
	//
	// public TTypeRule NewArrayExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TType type = OrigamiExpressionRules.this.parseType(env, t.get(_type),
	// null);
	// TCode[] expr = null;
	// if (t.has(_expr)) {
	// Tree<?> exprs = t.get(_expr);
	// expr = new TCode[exprs.size()];
	// for (int i = 0; i < expr.length; i++) {
	// expr[i] = OrigamiExpressionRules.this.typeExpr(env, exprs.get(i));
	// }
	// }
	//
	// return new ArrayCode(type, expr);
	// }
	// };
	//
	// /* IfExpr */
	//
	// public TTypeRule IfExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode condCode = OrigamiExpressionRules.this.typeCondition(env,
	// t.get(_cond));
	// TCode thenCode = OrigamiExpressionRules.this.typeExprOrErrorCode(env,
	// t.get(_then));
	// TCode elseCode = t.has(_else) ?
	// OrigamiExpressionRules.this.typeExprOrErrorCode(env, t.get(_else))
	// : new EmptyCode(env);
	// return new IfCode(env, condCode, thenCode, elseCode);
	// }
	// };
	//
	// /* AndExpr */
	//
	// public TTypeRule AndExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode left = OrigamiExpressionRules.this.typeCondition(env,
	// t.get(_left));
	// TCode right = OrigamiExpressionRules.this.typeCondition(env,
	// t.get(_left));
	// return new AndCode(env, left, right);
	// }
	// };
	//
	// public TTypeRule OrExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode left = OrigamiExpressionRules.this.typeCondition(env,
	// t.get(_left));
	// TCode right = OrigamiExpressionRules.this.typeCondition(env,
	// t.get(_left));
	// return new OrCode(env, left, right);
	// }
	// };
	//
	// public TTypeRule NameExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// String name = t.getString();
	// ONameEntity nameDecl = env.get(name, ONameEntity.class, (e, c) ->
	// e.isName(env) ? e : null);
	// if (nameDecl == null) {
	// throw new ErrorCode(env, t, OFmt.undefined_name__YY0, name);
	// }
	// return nameDecl.nameCode(env, name);
	// }
	// };
	//
	// class VarRule extends TypeRule {
	// final boolean isReadOnly;
	//
	// VarRule(boolean isReadOnly) {
	// this.isReadOnly = isReadOnly;
	// }
	//
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// String name = t.getStringAt(_name, "");
	// OAnno anno = OrigamiExpressionRules.this.parseAnno(env, "public,static",
	// t.get(_anno, null));
	// anno.setReadOnly(this.isReadOnly);
	// TCode right = null;
	// TType type = env.t(Object.class);
	// if (t.has(_expr)) {
	// right = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
	// if (t.has(_type)) {
	// type = OrigamiExpressionRules.this.parseType(env, t.get(_type, null),
	// type);
	// } else {
	// type = right.valueType();
	// }
	// type = OrigamiExpressionRules.this.parseTypeArity(env, type, t);
	// right = OrigamiExpressionRules.this.typeCheck(env, type, right);
	// // ODebug.trace("right %s", right);
	// } else {
	// type = OrigamiExpressionRules.this.parseType(env, t.get(_type, null),
	// type);
	// type = OrigamiExpressionRules.this.parseTypeArity(env, type, t);
	// right = new DefaultValueCode(type);
	// }
	//
	// if (OrigamiExpressionRules.this.isTopLevel(env)) {
	// OVariable var = new OGlobalVariable(env, anno, name, type, right);
	// OrigamiExpressionRules.this.defineName(env, t, var);
	// return new EmptyCode(env);
	// }
	// OVariable var = new OLocalVariable(this.isReadOnly, name, type);
	// OrigamiExpressionRules.this.defineName(env, t, var);
	// return var.defineCode(env, right);
	// }
	// }
	//
	// public TTypeRule CastExpr = new TypeRule() {
	// @Override
	// public TCode typeRule(TEnv env, Tree<?> t) {
	// TCode expr = OrigamiExpressionRules.this.typeExpr(env, t.get(_expr));
	// if (t.has(_type)) {
	// TType type = OrigamiExpressionRules.this.parseType(env, t.get(_type));
	// expr = expr.asType(env, type);
	// if (expr instanceof CastCode) {
	// CastCode node = (CastCode) expr;
	// if (node.isStupidCast()) {
	// throw node.newErrorCode(env);
	// }
	// node.setMatchCost(0);
	// }
	// return expr;
	// }
	// return new CastCode(env.t(OUntypedType.class), 0, expr);
	// }
	// };

}
