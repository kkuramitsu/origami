package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFunction;
import blue.origami.transpiler.TType;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDeclCode;
import blue.origami.util.ODebug;

public class FuncDecl extends SyntaxRule implements TTypeRule {

	boolean isPublic = true;

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, null);
		String[] paramNames = this.parseParamNames(env, t.get(_param, null));
		TType[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null), null);
		TType returnType = env.parseType(env, t.get(_type, null), TType.tUntyped);
		if (returnType.isUntyped()) {
			returnType = TType.tVar("return");
		}
		if (this.isPublic) {
			TFunction tf = env.get(name, TFunction.class);
			if (tf != null) {
				ODebug.trace("duplicated name %s", name);
			}
		}
		TFunction tf = new TFunction(this.isPublic, name, returnType, paramNames, paramTypes, t.get(_body, null));
		env.addFunction(name, tf);
		return new TDeclCode();
	}

}
