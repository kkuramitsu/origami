package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Ty;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncCode;

public class FuncExpr extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		// String name = t.getStringAt(_name, null);
		String[] paramNames = this.parseParamNames(env, t.get(_param, null));
		Ty[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null), null);
		Ty returnType = env.parseType(env, t.get(_type, null), Ty.tUntyped);
		if (returnType.isUntyped()) {
			returnType = Ty.tVar("return");
		}
		Code body = env.parseCode(env, t.get(_body, null));
		return new FuncCode(paramNames, paramTypes, returnType, body);
	}

}