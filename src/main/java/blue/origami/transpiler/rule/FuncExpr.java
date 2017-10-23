package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.type.Ty;

public class FuncExpr extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(Env env, AST t) {
		AST[] paramNames = this.parseParamNames(env, t.get(_param));
		Ty[] paramTypes = this.parseParamTypes(env, t.get(_param));
		Ty returnType = this.parseReturnType(env, null, t.get(_type));
		return new FuncCode(paramNames, paramTypes, returnType, t.get(_body));
	}

}