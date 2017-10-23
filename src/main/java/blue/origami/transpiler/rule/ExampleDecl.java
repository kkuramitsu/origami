package blue.origami.transpiler.rule;

import blue.origami.transpiler.AST;
import blue.origami.transpiler.Env;
import blue.origami.transpiler.code.Code;
import blue.origami.transpiler.code.DoneCode;

public class ExampleDecl implements ParseRule, Symbols {

	@Override
	public Code apply(Env env, AST t) {
		String name = t.getStringAt(_name, "main");
		env.getTranspiler().addExample(name, t.get(_body));
		return new DoneCode();
	}

}
