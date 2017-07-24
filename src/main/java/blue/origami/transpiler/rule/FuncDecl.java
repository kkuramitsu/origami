package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFunction;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDeclCode;

public class FuncDecl extends SyntaxRule implements TTypeRule {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, null);
		String[] paramNames = this.parseParamNames(env, t.get(_param, null));
		TType[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null), null);
		TType returnType = env.parseType(env, t.get(_type, null), TType.tUntyped);
		if (returnType.isUntyped()) {
			returnType = TType.tVar("return");
		}
		TFunction tf = new TFunction(name, returnType, paramNames, paramTypes, t.get(_body, null));
		env.add(name, tf);
		return new TDeclCode();
	}

}
