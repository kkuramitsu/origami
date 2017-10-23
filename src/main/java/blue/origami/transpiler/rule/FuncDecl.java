package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.FuncMap;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.Ty;

public class FuncDecl extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		boolean isPublic = t.has(_public);
		String name = t.getStringAt(_name, null);
		AST[] paramNames = this.parseParamNames(env, t.get(_param));
		Ty[] paramTypes = this.parseParamTypes(env, t.get(_param));
		Ty returnType = this.parseReturnType(env, name, t.get(_type));
		Transpiler tr = env.getTranspiler();
		if (isPublic && !tr.isShellMode()) {
			FuncMap tf = env.get(name, FuncMap.class);
			if (tf != null) {
				env.reportWarning(t.get(_name), TFmt.redefined_name__YY1, name);
			}
		}
		FuncMap tf = new FuncMap(isPublic, t.get(_name), returnType, paramNames, paramTypes, t.get(_body));
		tr.addFunction(env, name, tf);
		return new DoneCode();
	}

}
