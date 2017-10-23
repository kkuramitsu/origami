package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.FuncMap;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.FuncTy;
import blue.origami.transpiler.type.Ty;

public class ArrowDecl extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(TEnv env, AST t) {
		String name = t.getStringAt(_name, null);
		Ty fromTy = this.parseReturnType(env, name, t.get(_from));
		Ty returnTy = this.parseReturnType(env, name, t.get(_to));
		Transpiler tr = env.getTranspiler();
		if (!tr.isShellMode()) {
			// CodeMap tf = env.findTypeMap(env, fromTy, returnTy);
			// if (tf != null) {
			// env.reportWarning(t.get(_name), TFmt.redefined_name__YY1, name);
			// }
		}
		String key = FuncTy.mapKey(fromTy, returnTy);
		FuncMap tf = new FuncMap(fromTy, returnTy, t.get(_name), t.get(_body));
		env.add(key, tf);
		return new DoneCode();
	}

}