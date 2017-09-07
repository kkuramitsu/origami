package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.FuncCode;
import blue.origami.transpiler.type.Ty;
import blue.origami.transpiler.type.VarDomain;

public class FuncExpr extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String[] paramNames = this.parseParamNames(env, t.get(_param, null));
		VarDomain dom = new VarDomain(paramNames);
		Ty[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null), dom);
		Ty returnType = this.parseReturnType(env, t.get(_type, null), dom);
		Code body = env.parseCode(env, t.get(_body, null));
		return new FuncCode(paramNames, paramTypes, returnType, dom, body);
	}

}