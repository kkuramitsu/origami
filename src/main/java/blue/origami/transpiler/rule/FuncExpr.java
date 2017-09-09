package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.type.Ty;

public class FuncExpr extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String[] paramNames = this.parseParamNames(env, t.get(_param, null));
		Ty[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null));
		Ty returnType = this.parseReturnType(env, null, t.get(_type, null));
		return new FuncCode(paramNames, paramTypes, returnType, t.get(_body, null));
	}

}