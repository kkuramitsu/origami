package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.TFmt;
import blue.origami.transpiler.TFunction;
import blue.origami.transpiler.TLog;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DeclCode;
import blue.origami.transpiler.type.Ty;

public class FuncDecl extends SyntaxRule implements ParseRule {

	boolean isPublic = false;

	@Override
	public Code apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, null);
		String[] paramNames = this.parseParamNames(env, t.get(_param, null));
		Ty[] paramTypes = this.parseParamTypes(env, paramNames, t.get(_param, null));
		Ty returnType = this.parseReturnType(env, name, t.get(_type, null));
		Transpiler tr = env.getTranspiler();
		if (this.isPublic && !tr.isShellMode()) {
			TFunction tf = env.get(name, TFunction.class);
			if (tf != null) {
				TLog log = this.reportWarning(null, t.get(_name), TFmt.redefined_name__YY0, name);
				env.reportLog(log);
			}
		}
		TFunction tf = new TFunction(this.isPublic, name, returnType, paramNames, paramTypes, t.get(_body, null));
		tr.addFunction(env, name, tf);
		return new DeclCode();
	}

}
