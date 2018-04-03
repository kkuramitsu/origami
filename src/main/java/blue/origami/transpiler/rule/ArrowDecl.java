package blue.origami.transpiler.rule;

import blue.origami.transpiler.Env;
import blue.origami.transpiler.FuncMap;
import blue.origami.transpiler.Transpiler;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;
import blue.origami.transpiler.type.Ty;
import origami.nez2.ParseTree;

public class ArrowDecl extends SyntaxRule implements ParseRule {

	@Override
	public Code apply(Env env, ParseTree t) {
		String name = t.get(_name).asString();
		Ty fromTy = this.parseReturnType(env, name, t.get(_from));
		Ty returnTy = this.parseReturnType(env, name, t.get(_to));
		Transpiler tr = env.getTranspiler();
		if (!tr.isShellMode()) {
			// CodeMap tf = env.findTypeMap(env, fromTy, returnTy);
			// if (tf != null) {
			// env.reportWarning(t.get(_name), TFmt.redefined_name__YY1, name);
			// }
		}
		String key = Ty.mapKey2(fromTy, returnTy);
		FuncMap tf = new FuncMap(env, fromTy, returnTy, env.s(t.get(_name)), t.get(_body));
		env.add(key, tf);
		return new DoneCode();
	}

}