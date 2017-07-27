package blue.origami.transpiler.rule;

import blue.origami.nez.ast.Tree;
import blue.origami.rule.OSymbols;
import blue.origami.transpiler.TEnv;
import blue.origami.transpiler.code.TCode;
import blue.origami.transpiler.code.TDeclCode;
import blue.origami.transpiler.code.TMultiCode;

public class ExampleDecl implements TTypeRule, OSymbols {

	@Override
	public TCode apply(TEnv env, Tree<?> t) {
		String name = t.getStringAt(_name, "main");
		TCode body = env.parseCode(env, t.get(_body));
		if (body instanceof TMultiCode) {
			for (TCode sub : body) {
				env.addExample(name, sub);
			}
		} else {
			env.addExample(name, body);
		}
		return new TDeclCode();
	}

}
